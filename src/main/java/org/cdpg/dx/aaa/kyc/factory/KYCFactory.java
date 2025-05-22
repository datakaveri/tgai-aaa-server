package org.cdpg.dx.aaa.kyc.factory;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.aaa.cache.service.CacheService;
import org.cdpg.dx.aaa.cache.service.CacheServiceImpl;
import org.cdpg.dx.aaa.kyc.handler.KYCHandler;
import org.cdpg.dx.aaa.kyc.service.KYCService;
import org.cdpg.dx.aaa.kyc.service.KYCServiceImpl;
import org.cdpg.dx.aaa.organization.dao.OrganizationDAOFactory;
import org.cdpg.dx.aaa.organization.factory.OrganizationFactory;
import org.cdpg.dx.aaa.organization.handler.OrganizationHandler;
import org.cdpg.dx.aaa.organization.service.OrganizationService;
import org.cdpg.dx.aaa.organization.service.OrganizationServiceImpl;
import org.cdpg.dx.database.postgres.service.PostgresService;
import org.cdpg.dx.keyclock.service.KeycloakUserService;

public class KYCFactory {
    private static final Logger LOGGER = LogManager.getLogger(KYCFactory.class);

    private KYCFactory() {}

    public static KYCHandler createHandler(Vertx vertx, JsonObject config) {

        WebClient webClient = WebClient.create(vertx);
        CacheService cacheService = new CacheServiceImpl();
        KeycloakUserService  keycloakUserService = new KeycloakUserService(config);

        KYCService kycService = new KYCServiceImpl(webClient, cacheService, keycloakUserService, config);
        return new KYCHandler(kycService);
    }
}
