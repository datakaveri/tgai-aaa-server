package org.cdpg.dx.aaa.organization.dao;

import io.vertx.core.Future;
import org.cdpg.dx.aaa.organization.models.OrganizationJoinRequest;
import org.cdpg.dx.aaa.organization.util.Status;

import java.util.List;
import java.util.UUID;

public interface OrganizationJoinRequestDAO {
  Future<OrganizationJoinRequest>  join(UUID organizationId, UUID userId);

  Future<List<OrganizationJoinRequest>> getRequests(UUID orgId);

  Future<Boolean> approve(UUID requestId, Status status);
}
