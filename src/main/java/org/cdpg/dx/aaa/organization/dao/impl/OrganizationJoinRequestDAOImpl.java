package org.cdpg.dx.aaa.organization.dao.impl;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.aaa.organization.dao.OrganizationDAO;
import org.cdpg.dx.aaa.organization.dao.OrganizationJoinRequestDAO;
import org.cdpg.dx.aaa.organization.models.OrganizationCreateRequest;
import org.cdpg.dx.aaa.organization.models.OrganizationJoinRequest;
import org.cdpg.dx.aaa.organization.models.Status;
import org.cdpg.dx.aaa.organization.util.Constants;
import org.cdpg.dx.database.postgres.models.*;
import org.cdpg.dx.database.postgres.service.PostgresService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class OrganizationJoinRequestDAOImpl implements OrganizationJoinRequestDAO {

    private static final Logger LOGGER = LogManager.getLogger(OrganizationJoinRequestDAOImpl.class);
    private final PostgresService postgresService;

    public OrganizationJoinRequestDAOImpl(PostgresService postgresService) {
        this.postgresService = postgresService;
    }

    @Override
    public Future<OrganizationJoinRequest> getById(UUID id) {
        SelectQuery selectQuery = new SelectQuery(
                Constants.ORG_JOIN_REQUEST_TABLE,
                List.of("*"),
                new Condition(Constants.ORG_JOIN_ID, Condition.Operator.EQUALS, List.of(id.toString())),
                null, null, null, null
        );

        return postgresService.select(selectQuery)
                .compose(result -> {
                    if (result.getRows().isEmpty()) {
                        return Future.failedFuture("No request found with the given ID.");
                    }
                    return Future.succeededFuture(OrganizationJoinRequest.fromJson(result.getRows().getJsonObject(0)));
                })
                .recover(err -> {
                    LOGGER.error("Error fetching request by ID: {}", err.getMessage(), err);
                    return Future.failedFuture(err);
                });
    }


    @Override
    public Future<List<OrganizationJoinRequest>> getAll(UUID orgId, Status status) {
        Condition conditions = new Condition(
                List.of(
                        new Condition(Constants.ORGANIZATION_ID, Condition.Operator.EQUALS, List.of(orgId.toString())),
                        new Condition(Constants.STATUS, Condition.Operator.EQUALS, List.of(status.getStatus()))
                ),
                Condition.LogicalOperator.AND
        );
        SelectQuery query = new SelectQuery(
                Constants.ORG_JOIN_REQUEST_TABLE,
                List.of("*"),
                conditions,
                null,
                null,
                null,
                null
        );

        return postgresService.select(query)
                .compose(result -> {
                    List<OrganizationJoinRequest> requests = result.getRows().stream()
                            .map(row -> OrganizationJoinRequest.fromJson((JsonObject) row))
                            .collect(Collectors.toList());
                    return Future.succeededFuture(requests);
                })
                .recover(err -> {
                    LOGGER.error("Error fetching join requests: {}", err.getMessage(), err);
                    return Future.failedFuture(err);
                });
    }

    @Override
    public Future<OrganizationJoinRequest> join(OrganizationJoinRequest organizationJoinRequest) {
      InsertQuery query = new InsertQuery();
      query.setTable(Constants.ORG_JOIN_REQUEST_TABLE);
      query.setColumns(List.copyOf(organizationJoinRequest.toNonEmptyFieldsMap().keySet()));
      query.setValues(List.copyOf(organizationJoinRequest.toNonEmptyFieldsMap().values()));

      LOGGER.debug("Insert Query: {}", query.toSQL());
      LOGGER.debug("Query Params: {}", query.getQueryParams());

        return postgresService.insert(query)
                .compose(result -> {
                    if (result.getRows().isEmpty()) {
                        return Future.failedFuture("Insert query failed.");
                    }
                    return Future.succeededFuture(OrganizationJoinRequest.fromJson(result.getRows().getJsonObject(0)));
                })
                .recover(err -> {
                    LOGGER.error("Error inserting join request: {}", err.getMessage(), err);
                    return Future.failedFuture(err);
                });
    }

    @Override
    public Future<Boolean> updateStatus(UUID requestId, Status status) {
        Map<String, Object> updateFields = Map.of(
                Constants.STATUS, status.getStatus(),
                Constants.PROCESSED_AT, Instant.now().toString()
        );

        List<String> columns = List.copyOf(updateFields.keySet());
        List<Object> values = List.copyOf(updateFields.values());

        Condition condition = new Condition(Constants.ORG_JOIN_ID, Condition.Operator.EQUALS, List.of(requestId.toString()));
        UpdateQuery query = new UpdateQuery(Constants.ORG_JOIN_REQUEST_TABLE, columns, values, condition, null, null);

        return postgresService.update(query)
                .compose(result -> {
                    if (result.getRows().isEmpty()) {
                        return Future.failedFuture("Update query returned no rows.");
                    }
                    return Future.succeededFuture(true);
                })
                .recover(err -> {
                    LOGGER.error("Error updating join request status for request {}: {}", requestId, err.getMessage(), err);
                    return Future.failedFuture(err);
                });
    }
}
