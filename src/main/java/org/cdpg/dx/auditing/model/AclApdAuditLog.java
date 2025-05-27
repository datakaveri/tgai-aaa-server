package org.cdpg.dx.auditing.model;

import io.vertx.core.json.JsonObject;
import java.util.UUID;

public class AclApdAuditLog implements AuditLog {
  private final UUID id;
  private final String assetName;
  private final UUID assetId;
  private final String assetType;
  private final String operation;
  private final String createdAt;
  private final String api;
  private final String method;
  private final Long size;
  private final String role;
  private final UUID userId;
  private final String originServer;
  private final boolean myActivityEnabled;

  //change this to AuditingHandler and it can be a helper
  public AclApdAuditLog(UUID id, String assetName, UUID assetId, String assetType, String operation, String createdAt,
                        String api, String method, Long size, String role, UUID userId, String originServer,
                        boolean myActivityEnabled) {
    this.id = id;
    this.assetName = assetName;
    this.assetId = assetId;
    this.assetType = assetType;
    this.operation = operation;
    this.createdAt = createdAt;
    this.api = api;
    this.method = method;
    this.size = size;
    this.role = role;
    this.userId = userId;
    this.originServer = originServer;
    this.myActivityEnabled = myActivityEnabled;
  }

  //    UUID id, //primary key, not required to be set
//    String assetName,
//    UUID assetId,
//    String assetType,
//    String operation, //PUT, POST
//    String createdAt,
//    String api,
//    String method,
//    Long size,//size = 0, bcz it is sent in PUT and POST
//    String role,
//    UUID userId,
//    String originServer, //=ACL
//    boolean myActivityEnabled // true (not sure about the requirement)

  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    json.put("id", id);
    json.put("asset_name", assetName);
    json.put("asset_id", assetId);
    json.put("asset_type", assetType);
    json.put("operation", operation);
    json.put("created_at", createdAt);
    json.put("api", api);
    json.put("method", method);
    json.put("size", size);
    json.put("role", role);
    json.put("user_id", userId);
    json.put("origin_server", originServer);
    json.put("my_activity_enabled", myActivityEnabled);
    return json;
  }

}