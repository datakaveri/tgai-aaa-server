package org.cdpg.dx.auth.authentication.handler;


import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthenticationHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.auth.authentication.util.BearerTokenExtractor;

public class KeycloakJwtAuthHandler implements AuthenticationHandler {
    private static final Logger LOGGER = LogManager.getLogger(KeycloakJwtAuthHandler.class);
    private final JWTAuth jwtAuth;

    public KeycloakJwtAuthHandler(JWTAuth jwtAuth) {
        this.jwtAuth = jwtAuth;
    }

    @Override
    public void handle(RoutingContext ctx) {

        String token = BearerTokenExtractor.extract(ctx);
        if (token == null || token.isBlank()) {
            LOGGER.warn("Missing or invalid Authorization header");
            ctx.response().setStatusCode(401).end("Missing Bearer token");
            return;
        }
        LOGGER.info("Extracted token: '{}'", token);
        LOGGER.info("Token length: {}", token.length());

        jwtAuth.authenticate(new JsonObject().put("token", token))
                .onSuccess(user -> {
                    LOGGER.debug("User principal: " + user.principal().encodePrettily());
                    LOGGER.info("Authentication success for user: {}", user.principal().getString("sub"));
                    ctx.setUser(user);
                    ctx.next();
                })
                .onFailure(err -> {
                    LOGGER.warn("Authentication failed: {}", err.getMessage());
                    ctx.response().setStatusCode(401).end("Unauthorized");
                });
    }
}

