package org.cdpg.dx.common.model;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.UUID;

public record DxUser(
        List<String> roles,
        String organisationId,
        String organisationName,
        UUID sub,
        boolean emailVerified,
        boolean kycVerified,
        String name,
        String preferredUsername,
        String givenName,
        String familyName,
        String email,
        List<String> pendingRoles // <-- added field
) {
    public JsonObject toJson() {
        return new JsonObject()
                .put("roles", roles != null ? new JsonArray(roles) : new JsonArray())
                .put("organisationId", organisationId)
                .put("organisationName", organisationName)
                .put("sub", sub != null ? sub.toString() : null)
                .put("emailVerified", emailVerified)
                .put("kycVerified", kycVerified)
                .put("name", name)
                .put("preferredUsername", preferredUsername)
                .put("givenName", givenName)
                .put("familyName", familyName)
                .put("email", email)
                .put("pending_roles", pendingRoles != null ? new JsonArray(pendingRoles) : new JsonArray());
    }

    public static DxUser withPendingRoles(DxUser user, List<String> pendingRoles) {
        return new DxUser(
                user.roles(),
                user.organisationId(),
                user.organisationName(),
                user.sub(),
                user.emailVerified(),
                user.kycVerified(),
                user.name(),
                user.preferredUsername(),
                user.givenName(),
                user.familyName(),
                user.email(),
                pendingRoles
        );
    }
}
