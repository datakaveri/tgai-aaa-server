package org.cdpg.dx.aaa.organization.dao.impl;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.cdpg.dx.aaa.organization.dao.OrganizationUserDAO;
import org.cdpg.dx.aaa.organization.models.OrganizationCreateRequest;
import org.cdpg.dx.aaa.organization.models.OrganizationUser;
import org.cdpg.dx.aaa.organization.util.Constants;
import org.cdpg.dx.aaa.organization.models.Role;
import org.cdpg.dx.database.postgres.base.dao.AbstractBaseDAO;
import org.cdpg.dx.database.postgres.models.*;
import org.cdpg.dx.database.postgres.service.PostgresService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.cdpg.dx.aaa.organization.util.Constants.ORG_ID;
import static org.cdpg.dx.aaa.organization.util.Constants.ORG_USER_ID;

public class OrganizationUserDAOImpl extends AbstractBaseDAO<OrganizationUser> implements  OrganizationUserDAO {

  private static final Logger LOGGER = LoggerFactory.getLogger(OrganizationUserDAOImpl.class);

  public OrganizationUserDAOImpl(PostgresService postgresService) {
    super(postgresService, Constants.ORG_USER_TABLE, ORG_USER_ID, OrganizationUser::fromJson);
  }

 // TODO - relook into this
  @Override
  public Future<Boolean> deleteUsersByOrgId(UUID orgId, List<UUID> uuids) {
    if (uuids == null || uuids.isEmpty()) {
      return Future.failedFuture("User IDs list is empty");
    }

    Condition conditions = new Condition(
            List.of(
                    new Condition(Constants.ORGANIZATION_ID, Condition.Operator.EQUALS, List.of(orgId.toString())),
                    new Condition(Constants.USER_ID, Condition.Operator.IN, new ArrayList<>(uuids))
            ),
            Condition.LogicalOperator.AND
    );

    DeleteQuery query = new DeleteQuery(Constants.ORG_USER_TABLE,  new Condition(Constants.ORGANIZATION_ID, Condition.Operator.EQUALS, List.of(orgId)), null, null);

    return postgresService.delete(query)
            .map(QueryResult::isRowsAffected)
            .recover(err -> {
              LOGGER.error("Failed to delete users {} from organization {}: {}", uuids, orgId, err.getMessage());
              return Future.succeededFuture(false);
            });
  }

  @Override
  public Future<Boolean> isOrgAdmin(UUID orgid, UUID userid) {
    Condition conditions = new Condition(
      List.of(
        new Condition(Constants.ORGANIZATION_ID, Condition.Operator.EQUALS, List.of(orgid.toString())),
        new Condition(Constants.USER_ID, Condition.Operator.EQUALS, List.of(userid.toString()))
      ),
      Condition.LogicalOperator.AND
    );
    SelectQuery query = new SelectQuery(Constants.ORG_USER_TABLE, List.of(Constants.ROLE), conditions, null, null, null, null);

    return postgresService.select(query)
      .compose(result -> {
        if (result.getRows().isEmpty()) {
          return Future.failedFuture("User is not part of this organization");
        }

        String roleStr = result.getRows().getJsonObject(0).getString(Constants.ROLE);
        if(roleStr.equals(Role.ADMIN.getRoleName()))
        {
          return Future.succeededFuture(true);
        } else {
          return Future.failedFuture("User is not an admin of this organization");
        }
      }).recover(err -> {
        LOGGER.error("Error executing select query for org {} and user {}: {}", orgid, userid, err.getMessage());
        return Future.failedFuture(err);
      });
  }


}
