package iudx.aaa.server.organization.service;

import io.vertx.core.Promise;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Tuple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import iudx.aaa.server.database.postgres.service.PostgresService;
import iudx.aaa.server.common.models.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.vertx.core.Future;
import org.apache.maven.model.Organization;


public class OrganizationServiceImpl implements OrganizationService {


  private final PostgresService postgresService;
  private static final Logger LOGGER = LogManager.getLogger(OrganizationServiceImpl.class);


  public OrganizationServiceImpl(PostgresService postgresService) {
    LOGGER.info("Info: org service impl constructor");
    this.postgresService = postgresService;
  }


  @Override
  public Future<JsonObject> addOrganization(JsonObject request) {
    LOGGER.info("Info: Add organization");
    Promise<JsonObject> promise = Promise.promise();

//    String id = request.getString("id");
    String name = request.getString("name");

    UUID uuid = UUID.randomUUID();

    if ( name == null ) {
      return Future.failedFuture("Missing required fields: name");
    }

    JsonArray params = new JsonArray()
      .add(uuid.toString())
      .add(name);

    LOGGER.info("Insert Parameters: {}", params.encode());

    String query = "INSERT INTO tgai.organization (id, name, created_at, updated_at) VALUES (?, ?, NOW(), NOW())";
    LOGGER.info("Query: {}", query);



    postgresService.executeInsert(query,params).onComplete(
      pgHandler -> {
        if (pgHandler.succeeded()) {
          LOGGER.info("Organization inserted successfully: {}", pgHandler.result());
          promise.complete(pgHandler.result());
        } else {
          LOGGER.error("Failed to insert organization: {}", pgHandler.cause().getMessage());
          promise.fail(pgHandler.cause());
        }
    });
    return promise.future();
  }

  @Override
  public Future<JsonObject> getOrganization(JsonObject request) {
    LOGGER.info("Info: Get organization");

    Promise<JsonObject> promise = Promise.promise();
    String id = request.getString("id");

    String query = "SELECT * FROM tgai.organization";
    JsonArray param = new JsonArray().add(id);
    LOGGER.info("Query: {}", query);

    postgresService.executeQuery("SELECT current_database(), current_user").onComplete(pgHandler -> {
      if (pgHandler.succeeded()) {
        LOGGER.info("Connected to DB: {}", pgHandler.result().encodePrettily());
      } else {
        LOGGER.error("DB Connection Failed", pgHandler.cause());
      }
    });

    postgresService.executeQuery(query).onComplete(
      pgHandler -> {
        if (pgHandler.succeeded()) {
          LOGGER.info("Query Result: {}", pgHandler.result().encodePrettily());
          promise.complete(pgHandler.result());
        } else {
          promise.fail(pgHandler.cause());
        }
      });
    return promise.future();
  }

  @Override
  public Future<JsonObject> registerOrganization(JsonObject request, String userId) {
    LOGGER.info("Info: Register Organization Request");

    Promise<JsonObject> promise = Promise.promise();
    String name = request.getString("org_name");

    if (name == null || name.isBlank()) {
      return Future.failedFuture("Missing required field: org_name");
    }

    UUID requestId = UUID.randomUUID(); // Generate unique request ID

    JsonArray requestParams = new JsonArray()
      .add(requestId.toString())
      .add(name)  // org_name stored in request
      .add(userId)
      .add("pending");

    String insertRequestQuery = "INSERT INTO tgai.org_requests (id, org_name, user_id, status) VALUES (?, ?, ?, ?)";

    postgresService.executeInsert(insertRequestQuery, requestParams)
      .onSuccess(pgHandler -> {
        LOGGER.info("Organization registration request submitted successfully");

        JsonObject response = new JsonObject()
          .put("status", "success")
          .put("message", "Organization registration request submitted")
          .put("request_id", requestId.toString())
          .put("status", "pending");

        promise.complete(response);
      })
      .onFailure(err -> {
        LOGGER.error("Failed to register organization request: {}", err.toString());
        promise.fail(err);
      });

    return promise.future();
  }


}
