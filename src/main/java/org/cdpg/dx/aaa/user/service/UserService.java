package org.cdpg.dx.aaa.user.service;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import org.cdpg.dx.aaa.organization.models.ProviderRoleRequest;
import org.cdpg.dx.keyclock.model.DxUser;

public interface UserService {

    Future<DxUser> getDxUser(RoutingContext ctx);
    Future<DxUser> getKeycloakDxUser(String userId);

}
