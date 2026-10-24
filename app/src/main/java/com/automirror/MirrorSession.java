package com.automirror;

import androidx.car.app.Screen;
import androidx.car.app.Session;
import android.content.Intent;

/**
 * Sesión del app de coche: única pantalla de espejo.
 */
public class MirrorSession extends Session {

    @Override
    public Screen onCreateScreen(Intent intent) {
        return new MirrorScreen(getCarContext());
    }
}
