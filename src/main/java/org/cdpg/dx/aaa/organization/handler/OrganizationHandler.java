package org.cdpg.dx.aaa.organization.handler;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Tuple;
import iudx.aaa.server.apiserver.KeycloakHandler;
import iudx.aaa.server.apiserver.models.Response;
import iudx.aaa.server.apiserver.models.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.aaa.organization.models.*;
import org.cdpg.dx.aaa.organization.service.OrganizationService;
import org.cdpg.dx.aaa.organization.util.Constants;
import org.cdpg.dx.common.response.ResponseBuilder;
import org.cdpg.dx.common.util.RequestHelper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static iudx.aaa.server.apiserver.util.Constants.USER;


public class OrganizationHandler {

    private static final Logger LOGGER = LogManager.getLogger(iudx.aaa.server.apiserver.OrganizationHandler.class);
    private final OrganizationService organizationService;


    public OrganizationHandler(OrganizationService organizationService, KeycloakHandler keycloakHandler, PgPool pool){
        this.organizationService = organizationService;
    }

    public void updateOrganisationById(RoutingContext ctx) {

        UUID orgId = RequestHelper.getPathParamAsUUID(ctx, "id");
        UpdateOrgDTO updateOrgDTO = RequestHelper.parseBody(ctx, UpdateOrgDTO::fromJson);

        organizationService.updateOrganizationById(orgId, updateOrgDTO)
                .onSuccess(updatedOrg -> ResponseBuilder.sendSuccess(ctx,  updatedOrg))
                .onFailure(err ->{
                    LOGGER.error("Failed to Update Organization id: {}, message: {}", orgId, err.getMessage(), err);
                    ctx.fail(err);
                });
    }

    public void deleteOrganisationById(RoutingContext ctx) {

        UUID orgId = RequestHelper.getPathParamAsUUID(ctx, "id");

        organizationService.deleteOrganization(orgId)
                .onSuccess(updatedOrg -> ResponseBuilder.sendSuccess(ctx,  updatedOrg))
                .onFailure(ctx::fail);

    }

    public void listAllOrganisations(RoutingContext ctx) {

        organizationService.getOrganizations()
                .onSuccess(orgs -> {
                    ResponseBuilder.sendSuccess(ctx,  orgs);
                })
                .onFailure(ctx::fail);

    }

//    public void approveJoinOrganisationRequests(RoutingContext ctx) {
//
//        JsonObject OrgRequestJson = ctx.body().asJsonObject();
//
//        UUID requestId;
//        Status status;
//
//        JsonObject responseObject = OrgRequestJson.copy();
//        responseObject.remove("status");
//
//        requestId = UUID.fromString(OrgRequestJson.getString("req_id"));
//        status = Status.fromString(OrgRequestJson.getString("status"));
//
//
//        organizationService.updateOrganizationJoinRequestStatus(requestId, status)
//                .onSuccess(approved -> {
//                    if(approved){
//
//                        processSuccess(ctx, responseObject, 200, "Approved Organisation Join Request");
//                    }
//                    else {
//
//                        processFailure(ctx, 400, "Request Not Found");
//                    }
//                })
//                .onFailure(err -> processFailure(ctx, 500, "Failed to approve Organisation Join Request"));
//
//    }

    public void getJoinOrganisationRequests(RoutingContext ctx) {

        UUID orgId = RequestHelper.getPathParamAsUUID(ctx, "id");
        organizationService.getOrganizationPendingJoinRequests(orgId)
                .onSuccess(requests -> ResponseBuilder.sendSuccess(ctx,  requests))
                .onFailure(ctx::fail);

//        organizationService.getOrganizationPendingJoinRequests(orgId)
//                .onSuccess(requests -> {
//                    JsonArray jsonArray = new JsonArray();
//                    List<Future> futures = new ArrayList<>();
//                    for (OrganizationJoinRequest req : requests) {
//                        JsonObject requestJson = req.toJson();
//                        String keycloak_id = requestJson.getString("user_id");
//
//                        Future<Void> future = keycloakHandler.getUsernameByKeycloakId(keycloak_id)
//                                .onSuccess(userdetails -> {
//                                    requestJson.put("requested_by_username", userdetails.getString("username"));
//                                    requestJson.put("requested_by_email", userdetails.getString("email"));
//                                })
//                                .onFailure(err -> {
//                                    LOGGER.error("Failed to fetch username for keycloak id: " + keycloak_id);
//                                })
//                                .mapEmpty();
//                        futures.add(future);
//                        jsonArray.add(requestJson);
//                    }
//                    CompositeFuture.all(futures)
//                            .onSuccess(v -> processSuccess(ctx, jsonArray, 200, "Retrieved Pending Join Requests"))
//                            .onFailure(err -> processFailure(ctx, 500, "Failed to fetch usernames for pending join requests"));
//                })
//                .onFailure(err -> processFailure(ctx, 500, "Failed to fetch pending join requests"));

    }

    public void joinOrganisationRequest(RoutingContext ctx) {

        JsonObject OrgRequestJson = ctx.body().asJsonObject();
        OrganizationJoinRequest organizationJoinRequest;
        User user = ctx.get(USER);
        OrgRequestJson.put("user_id", user.getUserId());

        System.out.println("OrgRequestJson: " + OrgRequestJson);

        try {
            organizationJoinRequest = OrganizationJoinRequest.fromJson(OrgRequestJson);

        } catch (Exception e) {
            processFailure(ctx, 400, "Invalid request payload: " + e.getMessage());
            return;
        }

        keycloakHandler.getUsernameByKeycloakId(user.getUserId())
                .compose(userdetails -> {
                    System.out.println("User details: " + userdetails);
                    return pool.withConnection(conn ->
                            conn.preparedQuery("SELECT COUNT(*) FROM aaa.users WHERE id = $1")
                                    .execute(Tuple.of(UUID.fromString(user.getUserId())))
                                    .compose(rows -> {
                                        if (rows.iterator().next().getInteger(0) == 0) {
                                            // User does not exist, perform the INSERT
                                            return conn.preparedQuery("INSERT INTO aaa.users (id, phone, created_at, updated_at, userinfo) VALUES ($1, $2, $3, $4, $5)")
                                                    .execute(Tuple.of(
                                                            UUID.fromString(user.getUserId()),
                                                            "000000000",
                                                            LocalDateTime.now(),
                                                            LocalDateTime.now(),
                                                            new JsonObject().put("email", userdetails.getString("email"))
                                                    ));
                                        }
                                        // User already exists, return a succeeded future
                                        return Future.succeededFuture();
                                    })
                    );
                })
                .compose(inserted ->
                        organizationService.joinOrganizationRequest(organizationJoinRequest)
                                .onSuccess(createdRequest -> processSuccess(ctx, createdRequest.toJson(), 201, "Created Join request"))
                                .onFailure(err ->
                                        processFailure(ctx, 500, "Internal Server Error: " + err.getMessage())
                                ));

    }

    public void approveOrganisationRequest(RoutingContext ctx) {
        JsonObject OrgRequestJson = ctx.body().asJsonObject();

        UUID requestId = UUID.fromString(OrgRequestJson.getString("req_id"));
        Status status = Status.fromString(OrgRequestJson.getString("status"));

        JsonObject responseObject = OrgRequestJson.copy();
        responseObject.remove("status");

        organizationService.updateOrganizationCreateRequestStatus(requestId, status)
                .compose(approved -> {
                    if (!approved) {
                        return Future.failedFuture("Request Not Found");
                    }
                    return organizationService.getOrganizationCreateRequests(requestId);
                })
                .compose(createRequest -> {
                    JsonObject createRequestJson = createRequest.toJson();
                    return organizationService.getOrganizationByName(createRequestJson.getString("name")).map(org -> new JsonObject()
                            .put("orgJson", org.toJson())
                            .put("createRequestJson", createRequestJson));
                })
                .compose(org -> {
                    JsonObject orgJson = org.getJsonObject("orgJson");
                    JsonObject createRequestJson = org.getJsonObject("createRequestJson");
                    return pool.withConnection(conn ->
                            conn.preparedQuery("INSERT INTO resource_server (id, name, owner_id, url, created_at, updated_at) VALUES ($1, $2, $3, $4, $5, $6)")
                                    .execute(Tuple.of(
                                            orgJson.getString(Constants.ORG_ID),
                                            orgJson.getString(Constants.ORG_NAME),
                                            createRequestJson.getString(Constants.REQUESTED_BY),
                                            orgJson.getString(Constants.ORG_ID),
                                            LocalDateTime.parse(orgJson.getString(Constants.CREATED_AT)),
                                            LocalDateTime.parse(orgJson.getString(Constants.UPDATED_AT))
                                    ))
                    );
                })
                .onSuccess(inserted -> {
                    processSuccess(ctx, responseObject, 200, "Approved Organisation Create Request and updated resource server table");
                })
                .onFailure(err -> {
                    if ("Request Not Found".equals(err.getMessage())) {
                        processFailure(ctx, 400, err.getMessage());
                    } else {
                        processFailure(ctx, 500, "Internal Server Error: " + err.getMessage());
                    }
                });
    }

    public void getOrganisationRequest(RoutingContext ctx) {

        organizationService.getAllPendingOrganizationCreateRequests()
                .onSuccess(requests -> {
                    JsonArray jsonArray = new JsonArray();
                    List<Future> futures = new ArrayList<>();

                    for (OrganizationCreateRequest req : requests) {
                        JsonObject requestJson = req.toJson();
                        String keycloak_id = requestJson.getString("requested_by");

                        Future<Void> future = keycloakHandler.getUsernameByKeycloakId(keycloak_id)
                                .onSuccess(userdetails -> {
                                    requestJson.put("requested_by_username", userdetails.getString("username"));
                                    requestJson.put("requested_by_email", userdetails.getString("email"));
                                })
                                .onFailure(err -> {
                                    LOGGER.error("Failed to fetch username for keycloak id: " + keycloak_id);
                                })
                                .mapEmpty();
                        futures.add(future);
                        jsonArray.add(requestJson);
                    }

                    CompositeFuture.all(futures)
                            .onSuccess(v -> processSuccess(ctx, jsonArray, 200, "Retrieved Pending Create Requests"))
                            .onFailure(err -> processFailure(ctx, 500, "Failed to fetch usernames for pending create requests"));

                })
                .onFailure(err -> processFailure(ctx, 500, "Failed to fetch pending create requests"));

    }

    public void createOrganisationRequest(RoutingContext ctx) {
        JsonObject OrgRequestJson = ctx.body().asJsonObject();
        OrganizationCreateRequest organizationCreateRequest;

        User user = ctx.get(USER);

        OrgRequestJson.put("requested_by", user.getUserId());

        try {
            organizationCreateRequest = OrganizationCreateRequest.fromJson(OrgRequestJson);

        } catch (Exception e) {
            processFailure(ctx, 400, "Invalid request payload: " + e.getMessage());
            return;
        }

        keycloakHandler.getUsernameByKeycloakId(user.getUserId())
                .compose(userdetails -> {
                    System.out.println("User details: " + userdetails);
                    return pool.withConnection(conn ->
                            conn.preparedQuery("SELECT COUNT(*) FROM aaa.users WHERE id = $1")
                                    .execute(Tuple.of(UUID.fromString(user.getUserId())))
                                    .compose(rows -> {
                                        if (rows.iterator().next().getInteger(0) == 0) {
                                            // User does not exist, perform the INSERT
                                            return conn.preparedQuery("INSERT INTO aaa.users (id, phone, created_at, updated_at, userinfo) VALUES ($1, $2, $3, $4, $5)")
                                                    .execute(Tuple.of(
                                                            UUID.fromString(user.getUserId()),
                                                            "000000000",
                                                            LocalDateTime.now(),
                                                            LocalDateTime.now(),
                                                            new JsonObject().put("email", userdetails.getString("email"))
                                                    ));
                                        }
                                        // User already exists, return a succeeded future
                                        return Future.succeededFuture();
                                    })
                    );
                })
                .compose(inserted ->
                        organizationService.createOrganizationRequest(organizationCreateRequest)
                )
                .onSuccess(createdRequest ->
                        processSuccess(ctx, createdRequest.toJson(), 201, "Organisation Successfully Created")
                )
                .onFailure(err ->
                        processFailure(ctx, 500, "Internal Server Error: " + err.getMessage())
                );
    }

    public void deleteOrganisationUserById(RoutingContext ctx) {

        String idParam = String.valueOf(ctx.pathParam("id"));
        UUID orgId;

        orgId = UUID.fromString(idParam);

        JsonObject jsonRequest = ctx.body().asJsonObject();
        UUID userId = UUID.fromString(jsonRequest.getString("user_id"));

        JsonObject responseObject = new JsonObject();

        organizationService.deleteOrganizationUser(orgId, userId)
                .onSuccess(deleted -> {
                    if(deleted){
                        processSuccess(ctx, responseObject, 200, "Deleted Organisation User");
                    }
                    else {
                        processFailure(ctx, 400, "Organisation User Not Found");
                    }
                })
                .onFailure(err -> processFailure(ctx, 500, "Failed to Delete Organisation User"));

    }

    public void getOrganisationUsers(RoutingContext ctx) {

        String idParam = String.valueOf(ctx.pathParam("id"));
        UUID orgId;

        orgId = UUID.fromString(idParam);

        organizationService.getOrganizationUsers(orgId)
                .onSuccess(requests -> {
                    JsonArray jsonArray = new JsonArray();
                    for (OrganizationUser req : requests) {
                        jsonArray.add(req.toJson());
                    }
                    processSuccess(ctx, jsonArray, 200, "Retrieved Organisation Users");
                })
                .onFailure(err -> processFailure(ctx, 500, "Failed to fetch organisation users"));

    }

    public void updateOrganisationUserRole(RoutingContext ctx) {

        JsonObject OrgRequestJson = ctx.body().asJsonObject();

        String OrgidParam = String.valueOf(OrgRequestJson.getString("org_id"));
        UUID orgId;

        orgId = UUID.fromString(OrgidParam);

        String UseridParam = String.valueOf(OrgRequestJson.getString("user_id"));
        UUID user_id;

        user_id = UUID.fromString(UseridParam);

        Role role;
        role = Role.fromString(OrgRequestJson.getString("role"));

        UUID userId = UUID.fromString(OrgRequestJson.getString("user_id"));


        organizationService.updateUserRole(orgId,userId, role)
                .onSuccess(updated -> {
                    if(updated){
                        processSuccess(ctx, new JsonObject(), 200, "Updated Organisation User Role");
                    }
                    else {
                        processFailure(ctx, 400, "Organisation User Not Found");
                    }
                })
                .onFailure(err -> processFailure(ctx, 500, "Failed to Update Organisation User Role"));

    }

    public Future<Void> processFailure(RoutingContext ctx, int statusCode, String msg){

        if(statusCode == 400) {

            return ctx.response()
                    .setStatusCode(statusCode)
                    .putHeader("Content-Type", "application/json")
                    .end(new JsonObject()
                            .put("type", "urn:dx:as:MissingInformation")
                            .put("title", "Not Found")
                            .put("detail", msg)
                            .encode());
        } else if (statusCode == 401) {
            return ctx.response()
                    .setStatusCode(statusCode)
                    .putHeader("Content-Type", "application/json")
                    .end(new JsonObject()
                            .put("type", "urn:dx:as:InvalidAuthenticationToken")
                            .put("title", "Token Authentication Failed")
                            .put("detail", msg)
                            .encode());
        }
        else {
            return ctx.response()
                    .setStatusCode(statusCode)
                    .putHeader("Content-Type", "application/json")
                    .end(new JsonObject()
                            .put("type", "urn:dx:as:InternalServerError")
                            .put("title", "Internal Server Error")
                            .put("detail", msg)
                            .encode());
        }
    }

    public Future<Void> processSuccess(RoutingContext ctx, JsonObject results, int statusCode, String msg){


        JsonObject response = new JsonObject()
                .put("type", "urn:dx:as:Success")
                .put("title",  msg)
                .put("results",  results);

        return ctx.response()
                .setStatusCode(statusCode)
                .putHeader("Content-Type", "application/json")
                .end(response.encode());
    }

    public Future<Void> processSuccess(RoutingContext ctx, JsonArray results, int statusCode, String msg){


        JsonObject response = new JsonObject()
                .put("type", "urn:dx:as:Success")
                .put("title",  msg)
                .put("results",  results);

        return ctx.response()
                .setStatusCode(statusCode)
                .putHeader("Content-Type", "application/json")
                .end(response.encode());
    }
}
