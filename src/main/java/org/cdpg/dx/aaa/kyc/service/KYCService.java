package org.cdpg.dx.aaa.kyc.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public interface KYCService {

    Future<JsonObject> getKYCData(String userId, String authCode, String codeVerifier);

    Future<JsonObject> confirmKYCData(String userId, String codeVerifier);

    JsonObject parseKYCxml(String xmlData);
}
