package org.cdpg.dx.aaa.organization.handler;


import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.aaa.audit.util.AuditingHelper;
import org.cdpg.dx.aaa.email.util.EmailHelper;
import org.cdpg.dx.aaa.organization.models.*;
import org.cdpg.dx.aaa.organization.service.OrganizationService;
import org.cdpg.dx.aaa.organization.util.ProviderRoleRequestMapper;
import org.cdpg.dx.aaa.user.service.UserService;
import org.cdpg.dx.auditing.model.AuditLog;
import org.cdpg.dx.common.exception.DxForbiddenException;
import org.cdpg.dx.common.exception.DxNotFoundException;
import org.cdpg.dx.common.response.ResponseBuilder;
import org.cdpg.dx.common.util.RequestHelper;
import org.cdpg.dx.common.util.RoutingContextHelper;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;



public class OrganizationHandler {

    private static final Logger LOGGER = LogManager.getLogger(OrganizationHandler.class);
    private final OrganizationService organizationService;
    private final UserService userService;
    private final EmailHelper emailHelper;


    public OrganizationHandler(OrganizationService organizationService, UserService userService,EmailHelper emailHelper) {
        this.organizationService = organizationService;
        this.userService = userService;
        this.emailHelper = emailHelper;
    }

    public void updateOrganisationById(RoutingContext ctx) {

        UUID orgId = RequestHelper.getPathParamAsUUID(ctx, "id");
        UpdateOrgDTO updateOrgDTO = RequestHelper.parseBody(ctx, UpdateOrgDTO::fromJson);

        organizationService.updateOrganizationById(orgId, updateOrgDTO)
                .onSuccess(updatedOrg ->{
                    AuditLog auditLog = AuditingHelper.createAuditLog(ctx.user(),
                            RoutingContextHelper.getRequestPath(ctx), "PUT", "Update Organization By ID");
                    RoutingContextHelper.setAuditingLog(ctx, auditLog);
                    ResponseBuilder.sendSuccess(ctx, updatedOrg);
                })
                .onFailure(err -> {
                    LOGGER.error("Failed to Update Organization id: {}, message: {}", orgId, err.getMessage(), err);
                    ctx.fail(err);
                });
    }

    public void deleteOrganisationById(RoutingContext ctx) {

        UUID orgId = RequestHelper.getPathParamAsUUID(ctx, "id");
        organizationService.deleteOrganization(orgId)
                .onSuccess(updatedOrg -> {
                    AuditLog auditLog = AuditingHelper.createAuditLog(ctx.user(),
                            RoutingContextHelper.getRequestPath(ctx), "DELETE", "Delete Organization By ID");
                    RoutingContextHelper.setAuditingLog(ctx, auditLog);
                    ResponseBuilder.sendSuccess(ctx, "Organisation deleted Successfully!");
                })
                .onFailure(ctx::fail);

    }

    public void listAllOrganisations(RoutingContext ctx) {

        organizationService.getOrganizations()
                .onSuccess(orgs -> {
                    AuditLog auditLog = AuditingHelper.createAuditLog(ctx.user(),
                            RoutingContextHelper.getRequestPath(ctx), "GET", "List All Organisations");
                    RoutingContextHelper.setAuditingLog(ctx, auditLog);
                    ResponseBuilder.sendSuccess(ctx, orgs);
                })
                .onFailure(ctx::fail);

    }

    public void approveJoinOrganisationRequests(RoutingContext ctx) {

        JsonObject OrgRequestJson = ctx.body().asJsonObject();

        UUID requestId = RequestHelper.getPathParamAsUUID(ctx, "req_id");

        Status status = Status.fromString(OrgRequestJson.getString("status"));


        organizationService.updateOrganizationJoinRequestStatus(requestId, status)
                .onSuccess(approved -> {
                    if (approved) {
                        AuditLog auditLog = AuditingHelper.createAuditLog(ctx.user(),
                                RoutingContextHelper.getRequestPath(ctx), "PUT", "Approved Join Request");
                        RoutingContextHelper.setAuditingLog(ctx, auditLog);
                        ResponseBuilder.sendSuccess(ctx,  "Approved Organisation Join Request");
                    } else {
                        ctx.fail(new DxNotFoundException("Request Not Found"));
                    }
                })
                .onFailure(ctx::fail);

    }

    public void getJoinOrganisationRequests(RoutingContext ctx) {

        UUID orgId = RequestHelper.getPathParamAsUUID(ctx, "id");
        organizationService.getOrganizationPendingJoinRequests(orgId)
                .onSuccess(requests -> {
                    AuditLog auditLog = AuditingHelper.createAuditLog(ctx.user(),
                            RoutingContextHelper.getRequestPath(ctx), "GET", "Get Pending Join Requests");
                    RoutingContextHelper.setAuditingLog(ctx, auditLog);
                    ResponseBuilder.sendSuccess(ctx, requests);
                })
                .onFailure(ctx::fail);
    }

    public void joinOrganisationRequest(RoutingContext ctx) {

        UUID orgId = RequestHelper.getPathParamAsUUID(ctx, "id");
        JsonObject OrgRequestJson = ctx.body().asJsonObject();
        OrganizationJoinRequest organizationJoinRequest;
        User user = ctx.user();
        OrgRequestJson.put("user_id", user.subject());

        String userName = user.principal().getString("name");
        OrgRequestJson.put("user_name", userName);
        OrgRequestJson.put("organization_id", orgId.toString());

        System.out.println("OrgRequestJson: " + OrgRequestJson.encodePrettily());

        organizationJoinRequest = OrganizationJoinRequest.fromJson(OrgRequestJson);

        organizationService.joinOrganizationRequest(organizationJoinRequest)
                .onSuccess(createdRequest -> {
                    AuditLog auditLog = AuditingHelper.createAuditLog(ctx.user(),
                            RoutingContextHelper.getRequestPath(ctx), "POST", "Create Join Organization Request");
                    RoutingContextHelper.setAuditingLog(ctx, auditLog);
                    ResponseBuilder.sendSuccess(ctx, "Created Join request");
                })
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
                    AuditLog auditLog = AuditingHelper.createAuditLog(ctx.user(),
                            RoutingContextHelper.getRequestPath(ctx), "PUT", "Approve Create Organisation Request");
                    RoutingContextHelper.setAuditingLog(ctx, auditLog);
                    ResponseBuilder.sendSuccess(ctx, "Updated Sucessfully");
                })
                .onFailure(ctx::fail);
    }

    public void getOrganisationRequest(RoutingContext ctx) {

        organizationService.getAllOrganizationCreateRequests()
                .onSuccess(requests -> {
                    AuditLog auditLog = AuditingHelper.createAuditLog(ctx.user(),
                            RoutingContextHelper.getRequestPath(ctx), "GET", "Get All Organisation Requests");
                    RoutingContextHelper.setAuditingLog(ctx, auditLog);
                    ResponseBuilder.sendSuccess(ctx, requests);

                })
                .onFailure(ctx::fail);

    }

  public void createOrganisationRequest(RoutingContext ctx) {
    JsonObject OrgRequestJson = ctx.body().asJsonObject();

    User user = ctx.user();

    OrgRequestJson.put("requested_by", user.subject());
    OrgRequestJson.put("user_name", user.principal().getString("name"));

    String userName = user.principal().getString("name");
    String emailId = user.principal().getString("email");
    String orgName = OrgRequestJson.getString("name");

    OrganizationCreateRequest organizationCreateRequest = OrganizationCreateRequest.fromJson(OrgRequestJson);


      organizationService.getAllPendingGrantedOrganizationCreateRequests().
        compose(requests -> {
          for (OrganizationCreateRequest request : requests) {
              System.out.println(request.name());
            if (request.name().equalsIgnoreCase(orgName)) {
              return Future.failedFuture(new DxForbiddenException("Organisation name already exists/ under review"));
            }
          }
            return organizationService.createOrganizationRequest(organizationCreateRequest);
        })
      .compose(requests ->

        ctx.vertx().fileSystem()
          .readFile("src/main/resources/templates/request-create-organization.html")
          .compose(buffer -> {

            String htmlTemplate = buffer.toString(StandardCharsets.UTF_8);

            String adminPortalUrl = "https://staging.catalogue.tgdex.iudx.io/";
            htmlTemplate = htmlTemplate.replace("${adminPortalUrl}", adminPortalUrl);
            htmlTemplate = htmlTemplate.replace("${userName}",userName);
            htmlTemplate = htmlTemplate.replace("${emailId}",emailId);


            String receiver = "sample_email@gmail.com";
            String sender = "no-reply.dev@iudx.io";
            String subject = "New Organization Creation Request";



            return emailHelper.sendMail(sender, receiver, subject, htmlTemplate)
              .map(v -> requests);
          })
      )
      .onSuccess(requests -> {
          AuditLog auditLog = AuditingHelper.createAuditLog(ctx.user(),
                  RoutingContextHelper.getRequestPath(ctx), "POST", "Create Organisation Request");
          RoutingContextHelper.setAuditingLog(ctx, auditLog);
          ResponseBuilder.sendSuccess(ctx, requests);
      })
      .onFailure(ctx::fail);
  }

    public void deleteOrganisationUserById(RoutingContext ctx) {
        UUID  orgId = RequestHelper.getPathParamAsUUID(ctx, "id");
        UUID userId = RequestHelper.getPathParamAsUUID(ctx, "user_id");

        organizationService.deleteOrganizationUser(orgId, userId)
                .onSuccess(deleted -> {
                    if (deleted) {
                        AuditLog auditLog = AuditingHelper.createAuditLog(ctx.user(),
                                RoutingContextHelper.getRequestPath(ctx), "DELETE", "Delete Organisation User");
                        RoutingContextHelper.setAuditingLog(ctx, auditLog);
                        ResponseBuilder.sendSuccess(ctx, "Deleted Organisation User");
                    } else {
                        ctx.fail(new DxNotFoundException( "Organisation User Not Found"));
                    }
                })
                .onFailure(ctx::fail);

    }

    public void getOrganisationUserInfo(RoutingContext ctx) {
        UUID  orgId = RequestHelper.getPathParamAsUUID(ctx, "id");
        UUID userId = RequestHelper.getPathParamAsUUID(ctx, "user_id");

        // TODO check this belogns to the org

        userService.getUserInfoByID(userId).onSuccess(users -> {
                    AuditLog auditLog = AuditingHelper.createAuditLog(ctx.user(),
                            RoutingContextHelper.getRequestPath(ctx), "GET", "Get User Info By ID");
                    RoutingContextHelper.setAuditingLog(ctx, auditLog);
                    ResponseBuilder.sendSuccess(ctx, users);
                })
                .onFailure(ctx::fail);

    }

    public void getOrganisationUsers(RoutingContext ctx) {

        UUID orgId = RequestHelper.getPathParamAsUUID(ctx, "id");

        organizationService.getOrganizationUsers(orgId)
                .onSuccess(users -> {
                    AuditLog auditLog = AuditingHelper.createAuditLog(ctx.user(),
                            RoutingContextHelper.getRequestPath(ctx), "GET", "Get Organisation Users by OrgID");
                    RoutingContextHelper.setAuditingLog(ctx, auditLog);
                    ResponseBuilder.sendSuccess(ctx, users);
                })
                .onFailure(ctx::fail);

    }

    public void updateOrganisationUserRole(RoutingContext ctx) {

        JsonObject OrgRequestJson = ctx.body().asJsonObject();

        Role role;
        role = Role.fromString(OrgRequestJson.getString("role"));

        UUID  orgId = RequestHelper.getPathParamAsUUID(ctx, "id");
        UUID userId = RequestHelper.getPathParamAsUUID(ctx, "user_id");

        organizationService.updateUserRole(orgId, userId, role)
                .onSuccess(updated -> {
                    if (updated) {
                        AuditLog auditLog = AuditingHelper.createAuditLog(ctx.user(),
                                RoutingContextHelper.getRequestPath(ctx), "PUT", "Update Organisation User Role");
                        RoutingContextHelper.setAuditingLog(ctx, auditLog);
                        ResponseBuilder.sendSuccess(ctx,"Updated Organisation User Role");
                    } else {
                        ctx.fail(new DxNotFoundException( "Organisation User Not Found"));
                    }
                })
                .onFailure(ctx::fail);;

    }

    public void createProviderRequest(RoutingContext ctx) {

        User user = ctx.user();
        LOGGER.debug("User: {}", user);
        if (user == null || user.subject() == null || user.principal() == null) {
            ctx.fail(new DxForbiddenException("User not found"));
            return;
        }

        String userId = user.subject();
        String orgID = user.principal().getString("organisation_id");

        if (userId == null || userId.isEmpty()) {
            ctx.fail(new DxForbiddenException("User not found"));
            return;
        }

        if (orgID == null || orgID.isEmpty()) {
            ctx.fail(new DxForbiddenException("User is not part any organisation"));
            return;
        }

        JsonObject req = new JsonObject().
                put("user_id", user.subject()).
                put("organization_id", orgID);

        ProviderRoleRequest providerRoleRequest = ProviderRoleRequest.fromJson(req);

        organizationService.createProviderRequest(providerRoleRequest)
                .onSuccess(requests -> {
                    AuditLog auditLog = AuditingHelper.createAuditLog(ctx.user(),
                            RoutingContextHelper.getRequestPath(ctx), "POST", "Create Provider Role Request");
                    RoutingContextHelper.setAuditingLog(ctx, auditLog);
                    ResponseBuilder.sendSuccess(ctx, "Created Request");
                })
                .onFailure(ctx::fail);
    }

    public void updateProviderRequest(RoutingContext ctx) {
        JsonObject OrgRequestJson = ctx.body().asJsonObject();
        UUID reqId = RequestHelper.getPathParamAsUUID(ctx, "id");
        Status status = Status.fromString(OrgRequestJson.getString("status"));

        organizationService.updateProviderRequestStatus(reqId,status)
                .onSuccess(requests -> {
                    AuditLog auditLog = AuditingHelper.createAuditLog(ctx.user(),
                            RoutingContextHelper.getRequestPath(ctx), "PUT", "Update Provider Role Request");
                    RoutingContextHelper.setAuditingLog(ctx, auditLog);
                    ResponseBuilder.sendSuccess(ctx, "Provider role updated");
                })
                .onFailure(ctx::fail);
    }

    public void getProviderRequest(RoutingContext ctx) {
        User user = ctx.user();
        LOGGER.debug("User: {}", user);
        if (user == null || user.subject() == null || user.principal() == null) {
            ctx.fail(new DxForbiddenException("User not found"));
            return;
        }

        String userId = user.subject();
        String orgID = user.principal().getString("organisation_id");

        if (userId == null || userId.isEmpty()) {
            ctx.fail(new DxForbiddenException("User not found"));
            return;
        }

        if (orgID == null || orgID.isEmpty()) {
            ctx.fail(new DxForbiddenException("User is not part any organisation"));
            return;
        }

        organizationService.getOrganizationUserInfo(UUID.fromString(user.subject())).compose(
                        orgUser -> {
                            if (orgUser == null || orgUser.role() != Role.ADMIN) {
                                return Future.failedFuture(new DxForbiddenException("User not found or not a admin"));
                            }
                            UUID orgId = orgUser.organizationId();
                            return organizationService.getAllPendingProviderRoleRequests(orgId);
                        }
                ).compose(requests -> {
                    List<Future<JsonObject>> enrichedFutures = requests.stream().map(req ->
                            organizationService.getOrganizationUserInfo(req.userId())
                                    .map(userInfo -> ProviderRoleRequestMapper.toJsonWithOrganisationUser(req, userInfo))
                    ).toList();
                    return Future.all(enrichedFutures).map(cf -> {
                        List<JsonObject> resultList = new java.util.ArrayList<>();
                        for (int i = 0; i < cf.size(); i++) {
                            resultList.add(cf.resultAt(i));
                        }
                        return resultList;
                    });
                })
                .onSuccess(enrichedRequests -> {
                    AuditLog auditLog = AuditingHelper.createAuditLog(ctx.user(),
                            RoutingContextHelper.getRequestPath(ctx), "GET", "Get Provider Role Requests");
                    RoutingContextHelper.setAuditingLog(ctx, auditLog);
                    ResponseBuilder.sendSuccess(ctx, enrichedRequests);
                })
                .onFailure(ctx::fail);
    }

  public void createProviderRole(RoutingContext ctx) {
    JsonObject providerRequestJson = ctx.body().asJsonObject();

    ProviderRoleRequest providerRoleRequest = ProviderRoleRequest.fromJson(providerRequestJson);

    organizationService.createProviderRole(providerRoleRequest)
      .onSuccess(org -> ResponseBuilder.sendSuccess(ctx, "Provider role granted successfully"))
      .onFailure(err -> {
        if (err instanceof DxForbiddenException) {
          ctx.fail(new DxForbiddenException("User is not part of any organisation or does not have permission to grant provider role"));
        } else if (err instanceof DxNotFoundException) {
          ctx.fail(new DxNotFoundException("User not found or does not have a pending provider role request"));
        } else {
          ctx.fail(err);
        }
      });

  }

    public void getOrganizationById(RoutingContext ctx) {
        UUID orgId = RequestHelper.getPathParamAsUUID(ctx, "id");

        organizationService.getOrganizationById(orgId)
                .onSuccess(org -> {
                    AuditLog auditLog = AuditingHelper.createAuditLog(ctx.user(),
                            RoutingContextHelper.getRequestPath(ctx), "GET", "Get Organization By ID");
                    RoutingContextHelper.setAuditingLog(ctx, auditLog);
                    ResponseBuilder.sendSuccess(ctx, org.toJson());
                })
                .onFailure(err -> {
                    if (err instanceof DxNotFoundException) {
                        ctx.fail(new DxNotFoundException("Organization not found with id: " + orgId));
                    } else {
                        ctx.fail(err);
                    }
                });
    }

}
