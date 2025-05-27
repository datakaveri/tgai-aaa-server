package org.cdpg.dx.aaa.audit.model;

import io.vertx.core.json.JsonObject;
import org.cdpg.dx.auditing.model.AuditLog;

import java.util.UUID;

public class AAAAuditlog implements AuditLog {
    private final UUID id;

    //change this to AuditingHandler and it can be a helper
    public AAAAuditlog(UUID id) {
        this.id = id;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.put("id", id);
        return json;
    }

}