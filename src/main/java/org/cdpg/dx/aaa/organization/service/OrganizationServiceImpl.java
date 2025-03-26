package org.cdpg.dx.aaa.organization.service;

import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import iudx.aaa.server.database.postgres.service.PostgresService;

import java.util.UUID;

import io.vertx.core.Future;


public class OrganizationServiceImpl implements OrganizationService {


  private final PostgresService postgresService;
  private static final Logger LOGGER = LogManager.getLogger(OrganizationServiceImpl.class);


  public OrganizationServiceImpl(PostgresService postgresService) {
    LOGGER.info("Info: org service impl constructor");
    this.postgresService = postgresService;
  }



  @Override
  public Future<JsonObject> getOrganization(JsonObject request) {
    LOGGER.info("Info: Get organization");

    Promise<JsonObject> promise = Promise.promise();
    String id = request.getString("id");

    String query = "SELECT * FROM tgai.organization";
    LOGGER.info("Query: {}", query);

    postgresService.executeQuery(query).onComplete(pgHandler -> {
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
  public Future<JsonObject> registerOrganization(JsonObject request) {
    LOGGER.info("Info: Register Organization Request");

    Promise<JsonObject> promise = Promise.promise();
    String userId = request.getString("user_id");

    UUID requestId = UUID.randomUUID(); // Generate unique request ID
    String status = "PENDING";

    // ✅ Manually construct the query string
    String insertRequestQuery = String.format(
      "INSERT INTO tgai.org_requests (id, user_id, status) VALUES ('%s', '%s', '%s')",
      requestId, userId, status
    );

    postgresService.executeQuery(insertRequestQuery)
      .onSuccess(pgHandler -> {
        LOGGER.info("Organization registration request submitted successfully");

        JsonObject response = new JsonObject()
          .put("status", "success")
          .put("message", "Organization registration request submitted")
          .put("request_id", requestId.toString())
          .put("status", status);

        promise.complete(response);
      })
      .onFailure(err -> {
        LOGGER.error("Failed to register organization request: {}", err.toString());
        promise.fail(err);
      });

    return promise.future();
  }

  @Override
  public Future<JsonObject> getOrganizationRequest(JsonObject request) {
    LOGGER.info("Info: Get organization Requests");

    Promise<JsonObject> promise = Promise.promise();

    String query = "SELECT * FROM tgai.org_requests";
    LOGGER.info("Query: {}", query);

    postgresService.executeQuery(query).onComplete(pgHandler -> {
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


}
