package org.cdpg.dx.aaa.organization.factory;

import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.cdpg.dx.aaa.organization.dao.OrganizationDAOFactory;
import org.cdpg.dx.aaa.organization.handler.OrganizationHandler;
import org.cdpg.dx.aaa.organization.service.OrganizationService;
import org.cdpg.dx.aaa.organization.service.OrganizationServiceImpl;
import org.cdpg.dx.database.postgres.service.PostgresService;
import org.cdpg.dx.keyclock.service.KeycloakUserService;

import java.util.List;


public class OrganizationFactory {

    private static final Logger LOGGER = LogManager.getLogger(OrganizationFactory.class);

    private OrganizationFactory() {}

    public static OrganizationHandler createHandler(PostgresService pgService, JsonObject config) {

        OrganizationDAOFactory organizationDAOFactory = new OrganizationDAOFactory(pgService);
        KeycloakUserService  keycloakUserService = new KeycloakUserService(config);
        OrganizationService organizationService = new OrganizationServiceImpl(organizationDAOFactory, keycloakUserService);

        return new OrganizationHandler(organizationService);
    }
}
