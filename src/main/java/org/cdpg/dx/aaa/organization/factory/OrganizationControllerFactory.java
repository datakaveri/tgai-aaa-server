package org.cdpg.dx.aaa.organization.factory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.cdpg.dx.aaa.organization.controller.OrganizationController;
import org.cdpg.dx.aaa.organization.dao.OrganizationDAOFactory;
import org.cdpg.dx.aaa.organization.handler.OrganizationHandler;
import org.cdpg.dx.aaa.organization.service.OrganizationService;
import org.cdpg.dx.aaa.organization.service.OrganizationServiceImpl;
import org.cdpg.dx.database.postgres.service.PostgresService;
import org.cdpg.dx.keyclock.service.KeycloakUserService;


public class OrganizationControllerFactory {

    private static final Logger LOGGER = LogManager.getLogger(OrganizationControllerFactory.class);

    private OrganizationControllerFactory() {}

    public static OrganizationController create(PostgresService pgService, KeycloakUserService keycloakUserService) {

        OrganizationDAOFactory organizationDAOFactory = new OrganizationDAOFactory(pgService);
        OrganizationService organizationService = new OrganizationServiceImpl(organizationDAOFactory, keycloakUserService);
        OrganizationHandler  organizationHandler = new OrganizationHandler(organizationService);
        return new OrganizationController(organizationHandler);
    }
}
