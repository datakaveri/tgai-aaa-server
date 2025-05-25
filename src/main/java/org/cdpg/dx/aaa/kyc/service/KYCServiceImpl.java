package org.cdpg.dx.aaa.kyc.service;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import org.cdpg.dx.aaa.cache.service.CacheService;
import org.cdpg.dx.common.exception.DxValidationException;
import org.cdpg.dx.keyclock.service.KeycloakUserService;
import org.json.JSONObject;
import org.json.XML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;

public class KYCServiceImpl implements KYCService {
    private static final Logger LOGGER = LoggerFactory.getLogger(KYCServiceImpl.class);

    private static final String GRANT_TYPE = "authorization_code";

    private static final String CONFIG_TOKEN_URL = "digilockerTokenUrl";
    private static final String CONFIG_AADHAAR_URL = "digilockerAadhaarUrl";
    private static final String CONFIG_CLIENT_ID = "clientId";
    private static final String CONFIG_CLIENT_SECRET = "clientSecret";
    private static final String CONFIG_REDIRECT_URI = "redirectUri";

    private final WebClient webClient;
    private final CacheService cacheService;
    private final KeycloakUserService keycloakUserService;
    private final JsonObject config;

    public KYCServiceImpl(WebClient webClient,
                          CacheService cacheService,
                          KeycloakUserService keycloakUserService,
                          JsonObject config) {
        this.webClient = webClient;
        this.cacheService = cacheService;
        this.keycloakUserService = keycloakUserService;
        this.config = config;
    }

    @Override
    public Future<JsonObject> getKYCData(UUID userId, String authCode, String codeVerifier) {
        MultiMap tokenRequestForm = MultiMap.caseInsensitiveMultiMap()
                .add("grant_type", GRANT_TYPE)
                .add("client_id", config.getString(CONFIG_CLIENT_ID))
                .add("client_secret", config.getString(CONFIG_CLIENT_SECRET))
                .add("code", authCode)
                .add("code_verifier", codeVerifier)
                .add("redirect_uri", config.getString(CONFIG_REDIRECT_URI));

        return webClient.postAbs(config.getString(CONFIG_TOKEN_URL))
                .sendForm(tokenRequestForm)
                .compose(tokenResponse -> handleTokenResponse(tokenResponse.bodyAsJsonObject(), userId.toString(), codeVerifier));
    }

    private Future<JsonObject> handleTokenResponse(JsonObject tokenResponse, String userId, String codeVerifier) {
        String accessToken = tokenResponse.getString("access_token");
        String aadhaarAvailable = tokenResponse.getString("eaadhaar");

        if (accessToken == null || accessToken.isBlank()) {
            LOGGER.error("Access token missing in response: {}", tokenResponse.encodePrettily());
            return Future.failedFuture(new DxValidationException("Missing access token from DigiLocker"));
        }

        if (!"Y".equalsIgnoreCase(aadhaarAvailable)) {
            return Future.failedFuture(new DxValidationException("Aadhaar details not available for this user in DigiLocker"));
        }

        return fetchAadhaarDetails(accessToken, userId, codeVerifier);
    }

    private Future<JsonObject> fetchAadhaarDetails(String accessToken, String userId, String codeVerifier) {
        return webClient.getAbs(config.getString(CONFIG_AADHAAR_URL))
                .bearerTokenAuthentication(accessToken)
                .send()
                .compose(response -> {
                    if (response.statusCode() != 200) {
                        LOGGER.error("Failed to fetch Aadhaar details: statusCode={}, body={}",
                                response.statusCode(), response.bodyAsString());
                        return Future.failedFuture(new DxValidationException("Failed to fetch Aadhaar details"));
                    }

                    try {
                        JsonObject aadhaarJson = parseKYCxml(response.bodyAsString());
                        aadhaarJson.put("code_verifier", codeVerifier);
                        cacheService.store(userId, aadhaarJson);
                        return Future.succeededFuture(new JsonObject().put("aadhaarDetails", aadhaarJson));
                    } catch (DxValidationException e) {
                        return Future.failedFuture(e);
                    }
                });
    }
    //TODO remove code_verifier from cachedData
    @Override
    public Future<JsonObject> confirmKYCData(UUID userId, String codeVerifier) {
        Promise<JsonObject> promise = Promise.promise();

        cacheService.retrieve(userId.toString()).onComplete(ar -> {
            if (ar.failed()) {
                String msg = "Cache retrieval failed for userId " + userId + ": " + ar.cause().getMessage();
                LOGGER.error(msg);
                promise.fail(new DxValidationException(msg));
                return;
            }

            JsonObject cachedData = ar.result();
            if (cachedData == null) {
                String msg = "No cached KYC data for userId: " + userId;
                LOGGER.warn(msg);
                promise.fail(new DxValidationException(msg));
                return;
            }

            boolean isVerified = codeVerifier.equals(cachedData.getString("code_verifier"));
            if (isVerified) {
                keycloakUserService.setKycVerifiedTrueWithData(userId, cachedData)
                    .onComplete(kycResult -> {
                        if (kycResult.succeeded() && kycResult.result()) {
                            LOGGER.info("KYC verification successful for userId: {}", userId);
                            promise.complete(cachedData);
                        } else {
                            String msg = "Failed to update KYC status in Keycloak for userId: " + userId;
                            LOGGER.error(msg);
                            promise.fail(new DxValidationException(msg));
                        }
                    });
            } else {
                keycloakUserService.setKycVerifiedFalse(userId)
                    .onComplete(kycResult -> {
                        String msg = "KYC verification failed due to code mismatch for userId: " + userId;
                        LOGGER.warn(msg);
                        promise.fail(new DxValidationException(msg));
                    });
            }
        });

        return promise.future();
    }


    private JsonObject parseKYCxml(String xmlData) {
        try {
            JSONObject jsonObject = XML.toJSONObject(xmlData);
            Map<String, Object> map = jsonObject.toMap();

            JsonObject root = new JsonObject(map);
            JsonObject certificate = root.getJsonObject("Certificate");
            if (certificate == null) {
                throw new DxValidationException("Missing 'Certificate' field in Aadhaar XML.");
            }

            JsonObject certificateData = certificate.getJsonObject("CertificateData");
            if (certificateData == null) {
                throw new DxValidationException("Missing 'CertificateData' field in Aadhaar XML.");
            }

            JsonObject kycRes = certificateData.getJsonObject("KycRes");
            if (kycRes == null) {
                throw new DxValidationException("Missing 'KycRes' field in Aadhaar XML.");
            }

            JsonObject uidData = kycRes.getJsonObject("UidData");
            if (uidData == null) {
                throw new DxValidationException("Missing 'UidData' field in Aadhaar XML.");
            }

            // Extract only the required fields
            JsonObject result = new JsonObject();
            if (uidData.containsKey("uid")) {
                result.put("uid", uidData.getString("uid"));
            } else {
                throw new DxValidationException("Missing 'uid' field in Aadhaar XML.");
            }

            if (uidData.containsKey("Poa")) {
                result.put("Poa", uidData.getJsonObject("Poa"));
            }

            if (uidData.containsKey("LData")) {
                result.put("LData", uidData.getJsonObject("LData"));
            }

            if (uidData.containsKey("Poi")) {
                result.put("Poi", uidData.getJsonObject("Poi"));
            }

            return result;

        } catch (DxValidationException ve) {
            throw ve; // rethrow explicitly known validation exceptions
        } catch (Exception e) {
            LOGGER.error("Error while parsing Aadhaar XML data", e);
            throw new DxValidationException("Invalid Aadhaar XML format");
        }
    }
}
