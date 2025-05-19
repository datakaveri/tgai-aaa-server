package org.cdpg.dx.aaa.organization.models;

import io.vertx.core.json.JsonObject;
import org.cdpg.dx.aaa.organization.util.Constants;

import java.util.HashMap;
import java.util.Map;

public record UpdateOrgDTO(
        String orgName,
        String orgLogo,
        String entityType,
        String orgSector,
        String websiteLink,
        String address,
        String certificatePath,
        String pancardPath,
        String relevantDocPath,
        String updatedAt
) {

  public Map<String, Object> toNonEmptyFieldsMap() {
    HashMap<String, Object> map = new HashMap<>();

    if (orgName != null) map.put(Constants.ORG_NAME, orgName);
    if (orgLogo != null) map.put(Constants.ORG_LOGO, orgLogo);
    if (entityType != null) map.put(Constants.ENTITY_TYPE, entityType);
    if (orgSector != null) map.put(Constants.ORG_SECTOR, orgSector);
    if (websiteLink != null) map.put(Constants.ORG_WEBSITE, websiteLink);
    if (address != null) map.put(Constants.ORG_ADDRESS, address);
    if (certificatePath != null) map.put(Constants.CERTIFICATE, certificatePath);
    if (pancardPath != null) map.put(Constants.PANCARD, pancardPath);
    if (relevantDocPath != null) map.put(Constants.RELEVANT_DOC, relevantDocPath);
    if (updatedAt != null) map.put(Constants.UPDATED_AT, updatedAt);

    return map;
  }

  public static UpdateOrgDTO fromJson(JsonObject orgDetails) {
    return new UpdateOrgDTO(
            orgDetails.getString(Constants.ORG_NAME),
            orgDetails.getString(Constants.ORG_LOGO),
            orgDetails.getString(Constants.ENTITY_TYPE),
            orgDetails.getString(Constants.ORG_SECTOR),
            orgDetails.getString(Constants.ORG_WEBSITE),
            orgDetails.getString(Constants.ORG_ADDRESS),
            orgDetails.getString(Constants.CERTIFICATE),
            orgDetails.getString(Constants.PANCARD),
            orgDetails.getString(Constants.RELEVANT_DOC),
            orgDetails.getString(Constants.UPDATED_AT)
    );
  }
}
