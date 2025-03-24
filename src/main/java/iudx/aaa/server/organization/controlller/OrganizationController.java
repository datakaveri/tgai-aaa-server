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
import iudx.aaa.server.apiserver.IntrospectToken;
import iudx.aaa.server.common.models.response.FailureResponseHandler;
import iudx.aaa.server.common.models.response.ResponseType;
import iudx.aaa.server.common.models.response.SuccessResponseHandler;
import iudx.aaa.server.organization.service.OrganizationService;
import iudx.aaa.server.token.TokenService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

import static iudx.aaa.server.apd.Constants.TOKEN_SERVICE_ADDRESS;
import static iudx.aaa.server.apiserver.util.Constants.OBTAINED_USER_ID;

public class OrganizationController extends AbstractVerticle {
  private static final Logger LOGGER = LogManager.getLogger(OrganizationController.class);
  private final OrganizationService organizationService;
//  private TokenService tokenService;
  private final Router router;
  private static final String ROUTE_ADD_ORG = "/";
  private static final String ROUTE_GET_ORG = "/";
  private static final String ROUTE_ADD_ORG_REQUEST="/register";
  private static final String ROUTE_GET_ORG_REQUEST="/requests";


  public OrganizationController(OrganizationService organizationService) {
    LOGGER.info("Inside org controller constructor");
    this.router = Router.router(vertx);// Creates a new Router instance
    this.organizationService = organizationService;
//    this.tokenService = tokenService;
    setupRoutes();
  }

    //Routes for organization
  private void setupRoutes() {
    LOGGER.info("Inside org controller setup routes");
    router.route().handler(BodyHandler.create()); // This ensures BodyHandler applies globally
    router.get(ROUTE_GET_ORG).handler(this::getOrganizationHandler);
    router.post(ROUTE_ADD_ORG_REQUEST).handler(this::registerOrganizationHandler);
    router.get(ROUTE_GET_ORG_REQUEST).handler(this::getOrganizationRequestHandler);

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

    String accessToken = routingContext.request().headers().get("Authorization");
    LOGGER.info("Getting authorization header:{}",accessToken);

//    IntrospectToken introspectToken = new IntrospectToken();
//    introspectToken.setAccessToken(accessToken);
//
//    tokenService.validateToken(
//      introspectToken,
//      handler -> {
//        if (handler.succeeded()) {
//         LOGGER.info("Handler result: {}", handler.result().encode());
//        } else {
//          LOGGER.info("Handler result: {}", handler.cause().getLocalizedMessage());
//        }
//      });

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

    //String userId = routingContext.user().principal().getString("user_id"); // Assuming authentication is in place
//    LOGGER.info("User ID from context: {}", userId);

    organizationService.registerOrganization(requestBody)
      .onSuccess(result -> {
        LOGGER.info("Organization registration request processed successfully: {}", result.encodePrettily());
        SuccessResponseHandler.handleSuccessResponse(response, ResponseType.Created.getCode(), result);
      })
      .onFailure(err -> {
        LOGGER.error("Failed to register organization: {}", err.getMessage());
        FailureResponseHandler.processBackendResponse(response, err.getMessage());
      });
  }

  private void getOrganizationRequestHandler(RoutingContext routingContext) {
    LOGGER.info("Info: Get organization requests");

    HttpServerResponse response = routingContext.response();
    JsonObject request = new JsonObject();  // Create an empty request if no params are needed

    organizationService.getOrganizationRequest(request).onComplete(ar -> {
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



}



