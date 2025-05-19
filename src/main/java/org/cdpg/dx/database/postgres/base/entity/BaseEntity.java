package org.cdpg.dx.database.postgres.base.entity;

import io.vertx.core.json.JsonObject;
import java.util.Map;

public interface BaseEntity<T> {
  Map<String, Object> toNonEmptyFieldsMap();

  JsonObject toJson();

  String getTableName();
}