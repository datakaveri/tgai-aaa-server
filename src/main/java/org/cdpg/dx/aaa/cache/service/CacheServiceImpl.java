package org.cdpg.dx.aaa.cache.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.cdpg.dx.aaa.organization.service.OrganizationServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheServiceImpl implements CacheService {

    private static final Logger LOG = LoggerFactory.getLogger(CacheServiceImpl.class);

    public CacheServiceImpl() {
        // Constructor
    }

    @Override
    public void store(String key, JsonObject data) {
        vertx.sharedData()
                .<String, JsonObject>getLocalMap("digilocker_auth_cache")
                .put(key, data);

    }

    @Override
    public Future<JsonObject> retrieve(String key) {
        JsonObject cachedData = vertx.sharedData()
                .<String, JsonObject>getLocalMap("digilocker_auth_cache")
                .get(key);

        return Future.succeededFuture(cachedData);
    }


}
