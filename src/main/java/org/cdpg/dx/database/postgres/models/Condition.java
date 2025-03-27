package org.cdpg.dx.database.postgres.models;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.stream.Collectors;

@DataObject(generateConverter = true)
public class Condition implements ConditionComponent {
    private String column;
    private Operator operator;
    private List<Object> values;

    public enum Operator {
        EQUALS("="), NOT_EQUALS("!="), GREATER(">"), LESS("<"), GREATER_EQUALS(">="), LESS_EQUALS("<="),
        LIKE("LIKE"), IN("IN"), NOT_IN("NOT IN"), BETWEEN("BETWEEN"),
        IS_NULL("IS NULL"), IS_NOT_NULL("IS NOT NULL");

        private final String symbol;

        Operator(String symbol) {
            this.symbol = symbol;
        }

        public String getSymbol() {
            return symbol;
        }
    }

    // Default constructor (Needed for deserialization)
    public Condition() {}

    // Constructor
    public Condition(String column, Operator operator, List<Object> values) {
        this.column = column;
        this.operator = operator;
        this.values = values;
    }

    // JSON Constructor
    public Condition(JsonObject json) {
        ConditionConverter.fromJson(json, this); // Use the generated converter
    }

    // Convert to JSON
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        ConditionConverter.toJson(this, json);
        return json;
    }

    // Getters & Setters
    public String getColumn() { return column; }
    public void setColumn(String column) { this.column = column; }

    public Operator getOperator() { return operator; }
    public void setOperator(Operator operator) { this.operator = operator; }

    public List<Object> getValues() { return values; }
    public void setValues(List<Object> values) { this.values = values; }

    @Override
    public String toSQL() {
        return switch (operator) {
            case EQUALS, NOT_EQUALS, GREATER, LESS, GREATER_EQUALS, LESS_EQUALS, LIKE ->
                    column + " " + operator.getSymbol() + " ?";
            case IN, NOT_IN ->
                    column + " " + operator.getSymbol() + " (" +
                            values.stream().map(v -> "?").collect(Collectors.joining(", ")) + ")";
            case BETWEEN ->
                    column + " BETWEEN ? AND ?";
            case IS_NULL, IS_NOT_NULL ->
                    column + " " + operator.getSymbol();
        };
    }

    @Override
    public List<Object> getQueryParams() {
        return values;
    }
}
