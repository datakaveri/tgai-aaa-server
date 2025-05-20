package org.cdpg.dx.aaa.organization.controller;


import io.vertx.ext.web.openapi.RouterBuilder;
import org.cdpg.dx.aaa.apiserver.ApiController;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.aaa.organization.handler.OrganizationHandler;

public class OrganizationController implements ApiController {
    private static final Logger LOGGER = LogManager.getLogger(OrganizationController.class);
    private final OrganizationHandler  organizationHandler;

    public OrganizationController(OrganizationHandler  organizationHandler) {
        this.organizationHandler = organizationHandler;
    }

    @Override
    public void register(RouterBuilder routerBuilder) {

        routerBuilder
            .operation("get-auth-v1-getAllOrgReq")
            .handler(organizationHandler::getOrganisationRequest);

        routerBuilder
          .operation("post-auth-v1-organisations-request")
            .handler(organizationHandler::createOrganisationRequest);

        routerBuilder
          .operation("post-auth-v1-organisations-request")
          .handler(organizationHandler::approveOrganisationRequest);

        routerBuilder
          .operation("post-auth-v1-joinOrg")
            .handler(organizationHandler::joinOrganisationRequest);

        routerBuilder
          .operation("get-auth-v1-organisations-join")
            .handler(organizationHandler::getJoinOrganisationRequests);

        routerBuilder
          .operation("post-auth-v1-approve")
            .handler(organizationHandler::approveJoinOrganisationRequests);

        routerBuilder
                .operation("get-auth-v1-org")
                //.handler(ctx -> fetchRoles.fetch(ctx, Set.of()))
                //.handler(ctx -> roleAuthorisationHandler.validateRole(ctx, Set.of()))
                .handler(organizationHandler::listAllOrganisations);

        routerBuilder
                .operation("delete-auth-v1-organisations-id")
                //.handler(ctx -> fetchRoles.fetch(ctx, Set.of(Roles.COS_ADMIN)))
                //.handler(ctx -> roleAuthorisationHandler.validateRole(ctx, Set.of(Roles.COS_ADMIN)))
                .handler(organizationHandler::deleteOrganisationById);

        routerBuilder
                .operation("put-auth-v1-organisations-id")
                //.handler(ctx -> fetchRoles.fetch(ctx, Set.of(Roles.COS_ADMIN)))
                //.handler(ctx -> roleAuthorisationHandler.validateRole(ctx, Set.of(Roles.COS_ADMIN)))
                .handler(organizationHandler::updateOrganisationById);

        //Organization User
        routerBuilder
                .operation("get-auth-v1-organisations-users-id")
                //.handler(ctx -> fetchRoles.fetch(ctx, Set.of(Roles.COS_ADMIN, Roles.ADMIN)))
                //.handler(ctx -> roleAuthorisationHandler.validateRole(ctx, Set.of(Roles.COS_ADMIN, Roles.ADMIN)))
                .handler(organizationHandler::getOrganisationUsers);

        routerBuilder
                .operation("delete-auth-v1-organisations-users-id")
                //.handler(ctx -> fetchRoles.fetch(ctx, Set.of(Roles.COS_ADMIN, Roles.ADMIN)))
                //.handler(ctx -> roleAuthorisationHandler.validateRole(ctx, Set.of(Roles.COS_ADMIN, Roles.ADMIN)))
                .handler(organizationHandler::deleteOrganisationUserById);

        routerBuilder
                .operation("put-auth-v1-organization-users-role")
                //.handler(ctx -> fetchRoles.fetch(ctx, Set.of(Roles.COS_ADMIN, Roles.ADMIN)))
                //.handler(ctx -> roleAuthorisationHandler.validateRole(ctx, Set.of(Roles.COS_ADMIN, Roles.ADMIN)))
                .handler(organizationHandler::updateOrganisationUserRole);
    }


}
