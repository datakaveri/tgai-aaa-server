package org.cdpg.dx.aaa.organization.dao.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.aaa.organization.dao.OrganizationCreateRequestDAO;
import org.cdpg.dx.aaa.organization.dao.ProviderRoleRequestDAO;
import org.cdpg.dx.aaa.organization.models.OrganizationCreateRequest;
import org.cdpg.dx.aaa.organization.models.ProviderRoleRequest;
import org.cdpg.dx.aaa.organization.util.Constants;
import org.cdpg.dx.database.postgres.base.dao.AbstractBaseDAO;
import org.cdpg.dx.database.postgres.service.PostgresService;

import static org.cdpg.dx.aaa.organization.util.Constants.ORG_ID;

public class ProviderRoleRequestDAOImpl extends AbstractBaseDAO<ProviderRoleRequest> implements ProviderRoleRequestDAO {

    private static final Logger LOGGER = LogManager.getLogger(ProviderRoleRequestDAOImpl.class);

    public ProviderRoleRequestDAOImpl(PostgresService postgresService) {
        super(postgresService, "provider_requests", "id", ProviderRoleRequest::fromJson);
    }

}