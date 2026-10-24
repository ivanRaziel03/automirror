package com.automirror.car;

import androidx.car.app.CarAppService;
import androidx.car.app.Session;
import androidx.car.app.SessionInfo;

import com.automirror.MirrorSession;

/** Servicio bindings del host Android Auto ( nhờ. esta es la clase con el filtro androidx.car.app.CarAppService). */
public class MirrorCarAppService extends CarAppService {

    @Override
    public Session createSession(SessionInfo sessionInfo) {
        return new MirrorSession();
    }
}
