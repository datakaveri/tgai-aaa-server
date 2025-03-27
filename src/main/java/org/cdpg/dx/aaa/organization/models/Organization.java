package org.cdpg.dx.aaa.organization.models;


import io.vertx.core.json.JsonObject;

import java.time.LocalDateTime;

public record Organization(String id, String name, LocalDateTime createdAt, LocalDateTime updatedAt) {
  public JsonObject toJson() {
    return JsonObject.mapFrom(this);
  }
}
