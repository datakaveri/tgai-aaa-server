package org.cdpg.dx.common.request;

import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.common.exception.DxBadRequestException;
import org.cdpg.dx.database.postgres.models.OrderBy;

import java.util.*;

public class PaginationRequestBuilder {

  private static final Logger LOGGER = LogManager.getLogger(PaginationRequestBuilder.class);

  private final RoutingContext ctx;
  private Map<String, String> allowedFiltersDbMap = Collections.emptyMap();
  private Map<String, Object> additionalFilters = Collections.emptyMap();
  private Set<String> allowedTimeFields = Collections.emptySet();
  private String defaultTimeField = null;
  private Set<String> allowedSortFields = Collections.emptySet();
  private String defaultSortBy = null;
  private String defaultOrder = "desc";

  private PaginationRequestBuilder(RoutingContext ctx) {
    this.ctx = ctx;
  }

  public static PaginationRequestBuilder from(RoutingContext ctx) {
    return new PaginationRequestBuilder(ctx);
  }

  public PaginationRequestBuilder allowedFiltersDbMap(Map<String, String> allowedFiltersDbMap) {
    this.allowedFiltersDbMap = allowedFiltersDbMap != null ? allowedFiltersDbMap : Collections.emptyMap();
    return this;
  }

  public PaginationRequestBuilder additionalFilters(Map<String, Object> additionalFilters) {
    this.additionalFilters = additionalFilters != null ? additionalFilters : Collections.emptyMap();
    return this;
  }

  public PaginationRequestBuilder allowedTimeFields(Set<String> allowedTimeFields) {
    this.allowedTimeFields = allowedTimeFields != null ? allowedTimeFields : Collections.emptySet();
    return this;
  }

  public PaginationRequestBuilder defaultTimeField(String defaultTimeField) {
    this.defaultTimeField = defaultTimeField;
    return this;
  }

  public PaginationRequestBuilder allowedSortFields(Set<String> allowedSortFields) {
    this.allowedSortFields = allowedSortFields != null ? allowedSortFields : Collections.emptySet();
    return this;
  }

  public PaginationRequestBuilder defaultSort(String field, String order) {
    this.defaultSortBy = field;
    if(order != null) {
      this.defaultOrder = order;
    }
    return this;
  }

  public PaginatedRequest build() {
    Set<String> allowedKeys = getAllowedQueryParams();

    for (String param : ctx.queryParams().names()) {
      if (!allowedKeys.contains(param)) {
        throw new DxBadRequestException("Invalid query parameter: " + param);
      }
    }

    int page = parseIntOrDefault(ctx.queryParam("page"), 1);
    int size = parseIntOrDefault(ctx.queryParam("size"), 1000);

    Map<String, Object> mappedFilters = new HashMap<>();

    allowedFiltersDbMap.forEach((apiParam, dbField) -> {
      String value = getFirstQueryParam(apiParam);
      if (value != null) {
        mappedFilters.put(dbField, value);
      }
    });

    if (additionalFilters != null && !additionalFilters.isEmpty()) {
      mappedFilters.putAll(additionalFilters);
    }


    List<TemporalRequest> temporalRequests = extractTemporalRequests();
    List<OrderBy> orderByList = extractSortOrders();

    LOGGER.debug("Pagination built with page: {}, size: {}, filters: {}, temporal: {}, sort: {}",
            page, size, mappedFilters, temporalRequests, orderByList);

    return new PaginatedRequest(page, size, mappedFilters, temporalRequests, orderByList);
  }

  private List<TemporalRequest> extractTemporalRequests() {
    List<TemporalRequest> temporalRequests = new ArrayList<>();
    if (defaultTimeField != null) {
      String time = getFirstQueryParam("time");
      String endtime = getFirstQueryParam("endtime");
      String timerel = getFirstQueryParam("timerel");

      if (endtime != null && time == null) {
        throw new DxBadRequestException("Parameter 'endtime' cannot be used without 'time'.");
      }
      if (time != null && timerel == null) {
        throw new DxBadRequestException("Parameter 'timerel' is required when 'temporal query' is provided.");
      }
      if (timerel != null) {
        TemporalRequest tr = TemporalRequestHelper.buildTemporalRequest(defaultTimeField, timerel, time, endtime);
        if (tr != null) temporalRequests.add(tr);
      }
    }

    for (String timeField : allowedTimeFields) {
      String t = getFirstQueryParam(timeField + "_time");
      String et = getFirstQueryParam(timeField + "_endtime");
      String trl = getFirstQueryParam(timeField + "_timerel");
      if (trl != null) {
        TemporalRequest tr = TemporalRequestHelper.buildTemporalRequest(timeField, trl, t, et);
        if (tr != null) temporalRequests.add(tr);
      }
    }
    return temporalRequests;
  }

  private List<OrderBy> extractSortOrders() {
    List<OrderBy> orderByList = new ArrayList<>();
    String sortParam = getFirstQueryParam("sort");
    final int MAX_SORT_FIELDS = 3;

    if (sortParam != null && !sortParam.isEmpty()) {
      String[] items = sortParam.split(";");
      if (items.length > MAX_SORT_FIELDS) {
        throw new DxBadRequestException("Too many sort fields. Max allowed is " + MAX_SORT_FIELDS);
      }
      for (String item : items) {
        String[] parts = item.split(":");
        if (parts.length != 2) {
          throw new DxBadRequestException("Invalid sort format: " + item + ". Expected field:order");
        }
        String field = parts[0].trim();
        String direction = parts[1].trim().toLowerCase();

        if (!allowedSortFields.contains(field)) {
          throw new DxBadRequestException("Invalid sort field: " + field);
        }
        if (!direction.equals("asc") && !direction.equals("desc")) {
          throw new DxBadRequestException("Invalid sort order: " + direction);
        }

        OrderBy order = new OrderBy(field, OrderBy.Direction.valueOf(direction.toUpperCase()));
        orderByList.add(order);
      }
    } else if (defaultSortBy != null) {
      orderByList.add(new OrderBy(defaultSortBy, OrderBy.Direction.valueOf(defaultOrder.toUpperCase())));
    }
    return orderByList;
  }

  private Set<String> getAllowedQueryParams() {
    Set<String> allowedKeys = new HashSet<>(allowedFiltersDbMap.keySet());
    allowedKeys.add("page");
    allowedKeys.add("size");
    allowedKeys.add("time");
    allowedKeys.add("endtime");
    allowedKeys.add("timerel");

    for (String timeField : allowedTimeFields) {
      allowedKeys.add(timeField + "_time");
      allowedKeys.add(timeField + "_endtime");
      allowedKeys.add(timeField + "_timerel");
    }
    allowedKeys.add("sort");
    return allowedKeys;
  }

  private static int parseIntOrDefault(List<String> values, int defaultValue) {
    if (values == null || values.isEmpty()) return defaultValue;
    try {
      return Integer.parseInt(values.get(0));
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  private String getFirstQueryParam(String paramName) {
    List<String> values = ctx.queryParam(paramName);
    return (values != null && !values.isEmpty()) ? values.get(0) : null;
  }
}
