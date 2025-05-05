package org.cdpg.dx.aaa.organization.models;

import org.cdpg.dx.aaa.organization.util.Constants;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public record UpdateOrgDTO(
  Optional<String> orgName,
  Optional<String> orgLogo,
  Optional<String> entityType,
  Optional<String> orgSector,
  Optional<String> websiteLink,
  Optional<String> address,
  Optional<String> certificatePath,
  Optional<String> pancardPath,
  Optional<String> relevantDocPath,
  Optional<String> updatedAt
) {

  public Map<String, Object> toNonEmptyFieldsMap() {
    HashMap<String, Object> map = new HashMap<>();

    orgName.ifPresent(name -> map.put(Constants.ORG_NAME, name));
    orgLogo.ifPresent(logo -> map.put(Constants.ORG_LOGO, logo));
    entityType.ifPresent(type -> map.put(Constants.ENTITY_TYPE, type));
    orgSector.ifPresent(sector -> map.put(Constants.ORG_SECTOR, sector));
    websiteLink.ifPresent(site -> map.put(Constants.ORG_WEBSITE, site));
    address.ifPresent(addr -> map.put(Constants.ORG_ADDRESS, addr));
    certificatePath.ifPresent(cert -> map.put(Constants.CERTIFICATE, cert));
    pancardPath.ifPresent(card -> map.put(Constants.PANCARD, card));
    relevantDocPath.ifPresent(doc -> map.put(Constants.RELEVANT_DOC, doc));
    updatedAt.ifPresent(update -> map.put(Constants.UPDATED_AT, update));

    return map;
  }
}
