package org.cdpg.dx.aaa.user.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.cdpg.dx.aaa.credit.service.CreditService;
import org.cdpg.dx.aaa.organization.models.OrganizationCreateRequest;
import org.cdpg.dx.aaa.organization.models.OrganizationJoinRequest;
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
        Future<List<OrganizationJoinRequest>> joinRequests = Future.succeededFuture(null);
        Future<List<OrganizationCreateRequest>> createRequests = Future.succeededFuture(null);

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
            System.out.println("here");
            joinRequests = organizationService.getOrganizationJoinRequestsByUser(dxUser.sub());
        }

        Future<Boolean> pendingCompute = creditService.hasPendingComputeRequest(dxUser.sub());
        createRequests = organizationService.getOrganizationCreateRequestsByUserId(dxUser.sub());

        return Future.all(pendingProvider, pendingCompute, joinRequests, createRequests)
                .map(cf -> {
                    List<String> pendingRoles = new ArrayList<>();
                    if (cf.resultAt(0)) pendingRoles.add("provider");
                    if (cf.resultAt(1)) pendingRoles.add("compute");
                    List<OrganizationJoinRequest> joinReqList = cf.resultAt(2);
                    OrganizationJoinRequest lastJoinReq = (joinReqList != null && !joinReqList.isEmpty())
                            ? joinReqList.get(joinReqList.size() - 1)
                            : null;
                    List<OrganizationCreateRequest> createReqList = cf.resultAt(3);
                    List<JsonObject> createReqJsons = new ArrayList<>();
                    if (createReqList != null) {
                        for (OrganizationCreateRequest req : createReqList) {
                            createReqJsons.add(req.toJson());
                        }
                    }
                    return DxUser.withPendingRoles(dxUser, pendingRoles, lastJoinReq!=null ? lastJoinReq.toJson() : new JsonObject(), createReqJsons);
                });
    }
    @Override
    public Future<DxUser> getUserInfoByID(UUID userId){
        return keycloakUserService.getUserById(userId)
                .compose(this::getUserInfo);
    }

}
