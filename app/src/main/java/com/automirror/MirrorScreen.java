package com.automirror;

import androidx.car.app.AppManager;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.SurfaceCallback;
import androidx.car.app.SurfaceContainer;
import androidx.car.app.navigation.model.MapWithContentTemplate;

/**
 * Pantalla del coche: template vacío de mapa y registro del SurfaceCallback.
 * Cuando el host entrega la Surface, MirrorService redirige la captura del teléfono hacia ella.
 */
public class MirrorScreen extends Screen {

    private boolean callbackRegistered;

    public MirrorScreen(CarContext context) {
        super(context);
    }

    @Override
    public androidx.car.app.model.Template onGetTemplate() {
        registerSurfaceCallback();
        return new MapWithContentTemplate.Builder().build();
    }

    private void registerSurfaceCallback() {
        if (callbackRegistered) {
            return;
        }
        callbackRegistered = true;
        SurfaceCallback callback = new SurfaceCallback() {
            @Override
            public void onSurfaceAvailable(SurfaceContainer container) {
                MirrorService.pushCarSurface(
                        container.getSurface(),
                        container.getWidth(),
                        container.getHeight(),
                        container.getDpi());
            }

            @Override
            public void onSurfaceDestroyed(SurfaceContainer container) {
                MirrorService.pushCarSurface(null, 0, 0, 0);
            }
        };
        getCarContext().getCarService(AppManager.class).setSurfaceCallback(callback);
    }
}
