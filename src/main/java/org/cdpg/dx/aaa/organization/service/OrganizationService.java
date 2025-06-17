package org.cdpg.dx.aaa.organization.service;


import io.vertx.core.Future;
import org.cdpg.dx.aaa.organization.models.*;
import org.cdpg.dx.aaa.organization.models.Role;
import org.cdpg.dx.aaa.organization.models.Status;
import org.cdpg.dx.common.request.PaginatedRequest;
import org.cdpg.dx.database.postgres.models.PaginatedResult;

import java.util.List;
import java.util.UUID;

public interface OrganizationService {



  // ************ ORGANIZATION CREATE REQUEST *********
  Future<OrganizationCreateRequest> getOrganizationCreateRequests(UUID requestId);

  Future<List<OrganizationCreateRequest>> getOrganizationCreateRequestsByUserId(UUID userId);

  Future<List<OrganizationCreateRequest>> getAllPendingGrantedOrganizationCreateRequests();
  Future <PaginatedResult<OrganizationCreateRequest>> getAllOrganizationCreateRequests(PaginatedRequest request);

  Future<OrganizationCreateRequest> createOrganizationRequest(OrganizationCreateRequest organizationCreateRequest);

  // status enum , adding a new org entry and updating orgCreateRequest
  // update orgUser role
  Future<Boolean> updateOrganizationCreateRequestStatus(UUID requestId, Status status);

//   ************ ORGANIZATION JOIN REQUEST *********

  Future<OrganizationJoinRequest> joinOrganizationRequest(OrganizationJoinRequest organizationJoinRequest);

  Future<PaginatedResult<OrganizationJoinRequest>> getOrganizationPendingJoinRequests(PaginatedRequest paginatedRequest);

  Future<Boolean> updateOrganizationJoinRequestStatus(UUID requestId, Status Status);

  Future<List<OrganizationJoinRequest>> getOrganizationJoinRequestsByUser(UUID userId);

  // ************ ORGANIZATION *********
  Future<Boolean> deleteOrganization(UUID orgId);

  Future<List<Organization>> getOrganizations();

  Future<PaginatedResult<Organization>> getOrganizations(PaginatedRequest paginatedRequest);


  Future<Organization> getOrganizationById(UUID orgId);

  Future<Organization> getOrganizationByName(String orgName);

  //update organization
  Future<Organization> updateOrganizationById(UUID orgId,UpdateOrgDTO updateOrgDTO);

  // ************ ORGANIZATION USERS *********

  //delete from orgUser Table
  Future<Boolean> deleteOrganizationUser(UUID orgId,UUID userId);


  Future<PaginatedResult<OrganizationUser>> getOrganizationUsers(PaginatedRequest paginatedRequest); //UUID orgId

  Future<Boolean> updateUserRole(UUID orgId, UUID userId, Role Role);

  Future<Boolean> isOrgAdmin (UUID orgid, UUID userid);

  Future<OrganizationUser> getOrganizationUserInfo(UUID userId);

  // to check - info about the users
 // Future<OrganizationUser> getOrganizationUserById(UUID userId);

  Future<List<OrganizationUser>> getOrganisationAdminId(UUID orgId);


  //############################Provider Request#############################

  Future<ProviderRoleRequest> createProviderRequest(ProviderRoleRequest providerRoleRequest);

  Future<Boolean> updateProviderRequestStatus(UUID requestId, Status status);

  Future<List<ProviderRoleRequest>> getAllPendingProviderRoleRequests(UUID orgId);

  Future<Boolean> hasPendingProviderRole(UUID userId, UUID orgId);

  Future<Boolean> createProviderRole(ProviderRoleRequest providerRoleRequest);

}

