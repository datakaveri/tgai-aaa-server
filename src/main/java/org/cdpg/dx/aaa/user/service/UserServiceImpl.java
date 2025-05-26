package org.cdpg.dx.aaa.user.service;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import org.cdpg.dx.aaa.organization.util.RoutingContextHelper;
import org.cdpg.dx.keyclock.model.DxUser;
import org.cdpg.dx.keyclock.service.KeycloakUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class UserServiceImpl implements UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);
    private final KeycloakUserService keycloakUserService;

    public UserServiceImpl(KeycloakUserService keycloakUserService) {
        this.keycloakUserService = keycloakUserService;
    }

    @Override
    public Future<DxUser> getDxUser(RoutingContext ctx) {
        RoutingContextHelper routingContextHelper = new RoutingContextHelper();
        DxUser dxUser = routingContextHelper.fromPrincipal(ctx);

        return Future.succeededFuture(dxUser);
    }

    @Override
    public Future<DxUser> getKeycloakDxUser(String userId) {
        Future<DxUser> future = keycloakUserService.getUserById(UUID.fromString(userId))
                .onSuccess(dxUser -> {
                    LOGGER.info("Successfully retrieved user with ID: {}", userId);
                })
                .onFailure(err -> {
                    LOGGER.error("Failed to retrieve user with ID: {}. Error: {}", userId, err.getMessage());
                });
        return future;
    }
}
