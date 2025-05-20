package org.cdpg.dx.aaa.kyc.factory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.aaa.kyc.handler.KYCHandler;
import org.cdpg.dx.aaa.kyc.service.KYCService;
import org.cdpg.dx.aaa.kyc.service.KYCServiceImpl;
import org.cdpg.dx.aaa.organization.dao.OrganizationDAOFactory;
import org.cdpg.dx.aaa.organization.factory.OrganizationFactory;
import org.cdpg.dx.aaa.organization.handler.OrganizationHandler;
import org.cdpg.dx.aaa.organization.service.OrganizationService;
import org.cdpg.dx.aaa.organization.service.OrganizationServiceImpl;
import org.cdpg.dx.database.postgres.service.PostgresService;

public class KYCFactory {
    private static final Logger LOGGER = LogManager.getLogger(KYCFactory.class);

    private KYCFactory() {}

    public static KYCHandler createHandler(PostgresService pgService) {

        KYCService kycService = new KYCServiceImpl();

        return new KYCHandler(kycService);
    }
}
