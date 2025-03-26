package org.cdpg.dx.aaa.organization.service;


import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;



import java.util.List;

public interface OrganizationService {

  Future<JsonObject> getOrganization(JsonObject request);

  Future<JsonObject> getOrganizationRequest(JsonObject request);

  Future<JsonObject> registerOrganization(JsonObject request);

}
