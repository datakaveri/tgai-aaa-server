package org.cdpg.dx.common.request;

import io.vertx.core.MultiMap;
import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.common.exception.DxBadRequestException;
import org.cdpg.dx.database.postgres.models.OrderBy;

public class PaginationRequestBuilder {
  private static final Logger LOGGER = LogManager.getLogger(PaginationRequestBuilder.class);

  public static PaginatedRequest fromRoutingContext(PaginationRequestConfig config) {
    MultiMap params = config.getCtx().request().params(true);
    LOGGER.debug("Starting to build PaginatedRequest with parameters: {}", params);

    int page = parsePage(params);
    int size = parseSize(params);
    LOGGER.debug("Parsed pagination parameters - page: {}, size: {}", page, size);

    Map<String, String> filters = extractFilters(params, config);
    LOGGER.debug("Extracted filters: {}", filters);

    List<TemporalRequest> temporalRequests = getTemporalRequests(params, config);
    LOGGER.debug("Extracted temporal requests: {}", temporalRequests);

    List<OrderBy> orderByList = getSortOrders(params, config);
    LOGGER.debug("Extracted order by list: {}", orderByList);

    PaginatedRequest request =
        new PaginatedRequest(page, size, filters, temporalRequests, orderByList);
    LOGGER.debug("PaginatedRequest successfully built: {}", request);
    return request;
  }

  private static int parsePage(MultiMap params) {
    int page = parseIntOrDefault(params.getAll("page"), 1);
    LOGGER.debug("parsePage(): Using page number {}", page);
    return page;
  }

  private static int parseSize(MultiMap params) {
    int size = parseIntOrDefault(params.getAll("size"), 1000);
    LOGGER.debug("parseSize(): Using page size {}", size);
    return size;
  }

  private static Map<String, String> extractFilters(
      MultiMap params, PaginationRequestConfig config) {
    Map<String, String> rawFilters = new HashMap<>();
    for (String key : config.getAllowedFilterKeys()) {
      List<String> values = params.getAll(key);
      if (!values.isEmpty()) {
        rawFilters.put(key, values.get(0));
        LOGGER.debug("Filter found - key: {}, value: {}", key, values.get(0));
      }
    }
    Map<String, String> mapped = FilterMapper.mapFilters(rawFilters, config.getApiToDbMap());
    if (config.getAdditionalFilters() != null) {
      LOGGER.debug("Adding additional filters: {}", config.getAdditionalFilters());
      mapped.putAll(config.getAdditionalFilters());
    }
    LOGGER.debug("Mapped filters after transformation: {}", mapped);
    return mapped;
  }

  private static List<TemporalRequest> getTemporalRequests(
      MultiMap params, PaginationRequestConfig config) {
    List<TemporalRequest> list = new ArrayList<>();

    String time = firstParam(params, "time");
    String endtime = firstParam(params, "endtime");
    String timeRel = firstParam(params, "timerel");

    if (endtime != null && time == null) {
      LOGGER.warn("Parameter 'endtime' provided without 'time'");
      throw new DxBadRequestException("Parameter 'endtime' cannot be used without 'time'.");
    }
    if (time != null && timeRel == null) {
      LOGGER.warn("Parameter 'timerel' missing when 'time' is provided");
      throw new DxBadRequestException(
          "Parameter 'timerel' is required when 'temporal query' is provided.");
    }
    if (timeRel != null) {
      TemporalRequest tr =
          TemporalRequestHelper.buildTemporalRequest(
              config.getDefaultTimeField(), timeRel, time, endtime);
      if (tr != null) {
        LOGGER.debug(
            "Adding temporal request for default field '{}': {}", config.getDefaultTimeField(), tr);
        list.add(tr);
      } else {
        LOGGER.warn("TemporalRequestHelper returned null for default temporal request");
      }
    }

    for (String timeField : config.getAllowedTimeFields()) {
      String t = firstParam(params, timeField + "_time");
      String et = firstParam(params, timeField + "_endtime");
      String trl = firstParam(params, timeField + "_timerel");

      if (trl != null) {
        TemporalRequest tr = TemporalRequestHelper.buildTemporalRequest(timeField, trl, t, et);
        if (tr != null) {
          LOGGER.debug("Adding temporal request for field '{}': {}", timeField, tr);
          list.add(tr);
        } else {
          LOGGER.warn(
              "TemporalRequestHelper returned null for temporal request with field '{}'",
              timeField);
        }
      }
    }

    return list;
  }

  private static List<OrderBy> getSortOrders(MultiMap params, PaginationRequestConfig config) {
    List<OrderBy> orderByList = new ArrayList<>();
    String sort = firstParam(params, "sort");
    final int MAX_SORT_FIELDS = 3;

    LOGGER.debug("Extracting sort orders from parameter: {}", sort);

    if (sort != null && !sort.isEmpty()) {
      String[] sortItems = sort.split(";");
      if (sortItems.length > MAX_SORT_FIELDS) {
        LOGGER.debug(
            "Too many sort fields: {} provided, max allowed is {}",
            sortItems.length,
            MAX_SORT_FIELDS);
        throw new DxBadRequestException("Too many sort fields. Max allowed is " + MAX_SORT_FIELDS);
      }

      for (String item : sortItems) {
        String[] parts = item.split(":");
        if (parts.length != 2) {
          LOGGER.warn("Invalid sort format detected: '{}'. Expected 'field:order'", item);
          throw new DxBadRequestException(
              "Invalid sort format: " + item + ". Expected field:order");
        }

        String field = parts[0].trim();
        String direction = parts[1].trim().toLowerCase();

        LOGGER.debug("Processing sort field: '{}', direction: '{}'", field, direction);

        if (!config.getAllowedSortFields().contains(field)) {
          LOGGER.warn("Invalid sort field encountered: '{}'", field);
          throw new DxBadRequestException("Invalid sort field: " + field);
        }
        if (!direction.equals("asc") && !direction.equals("desc")) {
          LOGGER.warn("Invalid sort order encountered: '{}'. Must be 'asc' or 'desc'", direction);
          throw new DxBadRequestException("Invalid sort order: " + direction);
        }

        String dbField = config.getApiToDbMap().get(field);
        LOGGER.debug("Mapping API sort field '{}' to DB field '{}'", field, dbField);

        OrderBy orderBy = new OrderBy(dbField, OrderBy.Direction.valueOf(direction.toUpperCase()));
        LOGGER.debug("Adding sort order: {}", orderBy);
        orderByList.add(orderBy);
      }
    } else {
      LOGGER.warn("No sort parameter provided so that using default sort order");
      OrderBy defaultOrder =
          new OrderBy(
              config.getDefaultSortBy(),
              OrderBy.Direction.valueOf(config.getDefaultOrder().toUpperCase()));
      orderByList.add(defaultOrder);
    }

    return orderByList;
  }

  private static int parseIntOrDefault(List<String> values, int defaultValue) {
    if (values == null || values.isEmpty()) {
      LOGGER.debug("Parameter missing, using default value: {}", defaultValue);
      return defaultValue;
    }
    try {
      int parsed = Integer.parseInt(values.getFirst());
      LOGGER.debug("Parsed integer parameter value: {}", parsed);
      return parsed;
    } catch (NumberFormatException e) {
      LOGGER.warn(
          "Failed to parse int from value '{}', using default {}", values.get(0), defaultValue);
      return defaultValue;
    }
  }

  // Helper to get first value of param or null
  private static String firstParam(MultiMap params, String key) {
    List<String> values = params.getAll(key);
    return values.isEmpty() ? null : values.getFirst();
  }
}
