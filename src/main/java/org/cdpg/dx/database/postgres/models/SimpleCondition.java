package org.cdpg.dx.database.postgres.models;

import java.util.List;

public record SimpleCondition(String condition, List<Object> params) implements ConditionComponent{
  @Override
  public String toSQL() {
    return condition; // Example: "id = ?"
  }

  @Override
  public List<Object> getQueryParams() {
    return params;
  }

}
