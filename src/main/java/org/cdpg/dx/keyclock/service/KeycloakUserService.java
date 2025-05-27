package org.cdpg.dx.keyclock.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.cdpg.dx.auth.authorization.model.DxRole;
import org.cdpg.dx.common.model.DxUser;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface KeycloakUserService {
    Future<List<DxUser>> getUsers(int page, int size);
    Future<DxUser> getUserById(UUID userId);
    Future<Boolean> updateUserAttributes(UUID userId, Map<String, String> attributes);
    Future<Boolean> deleteUser(UUID userId);
    Future<Boolean> enableUser(UUID userId);
    Future<Boolean> disableUser(UUID userId);
    Future<Boolean> addRoleToUser(UUID userId, DxRole role);
    Future<Boolean> removeRoleFromUser(UUID userId, DxRole dxRole);
    Future<Boolean> setOrganisationDetails(UUID userId, UUID orgId, String orgName);
    Future<Boolean> setKycVerifiedTrueWithData(UUID userId, JsonObject kycData);
    Future<Boolean> setKycVerifiedFalse(UUID userId);
}

