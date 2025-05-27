package org.cdpg.dx.aaa.admin.handler;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.aaa.credit.service.CreditService;
import org.cdpg.dx.aaa.organization.service.OrganizationService;
import org.cdpg.dx.aaa.user.service.UserService;
import org.cdpg.dx.common.response.ResponseBuilder;
import org.cdpg.dx.common.util.RequestHelper;
import org.cdpg.dx.common.util.RoutingContextHelper;
import org.cdpg.dx.common.model.DxUser;
import org.cdpg.dx.keyclock.service.KeycloakUserService;

import java.util.UUID;

public class AdminHandler {

    private static final Logger LOGGER = LogManager.getLogger(AdminHandler.class);
    private final UserService userService;
    private final KeycloakUserService keycloakUserService;
    private final CreditService creditService;
    private final OrganizationService organizationService;

    public AdminHandler(UserService userService, KeycloakUserService keycloakUserService,
                        CreditService creditService, OrganizationService organizationService) {
        this.userService = userService;
        this.keycloakUserService = keycloakUserService;
        this.creditService = creditService;
        this.organizationService = organizationService;
    }

    public void getDxUserInfo(RoutingContext ctx) {
        DxUser dxUser = RoutingContextHelper.fromPrincipal(ctx);
        System.out.println("dxUser: " + dxUser.toJson());
        userService.getUserInfo(dxUser)
                .onSuccess(response -> ResponseBuilder.sendSuccess(ctx, response))
                .onFailure(ctx::fail);
    }

    public void getDxUserFromKeycloak(RoutingContext ctx) {
        UUID userId = RequestHelper.getPathParamAsUUID(ctx, "id");

        userService.getUserInfoByID(userId)
                .compose(userService::getUserInfo)
                .onSuccess(response -> ResponseBuilder.sendSuccess(ctx, response))
                .onFailure(err -> {
                    LOGGER.error("Failed to get DxUser info: {}", err.getMessage(), err);
                    ctx.fail(err);
                });
    }


    public void getAllDxUsersKeycloak(RoutingContext ctx) {
        keycloakUserService.getUsers(1, 1)
                .compose(users -> {
                    JsonArray usersArray = new JsonArray();
                    for (DxUser user : users) {
                        return userService.getUserInfo(user)
                                .map(dxUser -> {
                                    usersArray.add(dxUser.toJson());
                                    return usersArray;
                                });
                    }
                    return Future.succeededFuture(usersArray);
                })
                .onSuccess(response -> ResponseBuilder.sendSuccess(ctx, response))
                .onFailure(err -> {
                    LOGGER.error("Failed to get all DxUsers: {}", err.getMessage(), err);
                    ctx.fail(err);
                });
    }

}
