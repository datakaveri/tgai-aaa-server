package org.cdpg.dx.aaa.organization.models;

import io.vertx.core.json.JsonObject;
import org.cdpg.dx.aaa.organization.util.Constants;

import java.util.Optional;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

public record OrganizationCreateRequest(
  Optional<UUID> id,
  UUID requestedBy,
  String name,
  Optional<String> logoPath,
  String entityType,
  String orgSector,
  String websiteLink,
  String address,
  String certificatePath,
  String pancardPath,
  Optional<String> relevantDocPath,
  String status,
  String empId,
  String jobTitle,
  String orgManagerphoneNo,
  Optional<String> createdAt,
  Optional<String> updatedAt
) {

  public static OrganizationCreateRequest fromJson(JsonObject json) {
    return new OrganizationCreateRequest(
      Optional.ofNullable(json.getString(Constants.ORG_CREATE_ID)).map(UUID::fromString),
      UUID.fromString(json.getString(Constants.REQUESTED_BY)),
      json.getString(Constants.ORG_NAME),
      Optional.ofNullable(json.getString(Constants.ORG_LOGO)), // constant name can still be ORG_LOGO
      json.getString(Constants.ENTITY_TYPE),
      json.getString(Constants.ORG_SECTOR),
      json.getString(Constants.ORG_WEBSITE),
      json.getString(Constants.ORG_ADDRESS),
      json.getString(Constants.CERTIFICATE),
      json.getString(Constants.PANCARD),
      Optional.ofNullable(json.getString(Constants.RELEVANT_DOC)),
      Optional.ofNullable(json.getString(Constants.STATUS)).orElse(Status.PENDING.getStatus()),
      json.getString(Constants.EMP_ID),
      json.getString(Constants.JOB_TITLE),
      json.getString(Constants.PHONE_NO),
      Optional.ofNullable(json.getString(Constants.CREATED_AT)),
      Optional.ofNullable(json.getString(Constants.UPDATED_AT))
    );
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    id.ifPresent(value -> json.put(Constants.ORG_CREATE_ID, value.toString()));
    json.put(Constants.REQUESTED_BY, requestedBy.toString());
    json.put(Constants.ORG_NAME, name);
    logoPath.ifPresent(value -> json.put(Constants.ORG_LOGO, value));
    json.put(Constants.ENTITY_TYPE, entityType);
    json.put(Constants.ORG_SECTOR, orgSector);
    json.put(Constants.ORG_WEBSITE, websiteLink);
    json.put(Constants.ORG_ADDRESS, address);
    json.put(Constants.CERTIFICATE, certificatePath);
    json.put(Constants.PANCARD, pancardPath);
    relevantDocPath.ifPresent(value -> json.put(Constants.RELEVANT_DOC, value));
    json.put(Constants.STATUS, status);
    json.put(Constants.EMP_ID, empId);
    json.put(Constants.JOB_TITLE, jobTitle);
    json.put(Constants.PHONE_NO, orgManagerphoneNo);
    createdAt.ifPresent(value -> json.put(Constants.CREATED_AT, value));
    updatedAt.ifPresent(value -> json.put(Constants.UPDATED_AT, value));
    return json;
  }

  public Map<String, Object> toNonEmptyFieldsMap() {
    Map<String, Object> map = new HashMap<>();
    id.ifPresent(value -> map.put(Constants.ORG_CREATE_ID, value));
    map.put(Constants.REQUESTED_BY, requestedBy.toString());
    map.put(Constants.ORG_NAME, name);
    logoPath.ifPresent(value -> map.put(Constants.ORG_LOGO, value));
    map.put(Constants.ENTITY_TYPE, entityType);
    map.put(Constants.ORG_SECTOR, orgSector);
    map.put(Constants.ORG_WEBSITE, websiteLink);
    map.put(Constants.ORG_ADDRESS, address);
    map.put(Constants.CERTIFICATE, certificatePath);
    map.put(Constants.PANCARD, pancardPath);
    relevantDocPath.ifPresent(value -> map.put(Constants.RELEVANT_DOC, value));
    map.put(Constants.STATUS, status);
    map.put(Constants.EMP_ID, empId);
    map.put(Constants.JOB_TITLE, jobTitle);
    map.put(Constants.PHONE_NO,orgManagerphoneNo);
    createdAt.ifPresent(value -> map.put(Constants.CREATED_AT, value));
    updatedAt.ifPresent(value -> map.put(Constants.UPDATED_AT, value));
    return map;
  }
}
