package org.cdpg.dx.aaa.organization.service;


import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.cdpg.dx.aaa.organization.models.*;


import java.util.List;
import java.util.UUID;

public interface OrganizationService {

  Future<JsonObject> getOrganizationCreateRequests(UUID requestId);

  Future<JsonObject> createOrganizationRequest(JsonObject request);

  Future<JsonObject> approveOrganizationCreateRequest(UUID requestId, JsonObject request);

  Future<JsonObject> rejectOrganizationCreateRequest(UUID requestId, JsonObject request);

//  Future<OrganizationJoinRequest> joinOrganizationRequest(UUID organizationId, UUID userId);
//
//  Future<List<OrganizationJoinRequest>> getOrganizationJoinRequests();
//
//  Future<OrganizationJoinRequest> approveOrganizationJoinRequest(UUID requestId);
//
//  Future<Boolean> deleteOrganization(UUID id);
//
//  Future<List<Organization>> getOrganization();
//
//  Future<Organization> updateOrganization(UUID orgId,UpdateOrgDTO updateOrgDTO);

}

