package org.cdpg.dx.aaa.organization.service;

import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.aaa.organization.models.Organization;
import org.cdpg.dx.database.postgres.models.*;
import org.cdpg.dx.database.postgres.service.*;

import java.awt.image.ComponentColorModel;
import java.lang.reflect.RecordComponent;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.Set;

import io.vertx.core.Future;

import javax.lang.model.element.Name;

import static org.cdpg.dx.database.postgres.models.ConditionGroup.LogicalOperator.AND;
import static org.cdpg.dx.database.postgres.models.OrderBy.Direction.DESC;


public class OrganizationServiceImpl implements OrganizationService {


  private final PostgresService postgresService;
  private static final Logger LOGGER = LogManager.getLogger(OrganizationServiceImpl.class);


  public OrganizationServiceImpl(PostgresService postgresService) {
    LOGGER.info("Info: org service impl constructor");
    this.postgresService = postgresService;
  }

  private InsertQuery createInsertQuery(Organization organization)
  {
    String table = "organization";

    Set<String> excludedColumns = Set.of("id", "created_at", "updated_at");

    List<String> columns = Arrays.stream(Organization.class.getRecordComponents())
      .map(RecordComponent::getName)
      .filter(col -> !excludedColumns.contains(col))
      .collect(Collectors.toList());


    List<Object> values = Arrays.stream(Organization.class.getRecordComponents())
      .filter(component->!excludedColumns.contains(component.getName()))
      .map(component ->
      {
        try {
          return component.getAccessor().invoke(organization);
        } catch (Exception e) {
          throw new RuntimeException("Error accessing record component", e);
        }
      })
      .collect(Collectors.toList());


    return new InsertQuery(table,columns,values);
  }

  private DeleteQuery createDeleteQuery(Organization organization)
  {
    String table =  "organization";

    ConditionComponent condition1 = new SimpleCondition("id = ?", List.of(organization.id()));
    ConditionComponent condition2 = new SimpleCondition("created_at < ?", List.of(organization.createdAt()));

    ConditionComponent conditionComponent = new ConditionGroup(List.of(condition1,condition2), AND);
    List<OrderBy> orderBy = List.of(new OrderBy("created_at", DESC));
    Integer limit = 5;
    DeleteQuery deleteQuery = new DeleteQuery(table,conditionComponent,orderBy,limit);

    return deleteQuery;
  }

  private Organization convertQueryResultToOrganization(QueryResult queryResult)
  {
    //List<JsonObject> rows, int totalCount, boolean hasMore

    return new Organization("id","name",LocalDateTime.now(), LocalDateTime.now());
  }


  @Override
  public Future<Organization> createOrganization(Organization organization)
  {

    InsertQuery query = createInsertQuery(organization);
    return postgresService.insert(query).map(this::convertQueryResultToOrganization);

  }

  @Override
  public Future<Organization> deleteOrganization(Organization organization)
  {

    DeleteQuery query = createDeleteQuery(organization);
    return postgresService.delete(query).map(this::convertQueryResultToOrganization);
  }


}
