package org.cdpg.dx.aaa.apiserver;

import static org.cdpg.dx.common.config.ServiceProxyAddressConstants.*;

import io.vertx.core.Vertx;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.aaa.organization.controller.OrganizationController;
import org.cdpg.dx.aaa.organization.factory.OrganizationFactory;
import org.cdpg.dx.aaa.organization.handler.OrganizationHandler;
import org.cdpg.dx.database.postgres.service.PostgresService;

public class ControllerFactory {
  private static final Logger LOGGER = LogManager.getLogger(ControllerFactory.class);

  private ControllerFactory() {}

  public static List<ApiController> createControllers(Vertx vertx) {
    PostgresService pgService = PostgresService.createProxy(vertx, POSTGRES_SERVICE_ADDRESS);

    OrganizationHandler organizationHandler = OrganizationFactory.createHandler(pgService);
    ApiController organizationController = new OrganizationController(organizationHandler);

    //TODO create other controllers

    return List.of(organizationController);
  }
}
