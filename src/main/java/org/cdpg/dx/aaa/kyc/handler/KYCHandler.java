package org.cdpg.dx.aaa.kyc.handler;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.aaa.kyc.service.KYCService;
import org.cdpg.dx.common.exception.DxBadRequestException;
import org.cdpg.dx.common.response.ResponseBuilder;

import java.util.UUID;


public class KYCHandler {
    private static final Logger LOGGER = LogManager.getLogger(KYCHandler.class);
    private final KYCService kycService;


    public KYCHandler(KYCService kycService) {
        this.kycService = kycService;
    }

    public void verifyKYC(RoutingContext ctx) {
        LOGGER.debug("verifyKYC: >>>>>>>>>>>>>>>>");
        User user = ctx.user();
        UUID userId = UUID.fromString(user.subject());

        JsonObject OrgRequestJson = ctx.body().asJsonObject();

        LOGGER.debug("orgRequestJson: " + OrgRequestJson);

        String code = OrgRequestJson.getString("auth_code");
        String codeVerifier = OrgRequestJson.getString("code_verifier");

        if (code == null || codeVerifier == null) {
            ctx.fail(new DxBadRequestException("Required parameter missing"));
            return;
        }
        kycService.getKYCData(userId, code, codeVerifier)
                .onSuccess(res -> {
                    ResponseBuilder.sendSuccess(ctx, res);
                })
                .onFailure(ctx::fail);


    }

    public void confirmKYC(RoutingContext ctx) {
        User user = ctx.user();
        UUID userId = UUID.fromString(user.subject());
        String codeVerifier = ctx.pathParam("id");

        if (codeVerifier == null) {
            ctx.fail(new DxBadRequestException("Missing required parameters"));
            return;
        }

        kycService.confirmKYCData(userId, codeVerifier, user.principal().getString("name"))
                .onSuccess(res -> {
                    ResponseBuilder.sendSuccess(ctx, res);
                })
                .onFailure(ctx::fail);
    }


}
