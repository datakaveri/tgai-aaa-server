package org.cdpg.dx.aaa.organization.service;

import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import iudx.aaa.server.policy.PolicyServiceImpl;
import org.cdpg.dx.aaa.organization.dao.OrganizationCreateRequestDAO;
import org.cdpg.dx.aaa.organization.models.OrgCreateRequestDTO;
import org.cdpg.dx.aaa.organization.models.OrganizationCreateRequest;
import org.cdpg.dx.aaa.organization.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class OrganizationServiceImpl implements OrganizationService{

  private static final Logger LOG = LoggerFactory.getLogger(OrganizationServiceImpl.class);
private final OrganizationCreateRequestDAO organizationCreateRequestDAO;

public OrganizationServiceImpl(OrganizationCreateRequestDAO organizationCreateRequestDAO)
{
  this.organizationCreateRequestDAO = organizationCreateRequestDAO;
}

  @Override
  public Future<JsonObject> getOrganizationCreateRequests(UUID requestId) {

   return organizationCreateRequestDAO.getById(requestId)
     .map(request->{
       if(request == null)
       {
         return new JsonObject().put("error","Request not found");
       }
       return request.toJson();
     });
  }

  @Override
  public Future<JsonObject> createOrganizationRequest(JsonObject json)
  {
    UUID id = UUID.randomUUID();
    OrganizationCreateRequest request = new OrganizationCreateRequest(
      id,
      UUID.fromString(json.getString(Constants.REQUESTED_BY)),
      json.getString(Constants.ORG_NAME),
      json.getString(Constants.ORG_DESCRIPTION),
      json.getString(Constants.DOCUMENTS_PATH),
      "pending",
      Instant.now().toString(),
      Optional.empty()
    );

    return organizationCreateRequestDAO.create(request).map(OrganizationCreateRequest::toJson);
  }

  @Override
  public Future<JsonObject> approveOrganizationCreateRequest(UUID requestId, JsonObject requestJson) {
    return organizationCreateRequestDAO.getById(requestId)
      .compose(request -> {
        if (request == null) {
          return Future.failedFuture("Request not found");
        }
        if (!Constants.STATUS_PENDING.equals(request.getStatus())) {
          return Future.failedFuture("Only pending requests can be approved");
        }

        return organizationCreateRequestDAO.approve(requestId, new OrgCreateRequestDTO(requestJson))
          .map(OrganizationCreateRequest::toJson);
      });
  }

  @Override
  public Future<JsonObject> rejectOrganizationCreateRequest(UUID requestId, JsonObject requestJson) {
    return organizationCreateRequestDAO.getById(requestId)
      .compose(request -> {
        if (request == null) {
          return Future.failedFuture("Request not found");
        }
        if (!Constants.STATUS_PENDING.equals(request.getStatus())) {
          return Future.failedFuture("Only pending requests can be rejected");
        }

        return organizationCreateRequestDAO.reject(requestId, new OrgCreateRequestDTO(requestJson))
          .map(OrganizationCreateRequest::toJson);
      });
  }


}


