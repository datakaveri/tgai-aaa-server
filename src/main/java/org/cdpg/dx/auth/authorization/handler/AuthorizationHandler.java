package org.cdpg.dx.auth.authorization.handler;



import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import org.cdpg.dx.auth.authorization.model.DxRole;
import org.cdpg.dx.common.exception.DxForbiddenException;
import org.cdpg.dx.common.exception.DxUnauthorizedException;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class AuthorizationHandler {

    public static Handler<RoutingContext> forRoles(DxRole... roles) {
        Set<String> allowed = Arrays.stream(roles)
                .map(DxRole::getRole)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        return ctx -> {
            User user = ctx.user();
            if (user == null) {
                ctx.fail(new DxUnauthorizedException("No User Found")); // Unauthorized
                return;
            }

            JsonObject principal = user.principal();
            JsonObject realmAccess = principal.getJsonObject("realm_access");

            if (realmAccess == null || !realmAccess.containsKey("roles")) {
                ctx.fail(new DxForbiddenException("User don't have any assigned")); // Forbidden
                return;
            }

            JsonArray userRoles = realmAccess.getJsonArray("roles");

            boolean allowedRole = userRoles.stream()
                    .map(Object::toString)
                    .map(String::toLowerCase)
                    .anyMatch(allowed::contains);

            if (allowedRole) {
                ctx.next();
            } else {
                ctx.fail(new DxForbiddenException("User don't sufficient role")); // Forbidden
            }
        };
    }

    public static Handler<RoutingContext> requireKycVerified() {
        return ctx -> {
            User user = ctx.user();
            if (user == null) {
                ctx.fail(new DxUnauthorizedException("No User Found")); // HTTP 401
                return;
            }

            JsonObject principal = user.principal();

            if (principal == null || !principal.containsKey("kyc_verified")) {
                ctx.fail(new DxForbiddenException("KYC verification status missing")); // HTTP 403
                return;
            }

            boolean isKycVerified = principal.getBoolean("kyc_verified", false);
            if (!isKycVerified) {
                ctx.fail(new DxForbiddenException("User's KYC is not verified")); // HTTP 403
                return;
            }

            ctx.next();
        };
    }
}
