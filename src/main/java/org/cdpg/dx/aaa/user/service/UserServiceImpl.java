package org.cdpg.dx.aaa.user.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.cdpg.dx.aaa.credit.service.CreditService;
import org.cdpg.dx.aaa.organization.service.OrganizationService;
import org.cdpg.dx.common.model.DxUser;
import org.cdpg.dx.keyclock.service.KeycloakUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserServiceImpl implements UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);
    private final KeycloakUserService keycloakUserService;
    private final OrganizationService organizationService;
    private final CreditService creditService;

    public UserServiceImpl(KeycloakUserService keycloakUserService, OrganizationService organizationService, CreditService creditService) {

        this.keycloakUserService = keycloakUserService;
        this.creditService = creditService;
        this.organizationService = organizationService;
    }

    @Override
    public Future<DxUser> getUserInfo(DxUser dxUser) {

        Future<Boolean> pendingProvider = Future.succeededFuture(false);

        UUID orgId = null;
        try {
            orgId = UUID.fromString(dxUser.organisationId());
        } catch (IllegalArgumentException | NullPointerException e) {
            // Handle invalid UUID
            LOGGER.error("Invalid UUID for organisationId: {}", dxUser.organisationId());

        }

        if(orgId!=null){
            pendingProvider = organizationService
                    .hasPendingProviderRole(dxUser.sub(), orgId);
        }

        Future<Boolean> pendingCompute = creditService.hasPendingComputeRequest(dxUser.sub());

        return Future.all(pendingProvider, pendingCompute)
                .map(cf -> {
                    List<String> pendingRoles = new ArrayList<>();
                    if (cf.resultAt(0)) pendingRoles.add("provider");
                    if (cf.resultAt(1)) pendingRoles.add("compute");
                    return DxUser.withPendingRoles(dxUser, pendingRoles);
                });
    }
    @Override
    public Future<DxUser> getUserInfoByID(UUID userId){
        return keycloakUserService.getUserById(userId)
                .compose(this::getUserInfo);
    }

}
