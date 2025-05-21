package org.cdpg.dx.aaa.kyc.handler;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.aaa.kyc.service.KYCService;
import org.cdpg.dx.aaa.organization.handler.OrganizationHandler;
import org.cdpg.dx.aaa.organization.service.OrganizationService;


public class KYCHandler {
    private static final Logger LOGGER = LogManager.getLogger(KYCHandler.class);
    private final KYCService kycService;


    public KYCHandler(KYCService kycService) {
        this.kycService = kycService;
    }

    public void verifyKYC(RoutingContext routingContext){
        User user = routingContext.user();

        System.out.println("user: " + user);

        JsonObject OrgRequestJson = routingContext.body().asJsonObject();

        System.out.println("orgRequestJson: " + OrgRequestJson);

        String code = OrgRequestJson.getString("auth_code");
        String codeVerifier = OrgRequestJson.getString("code_verifier");

        if (code == null || codeVerifier == null) {
            routingContext.response().setStatusCode(400).end("Missing required parameters");
            return;
        }
        kycService.getKYCData(user.get("username"), code, codeVerifier)
                .onComplete(ar -> {
                    if (ar.succeeded()) {
                        routingContext.response().setStatusCode(200).end(ar.toString());
                    } else {
                        routingContext.response().setStatusCode(500).end(ar.cause().getMessage());
                    }
                });


    }

    public void confirmKYC(RoutingContext routingContext){
        User user = routingContext.get("user");
        System.out.println("user: " + user);

        String codeVerifier = routingContext.pathParam("id");

        if (codeVerifier == null) {
            routingContext.response().setStatusCode(400).end("Missing required parameters");
            return;
        }

        kycService.confirmKYCData(user.get("username"), codeVerifier)
                .onComplete(ar -> {
                    if (ar.succeeded()) {
                        routingContext.response().setStatusCode(200).end(ar.toString());
                    } else {
                        routingContext.response().setStatusCode(500).end(ar.cause().getMessage());
                    }
                });
    }


}
