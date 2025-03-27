package org.cdpg.dx.aaa.organization.service;


import io.vertx.core.Future;
import org.cdpg.dx.aaa.organization.models.Organization;


import java.util.List;

public interface OrganizationService {


  Future<Organization> createOrganization(Organization organization);

  Future<Boolean> deleteOrganization(Organization organization);

  Future<Organization> updateOrganization(Organization organization);

  Future<Organization> getOrganization(Organization organization);
  //Future<JsonObject> registerOrganization(JsonObject request);

}
