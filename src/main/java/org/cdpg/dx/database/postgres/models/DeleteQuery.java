package org.cdpg.dx.database.postgres.models;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Objects;

@DataObject(generateConverter = true)
public class DeleteQuery implements Query {
    private  String table;
    private  ConditionComponent condition;
    private  List<OrderBy> orderBy;
    private  Integer limit; // Optional, so keep as Integer

    // Constructor (OrderBy & Limit are optional)
    public DeleteQuery(String table, ConditionComponent condition, List<OrderBy> orderBy, Integer limit) {
        this.table = Objects.requireNonNull(table, "Table name cannot be null");
        this.condition = Objects.requireNonNull(condition, "Condition cannot be null");
        this.orderBy = orderBy != null ? List.copyOf(orderBy) : List.of();
        this.limit = limit; // Can be null (optional)
    }

    // JSON Constructor
    public DeleteQuery(JsonObject json) {
        DeleteQueryConverter.fromJson(json, this);
    }

    // Convert to JSON
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        DeleteQueryConverter.toJson(this, json);
        return json;
    }

    public String getTable() { return table; }
    public ConditionComponent getCondition() { return condition; }
    public List<OrderBy> getOrderBy() { return orderBy; }
    public Integer getLimit() { return limit; }

    @Override
    public String toSQL() {
        StringBuilder query = new StringBuilder("DELETE FROM " + table + " WHERE " + condition.toSQL());

        if (!orderBy.isEmpty()) {
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
        return condition.getQueryParams();
    }
}