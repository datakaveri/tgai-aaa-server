package org.cdpg.dx.aaa.apiserver;

import static org.cdpg.dx.common.config.ServiceProxyAddressConstants.*;

import io.vertx.core.Vertx;
import java.util.List;

import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.aaa.admin.controller.AdminController;
import org.cdpg.dx.aaa.admin.handler.AdminHandler;
import org.cdpg.dx.aaa.credit.factory.CreditControllerFactory;
import org.cdpg.dx.aaa.credit.service.CreditService;
import org.cdpg.dx.aaa.kyc.controller.KYCController;
import org.cdpg.dx.aaa.kyc.factory.KYCFactory;
import org.cdpg.dx.aaa.kyc.handler.KYCHandler;
import org.cdpg.dx.aaa.organization.factory.OrganizationControllerFactory;
import org.cdpg.dx.aaa.organization.service.OrganizationService;
import org.cdpg.dx.aaa.user.service.UserService;
import org.cdpg.dx.aaa.user.service.UserServiceImpl;
import org.cdpg.dx.database.postgres.service.PostgresService;
import org.cdpg.dx.keyclock.service.KeycloakUserService;
import org.cdpg.dx.keyclock.service.KeycloakUserServiceImpl;

public class ControllerFactory {
  private static final Logger LOGGER = LogManager.getLogger(ControllerFactory.class);

  private ControllerFactory() {}

  public static List<ApiController> createControllers(Vertx vertx, JsonObject config) {
    PostgresService pgService = PostgresService.createProxy(vertx, POSTGRES_SERVICE_ADDRESS);

    KeycloakUserService keycloakUserService = new KeycloakUserServiceImpl(config);


    CreditService creditService = CreditControllerFactory.createService(pgService, keycloakUserService);
    OrganizationService  organizationService = OrganizationControllerFactory.createService(pgService, keycloakUserService);

    UserService userService = new UserServiceImpl(keycloakUserService, organizationService, creditService);
;
    ApiController creditApiController =  CreditControllerFactory.create(creditService);

    KYCHandler kycHandler = KYCFactory.createHandler(vertx, config);
    ApiController kycController = new KYCController(kycHandler);
    ApiController organizationController = OrganizationControllerFactory.create(organizationService, userService);

      AdminHandler adminHandler = new AdminHandler(userService, keycloakUserService, creditService, organizationService);
      ApiController adminController = new AdminController(adminHandler);

    //TODO create other controllers

    return List.of(organizationController, creditApiController, kycController, adminController);
  }
}
