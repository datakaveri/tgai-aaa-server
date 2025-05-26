package org.cdpg.dx.aaa.admin.handler;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.aaa.credit.service.CreditService;
import org.cdpg.dx.aaa.organization.handler.OrganizationHandler;
import org.cdpg.dx.aaa.organization.service.OrganizationService;
import org.cdpg.dx.aaa.user.service.UserService;
import org.cdpg.dx.common.response.ResponseBuilder;
import org.cdpg.dx.keyclock.service.KeycloakUserService;

import java.util.UUID;

public class AdminHandler {

        private static final Logger LOGGER = LogManager.getLogger(AdminHandler.class);
        private final UserService userService;
    private final KeycloakUserService keycloakUserService;
    private final CreditService creditService;
    private final OrganizationService organizationService;

        public AdminHandler(UserService userService, KeycloakUserService keycloakUserService, CreditService creditService, OrganizationService organizationService) {
            this.userService = userService;
            this.keycloakUserService = keycloakUserService;
            this.creditService = creditService;
            this.organizationService = organizationService;
        }

    public void getDxUserInfo(RoutingContext ctx) {
        userService.getDxUser(ctx)
                .compose(dxUser -> {
                    Future<Boolean> pendingProvider = organizationService.hasPendingProviderRole(dxUser.sub(), UUID.fromString(dxUser.organisationId()));
                    Future<Boolean> pendingCompute = creditService.hasPendingComputeRequest(dxUser.sub());

                    return CompositeFuture.all(pendingProvider, pendingCompute)
                            .map(cf -> {
                                JsonObject response = dxUser.toJson();
                                JsonArray pendingArray = new JsonArray();
                                if (cf.resultAt(0)) {
                                    pendingArray.add("provider");
                                }
                                if (cf.resultAt(1)) {
                                    pendingArray.add("compute");
                                }
                                response.put("pending", pendingArray);
                                return response;
                            });
                })
                .onSuccess(response -> {
                    ResponseBuilder.sendSuccess(ctx, response);
                })
                .onFailure(err -> {
                    LOGGER.error("Failed to get DxUser info: {}", err.getMessage(), err);
                    ctx.fail(err);
                });
    }

        public void getDxUserFromKeycloak(RoutingContext ctx) {
            String userId = ctx.pathParam("id");

            keycloakUserService.getUserById(UUID.fromString(userId))
                    .compose(dxUser -> {
                        Future<Boolean> pendingProvider = organizationService.hasPendingProviderRole(dxUser.sub(), UUID.fromString(dxUser.organisationId()));
                        Future<Boolean> pendingCompute = creditService.hasPendingComputeRequest(dxUser.sub());

                        return CompositeFuture.all(pendingProvider, pendingCompute)
                                .map(cf -> {
                                    JsonObject response = dxUser.toJson();
                                    JsonArray pendingArray = new JsonArray();
                                    if (cf.resultAt(0)) {
                                        pendingArray.add("provider");
                                    }
                                    if (cf.resultAt(1)) {
                                        pendingArray.add("compute");
                                    }
                                    response.put("pending", pendingArray);
                                    return response;
                                });
                    })
                    .onSuccess(response -> {
                        ResponseBuilder.sendSuccess(ctx, response);
                    })
                    .onFailure(err -> {
                        LOGGER.error("Failed to get DxUser info: {}", err.getMessage(), err);
                        ctx.fail(err);
                    });
        }
}
