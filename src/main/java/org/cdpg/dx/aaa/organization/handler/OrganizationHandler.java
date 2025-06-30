package org.cdpg.dx.aaa.organization.handler;


import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.aaa.audit.util.AuditingHelper;
import org.cdpg.dx.aaa.email.util.EmailComposer;
import org.cdpg.dx.aaa.organization.models.*;
import org.cdpg.dx.aaa.organization.service.OrganizationService;
import org.cdpg.dx.aaa.organization.util.ProviderRoleRequestMapper;
import org.cdpg.dx.aaa.report.service.OrganizationCreateReportService;
import org.cdpg.dx.aaa.user.service.UserService;
import org.cdpg.dx.auditing.model.AuditLog;
import org.cdpg.dx.common.exception.DxConflictException;
import org.cdpg.dx.common.exception.DxForbiddenException;
import org.cdpg.dx.common.exception.DxNotFoundException;
import org.cdpg.dx.common.request.PaginatedRequest;
import org.cdpg.dx.common.request.PaginationRequestBuilder;
import org.cdpg.dx.common.response.ResponseBuilder;
import org.cdpg.dx.common.util.PaginationInfo;
import org.cdpg.dx.common.util.RequestHelper;
import org.cdpg.dx.common.util.RoutingContextHelper;
import org.cdpg.dx.keycloak.service.KeycloakUserService;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.cdpg.dx.aaa.credit.util.Constants.ALLOWED_FILTER_MAP_FOR_CREDIT_REQUEST;
import static org.cdpg.dx.aaa.credit.util.Constants.API_TO_DB_CREDIT_REQUEST;
import static org.cdpg.dx.aaa.organization.config.Constants.*;
import static org.cdpg.dx.database.postgres.util.Constants.DEFAULT_SORTING_ORDER;


public class OrganizationHandler {

    private static final Logger LOGGER = LogManager.getLogger(OrganizationHandler.class);
    private final OrganizationService organizationService;
    private final UserService userService;
    private final EmailComposer emailComposer;
    private final OrganizationCreateReportService organizationCreateReportService;


  public OrganizationHandler(OrganizationService organizationService, UserService userService , EmailComposer emailComposer, OrganizationCreateReportService organizationCreateReportService) {
        this.organizationService = organizationService;
        this.userService = userService;
        this.emailComposer = emailComposer;
        this.organizationCreateReportService = organizationCreateReportService;
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
        PaginatedRequest request = PaginationRequestBuilder.from(ctx)
                .allowedFiltersDbMap(ALLOWED_FILTER_MAP_FOR_ORG)
                .apiToDbMap(API_TO_DB_ORG)
                .allowedTimeFields(Set.of(CREATED_AT))
                .defaultTimeField(CREATED_AT)
                .defaultSort(CREATED_AT, DEFAULT_SORTING_ORDER)
                .allowedSortFields(API_TO_DB_ORG.keySet())
                .build();


        organizationService.getOrganizations(request)
                .onSuccess(orgs -> {
                    AuditLog auditLog = AuditingHelper.createAuditLog(ctx.user(),
                            RoutingContextHelper.getRequestPath(ctx), "GET", "List All Organisations");
                    RoutingContextHelper.setAuditingLog(ctx, auditLog);
                    ResponseBuilder.sendSuccess(ctx, orgs.data().stream()
                      .map(Organization::toFilteredJson).collect(Collectors.toList()), orgs.paginationInfo());

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
                        Future<Void> future = emailComposer.sendUserEmailForOrgJoinRequestApproval(requestId,status);

                    } else {
                        ctx.fail(new DxNotFoundException("Request Not Found"));
                    }
                })
                .onFailure(ctx::fail);

    }

    public void getJoinOrganisationRequests(RoutingContext ctx) {

      UUID orgId = RequestHelper.getPathParamAsUUID(ctx, "id");

      PaginatedRequest request = PaginationRequestBuilder.from(ctx)
        .allowedFiltersDbMap(ALLOWED_FILTER_MAP_FOR_ORG_JOIN_REQUEST)
              .apiToDbMap(API_TO_DB_ORG_JOIN_REQUEST)
        .additionalFilters(Map.of(ORGANIZATION_ID, orgId.toString()))
        .allowedTimeFields(Set.of(REQUESTED_AT))
        .defaultTimeField(REQUESTED_AT)
        .defaultSort(REQUESTED_AT, DEFAULT_SORTING_ORDER)
        .allowedSortFields(API_TO_DB_ORG_JOIN_REQUEST.keySet())
        .build();

    organizationService.getOrganizationPendingJoinRequests(request)
      .compose(result -> {
        List<Future<JsonObject>> enrichedFutures = result.data().stream()
          .map(joinRequest -> userService.getUserInfoByID(joinRequest.userId())
            .map(user -> {
              JsonObject enriched = joinRequest.toJson();
              enriched.put("roles", user.roles());
              return enriched;
            })
            .recover(err -> {
              //TODO remove this
              JsonObject enriched = joinRequest.toJson();
              enriched.put("roles", List.of());
              return Future.succeededFuture(enriched);
            })
          )
          .toList();

        return Future.all(enrichedFutures).map(cf -> {
          List<JsonObject> enrichedList = new java.util.ArrayList<>();
          for (int i = 0; i < cf.size(); i++) {
            enrichedList.add((JsonObject) cf.resultAt(i));
          }
          return Map.entry(enrichedList, result.paginationInfo());
        });
      })
      .onSuccess(entry -> {
        List<JsonObject> enrichedList = entry.getKey();
        PaginationInfo paginationInfo = entry.getValue();

        AuditLog auditLog = AuditingHelper.createAuditLog(
          ctx.user(), RoutingContextHelper.getRequestPath(ctx),
          "GET", "Get Pending Join Requests");
        RoutingContextHelper.setAuditingLog(ctx, auditLog);

        ResponseBuilder.sendSuccess(ctx, enrichedList, paginationInfo);
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


        organizationService.getOrganizationJoinRequestsByUser(UUID.fromString(user.subject()))
                .compose(joinRequests -> {
                    for (OrganizationJoinRequest request : joinRequests) {

                        if (request.organizationId().equals(orgId)) {
                            return Future.failedFuture(new DxConflictException("User already has a pending/ granted join request for this organization"));
                        }
                    }
                    return organizationService.joinOrganizationRequest(organizationJoinRequest)
                            .onSuccess(createdRequest -> {
                                AuditLog auditLog = AuditingHelper.createAuditLog(ctx.user(),
                                        RoutingContextHelper.getRequestPath(ctx), "POST", "Create Join Organization Request");
                                RoutingContextHelper.setAuditingLog(ctx, auditLog);
                                ResponseBuilder.sendSuccess(ctx, "Created Join request");
                                Future<Void> future = emailComposer.sendEmailForJoiningOrg(organizationJoinRequest,user);

                            })
                            .onFailure(ctx::fail);
                })
                .onFailure(ctx::fail);

    }

    public void approveOrganisationRequest(RoutingContext ctx) {
        JsonObject OrgRequestJson = ctx.body().asJsonObject();

        UUID requestId = UUID.fromString(OrgRequestJson.getString("req_id"));
        Status status = Status.fromString(OrgRequestJson.getString("status"));

        JsonObject responseObject = OrgRequestJson.copy();
        responseObject.remove("status");

        organizationService.updateOrganizationCreateRequestStatus(requestId, status)
                .onSuccess(updated -> {
                    AuditLog auditLog = AuditingHelper.createAuditLog(ctx.user(),
                            RoutingContextHelper.getRequestPath(ctx), "PUT", "Approve Create Organisation Request");
                    RoutingContextHelper.setAuditingLog(ctx, auditLog);
                    ResponseBuilder.sendSuccess(ctx, "Updated Sucessfully");
                    Future<Void> future = emailComposer.sendUserEmailForOrgCreateRequestApproval(requestId,status);

                })
                .onFailure(ctx::fail);
    }

    public void getOrganisationRequest(RoutingContext ctx) {
        PaginatedRequest request = PaginationRequestBuilder.from(ctx)
                .allowedFiltersDbMap(ALLOWED_FILTER_MAP_FOR_ORG_CREATE_REQUEST)
                .apiToDbMap(API_TO_DB_ORG_CREATE_REQUEST)
                .allowedTimeFields(Set.of(CREATED_AT))
                .defaultTimeField(CREATED_AT)
                .defaultSort(CREATED_AT, DEFAULT_SORTING_ORDER)
                .allowedSortFields(API_TO_DB_ORG_CREATE_REQUEST.keySet())
                .build();


        organizationService.getAllOrganizationCreateRequests(request)
                .onSuccess(res -> {
                    AuditLog auditLog = AuditingHelper.createAuditLog(ctx.user(),
                            RoutingContextHelper.getRequestPath(ctx), "GET", "Get All Organisation Requests");

                    RoutingContextHelper.setAuditingLog(ctx, auditLog);
                    ResponseBuilder.sendSuccess(ctx, res.data(), res.paginationInfo());

                })
                .onFailure(ctx::fail);

    }

  public void createOrganisationRequest(RoutingContext ctx) {
      JsonObject OrgRequestJson = ctx.body().asJsonObject();

      User user = ctx.user();

      OrgRequestJson.put("requested_by", user.subject());
      OrgRequestJson.put("user_name", user.principal().getString("name"));
      String orgName = OrgRequestJson.getString("name");

      OrganizationCreateRequest organizationCreateRequest = OrganizationCreateRequest.fromJson(OrgRequestJson);

      organizationService.getOrganizationCreateRequestsByUserId(UUID.fromString(user.subject()))
              .compose(createRequests -> {
                  for (OrganizationCreateRequest request : createRequests) {
                      if (request.requestedBy().equals(UUID.fromString(user.subject()))) {
                          return Future.failedFuture(new DxConflictException("Organisation create request already granted/ pending for this user"));
                      }
                  }
                  return organizationService.getAllPendingGrantedOrganizationCreateRequests()
                          .compose(requests -> {
                              for (OrganizationCreateRequest request : requests) {
                                  if (request.name().equalsIgnoreCase(orgName)) {
                                      return Future.failedFuture(new DxConflictException("Organisation name already exists/ under review"));
                                  }
                              }
                              return organizationService.getAllPendingGrantedOrganizationCreateRequests()
                                        .compose(pendingRequests -> {
                                            for (OrganizationCreateRequest request : requests) {
                                                if (request.managerEmail().equalsIgnoreCase(organizationCreateRequest.managerEmail())) {
                                                    return Future.failedFuture(new DxConflictException("Manager email is already in use for another organisation request"));
                                                }
                                            }
                                            return organizationService.createOrganizationRequest(organizationCreateRequest);
                                        }).onFailure(ctx::fail);
                          }).onFailure(ctx::fail);

              })
              .onSuccess(requests -> {
                  AuditLog auditLog = AuditingHelper.createAuditLog(ctx.user(),
                          RoutingContextHelper.getRequestPath(ctx), "POST", "Create Organisation Request");
                  RoutingContextHelper.setAuditingLog(ctx, auditLog);
                  ResponseBuilder.sendSuccess(ctx, requests);
                  emailComposer.sendEmailForCreatingOrg(organizationCreateRequest, user);
              })
              .onFailure(ctx::fail);
  }

    public void deleteOrganisationUserById(RoutingContext ctx) {
        UUID  orgId = RequestHelper.getPathParamAsUUID(ctx, "id");
        UUID userId = RequestHelper.getPathParamAsUUID(ctx, "user_id");

      if (orgId == null || userId == null) {
        ctx.fail(new DxNotFoundException("Organization ID or User ID is missing"));
        return;
      }

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

      PaginatedRequest request = PaginationRequestBuilder.from(ctx)
        .allowedFiltersDbMap(ALLOWED_FILTER_MAP_FOR_ORG_USERS)
              .apiToDbMap(API_TO_DB_ORG_USERS)
        .additionalFilters(Map.of(ORGANIZATION_ID, orgId.toString()))
        .allowedTimeFields(Set.of(CREATED_AT))
        .defaultTimeField(CREATED_AT)
        .defaultSort(CREATED_AT, DEFAULT_SORTING_ORDER)
        .allowedSortFields(API_TO_DB_ORG_USERS.keySet())
        .build();

      AuditLog auditLog = AuditingHelper.createAuditLog(ctx.user(),
                RoutingContextHelper.getRequestPath(ctx), "GET", "Get Organisation Users by OrgID");

        organizationService.getOrganizationUsers(request)
                .compose(res ->
                        userService.enrichWithUserRoles(
                                res.data(),
                                OrganizationUser::userId,
                                OrganizationUser::toJson
                        ).map(enriched -> Map.entry(enriched, res.paginationInfo()))
                )
                .onSuccess(entry -> {
                    RoutingContextHelper.setAuditingLog(ctx, auditLog);
                    ResponseBuilder.sendSuccess(ctx, entry.getKey(), entry.getValue());
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
                    Future<Void> future = emailComposer.sendEmailForProviderRole(providerRoleRequest,user);

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
                    Future<Void> future = emailComposer.sendUserEmailForProviderRoleApproval(reqId,status);

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
                            PaginatedRequest request = PaginationRequestBuilder.from(ctx)
                                    .allowedFiltersDbMap(ALLOWED_FILTER_MAP_FOR_PROVIDER_ROLE_REQUEST)
                                    .apiToDbMap(API_TO_DB_PROVIDER_ROLE_REQUEST)
                                    .additionalFilters(Map.of(ORGANIZATION_ID, orgId.toString()))
                                    .allowedTimeFields(Set.of(CREATED_AT))
                                    .defaultTimeField(CREATED_AT)
                                    .defaultSort(CREATED_AT, DEFAULT_SORTING_ORDER)
                                    .allowedSortFields(API_TO_DB_PROVIDER_ROLE_REQUEST.keySet())
                                    .build();

                            return organizationService.getAllPendingProviderRoleRequests(request);
                        }
                ).compose(requests -> {
            List<Future<JsonObject>> enrichedFutures = requests.data().stream()
              .map(req ->
                organizationService.getOrganizationUserInfo(req.userId())
                  .compose(orgUser ->
                    userService.getUserInfoByID(req.userId())
                      .map(dxUser -> {
                        JsonObject enriched = ProviderRoleRequestMapper.toJsonWithOrganisationUser(req, orgUser);
                        enriched.put("roles", dxUser.roles());
                        return enriched;
                      })
                      .recover(err -> {
                        JsonObject enriched = ProviderRoleRequestMapper.toJsonWithOrganisationUser(req, orgUser);
                        enriched.put("roles", List.of());
                        return Future.succeededFuture(enriched);
                      })
                  )
              ).toList();
                    return Future.all(enrichedFutures).map(cf -> {
                        List<JsonObject> resultList = new java.util.ArrayList<>();
                        for (int i = 0; i < cf.size(); i++) {
                            resultList.add(cf.resultAt(i));
                        }
                        return Map.of(
                                "data", resultList,
                                "paginationInfo", requests.paginationInfo()
                        );
                    });
                })
                .onSuccess(enrichedRequests -> {
                    AuditLog auditLog = AuditingHelper.createAuditLog(ctx.user(),
                            RoutingContextHelper.getRequestPath(ctx), "GET", "Get Provider Role Requests");
                    RoutingContextHelper.setAuditingLog(ctx, auditLog);
                    ResponseBuilder.sendSuccess(ctx, enrichedRequests.get("data"), (PaginationInfo) enrichedRequests.get("paginationInfo"));
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

    public void getOrganizationCreateReport(RoutingContext ctx) {
        HttpServerResponse response = ctx.response();
        response
                .putHeader("Access-Control-Allow-Origin", "*")
                .putHeader("Access-Control-Allow-Headers", "Content-Type, Authorization")
                .putHeader("Access-Control-Allow-Methods", "GET, POST,PUT, DELETE, OPTIONS")
                .putHeader("Content-Type", "text/csv")
                .putHeader("Content-Disposition", "attachment; filename=\"org_create_request_report.csv\"")
                .setChunked(true);

        PaginatedRequest request = PaginationRequestBuilder.from(ctx)
                .allowedFiltersDbMap(ALLOWED_FILTER_MAP_FOR_ORG_CREATE_REQUEST)
                .apiToDbMap(API_TO_DB_ORG_CREATE_REQUEST)
                .allowedTimeFields(Set.of(CREATED_AT))
                .defaultTimeField(CREATED_AT)
                .defaultSort(CREATED_AT, DEFAULT_SORTING_ORDER)
                .allowedSortFields(API_TO_DB_ORG_CREATE_REQUEST.keySet())
                .build();

        organizationCreateReportService
                .streamAdminCsvBatchedCreateRequest(request)
                .onSuccess(
                        csvStream -> {
                            if (csvStream == null) {
                                response.end();
                                return;
                            }
                            csvStream
                                    .exceptionHandler(
                                            err -> {
                                                LOGGER.error("Failed to stream CSV", err);
                                                ctx.fail(err);
                                            })
                                    .handler(buffer -> response.write(buffer))
                                    .endHandler(v -> response.end());
                        })
                .onFailure(
                        err -> {
                            LOGGER.error("Failed to stream CSV", err);
                            ctx.fail(err);
                        });
    }

    public void getOrganizationJoinReport(RoutingContext ctx) {
        HttpServerResponse response = ctx.response();
        UUID orgId = RequestHelper.getPathParamAsUUID(ctx, "id");

        response
                .putHeader("Access-Control-Allow-Origin", "*")
                .putHeader("Access-Control-Allow-Headers", "Content-Type, Authorization")
                .putHeader("Access-Control-Allow-Methods", "GET, POST,PUT, DELETE, OPTIONS")
                .putHeader("Content-Type", "text/csv")
                .putHeader("Content-Disposition", "attachment; filename=\"org_create_request_report.csv\"")
                .setChunked(true);

        PaginatedRequest request = PaginationRequestBuilder.from(ctx)
                .allowedFiltersDbMap(ALLOWED_FILTER_MAP_FOR_ORG_JOIN_REQUEST)
                .apiToDbMap(API_TO_DB_ORG_JOIN_REQUEST)
                .additionalFilters(Map.of(ORGANIZATION_ID, orgId.toString()))
                .allowedTimeFields(Set.of("requested_at"))
                .defaultTimeField("requested_at")
                .defaultSort("requested_at", DEFAULT_SORTING_ORDER)
                .allowedSortFields(API_TO_DB_ORG_JOIN_REQUEST.keySet())
                .build();

        organizationCreateReportService
                .streamAdminCsvBatchedJoinRequest(request)
                .onSuccess(
                        csvStream -> {
                            if (csvStream == null) {
                                response.end();
                                return;
                            }
                            csvStream
                                    .exceptionHandler(
                                            err -> {
                                                LOGGER.error("Failed to stream CSV", err);
                                                ctx.fail(err);
                                            })
                                    .handler(buffer -> response.write(buffer))
                                    .endHandler(v -> response.end());
                        })
                .onFailure(
                        err -> {
                            LOGGER.error("Failed to stream CSV", err);
                            ctx.fail(err);
                        });
    }

    public void getOrganizationReport(RoutingContext ctx) {
        HttpServerResponse response = ctx.response();
        response
                .putHeader("Access-Control-Allow-Origin", "*")
                .putHeader("Access-Control-Allow-Headers", "Content-Type, Authorization")
                .putHeader("Access-Control-Allow-Methods", "GET, POST,PUT, DELETE, OPTIONS")
                .putHeader("Content-Type", "text/csv")
                .putHeader("Content-Disposition", "attachment; filename=\"org_create_request_report.csv\"")
                .setChunked(true);

        PaginatedRequest request = PaginationRequestBuilder.from(ctx)
                .allowedFiltersDbMap(ALLOWED_FILTER_MAP_FOR_ORG)
                .apiToDbMap(API_TO_DB_ORG_USERS)
                .allowedTimeFields(Set.of(CREATED_AT))
                .defaultTimeField(CREATED_AT)
                .defaultSort(CREATED_AT, DEFAULT_SORTING_ORDER)
                .allowedSortFields(API_TO_DB_ORG_USERS.keySet())
                .build();

        organizationCreateReportService
                .streamAdminCsvBatchedOrganization(request)
                .onSuccess(
                        csvStream -> {
                            if (csvStream == null) {
                                response.end();
                                return;
                            }
                            csvStream
                                    .exceptionHandler(
                                            err -> {
                                                LOGGER.error("Failed to stream CSV", err);
                                                ctx.fail(err);
                                            })
                                    .handler(buffer -> response.write(buffer))
                                    .endHandler(v -> response.end());
                        })
                .onFailure(
                        err -> {
                            LOGGER.error("Failed to stream CSV", err);
                            ctx.fail(err);
                        });
    }

    public void getProviderRequestReport(RoutingContext ctx) {
        HttpServerResponse response = ctx.response();
        response
                .putHeader("Access-Control-Allow-Origin", "*")
                .putHeader("Access-Control-Allow-Headers", "Content-Type, Authorization")
                .putHeader("Access-Control-Allow-Methods", "GET, POST,PUT, DELETE, OPTIONS")
                .putHeader("Content-Type", "text/csv")
                .putHeader("Content-Disposition", "attachment; filename=\"org_create_request_report.csv\"")
                .setChunked(true);

        PaginatedRequest request = PaginationRequestBuilder.from(ctx)
                .allowedFiltersDbMap(ALLOWED_FILTER_MAP_FOR_PROVIDER_ROLE_REQUEST)
                .apiToDbMap(API_TO_DB_PROVIDER_ROLE_REQUEST)
                .allowedTimeFields(Set.of(CREATED_AT))
                .defaultTimeField(CREATED_AT)
                .defaultSort(CREATED_AT, DEFAULT_SORTING_ORDER)
                .allowedSortFields(API_TO_DB_PROVIDER_ROLE_REQUEST.keySet())
                .build();

        organizationCreateReportService
                .streamAdminCsvBatchedProviderRequest(request)
                .onSuccess(
                        csvStream -> {
                            if (csvStream == null) {
                                response.end();
                                return;
                            }
                            csvStream
                                    .exceptionHandler(
                                            err -> {
                                                LOGGER.error("Failed to stream CSV", err);
                                                ctx.fail(err);
                                            })
                                    .handler(buffer -> response.write(buffer))
                                    .endHandler(v -> response.end());
                        })
                .onFailure(
                        err -> {
                            LOGGER.error("Failed to stream CSV", err);
                            ctx.fail(err);
                        });
    }

    public void getComputeRoleReport(RoutingContext ctx) {
        HttpServerResponse response = ctx.response();
        response
                .putHeader("Access-Control-Allow-Origin", "*")
                .putHeader("Access-Control-Allow-Headers", "Content-Type, Authorization")
                .putHeader("Access-Control-Allow-Methods", "GET, POST,PUT, DELETE, OPTIONS")
                .putHeader("Content-Type", "text/csv")
                .putHeader("Content-Disposition", "attachment; filename=\"org_create_request_report.csv\"")
                .setChunked(true);

        PaginatedRequest request = PaginationRequestBuilder.from(ctx)
                .allowedFiltersDbMap(ALLOWED_FILTER_MAP_FOR_COMPUTE_ROLE)
                .apiToDbMap(API_TO_DB_COMPUTE_ROLE_REQUEST)
                .allowedTimeFields(Set.of(CREATED_AT))
                .defaultTimeField(CREATED_AT)
                .defaultSort(CREATED_AT, DEFAULT_SORTING_ORDER)
                .allowedSortFields(API_TO_DB_COMPUTE_ROLE_REQUEST.keySet())
                .build();

        organizationCreateReportService
                .streamAdminCsvBatchedComputeRequest(request)
                .onSuccess(
                        csvStream -> {
                            if (csvStream == null) {
                                response.end();
                                return;
                            }
                            csvStream
                                    .exceptionHandler(
                                            err -> {
                                                LOGGER.error("Failed to stream CSV", err);
                                                ctx.fail(err);
                                            })
                                    .handler(buffer -> response.write(buffer))
                                    .endHandler(v -> response.end());
                        })
                .onFailure(
                        err -> {
                            LOGGER.error("Failed to stream CSV", err);
                            ctx.fail(err);
                        });
    }

    public void getCreditRequestReport(RoutingContext ctx) {
        HttpServerResponse response = ctx.response();
        response
                .putHeader("Access-Control-Allow-Origin", "*")
                .putHeader("Access-Control-Allow-Headers", "Content-Type, Authorization")
                .putHeader("Access-Control-Allow-Methods", "GET, POST,PUT, DELETE, OPTIONS")
                .putHeader("Content-Type", "text/csv")
                .putHeader("Content-Disposition", "attachment; filename=\"credit_request_report.csv\"")
                .setChunked(true);

        PaginatedRequest request = PaginationRequestBuilder.from(ctx)
                .allowedFiltersDbMap(ALLOWED_FILTER_MAP_FOR_CREDIT_REQUEST)
                .apiToDbMap(API_TO_DB_CREDIT_REQUEST)
                .allowedTimeFields(Set.of(REQUESTED_AT))
                .defaultTimeField(REQUESTED_AT)
                .defaultSort(REQUESTED_AT, DEFAULT_SORTING_ORDER)
                .allowedSortFields(API_TO_DB_CREDIT_REQUEST.keySet())
                .build();

        organizationCreateReportService
                .streamAdminCsvBatchedCredit(request)
                .onSuccess(
                        csvStream -> {
                            if (csvStream == null) {
                                response.end();
                                return;
                            }
                            csvStream
                                    .exceptionHandler(
                                            err -> {
                                                LOGGER.error("Failed to stream CSV", err);
                                                ctx.fail(err);
                                            })
                                    .handler(buffer -> response.write(buffer))
                                    .endHandler(v -> response.end());
                        })
                .onFailure(
                        err -> {
                            LOGGER.error("Failed to stream CSV", err);
                            ctx.fail(err);
                        });
    }



}
