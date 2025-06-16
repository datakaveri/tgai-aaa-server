package org.cdpg.dx.database.postgres.util;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class EntityUtil {
  public static void putIfPresent(Map<String, Object> map, String key, Optional<?> value) {
    value.ifPresent(v -> map.put(key, v));
  }

  public static <T> void putIfNonEmpty(Map<String, Object> map, String key, T value) {
    if (value != null) {
      map.put(key, value);
    }
  }

  public static void putIfNonEmpty(Map<String, Object> map, String key, String value) {
    if (value != null && !value.isEmpty()) {
      map.put(key, value);
    }
  }

  public static UUID parseUUID(String value) {
    if (value != null && !value.isEmpty()) {
      return UUID.fromString(value);
    }
    System.out.printf("requested UUID is null or empty: %s%n", value);
    return null;
  }
}
