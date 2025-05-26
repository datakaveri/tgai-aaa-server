package org.cdpg.dx.keyclock.model;

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
        String email
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
                .put("email", email);
    }
}
