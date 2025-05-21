package org.cdpg.dx.keyclock.service;


import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class KeycloakUserService {

    private static final Logger LOGGER = LogManager.getLogger(KeycloakUserService.class);

    private final Keycloak keycloak;
    private final String realm;

//    "keycloakRealm": "myrealm",
//            "keycloakUrl": "http://localhost:8080/",
//            "keycloakAdminClientId": "admin-cli",
//            "keycloakAdminClientSecret": "",
//            "keycloakAdminPoolSize": "10",
//            "keycloakJwtLeeway": 90,
//            "keycloakCertUrl": "http://localhost:8080/realms/myrealm/protocol/openid-connect/certs",
//            "iss": "http://localhost:8080/realms/myrealm",
//            "jwtIgnoreExpiry": true,
//            "jwksRefreshIntervalMs": 21600000,
//            "adminUsername": "admin",
//            "adminPassword": "admin"

    public KeycloakUserService(JsonObject config) {
        this.keycloak = KeycloakBuilder.builder()
                .serverUrl(config.getString("keycloakUrl"))
                .realm("master") // Auth realm to log in as admin
                .clientId(config.getString("keycloakAdminClientId"))
                //.clientSecret(config.getString("clientSecret"))
                .username(config.getString("adminUsername"))
                .password(config.getString("adminPassword"))
                .grantType("password")
                .build();

        this.realm = config.getString("keycloakRealm"); // Target realm for user operations
    }

    /**
     * Reusable method to update a user's custom attribute.
     */
    private boolean updateUserAttribute(String userId, String key, String value) {
        try {
            UserRepresentation user = keycloak.realm(realm).users().get(userId).toRepresentation();

            Map<String, List<String>> attributes = user.getAttributes();
            if (attributes == null) {
                attributes = new HashMap<>();
            }

            attributes.put(key, List.of(value));
            user.setAttributes(attributes);
            keycloak.realm(realm).users().get(userId).update(user);

            LOGGER.info("Updated attribute '{}' to '{}' for user {}", key, value, userId);
            return true;

        } catch (Exception e) {
            LOGGER.error("Error updating attribute '{}' for user {}: {}", key, userId, e.getMessage(), e);
        }

        return false;
    }

    // Public method to set kyc_verified to true
    public boolean setKycVerifiedTrue(String userId) {
        return updateUserAttribute(userId, "kyc_verified", "true");
    }

    // Public method to set kyc_verified to false
    public boolean setKycVerifiedFalse(String userId) {
        return updateUserAttribute(userId, "kyc_verified", "false");
    }

    // Public method to set organisation_id
    public boolean setOrganisationId(UUID userId, UUID orgId) {
        LOGGER.debug("Setting organisation_id for user {} to {}", userId, orgId);
        return updateUserAttribute(userId.toString(), "organisation_id", orgId.toString());
    }
}

