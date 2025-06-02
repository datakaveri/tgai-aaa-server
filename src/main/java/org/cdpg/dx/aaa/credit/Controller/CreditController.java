package org.cdpg.dx.aaa.credit.Controller;

import io.vertx.ext.web.openapi.RouterBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.aaa.apiserver.ApiController;
import org.cdpg.dx.aaa.credit.handler.CreditHandler;
import org.cdpg.dx.auth.authorization.handler.AuthorizationHandler;
import org.cdpg.dx.auth.authorization.model.DxRole;

public class CreditController implements ApiController {
  private static final Logger LOGGER = LogManager.getLogger(CreditController.class);
  private final CreditHandler creditHandler;

  public CreditController(CreditHandler creditHandler) {
    this.creditHandler = creditHandler;
  }

  @Override
  public void register(RouterBuilder routerBuilder) {

    routerBuilder
      .operation("post-auth-v1-credit-request")
      .handler(AuthorizationHandler.forRoles(DxRole.CONSUMER))
      .handler(creditHandler::createCreditRequest);

    routerBuilder
      .operation("get-auth-v1-credit")
      .handler(AuthorizationHandler.forRoles(DxRole.COS_ADMIN))
      .handler(creditHandler::getAllPendingCreditRequests);

    routerBuilder
      .operation("put-auth-v1-credit-request")
      .handler(AuthorizationHandler.forRoles(DxRole.COS_ADMIN))
      .handler(creditHandler::updateCreditRequestStatus);

    routerBuilder
      .operation("put-auth-v1-user-credit")
      .handler(AuthorizationHandler.forRoles(DxRole.COS_ADMIN))
      .handler(creditHandler::deductCredits);

    routerBuilder
      .operation("post-auth-v1-compute-role-request")
      .handler(creditHandler::createComputeRoleRequest);


    routerBuilder
      .operation("get-auth-v1-compute-role-request")
      .handler(AuthorizationHandler.forRoles(DxRole.COS_ADMIN))
      .handler(creditHandler::getAllComputeRequests);

    routerBuilder
      .operation("put-auth-v1-compute-role-request")
      .handler(AuthorizationHandler.forRoles(DxRole.COS_ADMIN))
      .handler(creditHandler::updateComputeRoleStatus);


  }
}

