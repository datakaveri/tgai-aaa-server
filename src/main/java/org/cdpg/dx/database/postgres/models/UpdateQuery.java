package org.cdpg.dx.database.postgres.models;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.stream.Collectors;

@DataObject(generateConverter = true)
public class UpdateQuery implements Query {
    private String table;
    private List<String> columns;
    private List<Object> values;
    private ConditionComponent condition;
    private List<OrderBy> orderBy;
    private Integer limit;

    // Default constructor (Needed for deserialization)
    public UpdateQuery() {}

    // Constructor
    public UpdateQuery(String table, List<String> columns, List<Object> values,
                       ConditionComponent condition, List<OrderBy> orderBy, Integer limit) {
        this.table = table;
        this.columns = columns;
        this.values = values;
        this.condition = condition;
        this.orderBy = orderBy;
        this.limit = limit;
    }

    // JSON Constructor
    public UpdateQuery(JsonObject json) {
        UpdateQueryConverter.fromJson(json, this);  // Use the generated converter
    }

    // Convert to JSON
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        UpdateQueryConverter.toJson(this, json);
        return json;
    }

    // Getters & Setters
    public String getTable() { return table; }
    public void setTable(String table) { this.table = table; }

    public List<String> getColumns() { return columns; }
    public void setColumns(List<String> columns) { this.columns = columns; }

    public List<Object> getValues() { return values; }
    public void setValues(List<Object> values) { this.values = values; }

    public ConditionComponent getCondition() { return condition; }
    public void setCondition(ConditionComponent condition) { this.condition = condition; }

    public List<OrderBy> getOrderBy() { return orderBy; }
    public void setOrderBy(List<OrderBy> orderBy) { this.orderBy = orderBy; }

    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }

    @Override
    public String toSQL() {
        String setClause = columns.stream()
                .map(column -> column + " = ?")
                .collect(Collectors.joining(", "));

        StringBuilder query = new StringBuilder("UPDATE " + table + " SET " + setClause);

        if (condition != null) {
            query.append(" WHERE ").append(condition.toSQL());
        }

        if (orderBy != null && !orderBy.isEmpty()) {
            query.append(" ORDER BY ").append(orderBy.stream()
                    .map(OrderBy::toSQL)
                    .collect(Collectors.joining(", ")));
        }

        if (limit != null) {
            query.append(" LIMIT ").append(limit);
        }

        return query.toString();
    }

    @Override
    public List<Object> getQueryParams() {
        List<Object> params = values.stream().toList();
        if (condition != null) {
            params.addAll(condition.getQueryParams());
        }
        return params;
    }
}
