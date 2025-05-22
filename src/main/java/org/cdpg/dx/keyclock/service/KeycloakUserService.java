package org.cdpg.dx.keyclock.service;

import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.auth.authorization.model.DxRole;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;

import java.util.Collections;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class KeycloakUserService {

    private static final Logger LOGGER = LogManager.getLogger(KeycloakUserService.class);

    private final Keycloak keycloak;
    private final String realm;

    public KeycloakUserService(JsonObject config) {
        this.keycloak = KeycloakBuilder.builder()
                .serverUrl(config.getString("keycloakUrl"))
                .realm("master") // Auth realm to log in as admin
                .clientId(config.getString("keycloakAdminClientId"))
                //.clientSecret(config.getString("clientSecret")) // Only needed for confidential clients
                .username(config.getString("adminUsername"))
                .password(config.getString("adminPassword"))
                .grantType("password")
                .build();

        this.realm = config.getString("keycloakRealm"); // Target realm for user operations
    }

    /**
     * Reusable method to update multiple custom attributes for a user.
     */
    private boolean updateUserAttributes(String userId, Map<String, String> attributesToUpdate) {
        try {
            UserResource userResource = keycloak.realm(realm).users().get(userId);
            UserRepresentation user = userResource.toRepresentation();

            Map<String, List<String>> currentAttributes = user.getAttributes();
            if (currentAttributes == null) {
                currentAttributes = new HashMap<>();
            }

            for (Map.Entry<String, String> entry : attributesToUpdate.entrySet()) {
                currentAttributes.put(entry.getKey(), List.of(entry.getValue()));
            }

            user.setAttributes(currentAttributes);

            LOGGER.info("Attributes before update for user {}: {}", userId, currentAttributes);
            userResource.update(user);

            // Confirm update
            UserRepresentation updated = userResource.toRepresentation();
            Map<String, List<String>> updatedAttributes = updated.getAttributes();

            for (Map.Entry<String, String> entry : attributesToUpdate.entrySet()) {
                String key = entry.getKey();
                String expectedValue = entry.getValue();
                String actualValue = updatedAttributes.getOrDefault(key, List.of("")).get(0);
                if (!expectedValue.equals(actualValue)) {
                    LOGGER.warn("Attribute {} update mismatch for user {}. Expected: {}, Found: {}", key, userId, expectedValue, actualValue);
                    return false;
                }
            }

            LOGGER.info("Successfully updated attributes for user {}: {}", userId, attributesToUpdate.keySet());
            return true;

        } catch (Exception e) {
            LOGGER.error("Error updating attributes {} for user {}: {}", attributesToUpdate.keySet(), userId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Set both `organisation_id` and `organisation_name` for a user.
     */
    public boolean setOrganisationDetails(UUID userId, UUID orgId, String orgName) {
        if (orgId == null || orgName == null || orgName.isBlank()) {
            LOGGER.warn("Invalid organisation_id or organisation_name for user {}", userId);
            return false;
        }

        Map<String, String> attributes = new HashMap<>();
        attributes.put("organisation_id", orgId.toString());
        attributes.put("organisation_name", orgName);

        LOGGER.debug("Setting organisation_id and organisation_name for user {} to {}, {}", userId, orgId, orgName);
        return updateUserAttributes(userId.toString(), attributes);
    }

    public boolean assignRealmRoleToUser(String userId, DxRole dxRole) {
        try {
            RealmResource realmResource = keycloak.realm(realm);
            UsersResource usersResource = realmResource.users();
            RoleRepresentation role = realmResource.roles().get(dxRole.getRole()).toRepresentation();

            if (role == null) {
                LOGGER.warn("Role '{}' not found in realm '{}'", dxRole.getRole(), realm);
                return false;
            }

            usersResource.get(userId).roles().realmLevel().add(Collections.singletonList(role));
            LOGGER.info("Assigned role '{}' to user '{}'", dxRole.getRole(), userId);
            return true;

        } catch (Exception e) {
            LOGGER.error("Failed to assign role '{}' to user '{}': {}", dxRole.getRole(), userId, e.getMessage(), e);
            return false;
        }
    }

    public boolean setKycVerifiedTrueWithData(String userId, JsonObject aadhaarKycJson) {
        if (aadhaarKycJson == null) {
            LOGGER.warn("aadhaarKycJson is null for user {}", userId);
            return false;
        }

        String minifiedJson = aadhaarKycJson.encode();
        LOGGER.debug("Setting kyc_verified to true and aadhaar_kyc_data for user {}: {}", userId, minifiedJson);

        Map<String, String> attributes = new HashMap<>();
        attributes.put("kyc_verified", "true");
        attributes.put("aadhaar_kyc_data", minifiedJson);

        return updateUserAttributes(userId, attributes);
    }

    /**
     * Set `kyc_verified` to "false" and clear `aadhaar_kyc_data` by setting it to an empty JSON.
     */
    public boolean setKycVerifiedFalse(String userId) {
        LOGGER.debug("Setting kyc_verified to false and clearing aadhaar_kyc_data for user {}", userId);

        Map<String, String> attributes = new HashMap<>();
        attributes.put("kyc_verified", "false");
        attributes.put("aadhaar_kyc_data", "{}");

        return updateUserAttributes(userId, attributes);
    }
}
