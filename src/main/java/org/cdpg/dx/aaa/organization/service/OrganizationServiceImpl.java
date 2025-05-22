package org.cdpg.dx.aaa.organization.service;

import io.vertx.core.Future;
import org.cdpg.dx.aaa.organization.dao.*;
import org.cdpg.dx.aaa.organization.models.*;
import org.cdpg.dx.aaa.organization.util.Constants;
import org.cdpg.dx.auth.authorization.model.DxRole;
import org.cdpg.dx.common.exception.BaseDxException;
import org.cdpg.dx.common.exception.DxNotFoundException;
import org.cdpg.dx.common.exception.NoRowFoundException;
import org.cdpg.dx.keyclock.service.KeycloakUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.Map;

public class OrganizationServiceImpl implements OrganizationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrganizationServiceImpl.class);

    private final OrganizationCreateRequestDAO createRequestDAO;
    private final OrganizationUserDAO orgUserDAO;
    private final OrganizationDAO orgDAO;
    private final OrganizationJoinRequestDAO joinRequestDAO;
    private final KeycloakUserService  keycloakUserService;

    public OrganizationServiceImpl(OrganizationDAOFactory factory, KeycloakUserService  keycloakUserService) {
        this.createRequestDAO = factory.organizationCreateRequest();
        this.orgUserDAO = factory.organizationUserDAO();
        this.orgDAO = factory.organizationDAO();
        this.joinRequestDAO = factory.organizationJoinRequestDAO();
        this.keycloakUserService = keycloakUserService;
    }

    @Override
    public Future<OrganizationCreateRequest> createOrganizationRequest(OrganizationCreateRequest request) {
        return createRequestDAO.create(request);
    }

    @Override
    public Future<List<OrganizationCreateRequest>> getAllPendingOrganizationCreateRequests() {
        Map<String, Object> filterMap = Map.of(
                Constants.STATUS, Status.PENDING.getStatus()
        );
        return createRequestDAO.getAllWithFilters(filterMap);
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
              request.relevantDocPath(), null,
            null
          );
          return orgDAO.create(org)
            .compose(createdOrg ->
            {
              OrganizationUser orgUser = new OrganizationUser(
                null,
                createdOrg.id(),
                request.requestedBy(),
                Role.ADMIN,
                request.jobTitle(),
                request.empId(),
                request.orgManagerphoneNo(),
                null,
                null
              );

              // TODO need to improve this , Setting role orgid to user
              keycloakUserService.assignRealmRoleToUser(request.requestedBy().toString(), DxRole.ORG_ADMIN);
              keycloakUserService.setOrganisationDetails(request.requestedBy(), createdOrg.id(), createdOrg.orgName());

              return orgUserDAO.create(orgUser).map(user -> true);

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
                                        Role.USER,
                                        joinRequest.jobTitle(),
                                        joinRequest.empId(),
                                        null,
                                        null,
                                        null
                                );

                                return orgUserDAO.create(newOrgUser)
                                        .map(createdUser -> {
                                            // Step 4: Call Keycloak and wrap boolean response in Future
                                            boolean success = keycloakUserService.setOrganisationDetails(
                                                    userId,
                                                    orgId,
                                                    organization.orgName()
                                            );
                                            return success;
                                        });
                            });
                });
    }

    @Override
    public Future<List<OrganizationJoinRequest>> getOrganizationPendingJoinRequests(UUID orgId) {
      Map<String, Object> filterMap = Map.of(
        Constants.STATUS, Status.PENDING.getStatus()
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
        Constants.ORG_ID, orgId.toString(),
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
    return orgUserDAO.isOrgAdmin(orgid,userid);
  }

  @Override
    public Future<Boolean> deleteOrganizationUser(UUID orgId, UUID userId) {

    return orgUserDAO.deleteUsersByOrgId(orgId, List.of(userId));
    }

    @Override
    public Future<Boolean> deleteOrganizationUsers(UUID orgId, List<UUID> userIds) {
        return orgUserDAO.deleteUsersByOrgId(orgId, userIds);
    }

    @Override
    public Future<OrganizationUser> getOrganizationUserInfo(UUID userId) {
        return orgUserDAO.get(userId);
    }

    @Override
    public Future<Organization> getOrganizationByName(String orgName) {

        Map<String, Object> filterMap = Map.of(Constants.ORG_NAME, orgName);

        return orgDAO.getAllWithFilters(filterMap).compose(orgList -> {
            if (orgList.isEmpty()) {
                return Future.failedFuture("Organization not found with name: " + orgName);
            } else if (orgList.size() > 1) {
                return Future.succeededFuture(orgList.getFirst());
            } else {
                return Future.failedFuture("Multiple organizations found with name: " + orgName);
            }
        }).recover(
        err -> {
            // Log or transform the error if needed
            return Future.failedFuture("Failed to fetch organization: " + err.getMessage());
        });
    }
}
