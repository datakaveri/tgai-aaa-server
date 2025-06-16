package org.cdpg.dx.common.request;

import io.vertx.ext.web.RoutingContext;
import java.util.Map;
import java.util.Set;

public class PaginationRequestConfig {
  private final RoutingContext ctx;
  private final Set<String> allowedFilterKeys;
  private final Map<String, String> apiToDbMap;
  private final Map<String, String> additionalFilters;
  private final Set<String> allowedTimeFields;
  private final String defaultTimeField;
  private final String defaultSortBy;
  private final String defaultOrder;
  private final Set<String> allowedSortFields;

  private PaginationRequestConfig(Builder builder) {
    this.ctx = builder.ctx;
    this.allowedFilterKeys = builder.allowedFilterKeys;
    this.apiToDbMap = builder.apiToDbMap;
    this.additionalFilters = builder.additionalFilters;
    this.allowedTimeFields = builder.allowedTimeFields;
    this.defaultTimeField = builder.defaultTimeField;
    this.defaultSortBy = builder.defaultSortBy;
    this.defaultOrder = builder.defaultOrder;
    this.allowedSortFields = builder.allowedSortFields;
  }

  public RoutingContext getCtx() {
    return ctx;
  }

  public Set<String> getAllowedFilterKeys() {
    return allowedFilterKeys;
  }

  public Map<String, String> getApiToDbMap() {
    return apiToDbMap;
  }

  public Map<String, String> getAdditionalFilters() {
    return additionalFilters;
  }

  public Set<String> getAllowedTimeFields() {
    return allowedTimeFields;
  }

  public String getDefaultTimeField() {
    return defaultTimeField;
  }

  public String getDefaultSortBy() {
    return defaultSortBy;
  }

  public String getDefaultOrder() {
    return defaultOrder;
  }

  public Set<String> getAllowedSortFields() {
    return allowedSortFields;
  }

  public static class Builder {
    private RoutingContext ctx;
    private Set<String> allowedFilterKeys;
    private Map<String, String> apiToDbMap;
    private Map<String, String> additionalFilters;
    private Set<String> allowedTimeFields;
    private String defaultTimeField;
    private String defaultSortBy;
    private String defaultOrder;
    private Set<String> allowedSortFields;

    public Builder ctx(RoutingContext ctx) {
      this.ctx = ctx;
      return this;
    }

    public Builder allowedFilterKeys(Set<String> allowedFilterKeys) {
      this.allowedFilterKeys = allowedFilterKeys;
      return this;
    }

    public Builder apiToDbMap(Map<String, String> apiToDbMap) {
      this.apiToDbMap = apiToDbMap;
      return this;
    }

    public Builder additionalFilters(Map<String, String> additionalFilters) {
      this.additionalFilters = additionalFilters;
      return this;
    }

    public Builder allowedTimeFields(Set<String> allowedTimeFields) {
      this.allowedTimeFields = allowedTimeFields;
      return this;
    }

    public Builder defaultTimeField(String defaultTimeField) {
      this.defaultTimeField = defaultTimeField;
      return this;
    }

    public Builder defaultSortBy(String defaultSortBy) {
      this.defaultSortBy = defaultSortBy;
      return this;
    }

    public Builder defaultOrder(String defaultOrder) {
      this.defaultOrder = defaultOrder;
      return this;
    }

    public Builder allowedSortFields(Set<String> allowedSortFields) {
      this.allowedSortFields = allowedSortFields;
      return this;
    }

    public PaginationRequestConfig build() {
      return new PaginationRequestConfig(this);
    }
  }
}
