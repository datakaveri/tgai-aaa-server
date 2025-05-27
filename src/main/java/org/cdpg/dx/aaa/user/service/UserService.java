package org.cdpg.dx.aaa.user.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.cdpg.dx.common.model.DxUser;

import java.util.UUID;

public interface UserService {

    Future<DxUser> getUserInfo(DxUser dxUser);
    Future<DxUser> getUserInfoByID(UUID userId);

}
