package org.cdpg.dx.keyclock.model;

import java.util.List;
import java.util.UUID;

public record DxUser(
        List<String> roles,
        String organisationId,
        String organisationName,
        UUID sub,
        boolean emailVerified,
        boolean kycVerified,
        String name,
        String preferredUsername,
        String givenName,
        String familyName,
        String email
) {
}
