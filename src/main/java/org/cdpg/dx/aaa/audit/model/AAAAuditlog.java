package org.cdpg.dx.aaa.audit.model;

import io.vertx.core.json.JsonObject;
import org.cdpg.dx.auditing.model.AuditLog;

import java.util.UUID;

public class AAAAuditlog implements AuditLog {
    private final UUID id;
    private final String assetName;
    private final UUID assetId;
    private final String assetType;
    private final String operation;
    private final String createdAt;
    private final String endpoint;
    private final String method;
    private final long size;
    private final String role;
    private final UUID userId;
    private final String originServer;
    private final boolean myActivityEnabled;
    private final String shortDescription;

    public AAAAuditlog(UUID id, String assetName, UUID assetId, String assetType, String operation, String createdAt,
                       String endpoint, String method, long size, String role, UUID userId, String originServer,
                       boolean myActivityEnabled, String shortDescription) {
        this.id = id;
        this.assetName = assetName;
        this.assetId = assetId;
        this.assetType = assetType;
        this.operation = operation;
        this.createdAt = createdAt;
        this.endpoint = endpoint;
        this.method = method;
        this.size = size;
        this.role = role;
        this.userId = userId;
        this.originServer = originServer;
        this.myActivityEnabled = myActivityEnabled;
        this.shortDescription = shortDescription;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.put("id", id);
        json.put("assetName", assetName);
        json.put("assetId", assetId);
        json.put("assetType", assetType);
        json.put("operation", operation);
        json.put("createdAt", createdAt);
        json.put("endpoint", endpoint);
        json.put("method", method);
        json.put("size", size);
        json.put("role", role);
        json.put("userId", userId);
        json.put("originServer", originServer);
        json.put("myActivityEnabled", myActivityEnabled);
        json.put("shortDescription", shortDescription);
        return json;
    }
}