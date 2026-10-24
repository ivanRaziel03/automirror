package com.automirror;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Pantalla principal del teléfono.
 * Pide el consentimiento de MediaProjection y arranca el servicio de captura.
 */
public class MainActivity extends Activity {

    private static final int REQUEST_PROJECTION = 42;
    private static final int REQUEST_NOTIF = 43;

    private MediaProjectionManager projectionManager;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        projectionManager = getSystemService(MediaProjectionManager.class);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = 48;
        root.setPadding(pad, (int) (pad * 1.5), pad, pad);

        status = new TextView(this);
        status.setTextSize(17f);
        status.setText(getString(R.string.status_idle));
        root.addView(status);

        Button start = new Button(this);
        start.setText(R.string.btn_start);
        start.setOnClickListener(v -> requestPermissionAndStart());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 32, 0, 0);
        root.addView(start, lp);

        Button stop = new Button(this);
        stop.setText(R.string.btn_stop);
        stop.setOnClickListener(v -> {
            stopService(new Intent(this, MirrorService.class));
            status.setText(R.string.status_idle);
        });
        root.addView(stop, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        setContentView(root);
    }

    private void requestPermissionAndStart() {
        // En Android 13+ se recomienda pedir POST_NOTIFICATIONS antes del servicio en primer plano.
        if (Build.VERSION.SDK_INT >= 33) {
            String np = Manifest.permission.POST_NOTIFICATIONS;
            if (checkSelfPermission(np) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{np}, REQUEST_NOTIF);
                return; // el callback de onRequestPermissionsResult repite el flujo
            }
        }
        startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_PROJECTION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIF) {
            // Continúa aunque las notificaciones se hayan denegado (el servicio sigue funcionando).
            startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_PROJECTION);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PROJECTION && resultCode == RESULT_OK && data != null) {
            Intent svc = new Intent(this, MirrorService.class);
            svc.putExtra("code", resultCode);
            svc.putExtra("data", data);
            startForegroundService(svc);
            status.setText(R.string.status_active);
        }
    }
}
