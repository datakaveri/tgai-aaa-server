package iudx.aaa.server.organization.service;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;


import java.util.List;

public interface OrganizationService {

  Future<JsonObject> addOrganization(JsonObject request);

  Future<JsonObject> getOrganization(JsonObject request);

  Future<JsonObject> registerOrganization(JsonObject request, String userId);

}
