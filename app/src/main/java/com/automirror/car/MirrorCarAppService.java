package com.automirror.car;

import android.content.pm.ApplicationInfo;

import androidx.car.app.CarAppService;
import androidx.car.app.Session;
import androidx.car.app.SessionInfo;
import androidx.car.app.validation.HostValidator;

import com.automirror.MirrorSession;

/** Servicio que enlaza con el host Android Auto (action androidx.car.app.CarAppService). */
public class MirrorCarAppService extends CarAppService {

    @Override
    public Session onCreateSession(SessionInfo sessionInfo) {
        return new MirrorSession();
    }

    @Override
    public HostValidator createHostValidator() {
        // APKs debug aceptan cualquier host (recomendación oficial del javadoc de la librería).
        if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR;
        }
        return new HostValidator.Builder(getApplicationContext())
                .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
                .build();
    }
}
