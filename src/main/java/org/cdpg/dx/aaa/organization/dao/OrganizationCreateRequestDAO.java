package org.cdpg.dx.aaa.organization.dao;


import io.vertx.core.Future;
import org.cdpg.dx.aaa.organization.models.OrgCreateRequestDTO;
import org.cdpg.dx.aaa.organization.models.OrganizationCreateRequest;

import java.util.UUID;

public interface OrganizationCreateRequestDAO {

  Future<OrganizationCreateRequest> create(OrganizationCreateRequest organizationCreateRequest);

  Future<OrganizationCreateRequest> getById(UUID id);

  Future<OrganizationCreateRequest> approve(UUID id, OrgCreateRequestDTO orgCreateRequestDTO);

  Future<OrganizationCreateRequest> reject(UUID id, OrgCreateRequestDTO orgCreateRequestDTO);
}
