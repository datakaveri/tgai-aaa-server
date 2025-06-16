package org.cdpg.dx.aaa.organization.service;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.cdpg.dx.aaa.organization.dao.*;
import org.cdpg.dx.aaa.organization.models.*;
import org.cdpg.dx.aaa.organization.util.Constants;
import org.cdpg.dx.auth.authorization.model.DxRole;
import org.cdpg.dx.common.exception.*;
import org.cdpg.dx.common.request.PaginatedRequest;
import org.cdpg.dx.database.postgres.models.PaginatedResult;
import org.cdpg.dx.keyclock.service.KeycloakUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Map;

public class OrganizationServiceImpl implements OrganizationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrganizationServiceImpl.class);

    private final OrganizationCreateRequestDAO createRequestDAO;
    private final OrganizationUserDAO orgUserDAO;
    private final OrganizationDAO orgDAO;
    private final OrganizationJoinRequestDAO joinRequestDAO;
    private final ProviderRoleRequestDAO providerRequestDAO;
    private final KeycloakUserService keycloakUserService;

    public OrganizationServiceImpl(OrganizationDAOFactory factory, KeycloakUserService keycloakUserService) {
        this.createRequestDAO = factory.organizationCreateRequest();
        this.orgUserDAO = factory.organizationUserDAO();
        this.orgDAO = factory.organizationDAO();
        this.joinRequestDAO = factory.organizationJoinRequestDAO();
        this.providerRequestDAO = factory.providerRoleRequestDAO();
        this.keycloakUserService = keycloakUserService;
    }

    @Override
    public Future<OrganizationCreateRequest> createOrganizationRequest(OrganizationCreateRequest request) {
        return createRequestDAO.create(request);
    }

    @Override
    public Future<List<OrganizationCreateRequest>> getAllPendingGrantedOrganizationCreateRequests() {
        Map<String, Object> filterMapPending = Map.of(
                Constants.STATUS, Status.PENDING.getStatus()
        );
        Map<String, Object> filterMapGranted = Map.of(
                Constants.STATUS, Status.PENDING.getStatus()
        );

        Future<List<OrganizationCreateRequest>> pendingFuture = createRequestDAO.getAllWithFilters(filterMapPending);
        Future<List<OrganizationCreateRequest>> grantedFuture = createRequestDAO.getAllWithFilters(filterMapGranted);

        return Future.all(pendingFuture, grantedFuture)
                .map(cf -> {
                    List<OrganizationCreateRequest> merged = new ArrayList<>();
                    merged.addAll(cf.resultAt(0));
                    merged.addAll(cf.resultAt(1));
                    return merged;
                });
    }

    @Override
    public Future<List<OrganizationCreateRequest>> getAllOrganizationCreateRequests() {
        return createRequestDAO.getAll();
    }

    @Override
    public Future<OrganizationCreateRequest> getOrganizationCreateRequests(UUID requestId) {
        return createRequestDAO.get(requestId);
    }

    @Override
    public Future<Boolean> updateOrganizationCreateRequestStatus(UUID requestId, Status status) {
        Map<String, Object> conditionMap = Map.of(
                Constants.ORG_CREATE_ID, requestId.toString()
        );
        Map<String, Object> updateDataMap = Map.of(
                Constants.STATUS, status.getStatus()
        );

        return createRequestDAO.update(conditionMap, updateDataMap).
                compose(updated -> {
                    if (!updated) return Future.failedFuture("Unexpected Error");
                    if (Status.GRANTED.getStatus().equals(status.getStatus())) {
                        return createOrganizationFromRequest(requestId);
                    }
                    return Future.succeededFuture(true);
                }).recover(dxEx -> {
                    if (dxEx instanceof NoRowFoundException) {
                        return Future.failedFuture(
                                new DxNotFoundException("No request found with given ID", dxEx)
                        );
                    }
                    return Future.failedFuture(dxEx);
                });

    }


    private Future<Boolean> createOrganizationFromRequest(UUID requestId) {
        return createRequestDAO.get(requestId)
                .compose(request -> {
                    Organization org = new Organization(
                            null,
                            request.name(),
                            request.logoPath(),
                            request.entityType(),
                            request.orgSector(),
                            request.websiteLink(),
                            request.address(),
                            request.certificatePath(),
                            request.pancardPath(),
                            request.relevantDocPath(),
                            request.orgDocuments(),
                      null,
                      null
                    );
                    return orgDAO.create(org)
                            .compose(createdOrg ->
                            {
                                OrganizationUser orgUser = new OrganizationUser(
                                        null,
                                        createdOrg.id(),
                                        request.requestedBy(),
                                        request.userName(),
                                        Role.ADMIN,
                                        request.jobTitle(),
                                        request.empId(),
                                        request.orgManagerphoneNo(),
                                        null,
                                        null
                                );
                                //TODO need to revert the update status and create organisation request if fails
                                return Future.all(
                                        keycloakUserService.addRoleToUser(request.requestedBy(), DxRole.ORG_ADMIN),
                                        keycloakUserService.setOrganisationDetails(request.requestedBy(), createdOrg.id(), createdOrg.orgName())
                                ).compose(compositeResult -> {
                                    boolean roleAssigned = compositeResult.resultAt(0);
                                    boolean orgDetailsSet = compositeResult.resultAt(1);

                                    if (!roleAssigned) {
                                        return Future.failedFuture("Failed to assign ORG_ADMIN role to user");
                                    }

                                    if (!orgDetailsSet) {
                                        return Future.failedFuture("Failed to set organization details for user");
                                    }

                                    return orgUserDAO.create(orgUser).map(user -> true);
                                });

                            });
                });
    }

    @Override
    public Future<Organization> getOrganizationById(UUID orgId) {
        return orgDAO.get(orgId);
    }

    @Override
    public Future<List<Organization>> getOrganizations() {
        return orgDAO.getAll();
    }

    @Override
    public Future<PaginatedResult<Organization>> getOrganizations(PaginatedRequest paginatedRequest) {
        return orgDAO.getAll(paginatedRequest);
    }

    @Override
    public Future<Organization> updateOrganizationById(UUID orgId, UpdateOrgDTO updateOrgDTO) {

        Map<String, Object> conditionMap = Map.of(
                Constants.ORG_CREATE_ID, orgId.toString()
        );
        Map<String, Object> updateDataMap = updateOrgDTO.toNonEmptyFieldsMap();

        return orgDAO.update(conditionMap, updateDataMap).compose(
                updated -> orgDAO.get(orgId)
        );
    }

    @Override
    public Future<Boolean> deleteOrganization(UUID orgId) {
        return orgDAO.delete(orgId);
    }

    @Override
    public Future<OrganizationJoinRequest> joinOrganizationRequest(OrganizationJoinRequest organizationJoinRequest) {
        return joinRequestDAO.create(organizationJoinRequest);
    }


    @Override
    public Future<Boolean> updateOrganizationJoinRequestStatus(UUID requestId, Status status) {
        Map<String, Object> conditionMap = Map.of(
                Constants.ORG_JOIN_ID, requestId.toString()
        );
        Map<String, Object> updateDataMap = Map.of(
                Constants.STATUS, status.getStatus()
        );

        return joinRequestDAO.update(conditionMap, updateDataMap)
                .compose(approved -> {
                    if (!approved) return Future.succeededFuture(false);
                    if (Status.GRANTED.getStatus().equals(status.getStatus())) {
                        return addUserToOrganizationFromRequest(requestId);
                    }
                    return Future.succeededFuture(true);
                }).recover(err -> {
                    BaseDxException dxEx = BaseDxException.from(err);
                    if (dxEx instanceof NoRowFoundException) {
                        return Future.failedFuture(
                                new DxNotFoundException("No request found with given ID", dxEx)
                        );
                    }
                    return Future.failedFuture(dxEx);
                });
    }

    private Future<Boolean> addUserToOrganizationFromRequest(UUID requestId) {
        return joinRequestDAO.get(requestId)
                .compose(joinRequest -> {
                    UUID orgId = joinRequest.organizationId();
                    UUID userId = joinRequest.userId();

                    return orgDAO.get(orgId)
                            .compose(organization -> {
                                OrganizationUser newOrgUser = new OrganizationUser(
                                        null,
                                        orgId,
                                        userId,
                                        joinRequest.userName(),
                                        Role.USER,
                                        joinRequest.jobTitle(),
                                        joinRequest.empId(),
                                        null,
                                        null,
                                        null
                                );

                                return orgUserDAO.create(newOrgUser)
                                        .compose(createdUser ->
                                                // Call Keycloak to set organization details
                                                keycloakUserService.setOrganisationDetails(
                                                        userId,
                                                        orgId,
                                                        organization.orgName()
                                                )
                                        )
                                        .compose(success -> {
                                            if (!success) {
                                                return Future.failedFuture("Failed to set organization details in Keycloak");
                                            }
                                            return Future.succeededFuture(true);
                                        });
                            });
                });
    }

    @Override
    public Future<List<OrganizationJoinRequest>> getOrganizationPendingJoinRequests(UUID orgId) {
        //TODO need to create another funtion for this
        Map<String, Object> filterMap = Map.of(
                Constants.ORGANIZATION_ID, orgId.toString()
        );

        return joinRequestDAO.getAllWithFilters(filterMap);
    }

    @Override
    public Future<List<OrganizationUser>> getOrganizationUsers(UUID orgId) {
        Map<String, Object> filterMap = Map.of(Constants.ORGANIZATION_ID, orgId.toString());
        return orgUserDAO.getAllWithFilters(filterMap);
    }


    @Override
    public Future<Boolean> updateUserRole(UUID orgId, UUID userId, Role role) {

        Map<String, Object> conditionMap = Map.of(
                Constants.ORGANIZATION_ID, orgId.toString(),
                Constants.USER_ID, userId.toString()
        );


        Map<String, Object> updateDataMap = Map.of(
                Constants.ROLE, role.getRoleName()
        );

        return orgUserDAO.update(conditionMap, updateDataMap)
                .recover(err -> {
                    BaseDxException dxEx = BaseDxException.from(err);
                    if (dxEx instanceof NoRowFoundException) {
                        return Future.failedFuture(
                                new DxNotFoundException("No matching user found in organization", dxEx)
                        );
                    }
                    return Future.failedFuture(dxEx);
                });
    }

    @Override
    public Future<Boolean> isOrgAdmin(UUID orgid, UUID userid) {
        return orgUserDAO.isOrgAdmin(orgid, userid);
    }

    @Override
    public Future<Boolean> deleteOrganizationUser(UUID orgId, UUID userId) {
        return orgUserDAO.deleteUserByOrgId(orgId, userId);
    }


    @Override
    public Future<OrganizationUser> getOrganizationUserInfo(UUID userId) {
        Map<String, Object> filterMap = Map.of(Constants.USER_ID, userId.toString());
        return orgUserDAO.getAllWithFilters(filterMap).compose(orgUserList -> {
            if (orgUserList.isEmpty()) {
                return Future.failedFuture(new DxNotFoundException("No user found !"));
            } else if (orgUserList.size() == 1) {
                return Future.succeededFuture(orgUserList.get(0));
            } else {
                LOGGER.error("multiple org users found");
                return Future.failedFuture(new DxPgException("Some thing went wrong"));
            }
        }).recover(
                err -> {
                    // Log or transform the error if needed
                    return Future.failedFuture(new DxPgException("Some thing went wrong", err));
                });
    }

    @Override
    public Future<ProviderRoleRequest> createProviderRequest(ProviderRoleRequest providerRoleRequest) {
        return providerRequestDAO.create(providerRoleRequest);
    }

    @Override
    public Future<Boolean> updateProviderRequestStatus(UUID requestId, Status status) {
        Map<String, Object> conditionMap = Map.of(
                Constants.ORG_CREATE_ID, requestId.toString()
        );
        Map<String, Object> updateDataMap = Map.of(
                Constants.STATUS, status.getStatus()
        );

        return providerRequestDAO.update(conditionMap, updateDataMap)
                .compose(updated -> {
                    if (!updated) {
                        return Future.failedFuture(new DxNotFoundException("Request not found"));
                    }
                    //Update in KC
                    if (Status.GRANTED.getStatus().equals(status.getStatus())) {
                        return providerRequestDAO.get(requestId)
                                .compose(providerRequest -> {
                                    // Update role in Keycloak
                                    return keycloakUserService.addRoleToUser(
                                                    providerRequest.userId(),
                                                    DxRole.PROVIDER
                                            )
                                            .compose(success -> {
                                                if (!success) {
                                                    return Future.failedFuture("Failed to assign PROVIDER role in Keycloak");
                                                }
                                                return Future.succeededFuture(true);
                                            });
                                });
                    }

                    return Future.succeededFuture(true);
                })
                .recover(err -> {
                    BaseDxException dxEx = BaseDxException.from(err);
                    if (dxEx instanceof NoRowFoundException) {
                        return Future.failedFuture(
                                new DxNotFoundException("No request found with given ID", dxEx)
                        );
                    }
                    return Future.failedFuture(dxEx);
                });
    }

    public Future<List<ProviderRoleRequest>> getAllPendingProviderRoleRequests(UUID orgId) {
        //TODO need to create another one
        Map<String, Object> filterMap = Map.of(
                Constants.ORGANIZATION_ID, orgId.toString()
        );
        return providerRequestDAO.getAllWithFilters(filterMap);
    }


    @Override
    public Future<Organization> getOrganizationByName(String orgName) {

        Map<String, Object> filterMap = Map.of(Constants.ORG_NAME, orgName);

        return orgDAO.getAllWithFilters(filterMap).compose(orgList -> {
            if (orgList.isEmpty()) {
                return Future.failedFuture("Organization not found with name: " + orgName);
            } else if (orgList.size() == 1) {
                return Future.succeededFuture(orgList.get(0));
            } else {
                return Future.failedFuture("Multiple organizations found with name: " + orgName);
            }
        }).recover(
                err -> {
                    // Log or transform the error if needed
                    return Future.failedFuture("Failed to fetch organization: " + err.getMessage());
                });
    }

    public Future<Boolean> hasPendingProviderRole(UUID userId, UUID orgId){
        Map<String, Object> filterMap = Map.of(
                Constants.STATUS, Status.PENDING.getStatus(), Constants.USER_ID, userId.toString(), Constants.ORGANIZATION_ID, orgId.toString()
        );

        return providerRequestDAO.getAllWithFilters(filterMap)
                .map(list -> !list.isEmpty());
    }

    public Future<List<OrganizationJoinRequest>> getOrganizationJoinRequestsByUser(UUID userId){
        Map<String, Object> pendingFilter = Map.of(
                Constants.USER_ID, userId.toString(),
                Constants.STATUS, Status.PENDING.getStatus()
        );
        Map<String, Object> grantedFilter = Map.of(
                Constants.USER_ID, userId.toString(),
                Constants.STATUS, Status.GRANTED.getStatus()
        );

        Future<List<OrganizationJoinRequest>> pendingFuture = joinRequestDAO.getAllWithFilters(pendingFilter);
        Future<List<OrganizationJoinRequest>> grantedFuture = joinRequestDAO.getAllWithFilters(grantedFilter);

        return Future.all(pendingFuture, grantedFuture)
                .map(cf -> {
                    List<OrganizationJoinRequest> merged = new java.util.ArrayList<>();
                    merged.addAll(cf.resultAt(0));
                    merged.addAll(cf.resultAt(1));
                    return merged;
                });
    }

    public Future<List<OrganizationCreateRequest>> getOrganizationCreateRequestsByUserId(UUID userId){

        Map<String, Object> pendingFilter = Map.of(
                Constants.REQUESTED_BY, userId.toString(),
                Constants.STATUS, Status.PENDING.getStatus()
        );
        Map<String, Object> grantedFilter = Map.of(
                Constants.REQUESTED_BY, userId.toString(),
                Constants.STATUS, Status.GRANTED.getStatus()
        );

        Future<List<OrganizationCreateRequest>> pendingFuture = createRequestDAO.getAllWithFilters(pendingFilter);
        Future<List<OrganizationCreateRequest>> grantedFuture = createRequestDAO.getAllWithFilters(grantedFilter);

        return Future.all(pendingFuture, grantedFuture)
                .map(cf -> {
                    List<OrganizationCreateRequest> merged = new java.util.ArrayList<>();
                    merged.addAll(cf.resultAt(0));
                    merged.addAll(cf.resultAt(1));
                    return merged;
                });
    }

  public Future<Boolean> createProviderRole(ProviderRoleRequest providerRoleRequest) {

    // Checks:
    // 1. User id is present in organisation of org admin
    // 2. If user Id is present in provider request and role status is pending, then change the status to granted

    UUID orgId = providerRoleRequest.orgId();
    UUID userId = providerRoleRequest.userId();
    String status = providerRoleRequest.status();

    Map<String, Object> filterMap = Map.of(
      Constants.ORGANIZATION_ID, orgId.toString(),
      Constants.USER_ID, userId.toString()
    );

    return orgUserDAO.getAllWithFilters(filterMap).compose(ar -> {
      if (ar.isEmpty()) {
        return Future.failedFuture(new DxNotFoundException("User not found in organization"));
      } else {
        Map<String, Object> filterMapProviderRole = Map.of(
          Constants.ORGANIZATION_ID, orgId.toString(),
          Constants.USER_ID, userId.toString()
        );

        return providerRequestDAO.getAllWithFilters(filterMapProviderRole)
          .compose(providerRoleRequests -> {
            if (providerRoleRequests.isEmpty()) {
              return providerRequestDAO.create(providerRoleRequest)
                .compose(createdRequest -> keycloakUserService.addRoleToUser(userId, DxRole.PROVIDER)
                  .compose(success -> {
                    if (!success) {
                      return Future.failedFuture("Failed to assign PROVIDER role in Keycloak");
                    }
                    return Future.succeededFuture(true);
                  }))
                .recover(err -> {
                  BaseDxException dxEx = BaseDxException.from(err);
                  if (dxEx instanceof NoRowFoundException) {
                    return Future.failedFuture(
                      new DxNotFoundException("No User found in Keycloak", dxEx)
                    );
                  }
                  return Future.failedFuture(dxEx);
                });
            } else {
              ProviderRoleRequest existingRequest = providerRoleRequests.get(0);
              if (!Status.PENDING.getStatus().equals(existingRequest.status())) {
                return Future.failedFuture(new DxConflictException("Provider role request is not in PENDING status"));
              }

              Map<String, Object> updateDataMap = Map.of(
                Constants.STATUS, status
              );

              Map<String, Object> conditionMap = Map.of(
                Constants.ORG_CREATE_ID, existingRequest.id().toString()
              );
              return providerRequestDAO.update(conditionMap, updateDataMap)
                .compose(updated -> {
                  if (!updated) {
                    return Future.failedFuture(new DxNotFoundException("Provider role request not found"));
                  }
                  return keycloakUserService.addRoleToUser(userId, DxRole.PROVIDER)
                    .compose(success -> {
                      if (!success) {
                        return Future.failedFuture("Failed to assign PROVIDER role in Keycloak");
                      }
                      return Future.succeededFuture(true);
                    });
                });
            }
          }).recover(err -> {
            BaseDxException dxEx = BaseDxException.from(err);
            if (dxEx instanceof NoRowFoundException) {
              return Future.failedFuture(
                new DxNotFoundException("No user found in organization", dxEx)
              );
            }
            return Future.failedFuture(dxEx);
          });
      }
    });
  }

}
