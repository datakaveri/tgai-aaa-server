package org.cdpg.dx.aaa.kyc.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import org.cdpg.dx.aaa.cache.service.CacheService;
import org.cdpg.dx.aaa.cache.service.CacheServiceImpl;
import org.json.XML;


public class KYCServiceImpl implements KYCService {

    private final Vertx vertx;
    private final JsonObject config;

    public KYCServiceImpl(Vertx vertx, JsonObject config) {
        this.vertx = vertx;
        this.config = config;
    }

    @Override
    public Future<JsonObject> getKYCData(String userId, String authCode, String codeVerifier) {
        CacheService cacheService = new CacheServiceImpl();

        String tokenUrl = config.getString("digilockerUrl") + "/1/token";
        MultiMap form = MultiMap.caseInsensitiveMultiMap();
        form.add("grant_type", "authorization_code");
        form.add("client_id", config.getString("digilockerClientId"));
        form.add("client_secret", config.getString("digilockerClientSecret"));
        form.add("code", authCode);
        form.add("code_verifier", codeVerifier);
        form.add("redirect_uri", config.getString("digilockerRedirectUrl"));

        return WebClient.create(vertx)
                .postAbs(tokenUrl)
                .sendForm(form)
                .compose(response -> {
                    if (response.statusCode() != 200) {
                        return Future.failedFuture("Failed to fetch access token");
                    }
                    String accessToken = response.bodyAsJsonObject().getString("access_token");

                    if ("Y".equals(response.bodyAsJsonObject().getString("eaadhaar"))) {
                        String aadhaarUrl = config.getString("digilockerUrl") + "/3/xml/eaadhaar";
                        return WebClient.create(vertx)
                                .getAbs(aadhaarUrl)
                                .bearerTokenAuthentication(accessToken)
                                .send()
                                .compose(aadhaarResponse -> {
                                    if (aadhaarResponse.statusCode() != 200) {
                                        return Future.failedFuture("Failed to fetch Aadhaar details");
                                    }

                                    String aadhaarXml = aadhaarResponse.bodyAsString();
                                    JsonObject aadhaarDetails = parseKYCxml(aadhaarXml);

                                    // Store the Aadhaar details in cache if successful
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
        CacheService cacheService = new CacheServiceImpl();
        Promise<JsonObject> promise = Promise.promise();

        cacheService.retrieve(userId).onComplete(ar -> {
            if (ar.succeeded()) {
                JsonObject cachedData = ar.result();
                if (cachedData != null) {
                    promise.complete(cachedData);
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
            return new JsonObject();
        }
    }
}
