package org.cdpg.dx.aaa.organization.models;

import io.vertx.core.json.JsonObject;
import org.cdpg.dx.aaa.organization.util.Constants;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public record Organization(
  Optional<UUID> id,
  String orgName,
  Optional<String> orgLogo,
  String entityType,
  String orgSector,
  String websiteLink,
  String address,
  String certificatePath,
  String pancardPath,
  Optional<String> relevantDocPath,
  Optional<String> createdAt,
  Optional<String> updatedAt
) {

  public static Organization fromJson(JsonObject orgDetails) {
    return new Organization(
      Optional.ofNullable(orgDetails.getString(Constants.ORG_ID)).map(UUID::fromString),
      orgDetails.getString(Constants.ORG_NAME),
      Optional.ofNullable(orgDetails.getString(Constants.ORG_LOGO)),
      orgDetails.getString(Constants.ENTITY_TYPE),
      orgDetails.getString(Constants.ORG_SECTOR),
      orgDetails.getString(Constants.ORG_WEBSITE),
      orgDetails.getString(Constants.ORG_ADDRESS),
      orgDetails.getString(Constants.CERTIFICATE),
      orgDetails.getString(Constants.PANCARD),
      Optional.ofNullable(orgDetails.getString(Constants.RELEVANT_DOC)),
      Optional.ofNullable(orgDetails.getString(Constants.CREATED_AT)),
      Optional.ofNullable(orgDetails.getString(Constants.UPDATED_AT))
    );
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();

    id.ifPresent(value -> json.put(Constants.ORG_ID, value.toString()));
    Optional.ofNullable(orgName).filter(e -> !e.isEmpty()).ifPresent(value -> json.put(Constants.ORG_NAME, value));
    orgLogo.ifPresent(value -> json.put(Constants.ORG_LOGO, value));
    Optional.ofNullable(entityType).filter(e -> !e.isEmpty()).ifPresent(value -> json.put(Constants.ENTITY_TYPE, value));
    Optional.ofNullable(orgSector).filter(e -> !e.isEmpty()).ifPresent(value -> json.put(Constants.ORG_SECTOR, value));
    Optional.ofNullable(websiteLink).filter(e -> !e.isEmpty()).ifPresent(value -> json.put(Constants.ORG_WEBSITE, value));
    Optional.ofNullable(address).filter(e -> !e.isEmpty()).ifPresent(value -> json.put(Constants.ORG_ADDRESS, value));
    Optional.ofNullable(certificatePath).filter(e -> !e.isEmpty()).ifPresent(value -> json.put(Constants.CERTIFICATE, value));
    Optional.ofNullable(pancardPath).filter(e -> !e.isEmpty()).ifPresent(value -> json.put(Constants.PANCARD, value));
    relevantDocPath.ifPresent(value -> json.put(Constants.RELEVANT_DOC, value));
    createdAt.ifPresent(value -> json.put(Constants.CREATED_AT, value));
    updatedAt.ifPresent(value -> json.put(Constants.UPDATED_AT, value));

    return json;
  }

  public Map<String, Object> toNonEmptyFieldsMap() {
    Map<String, Object> map = new HashMap<>();

    id.ifPresent(value -> map.put(Constants.ORG_ID, value.toString()));
    if (orgName != null && !orgName.isEmpty()) map.put(Constants.ORG_NAME, orgName);
    orgLogo.ifPresent(value -> map.put(Constants.ORG_LOGO, value));
    if (entityType != null && !entityType.isEmpty()) map.put(Constants.ENTITY_TYPE, entityType);
    if (orgSector != null && !orgSector.isEmpty()) map.put(Constants.ORG_SECTOR, orgSector);
    if (websiteLink != null && !websiteLink.isEmpty()) map.put(Constants.ORG_WEBSITE, websiteLink);
    if (address != null && !address.isEmpty()) map.put(Constants.ORG_ADDRESS, address);
    if (certificatePath != null && !certificatePath.isEmpty()) map.put(Constants.CERTIFICATE, certificatePath);
    if (pancardPath != null && !pancardPath.isEmpty()) map.put(Constants.PANCARD, pancardPath);
    relevantDocPath.ifPresent(value -> map.put(Constants.RELEVANT_DOC, value));
    createdAt.ifPresent(value -> map.put(Constants.CREATED_AT, value));
    updatedAt.ifPresent(value -> map.put(Constants.UPDATED_AT, value));

    return map;
  }
}
