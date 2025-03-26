package org.cdpg.dx.aaa.organization.service;


import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.cdpg.dx.aaa.organization.models.Organization;


import java.util.List;

public interface OrganizationService {


  Future<Organization> createOrganization(Organization organization);

  Future<Organization> deleteOrganization(Organization organization);

//  Future<Organization> updateOrganization(Organization organization);
//
//  Future<Organization> getOrganization(Organization organization);
//  Future<JsonObject> registerOrganization(JsonObject request);

}
