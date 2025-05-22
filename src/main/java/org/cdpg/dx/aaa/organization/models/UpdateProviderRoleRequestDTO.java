package org.cdpg.dx.aaa.organization.models;

import org.cdpg.dx.aaa.organization.util.Constants;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public record UpdateProviderRoleRequestDTO(
        String status
) {
    public Map<String, Object> toNonEmptyFieldsMap() {
        Map<String, Object> map = new HashMap<>();

        if (status != null && !status.isEmpty()) {
            map.put(Constants.STATUS, status);
        }

        return map;
    }
}
