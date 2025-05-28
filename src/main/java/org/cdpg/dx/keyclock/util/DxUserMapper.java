package org.cdpg.dx.keyclock.util;

import org.cdpg.dx.keyclock.config.KeycloakConstants;
import org.cdpg.dx.common.model.DxUser;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.*;
import java.util.stream.Collectors;

public class DxUserMapper {
    public static DxUser fromUserRepresentation(UserRepresentation user, List<RoleRepresentation> roles) {
        Map<String, List<String>> attrs = Optional.ofNullable(user.getAttributes()).orElse(Map.of());
        List<String> roleNames = roles.stream().map(RoleRepresentation::getName).collect(Collectors.toList());

        return new DxUser(
                roleNames,
                getAttr(attrs, KeycloakConstants.ORGANISATION_ID),
                getAttr(attrs, KeycloakConstants.ORGANISATION_NAME),
                UUID.fromString(user.getId()),
                user.isEmailVerified(),
                Boolean.parseBoolean(getAttr(attrs, KeycloakConstants.KYC_VERIFIED)),
                user.getFirstName() + " " + user.getLastName(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                new ArrayList<>()
        );
    }

    private static String getAttr(Map<String, List<String>> attrs, String key) {
        return attrs.getOrDefault(key, List.of("")).get(0);
    }
}
