package org.cdpg.dx.aaa.organization.controller;


import io.vertx.ext.web.openapi.RouterBuilder;
import org.cdpg.dx.aaa.apiserver.ApiController;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.aaa.organization.handler.OrganizationHandler;
import org.cdpg.dx.auth.authorization.handler.AuthorizationHandler;
import org.cdpg.dx.auth.authorization.model.DxRole;

public class OrganizationController implements ApiController {
    private static final Logger LOGGER = LogManager.getLogger(OrganizationController.class);
    private final OrganizationHandler organizationHandler;

    public OrganizationController(OrganizationHandler organizationHandler) {
        this.organizationHandler = organizationHandler;
    }

    @Override
    public void register(RouterBuilder routerBuilder) {

        routerBuilder
                .operation("get-auth-v1-getAllOrgReq")
                .handler(AuthorizationHandler.forRoles(DxRole.COS_ADMIN))
                .handler(organizationHandler::getOrganisationRequest);

        routerBuilder
                .operation("post-auth-v1-organisations-request")
                .handler(organizationHandler::createOrganisationRequest);

        routerBuilder
                .operation("post-auth-v1-approve-create_org")
                .handler(AuthorizationHandler.forRoles(DxRole.COS_ADMIN))
                .handler(organizationHandler::approveOrganisationRequest);

        routerBuilder
                .operation("post-auth-v1-joinOrg")
                .handler(organizationHandler::joinOrganisationRequest);

        routerBuilder
                .operation("get-auth-v1-organisations-join-requests")
                .handler(AuthorizationHandler.forRoles(DxRole.ORG_ADMIN))
                .handler(organizationHandler::getJoinOrganisationRequests);

        routerBuilder
                .operation("post-auth-v1-approve")
                .handler(AuthorizationHandler.forRoles(DxRole.ORG_ADMIN))
                .handler(organizationHandler::approveJoinOrganisationRequests);

        routerBuilder
                .operation("get-auth-v1-org")
                .handler(organizationHandler::listAllOrganisations);

        routerBuilder
                .operation("delete-auth-v1-organisations-id")
                .handler(AuthorizationHandler.forRoles(DxRole.COS_ADMIN))
                .handler(organizationHandler::deleteOrganisationById);

        routerBuilder
                .operation("put-auth-v1-organisations-id")
                .handler(AuthorizationHandler.forRoles(DxRole.COS_ADMIN))
                .handler(organizationHandler::updateOrganisationById);

        //Organization User
        routerBuilder
                .operation("get-auth-v1-organisations-users-id")
                .handler(AuthorizationHandler.forRoles(DxRole.ORG_ADMIN))
                .handler(organizationHandler::getOrganisationUsers);

        routerBuilder
                .operation("delete-auth-v1-organisations-users-id")
                .handler(AuthorizationHandler.forRoles(DxRole.ORG_ADMIN))
                .handler(organizationHandler::deleteOrganisationUserById);

        routerBuilder
                .operation("put-auth-v1-organization-users-role")
                .handler(AuthorizationHandler.forRoles(DxRole.ORG_ADMIN))
                .handler(organizationHandler::updateOrganisationUserRole);

        routerBuilder
                .operation("post-auth-v1-user-roles")
                .handler(organizationHandler::createProviderRequest);

        routerBuilder
                .operation("put-auth-v1-user-roles")
                .handler(AuthorizationHandler.forRoles(DxRole.ORG_ADMIN))
                .handler(organizationHandler::updateProviderRequest);


    }


}
