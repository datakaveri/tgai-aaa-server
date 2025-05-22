package org.cdpg.dx.aaa.apiserver;

import static org.cdpg.dx.common.config.ServiceProxyAddressConstants.*;

import io.vertx.core.Vertx;
import java.util.List;

import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.aaa.kyc.controller.KYCController;
import org.cdpg.dx.aaa.kyc.factory.KYCFactory;
import org.cdpg.dx.aaa.kyc.handler.KYCHandler;
import org.cdpg.dx.aaa.organization.controller.OrganizationController;
import org.cdpg.dx.aaa.organization.factory.OrganizationFactory;
import org.cdpg.dx.aaa.organization.handler.OrganizationHandler;
import org.cdpg.dx.database.postgres.service.PostgresService;
import io.vertx.core.json.JsonObject;

public class ControllerFactory {
  private static final Logger LOGGER = LogManager.getLogger(ControllerFactory.class);

  private ControllerFactory() {}

  public static List<ApiController> createControllers(Vertx vertx, JsonObject config) {
    PostgresService pgService = PostgresService.createProxy(vertx, POSTGRES_SERVICE_ADDRESS);

    OrganizationHandler organizationHandler = OrganizationFactory.createHandler(pgService, config);
    ApiController organizationController = new OrganizationController(organizationHandler);

    KYCHandler kycHandler = KYCFactory.createHandler(vertx, config);
    ApiController kycController = new KYCController(kycHandler);

    //TODO create other controllers

    return List.of(organizationController, kycController);
  }
}
