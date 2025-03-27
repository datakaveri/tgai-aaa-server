package org.cdpg.dx.database.postgres.models;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Objects;

@DataObject(generateConverter = true)
public class InsertQuery implements Query {
    private String table;
    private List<String> columns;
    private List<Object> values;

    public InsertQuery() {
        // Default constructor
    }

    public InsertQuery(String table, List<String> columns, List<Object> values) {
        this.table = table;
        this.columns = columns;
        this.values = values;
    }

    public InsertQuery(JsonObject json) {
        InsertQueryConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        InsertQueryConverter.toJson(this, json);
        return json;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }

    public List<Object> getValues() {
        return values;
    }

    public void setValues(List<Object> values) {
        this.values = values;
    }

    @Override
    public String toSQL() {
        String placeholders = "?,".repeat(columns.size()).replaceAll(",$", "");
        return "INSERT INTO " + table + " (" + String.join(", ", columns) + ") VALUES (" + placeholders + ")";
    }

    @Override
    public List<Object> getQueryParams() {
        return values;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InsertQuery that = (InsertQuery) o;
        return Objects.equals(table, that.table) &&
                Objects.equals(columns, that.columns) &&
                Objects.equals(values, that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(table, columns, values);
    }

    @Override
    public String toString() {
        return "InsertQuery{" +
                "table='" + table + '\'' +
                ", columns=" + columns +
                ", values=" + values +
                '}';
    }
}
