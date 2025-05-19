package org.cdpg.dx.database.postgres.models;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@DataObject(generateConverter = true)
public class UpdateQuery implements Query {
    private  String table;
    private List<String> columns;
    private  List<Object> values;
    private Condition condition;
    private  List<OrderBy> orderBy;
    private  Integer limit;
  private static final Logger LOGGER = LogManager.getLogger(UpdateQuery.class);

    public UpdateQuery(String table, List<String> columns, List<Object> values,
                       Condition condition, List<OrderBy> orderBy, Integer limit) {
        this.table = Objects.requireNonNull(table, "Table name cannot be null");
        this.columns = Objects.requireNonNull(columns, "Columns cannot be null");
        this.values = Objects.requireNonNull(values, "Values cannot be null");
        this.condition = condition;
        this.orderBy = orderBy;
        this.limit = limit;
    }

    public UpdateQuery(UpdateQuery other) {
        this.table = other.getTable();
        this.columns = other.getColumns();
        this.values = other.getValues();
        this.condition = other.getCondition();
        this.orderBy = other.getOrderBy();
        this.limit = other.getLimit();
    }
    public UpdateQuery(){}

    public UpdateQuery(JsonObject json) {
      UpdateQueryConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        UpdateQueryConverter.toJson(this, json);
        return json;
    }


  @Override
  public String toSQL() {
    StringBuilder query = new StringBuilder("UPDATE ").append(table).append(" SET ");

    // Use indexed placeholders for each SET column: $1, $2, $3, ...
    for (int i = 0; i < columns.size(); i++) {
      query.append(columns.get(i)).append(" = $").append(i + 1);
      if (i < columns.size() - 1) {
        query.append(", ");
      }
    }

    // Append condition if present, adjusting its placeholder index
    if (condition != null) {
      // Hacky but safe: replace $1 in condition SQL with correct index
      int condParamIndex = columns.size() + 1;
      String condSql = condition.toSQL().replace("$1", "$" + condParamIndex);
      query.append(" WHERE ").append(condSql);
    }

    if (orderBy != null && !orderBy.isEmpty()) {
      query.append(" ORDER BY ")
        .append(orderBy.stream().map(OrderBy::toSQL).collect(Collectors.joining(", ")));
    }

    if (limit != null) {
      query.append(" LIMIT ").append(limit);
    }

    query.append(" RETURNING *");
    LOGGER.info("Query is : {}", query.toString());

//    String q = query.toString().replace("$1", "'$1'::uuid");

    return query.toString();
  }



  @Override
    public List<Object> getQueryParams() {
//      List<Object> params = values;
      List<Object> params = new ArrayList<>(values);  // Make a mutable copy
      if (condition != null) {
        params.addAll(condition.getQueryParams());
      }
      return params;
    }

    public String getTable() {
        return table;
    }


    public List<String> getColumns() {
        return columns;
    }


    public List<Object> getValues() {
        return values;
    }


    public Condition getCondition() {
        return condition;
    }


    public Integer getLimit() {
        return limit;
    }

  public UpdateQuery setTable(String table) {
    this.table = table;
    return this;
  }

  public UpdateQuery setColumns(List<String> columns) {
    this.columns = columns;
    return this;
  }

  public UpdateQuery setValues(List<Object> values) {
    this.values = values;
    return this;
  }

  public UpdateQuery setCondition(Condition condition) {
    this.condition = condition;
    return this;
  }

  public UpdateQuery setOrderBy(List<OrderBy> orderBy) {
    this.orderBy = orderBy;
    return this;
  }

  public UpdateQuery setLimit(Integer limit) {
    this.limit = limit;
    return this;
  }

  public List<OrderBy> getOrderBy() {
        return orderBy;
    }

}
