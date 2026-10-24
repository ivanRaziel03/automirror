package com.automirror;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.view.Display;
import android.view.Surface;
import android.graphics.SurfaceTexture;

/**
 * Servicio en primer plano: mantiene la captura de la pantalla del teléfono
 * y redirige los frames a la Surface del coche cuando el host Android Auto la entrega.
 */
public class MirrorService extends Service {

    private static final String CHANNEL_ID = "automirror";
    private static final long POLL_MS = 250;

    /** Estado entregado por MirrorScreen (hilo del binder hacia el app host). */
    private static volatile Surface pendingSurface;
    private static volatile int pendingWidth, pendingHeight, pendingDpi;

    private MediaProjection projection;
    private MediaProjectionManager projectionManager;
    private VirtualDisplay virtualDisplay;
    private SurfaceTexture placeholderTexture;
    private Surface placeholderSurface;
    private Surface appliedSurface;
    private int baseWidth, baseHeight, baseDpi;
    private PowerManager.WakeLock wakeLock;
    private final Handler handler = new Handler(Looper.getMainLooper());

    /** Poller: aplica cambios de surface pendientes en el hilo principal. */
    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            handler.postDelayed(this, POLL_MS);
            applyPendingSurface();
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int onStartCommand(Intent intent, int flags, int startId) {
        startAsForeground();

        if (intent == null || !intent.hasExtra("code")) {
            stopSelf();
            return START_NOT_STICKY;
        }
        int code = intent.getIntExtra("code", -1);
        Intent data = intent.getParcelableExtra("data");

        projectionManager = getSystemService(MediaProjectionManager.class);
        Display display = ((DisplayManager) getSystemService(Context.DISPLAY_SERVICE))
                .getDisplay(Display.DEFAULT_DISPLAY);
        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        baseDpi = dm.densityDpi;
        android.graphics.Point size = new android.graphics.Point();
        display.getRealSize(size);
        baseWidth = size.x;
        baseHeight = size.y;

        PowerManager powerManager = getSystemService(PowerManager.class);
        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
                "AutoMirror::keepScreenForMirror");
        wakeLock.acquire();

        projection = projectionManager.getMediaProjection(code, data);
        if (projection == null) {
            stopSelf();
            return START_NOT_STICKY;
        }
        projection.registerCallback(new MediaProjection.Callback() {
            @Override
            public void onStop() {
                handler.post(() -> {
                    teardown();
                    stopSelf();
                });
            }
        }, handler);

        // Display fantasma: la Surface real del coche lo reemplazará en cuanto llegue.
        placeholderTexture = new SurfaceTexture(0);
        placeholderTexture.setDefaultBufferSize(baseWidth, baseHeight);
        placeholderSurface = new Surface(placeholderTexture);

        virtualDisplay = projection.createVirtualDisplay(
                "automirror", baseWidth, baseHeight, baseDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                placeholderSurface, null, handler);

        handler.post(pollRunnable);
        return START_STICKY;
    }

    /**
     * Llamado desde cualquier hilo por MirrorScreen cuando el host del coche
     * entrega (surface != null) o retira (surface == null) la pantalla del coche.
     */
    public static void pushCarSurface(Surface surface, int width, int height, int dpi) {
        pendingSurface = surface;
        pendingWidth = width;
        pendingHeight = height;
        pendingDpi = dpi;
    }

    private void applyPendingSurface() {
        if (virtualDisplay == null) {
            return;
        }
        Surface target = pendingSurface;
        if (target == appliedSurface) {
            return;
        }
        if (target != null) {
            if (pendingWidth > 0 && pendingHeight > 0) {
                virtualDisplay.setSurface(null);
                virtualDisplay.resize(pendingWidth, pendingHeight, pendingDpi);
            }
            virtualDisplay.setSurface(target);
            appliedSurface = target;
        } else {
            virtualDisplay.setSurface(placeholderSurface);
            appliedSurface = placeholderSurface;
        }
    }

    private void startAsForeground() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW);
            nm.createNotificationChannel(channel);
        }
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        builder.setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.status_active))
                .setSmallIcon(R.drawable.icon)
                .setOngoing(true);

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1, builder.build(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(1, builder.build());
        }
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(pollRunnable);
        teardown();
        super.onDestroy();
    }

    private void teardown() {
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
            appliedSurface = null;
        }
        if (projection != null) {
            projection.stop();
            projection = null;
        }
        if (placeholderSurface != null) {
            placeholderSurface.release();
            placeholderSurface = null;
        }
        if (placeholderTexture != null) {
            placeholderTexture.release();
            placeholderTexture = null;
        }
        if (wakeLock != null) {
            if (wakeLock.isHeld()) wakeLock.release();
            wakeLock = null;
        }
        pendingSurface = null;
    }
}
