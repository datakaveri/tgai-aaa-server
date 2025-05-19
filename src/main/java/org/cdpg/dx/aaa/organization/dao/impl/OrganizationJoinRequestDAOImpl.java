package org.cdpg.dx.aaa.organization.dao.impl;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.aaa.organization.dao.OrganizationDAO;
import org.cdpg.dx.aaa.organization.dao.OrganizationJoinRequestDAO;
import org.cdpg.dx.aaa.organization.models.OrganizationCreateRequest;
import org.cdpg.dx.aaa.organization.models.OrganizationJoinRequest;
import org.cdpg.dx.aaa.organization.models.OrganizationUser;
import org.cdpg.dx.aaa.organization.models.Status;
import org.cdpg.dx.aaa.organization.util.Constants;
import org.cdpg.dx.database.postgres.base.dao.AbstractBaseDAO;
import org.cdpg.dx.database.postgres.models.*;
import org.cdpg.dx.database.postgres.service.PostgresService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.cdpg.dx.aaa.organization.util.Constants.ORG_ID;

public class OrganizationJoinRequestDAOImpl extends AbstractBaseDAO<OrganizationJoinRequest> implements OrganizationJoinRequestDAO {

    private static final Logger LOGGER = LogManager.getLogger(OrganizationJoinRequestDAOImpl.class);

    public OrganizationJoinRequestDAOImpl(PostgresService postgresService) {
      super(postgresService, Constants.ORG_JOIN_REQUEST_TABLE, ORG_ID, OrganizationJoinRequest::fromJson);

    }


}
