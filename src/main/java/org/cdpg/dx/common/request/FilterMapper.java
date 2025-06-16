package org.cdpg.dx.common.request;

import java.util.HashMap;
import java.util.Map;

public class FilterMapper {
  public static Map<String, String> mapFilters(
      Map<String, String> filters, Map<String, String> apiToDbMap) {
    Map<String, String> mapped = new HashMap<>();
    filters.forEach((k, v) -> mapped.put(apiToDbMap.getOrDefault(k, k), v));
    return mapped;
  }
}
