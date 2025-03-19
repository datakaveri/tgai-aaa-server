package iudx.aaa.server.organization.controlller;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import iudx.aaa.server.common.models.response.FailureResponseHandler;
import iudx.aaa.server.common.models.response.ResponseType;
import iudx.aaa.server.common.models.response.SuccessResponseHandler;
import iudx.aaa.server.organization.service.OrganizationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

import static iudx.aaa.server.apiserver.util.Constants.OBTAINED_USER_ID;

public class OrganizationController extends AbstractVerticle {
  private static final Logger LOGGER = LogManager.getLogger(OrganizationController.class);
  private final OrganizationService organizationService;
  private final Router router;
  private static final String ROUTE_ADD_ORG = "/";
  private static final String ROUTE_GET_ORG = "/";
  private static final String ROUTE_ADD_ORG_REQUEST="/register";


  public OrganizationController(OrganizationService organizationService) {
    LOGGER.info("Inside org controller constructor");
    this.router = Router.router(vertx);// Creates a new Router instance
    this.organizationService = organizationService;
    setupRoutes();
  }

    //Routes for organization
  private void setupRoutes() {
    LOGGER.info("Inside org controller setup routes");
    router.route().handler(BodyHandler.create()); // This ensures BodyHandler applies globally
    router.post(ROUTE_ADD_ORG).handler(this::addOrganizationHandler);
    router.get(ROUTE_GET_ORG).handler(this::getOrganizationHandler);
    router.post(ROUTE_ADD_ORG_REQUEST).handler(this::registerOrganizationHandler);

  }

//  public OrganizationController(OrganizationService organizationService,Router router) {
//    this.organizationService = organizationService;
//    this.router = Router.router(vertx);// Creates a new Router instance
//    router.get("/org").handler(this::getOrganizationHandler);
//    router.post("/org").handler(this::addOrganizationHandler);
//  }

  public Router getRouter() {
    return this.router;
  }

  private void addOrganizationHandler(RoutingContext routingContext) {
    LOGGER.info("Info: Adding organization");
    HttpServerResponse response = routingContext.response();

    if (routingContext.body() == null || routingContext.body().length() == 0) {
      LOGGER.error("Request body is empty");
      FailureResponseHandler.processBackendResponse(response, "Request body is empty");
      return;
    }

    JsonObject requestBody;
    try {
      requestBody = routingContext.body().asJsonObject();
    } catch (Exception e) {
      LOGGER.error("Invalid JSON format: " + e.getMessage());
      FailureResponseHandler.processBackendResponse(response, "Invalid JSON format");
      return;
    }

    if (requestBody == null) {
      LOGGER.error("Request body is missing or not valid JSON");
      FailureResponseHandler.processBackendResponse(response, "Request body is missing or not valid JSON");
      return;
    }

    // Directly pass JsonObject to the service
    organizationService.addOrganization(requestBody).onComplete(ar -> {
      if (ar.succeeded()) {
        SuccessResponseHandler.handleSuccessResponse(
          response, ResponseType.Created.getCode(), ar.result()
        );
      } else {
        LOGGER.error("Failed to add organization: " + ar.cause().getMessage());
        FailureResponseHandler.processBackendResponse(response, ar.cause().getMessage());
      }
    });
  }

  private void getOrganizationHandler(RoutingContext routingContext) {
    LOGGER.info("Info: Get organization");

    HttpServerResponse response = routingContext.response();
    JsonObject request = new JsonObject();  // Create an empty request if no params are needed

    organizationService.getOrganization(request).onComplete(ar -> {
      if (ar.succeeded()) {
        JsonObject result = ar.result();
        JsonArray org = result.getJsonArray("results");

        LOGGER.info("Final Response Data: {}", result.encodePrettily());

        if (org == null || org.isEmpty()) {
          SuccessResponseHandler.handleSuccessResponse(
            response, ResponseType.NoContent.getCode(), (JsonArray) null
          );
        } else {
          SuccessResponseHandler.handleSuccessResponse(
            response, ResponseType.Ok.getCode(), new JsonObject().put("organizations", org)
          );
        }
      } else {
        LOGGER.error("Failed to retrieve organizations: " + ar.cause().getMessage());
        FailureResponseHandler.processBackendResponse(response, ar.cause().getMessage());
      }
    });
  }

  private void registerOrganizationHandler(RoutingContext routingContext) {
    LOGGER.info("Info: Register Organization Handler");

    HttpServerResponse response = routingContext.response();
    JsonObject requestBody;

    try {
      requestBody = routingContext.getBodyAsJson();
      if (requestBody == null) {
        throw new IllegalArgumentException("Invalid or missing request body");
      }
    } catch (Exception e) {
      LOGGER.error("Failed to parse request body: {}", e.getMessage());
      FailureResponseHandler.processBackendResponse(response, "Invalid JSON format");
      return;
    }
    // Extract user_id from routingContext
//    String userId = routingContext.get(OBTAINED_USER_ID);

    String userId = routingContext.user().principal().getString("user_id"); // Assuming authentication is in place
    LOGGER.info("User ID from context: {}", userId);

    organizationService.registerOrganization(requestBody, userId)
      .onSuccess(result -> {
        LOGGER.info("Organization registration request processed successfully: {}", result.encodePrettily());
        SuccessResponseHandler.handleSuccessResponse(response, ResponseType.Created.getCode(), result);
      })
      .onFailure(err -> {
        LOGGER.error("Failed to register organization: {}", err.getMessage());
        FailureResponseHandler.processBackendResponse(response, err.getMessage());
      });
  }



}



