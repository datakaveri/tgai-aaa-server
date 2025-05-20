package org.cdpg.dx.aaa.credit.models;

import io.vertx.core.json.JsonObject;
import org.cdpg.dx.aaa.credit.util.Constants;
import org.cdpg.dx.database.postgres.base.entity.BaseEntity;
import org.cdpg.dx.common.exception.DxValidationException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.cdpg.dx.common.util.ValidationUtils.requireNonNull;

public record CreditRequest(
  UUID id,
  UUID userId,
  double amount,
  String status,
  String requestedAt,
  String processedAt
) implements BaseEntity<CreditRequest> {

  public static CreditRequest fromJson(JsonObject json) {
    try {
      return new CreditRequest(
        json.containsKey(Constants.CREDIT_REQUEST_ID)
          ? UUID.fromString(json.getString(Constants.CREDIT_REQUEST_ID))
          : null,
        UUID.fromString(requireNonNull(json.getString(Constants.USER_ID), Constants.USER_ID)),
        json.getDouble(Constants.AMOUNT),
        json.getString(Constants.STATUS),
        json.getString(Constants.REQUESTED_AT),
        json.getString(Constants.PROCESSED_AT)
      );
    } catch (IllegalArgumentException e) {
      throw new DxValidationException("Missing or invalid field: " + e.getMessage());
    }
  }

  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    if (id != null) json.put(Constants.CREDIT_REQUEST_ID, id.toString());
    json.put(Constants.USER_ID, userId.toString());
    json.put(Constants.AMOUNT, amount);
    if (status != null && !status.isEmpty()) json.put(Constants.STATUS, status);
    if (requestedAt != null && !requestedAt.isEmpty()) json.put(Constants.REQUESTED_AT, requestedAt);
    if (processedAt != null && !processedAt.isEmpty()) json.put(Constants.PROCESSED_AT, processedAt);
    return json;
  }

  @Override
  public Map<String, Object> toNonEmptyFieldsMap() {
    Map<String, Object> map = new HashMap<>();
    if (id != null) map.put(Constants.CREDIT_REQUEST_ID, id.toString());
    map.put(Constants.USER_ID, userId.toString());
    map.put(Constants.AMOUNT, amount);
    if (status != null && !status.isEmpty()) map.put(Constants.STATUS, status);
    if (requestedAt != null && !requestedAt.isEmpty()) map.put(Constants.REQUESTED_AT, requestedAt);
    if (processedAt != null && !processedAt.isEmpty()) map.put(Constants.PROCESSED_AT, processedAt);
    return map;
  }

  @Override
  public String getTableName() {
    return Constants.CREDIT_REQUEST_TABLE;
  }
}
