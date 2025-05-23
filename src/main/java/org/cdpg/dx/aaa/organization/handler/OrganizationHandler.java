package org.cdpg.dx.aaa.organization.handler;


import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.aaa.organization.models.*;
import org.cdpg.dx.aaa.organization.service.OrganizationService;
import org.cdpg.dx.common.response.ResponseBuilder;
import org.cdpg.dx.common.util.RequestHelper;

import java.util.UUID;


public class OrganizationHandler {

    private static final Logger LOGGER = LogManager.getLogger(OrganizationHandler.class);
    private final OrganizationService organizationService;


    public OrganizationHandler(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    public void updateOrganisationById(RoutingContext ctx) {

        UUID orgId = RequestHelper.getPathParamAsUUID(ctx, "id");
        UpdateOrgDTO updateOrgDTO = RequestHelper.parseBody(ctx, UpdateOrgDTO::fromJson);

        organizationService.updateOrganizationById(orgId, updateOrgDTO)
                .onSuccess(updatedOrg -> ResponseBuilder.sendSuccess(ctx, updatedOrg))
                .onFailure(err -> {
                    LOGGER.error("Failed to Update Organization id: {}, message: {}", orgId, err.getMessage(), err);
                    ctx.fail(err);
                });
    }

    public void deleteOrganisationById(RoutingContext ctx) {

        UUID orgId = RequestHelper.getPathParamAsUUID(ctx, "id");
        organizationService.deleteOrganization(orgId)
                .onSuccess(updatedOrg -> ResponseBuilder.sendSuccess(ctx, "Organisation deleted Successfully!"))
                .onFailure(ctx::fail);

    }

    public void listAllOrganisations(RoutingContext ctx) {

        organizationService.getOrganizations()
                .onSuccess(orgs -> {
                    ResponseBuilder.sendSuccess(ctx, orgs);
                })
                .onFailure(ctx::fail);

    }

    public void approveJoinOrganisationRequests(RoutingContext ctx) {

        JsonObject OrgRequestJson = ctx.body().asJsonObject();

        UUID requestId;
        Status status;

        JsonObject responseObject = OrgRequestJson.copy();
        responseObject.remove("status");

        requestId = UUID.fromString(OrgRequestJson.getString("req_id"));
        status = Status.fromString(OrgRequestJson.getString("status"));


        organizationService.updateOrganizationJoinRequestStatus(requestId, status)
                .onSuccess(approved -> {
                    if (approved) {

                        processSuccess(ctx, responseObject, 200, "Approved Organisation Join Request");
                    } else {

                        processFailure(ctx, 400, "Request Not Found");
                    }
                })
                .onFailure(err -> processFailure(ctx, 500, "Failed to approve Organisation Join Request"));

    }

    public void getJoinOrganisationRequests(RoutingContext ctx) {

        UUID orgId = RequestHelper.getPathParamAsUUID(ctx, "id");
        organizationService.getOrganizationPendingJoinRequests(orgId)
                .onSuccess(requests -> ResponseBuilder.sendSuccess(ctx, requests))
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
        User user = ctx.user();
        OrgRequestJson.put("user_id", user.subject());

        String userName = user.principal().getString("user_name");
        OrgRequestJson.put("user_name", userName);

        System.out.println("OrgRequestJson: " + OrgRequestJson);


        organizationJoinRequest = OrganizationJoinRequest.fromJson(OrgRequestJson);

        organizationService.joinOrganizationRequest(organizationJoinRequest)
                .onSuccess(createdRequest -> ResponseBuilder.sendSuccess(ctx, "Created Join request"))
                .onFailure(ctx::fail);

    }

    public void approveOrganisationRequest(RoutingContext ctx) {
        LOGGER.debug("Got request>>>>>>>>>>>>>>>>>>>>");
        JsonObject OrgRequestJson = ctx.body().asJsonObject();

        UUID requestId = UUID.fromString(OrgRequestJson.getString("req_id"));
        Status status = Status.fromString(OrgRequestJson.getString("status"));

        JsonObject responseObject = OrgRequestJson.copy();
        responseObject.remove("status");
        LOGGER.debug("Calling service >>>>>>>>>>>>>>>>>>>>");
        organizationService.updateOrganizationCreateRequestStatus(requestId, status)
                .onSuccess(updated -> {
                    ResponseBuilder.sendSuccess(ctx, "Updated Sucessfully");
                })
                .onFailure(ctx::fail);
    }

    public void getOrganisationRequest(RoutingContext ctx) {

        organizationService.getAllPendingOrganizationCreateRequests()
                .onSuccess(requests -> {
                    ResponseBuilder.sendSuccess(ctx, requests);

                })
                .onFailure(ctx::fail);

    }

    public void createOrganisationRequest(RoutingContext ctx) {
        JsonObject OrgRequestJson = ctx.body().asJsonObject();


        User user = ctx.user();
        OrgRequestJson.put("requested_by", user.subject());

        String userName = user.principal().getString("user_name");
        OrgRequestJson.put("user_name", userName);

       OrganizationCreateRequest organizationCreateRequest = OrganizationCreateRequest.fromJson(OrgRequestJson);


        organizationService.createOrganizationRequest(organizationCreateRequest).
                onSuccess(requests -> {
                    ResponseBuilder.sendSuccess(ctx, requests);

                })
                .onFailure(ctx::fail);
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
                    if (deleted) {
                        processSuccess(ctx, responseObject, 200, "Deleted Organisation User");
                    } else {
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


        organizationService.updateUserRole(orgId, userId, role)
                .onSuccess(updated -> {
                    if (updated) {
                        processSuccess(ctx, new JsonObject(), 200, "Updated Organisation User Role");
                    } else {
                        processFailure(ctx, 400, "Organisation User Not Found");
                    }
                })
                .onFailure(err -> processFailure(ctx, 500, "Failed to Update Organisation User Role"));

    }

    public void createProviderRequest(RoutingContext ctx) {
        JsonObject OrgRequestJson = ctx.body().asJsonObject();
        User user = ctx.user();
        OrgRequestJson.put("user_id", user.subject());
        ProviderRoleRequest providerRoleRequest = ProviderRoleRequest.fromJson(OrgRequestJson);

        organizationService.createProviderRequest(providerRoleRequest)
                .onSuccess(requests -> ResponseBuilder.sendSuccess(ctx, requests))
                .onFailure(ctx::fail);
    }

    public void updateProviderRequest(RoutingContext ctx) {
        JsonObject OrgRequestJson = ctx.body().asJsonObject();

        Status status = Status.fromString(OrgRequestJson.getString("status"));

        organizationService.updateProviderRequestStatus(UUID.fromString(OrgRequestJson.getString("req_id")),status)
                .onSuccess(requests -> ResponseBuilder.sendSuccess(ctx, requests))
                .onFailure(ctx::fail);
    }


    public Future<Void> processFailure(RoutingContext ctx, int statusCode, String msg) {

        if (statusCode == 400) {

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
        } else {
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

    public Future<Void> processSuccess(RoutingContext ctx, JsonObject results, int statusCode, String msg) {


        JsonObject response = new JsonObject()
                .put("type", "urn:dx:as:Success")
                .put("title", msg)
                .put("results", results);

        return ctx.response()
                .setStatusCode(statusCode)
                .putHeader("Content-Type", "application/json")
                .end(response.encode());
    }

    public Future<Void> processSuccess(RoutingContext ctx, JsonArray results, int statusCode, String msg) {


        JsonObject response = new JsonObject()
                .put("type", "urn:dx:as:Success")
                .put("title", msg)
                .put("results", results);

        return ctx.response()
                .setStatusCode(statusCode)
                .putHeader("Content-Type", "application/json")
                .end(response.encode());
    }
}
