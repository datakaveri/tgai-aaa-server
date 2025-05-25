package org.cdpg.dx.keyclock.util;

import org.cdpg.dx.keyclock.config.KeycloakConstants;
import org.cdpg.dx.keyclock.model.DxUser;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class DxUserMapper {
    public static DxUser fromUserRepresentation(UserRepresentation user) {
        Map<String, List<String>> attrs = Optional.ofNullable(user.getAttributes()).orElse(Map.of());
        return new DxUser(
                user.getRealmRoles(),
                getAttr(attrs, KeycloakConstants.ORGANISATION_ID),
                getAttr(attrs, KeycloakConstants.ORGANISATION_NAME),
                UUID.fromString(user.getId()),
                user.isEmailVerified(),
                Boolean.parseBoolean(getAttr(attrs, KeycloakConstants.KYC_VERIFIED)),
                user.getFirstName() + " " + user.getLastName(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail()
        );
    }

    private static String getAttr(Map<String, List<String>> attrs, String key) {
        return attrs.getOrDefault(key, List.of("")).get(0);
    }
}
