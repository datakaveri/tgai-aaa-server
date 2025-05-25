package org.cdpg.dx.keyclock.factory;

import io.vertx.core.json.JsonObject;
import org.cdpg.dx.keyclock.service.KeycloakUserService;
import org.cdpg.dx.keyclock.service.KeycloakUserServiceImpl;

public class KeycloakServiceFactory {
    public static KeycloakUserService create(JsonObject config) {
        return new KeycloakUserServiceImpl(config);
    }
}
