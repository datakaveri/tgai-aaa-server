package org.cdpg.dx.aaa.kyc.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import org.cdpg.dx.aaa.cache.service.CacheService;
import org.json.XML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KYCServiceImpl implements KYCService {
    private static final Logger LOGGER = LoggerFactory.getLogger(KYCServiceImpl.class);


    private final WebClient webClient;
    private final CacheService cacheService;
    private final JsonObject config;

    public KYCServiceImpl(WebClient webClient, CacheService cacheService, JsonObject config) {
        this.webClient = webClient;
        this.cacheService = cacheService;
        this.config = config;
    }

    @Override
    public Future<JsonObject> getKYCData(String userId, String authCode, String codeVerifier) {
        String tokenUrl = config.getString("digilockerTokenUrl");
        String aadhaarUrl = config.getString("digilockerAadhaarUrl");

        MultiMap form = MultiMap.caseInsensitiveMultiMap();
        form.add("grant_type", "authorization_code");
        form.add("client_id", config.getString("clientId"));
        form.add("client_secret", config.getString("clientSecret"));
        form.add("code", authCode);
        form.add("code_verifier", codeVerifier);
        form.add("redirect_uri", config.getString("redirectUri"));

        return webClient
                .postAbs(tokenUrl)
                .sendForm(form)
                .compose(response -> {
                    if (response.statusCode() != 200) {
                        return Future.failedFuture("Failed to fetch access token");
                    }

                    JsonObject tokenResponse = response.bodyAsJsonObject();
                    String accessToken = tokenResponse.getString("access_token");

                    if ("Y".equals(tokenResponse.getString("eaadhaar"))) {
                        return webClient
                                .getAbs(aadhaarUrl)
                                .bearerTokenAuthentication(accessToken)
                                .send()
                                .compose(aadhaarResponse -> {
                                    if (aadhaarResponse.statusCode() != 200) {
                                        return Future.failedFuture("Failed to fetch Aadhaar details");
                                    }

                                    String aadhaarXml = aadhaarResponse.bodyAsString();
                                    JsonObject aadhaarDetails = parseKYCxml(aadhaarXml);

                                    cacheService.store(userId, aadhaarDetails.put("code_verifier", codeVerifier));

                                    return Future.succeededFuture(new JsonObject()
                                            .put("Aadhaar Details", aadhaarDetails));
                                });
                    } else {
                        return Future.failedFuture("Aadhaar details not available for this user in DigiLocker");
                    }
                });
    }

    @Override
    public Future<JsonObject> confirmKYCData(String userId, String codeVerifier) {
        Promise<JsonObject> promise = Promise.promise();

        cacheService.retrieve(userId).onComplete(ar -> {
            if (ar.succeeded()) {
                JsonObject cachedData = ar.result();
                if (cachedData != null) {
                    if (codeVerifier.equals(cachedData.getString("code_verifier"))) {
                        promise.complete(cachedData);
                    } else {
                        promise.fail("Code verifier mismatch");
                    }
                } else {
                    promise.fail("No data found in cache for userId: " + userId);
                }
            } else {
                promise.fail("Failed to retrieve data from cache: " + ar.cause());
            }
        });

        return promise.future();
    }

    @Override
    public JsonObject parseKYCxml(String xmlData) {
        try {
            JsonNode jsonNode = new ObjectMapper().readTree(XML.toJSONObject(xmlData).toString());
            return new JsonObject(jsonNode.toString());
        } catch (Exception e) {
            LOGGER.error("Failed to parse Aadhaar XML: {}", e.getMessage());
            return new JsonObject();
        }
    }
}
