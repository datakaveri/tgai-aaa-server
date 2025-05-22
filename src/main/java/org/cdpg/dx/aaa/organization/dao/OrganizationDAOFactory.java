package org.cdpg.dx.aaa.organization.dao;

import io.vertx.core.json.JsonObject;
import org.cdpg.dx.aaa.organization.dao.impl.*;
import org.cdpg.dx.aaa.organization.models.ProviderRoleRequest;
import org.cdpg.dx.database.postgres.service.PostgresService;

public class OrganizationDAOFactory {
    private final PostgresService postgresService;

    public OrganizationDAOFactory(PostgresService postgresService) {
        this.postgresService = postgresService;
    }

    public OrganizationDAO organizationDAO() {
        return new OrganizationDAOImpl(postgresService);
    }

    public OrganizationCreateRequestDAO organizationCreateRequest() {
        return new OrganizationCreateRequestDAOImpl(postgresService);
    }

    public OrganizationUserDAO organizationUserDAO() {
        return new OrganizationUserDAOImpl(postgresService);
    }

    public OrganizationJoinRequestDAO organizationJoinRequestDAO() {
        return new OrganizationJoinRequestDAOImpl(postgresService);
    }

    public ProviderRoleRequestDAO providerRoleRequestDAO() {
        return new ProviderRoleRequestDAOImpl(postgresService);
    }

}
