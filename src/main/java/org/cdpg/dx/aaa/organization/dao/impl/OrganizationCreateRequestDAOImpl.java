package org.cdpg.dx.aaa.organization.dao.impl;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.aaa.organization.dao.OrganizationCreateRequestDAO;
import org.cdpg.dx.aaa.organization.dao.OrganizationDAO;
import org.cdpg.dx.aaa.organization.models.Organization;
import org.cdpg.dx.aaa.organization.models.OrganizationCreateRequest;
import org.cdpg.dx.aaa.organization.models.Status;
import org.cdpg.dx.aaa.organization.util.Constants;
import org.cdpg.dx.database.postgres.base.dao.AbstractBaseDAO;
import org.cdpg.dx.database.postgres.models.Condition;
import org.cdpg.dx.database.postgres.models.SelectQuery;
import org.cdpg.dx.database.postgres.models.UpdateQuery;
import org.cdpg.dx.database.postgres.models.InsertQuery;
import org.cdpg.dx.database.postgres.service.PostgresService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.cdpg.dx.aaa.organization.util.Constants.ORGANIZATION_TABLE;
import static org.cdpg.dx.aaa.organization.util.Constants.ORG_ID;

public class OrganizationCreateRequestDAOImpl extends AbstractBaseDAO<OrganizationCreateRequest> implements OrganizationCreateRequestDAO {

    private static final Logger LOGGER = LogManager.getLogger(OrganizationCreateRequestDAOImpl.class);

    public OrganizationCreateRequestDAOImpl(PostgresService postgresService) {
        super(postgresService, Constants.ORG_CREATE_REQUEST_TABLE, ORG_ID, OrganizationCreateRequest::fromJson);
    }

}
