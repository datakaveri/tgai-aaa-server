package org.cdpg.dx.keyclock.client;

import io.vertx.core.json.JsonObject;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;

public class KeycloakClientProvider {
    private static Keycloak keycloakInstance;

    public static Keycloak getInstance(JsonObject config) {
        if (keycloakInstance == null) {
            keycloakInstance =  KeycloakBuilder.builder()
                    .serverUrl(config.getString("keycloakUrl"))
                    .realm("master") // Auth realm to log in as admin
                    .clientId(config.getString("keycloakAdminClientId"))
                    .username(config.getString("adminUsername"))
                    .password(config.getString("adminPassword"))
                    .grantType("password")
                    .build();
        }
        return keycloakInstance;
    }
}
