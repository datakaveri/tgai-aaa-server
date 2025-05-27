package org.cdpg.dx.keyclock.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.cdpg.dx.auth.authorization.model.DxRole;
import org.cdpg.dx.common.exception.KeycloakServiceException;
import org.cdpg.dx.common.util.BlockingExecutionUtil;
import org.cdpg.dx.keyclock.client.KeycloakClientProvider;
import org.cdpg.dx.keyclock.config.KeycloakConstants;
import org.cdpg.dx.common.model.DxUser;
import org.cdpg.dx.keyclock.util.DxUserMapper;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class KeycloakUserServiceImpl implements KeycloakUserService {
    private final Keycloak keycloak;
    private final String realm;

    private final static Logger LOGGER = LogManager.getLogger(KeycloakUserServiceImpl.class);

    public KeycloakUserServiceImpl(JsonObject config) {
        this.keycloak = KeycloakClientProvider.getInstance(config);
        this.realm = config.getString("keycloakRealm");
    }

    private UsersResource usersResource() {
        return keycloak.realm(realm).users();
    }

    @Override
    public Future<List<DxUser>> getUsers(int page, int size) {
        return BlockingExecutionUtil.runBlocking(() -> {
            try {
                List<UserRepresentation> reps = usersResource().list(page * size, size);
                return reps.stream().map(DxUserMapper::fromUserRepresentation).collect(Collectors.toList());
            } catch (Exception e) {
                throw new KeycloakServiceException("Failed to retrieve users from Keycloak", e);
            }
        });
    }

    @Override
    public Future<DxUser> getUserById(UUID userId) {
        return BlockingExecutionUtil.runBlocking(() -> {
            try {
                UserRepresentation user = usersResource().get(userId.toString()).toRepresentation();
                return DxUserMapper.fromUserRepresentation(user);
            } catch (Exception e) {
                e.printStackTrace();
                throw new KeycloakServiceException("Failed to retrieve user with ID: " + userId, e);
            }
        });
    }



    @Override
    public Future<Boolean> addRoleToUser(UUID userId, DxRole dxRole) {
        return BlockingExecutionUtil.runBlocking(() -> {
            try {
                RealmResource realmResource = keycloak.realm(realm);
                UsersResource usersResource = realmResource.users();
                RoleRepresentation role = realmResource.roles().get(dxRole.getRole()).toRepresentation();

                if (role == null) {
                    LOGGER.warn("Role '{}' not found in realm '{}'", dxRole.getRole(), realm);
                    throw new KeycloakServiceException(dxRole.getRole()  + " not available in KC");
                }

                usersResource.get(userId.toString()).roles().realmLevel().add(Collections.singletonList(role));
                LOGGER.info("Assigned role '{}' to user '{}'", dxRole.getRole(), userId);
                return true;
            } catch (Exception e) {
                LOGGER.error("Failed to assign role '{}' to user '{}': {}", dxRole.getRole(), userId, e.getMessage(), e);
                throw new KeycloakServiceException("Failed to assign role to user", e);
            }
        });
    }

    @Override
    public Future<Boolean> removeRoleFromUser(UUID userId, DxRole dxRole) {
        return BlockingExecutionUtil.runBlocking(() -> {
            try {
                RealmResource realmResource = keycloak.realm(realm);
                UsersResource usersResource = realmResource.users();
                RoleRepresentation role = realmResource.roles().get(dxRole.getRole()).toRepresentation();

                if (role == null) {
                    throw new KeycloakServiceException("Given role not available in KC");
                }

                usersResource.get(userId.toString()).roles().realmLevel().remove(Collections.singletonList(role));
                LOGGER.info("Removed role '{}' from user '{}'", dxRole.getRole(), userId);
                return true;
            } catch (Exception e) {
                LOGGER.error("Failed to remove role '{}' from user '{}': {}", dxRole.getRole(), userId, e.getMessage(), e);
                throw new KeycloakServiceException("Failed to remove role from user", e);
            }
        });
    }



    @Override
    public Future<Boolean> updateUserAttributes(UUID userId, Map<String, String> attributes) {
        return BlockingExecutionUtil.runBlocking(() -> {
            try {
                UserRepresentation user = usersResource().get(userId.toString()).toRepresentation();
                Map<String, List<String>> existingAttrs = Optional.ofNullable(user.getAttributes()).orElse(new HashMap<>());
                attributes.forEach((k, v) -> existingAttrs.put(k, List.of(v)));
                user.setAttributes(existingAttrs);
                usersResource().get(userId.toString()).update(user);
                return true;
            } catch (Exception e) {
                throw new KeycloakServiceException("Failed to update attributes for user with ID: " + userId, e);
            }
        });
    }

    @Override
    public Future<Boolean> deleteUser(UUID userId) {
        return BlockingExecutionUtil.runBlocking(() -> {
            try {
                usersResource().get(userId.toString()).remove();
                return true;
            } catch (Exception e) {
                throw new KeycloakServiceException("Failed to delete user with ID: " + userId, e);
            }
        });
    }

    @Override
    public Future<Boolean> enableUser(UUID userId) {
        return setUserEnabled(userId, true);
    }

    @Override
    public Future<Boolean> disableUser(UUID userId) {
        return setUserEnabled(userId, false);
    }



    @Override
    public Future<Boolean> setOrganisationDetails(UUID userId, UUID orgId, String orgName) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put(KeycloakConstants.ORGANISATION_ID, orgId.toString());
        attributes.put(KeycloakConstants.ORGANISATION_NAME, orgName);
        return updateUserAttributes(userId, attributes);
    }

    @Override
    public Future<Boolean> setKycVerifiedTrueWithData(UUID userId, JsonObject kycData) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put(KeycloakConstants.KYC_VERIFIED, "true");
        attributes.put(KeycloakConstants.AADHAAR_KYC_DATA, kycData.encode());
        return updateUserAttributes(userId, attributes);
    }

    @Override
    public Future<Boolean> setKycVerifiedFalse(UUID userId) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put(KeycloakConstants.KYC_VERIFIED, "false");
        attributes.put(KeycloakConstants.AADHAAR_KYC_DATA, "");
        return updateUserAttributes(userId, attributes);
    }

    private Future<Boolean> setUserEnabled(UUID userId, boolean enabled) {
        return BlockingExecutionUtil.runBlocking(() -> {
            try {
                UserRepresentation user = usersResource().get(userId.toString()).toRepresentation();
                user.setEnabled(enabled);
                usersResource().get(userId.toString()).update(user);
                return true;
            } catch (Exception e) {
                throw new KeycloakServiceException("Failed to " + (enabled ? "enable" : "disable") + " user with ID: " + userId, e);
            }
        });
    }
}
