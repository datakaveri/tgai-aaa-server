package org.cdpg.dx.aaa.apiserver;

import static org.cdpg.dx.common.config.ServiceProxyAddressConstants.*;

import io.vertx.core.Vertx;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//import org.cdpg.dx.aaa.activity.controller.ActivityController;
//import org.cdpg.dx.auditingserver.activity.factory.ActivityFactory;
//import org.cdpg.dx.auditingserver.activity.service.ActivityService;
import org.cdpg.dx.database.postgres.service.PostgresService;

public class ControllerFactory {
  private static final Logger LOGGER = LogManager.getLogger(ControllerFactory.class);

  private ControllerFactory() {}

  public static List<ApiController> createControllers(Vertx vertx) {
    PostgresService pgService = PostgresService.createProxy(vertx, POSTGRES_SERVICE_ADDRESS);

//    ActivityService activityService = ActivityFactory.create(pgService);

    return List.of();
  }
}
