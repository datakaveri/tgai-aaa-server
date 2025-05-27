package org.cdpg.dx.auditing.helper;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.cdpg.dx.auditing.model.AccessRequestDto;
import org.cdpg.dx.auditing.model.AclApdAuditLog;
import org.cdpg.dx.auditing.model.AuditLog;

public class AuditingHelper {
  private AuditingHelper() {
  }

  public static AuditLog createAuditLog(AccessRequestDto dto, User user, String endpoint, String method) {
    UUID id = UUID.randomUUID();
    String assetName = dto.getAssetName();
    UUID assetId = UUID.fromString(dto.getItemId());
    String assetType = dto.getAssetType();
    String operation = dto.getRequestType().getRequest();
    String createdAt = LocalDateTime.now().atZone(ZoneOffset.UTC).toString();
    long size = 0L; // Size is not applicable for AccessRequest, set to 0
    JsonObject principal = user.principal();
    JsonObject realmAccess = principal.getJsonObject("realm_access");
    JsonArray userRoles = realmAccess.getJsonArray("roles");
    String role = userRoles.contains("consumer") ? "consumer" : "provider";
    UUID userId = UUID.fromString(principal.getString("sub"));
    String originServer = "ACL";
    boolean myActivityEnabled = true;

    return new AclApdAuditLog(id, assetName, assetId, assetType, operation, createdAt,
        endpoint, method, size, role, userId, originServer, myActivityEnabled);
  }
}
