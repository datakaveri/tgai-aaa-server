package org.cdpg.dx.database.postgres.models;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.stream.Collectors;

@DataObject(generateConverter = true)
public class ConditionGroup implements ConditionComponent {
    private List<ConditionComponent> conditions;
    private LogicalOperator operator;

    public enum LogicalOperator {
        AND("AND"), OR("OR");

        private final String symbol;

        LogicalOperator(String symbol) {
            this.symbol = symbol;
        }

        public String getSymbol() {
            return symbol;
        }
    }

    // Default constructor (Needed for deserialization)
    public ConditionGroup() {}

    // Constructor
    public ConditionGroup(List<ConditionComponent> conditions, LogicalOperator operator) {
        this.conditions = conditions;
        this.operator = operator;
    }

    // JSON Constructor
    public ConditionGroup(JsonObject json) {
        ConditionGroupConverter.fromJson(json, this); // Use the generated converter
    }

    // Convert to JSON
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        ConditionGroupConverter.toJson(this, json);
        return json;
    }

    // Getters & Setters
    public List<ConditionComponent> getConditions() { return conditions; }
    public void setConditions(List<ConditionComponent> conditions) { this.conditions = conditions; }

    public LogicalOperator getOperator() { return operator; }
    public void setOperator(LogicalOperator operator) { this.operator = operator; }

    @Override
    public String toSQL() {
        return conditions.stream()
                .map(ConditionComponent::toSQL)
                .map(sql -> "(" + sql + ")")  // Ensure correct precedence
                .collect(Collectors.joining(" " + operator.getSymbol() + " "));
    }

    @Override
    public List<Object> getQueryParams() {
        return conditions.stream()
                .flatMap(condition -> condition.getQueryParams().stream())
                .toList();
    }
}