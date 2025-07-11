package org.cdpg.dx.aaa.kyc.service;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import org.cdpg.dx.aaa.cache.service.CacheService;
import org.cdpg.dx.aaa.kyc.dao.KYCTransactionDAO;
import org.cdpg.dx.aaa.kyc.model.KYCTransaction;
import org.cdpg.dx.aaa.kyc.util.Constants;
import org.cdpg.dx.common.exception.DxValidationException;
import org.cdpg.dx.database.postgres.service.PostgresService;
import org.cdpg.dx.keycloak.service.KeycloakUserService;
import org.json.JSONObject;
import org.json.XML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.apache.logging.log4j.ThreadContext.put;

public class KYCServiceImpl implements KYCService {
    private static final Logger LOGGER = LoggerFactory.getLogger(KYCServiceImpl.class);

    private static final String GRANT_TYPE = "authorization_code";

    private static final String CONFIG_TOKEN_URL = "digilockerTokenUrl";
    private static final String CONFIG_AADHAAR_URL = "digilockerAadhaarUrl";
    private static final String CONFIG_CLIENT_ID = "clientId";
    private static final String CONFIG_CLIENT_SECRET = "clientSecret";
    private static final String CONFIG_REDIRECT_URI = "redirectUri";

    private final WebClient webClient;
    private final KYCTransactionDAO kycTransactionDAO;
    private final KeycloakUserService keycloakUserService;
    private final JsonObject config;

    public KYCServiceImpl(WebClient webClient,
                          KYCTransactionDAO kycTransactionDAO,
                          KeycloakUserService keycloakUserService,
                          JsonObject config) {
        this.webClient = webClient;
        this.kycTransactionDAO = kycTransactionDAO;
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

  //condition checks:
  // 1. Check if a KYCtransaction already exists for this userId and confirmed flag is true
  // and same txnId for an existing userId with confirmed flag true - that means the user is using same aadhar -> success

  //2. Check if a KYCTransaction already exists for this userId and confirmed flag is true
  // 2.1 and a different txnId for an existing userId with confirmed flag true - that means the user is using someone else aadhar -> fraud
  // 2.2 and a different userId for an existing txnId with confirmed flag true - that means same adhaar is being used by multiple users

  //3. Check if a KYCTransaction already exists for this userId and confirmed flag is false
  // 3.1 and same txnId for an existing userId with confirmed flag false - that means the user is retrying KYC with same aadhar -> success
  // 3.2 and a different txnId for an existing userId with confirmed flag false - that means the user is retrying KYC with different aadhar -> fraud

  //4. Check if a KYCTransaction does not exist for this userId
  //then create a new KYCTransaction with confirmed flag false and return the aadhaar details

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

        JsonObject aadhaarJson = parseKYCxml(response.bodyAsString());
        String txnId = aadhaarJson.getString("txn");

        if (txnId == null || txnId.isBlank()) {
          LOGGER.error("Transaction ID missing in Aadhaar response: {}", aadhaarJson.encodePrettily());
          return Future.failedFuture(new DxValidationException("Missing transaction ID from Aadhaar response"));
        }

        UUID uuid = UUID.fromString(userId);

        Map<String,Object> txnMap = Map.of(Constants.TRANSACTION_ID,txnId);

        return kycTransactionDAO.getAllWithFilters(txnMap).compose(existingByTxn -> {
          if (existingByTxn != null && !existingByTxn.isEmpty()) {
            KYCTransaction txn = existingByTxn.get(0);
            if (!txn.userId().equals(uuid)) {
              LOGGER.warn("KYC Aadhaar already used by userId: {}", txn.userId());
              return Future.failedFuture(new DxValidationException("KYC failed: This Aadhaar is already linked to another user."));
            }
          }
          return kycTransactionDAO.get(uuid).compose(existingTransaction -> {
            if (existingTransaction != null) {
              boolean txnMatches = existingTransaction.transactionId().equals(txnId);
              boolean isConfirmed = existingTransaction.isConfirmed();

              if (txnMatches && isConfirmed) {
                LOGGER.warn("KYC already confirmed for userId: {}", userId);
                return Future.failedFuture(new DxValidationException("KYC already confirmed for this user"));
              }

              if (!txnMatches && isConfirmed) {
                LOGGER.warn("User with confirmed KYC trying different Aadhaar. userId: {}", userId);
                return Future.failedFuture(new DxValidationException("KYC failed: Aadhaar mismatch. This account is already verified with a different Aadhaar."));
              }

              if (!isConfirmed) {
                if (!txnMatches) {
                  LOGGER.warn("Unconfirmed KYC, but txnId mismatch. userId: {}", userId);
                  return Future.failedFuture(new DxValidationException("Aadhaar mismatch. You cannot restart KYC with a different Aadhaar."));
                } else {
                  LOGGER.info("Unconfirmed KYC retry allowed for userId: {}", userId);
                  return Future.succeededFuture(aadhaarJson);
                }
              }

              // Shouldn't reach here
              return Future.failedFuture(new DxValidationException("Unexpected KYC state for userId: " + userId));
            } else {
              // No existing transaction — create one
              KYCTransaction newTxn = new KYCTransaction(uuid, txnId, codeVerifier, false, null);
              return kycTransactionDAO.create(newTxn).map(created -> {
                LOGGER.info("KYC transaction created for userId: {}", userId);
                return aadhaarJson;
              });
            }
          });
        });
      });
  }


  //user id , transaction id, code verifier , confirmed flag , timestamp --> kycTransaction

    //TODO remove code_verifier from cachedData
    @Override
    public Future<JsonObject> confirmKYCData(UUID userId, String codeVerifier, String userName) {
        Promise<JsonObject> promise = Promise.promise();

        kycTransactionDAO.get(userId).onComplete(ar->{
            if (ar.failed()) {
                String msg = "KYC retrieval failed for " + userId + ": " + ar.cause().getMessage();
                LOGGER.error(msg);
                promise.fail(new DxValidationException(msg));
                return;
            }

            KYCTransaction kycTransaction = ar.result();
            if (kycTransaction == null) {
                String msg = "No KYC data for userId: " + userId;
                LOGGER.warn(msg);
                promise.fail(new DxValidationException(msg));
                return;
            }
            // Check if code_verifier matches
            boolean isVerified = codeVerifier.equals(kycTransaction.codeVerifier());
            System.out.println("isverified"+isVerified);
            if (isVerified) {
                keycloakUserService.setKycVerifiedTrueWithData(userId,userName,kycTransaction.transactionId())
                    .onComplete(kycResult -> {
                        if (kycResult.succeeded() && kycResult.result()) {
                            LOGGER.info("KYC verification successful for userId: {}", userId);
                            promise.complete(kycTransaction.toJson());
                        } else {
                            String msg = "Failed to update KYC status in Keycloak for userId: " + userId;
                            LOGGER.error(msg);
                            promise.fail(new DxValidationException(msg));
                        }
                    });
                Map<String,Object> conditionMap = Map.of(Constants.USER_ID, userId.toString(),
                        Constants.TRANSACTION_ID, kycTransaction.transactionId());
                Map<String, Object> updateFields = Map.of(Constants.CONFIRMED_FLAG,true,
                        Constants.UPDATED_AT, Instant.now().toString());
                kycTransactionDAO.update(conditionMap,updateFields).onComplete(var->{
                    if (var.succeeded()) {
                        LOGGER.info("KYC transaction updated for userId: {}", userId);
                    } else {
                        LOGGER.error("Failed to update KYC transaction for userId: {}. Error: {}", userId, ar.cause().getMessage());
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
            //System.out.println("jsonObject: " + jsonObject); // For debugging purposes
             // For debugging purposes
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

            if (uidData.containsKey("Pht")) {
                result.put("Pht", uidData.getString("Pht"));
            }

            if(kycRes.containsKey("txn")) {
                result.put("txn", kycRes.getString("txn"));
            }

            result.put("requestedAt", Instant.now().toString());

            return result;

        } catch (DxValidationException ve) {
            throw ve; // rethrow explicitly known validation exceptions
        } catch (Exception e) {
            LOGGER.error("Error while parsing Aadhaar XML data", e);
            throw new DxValidationException("Invalid Aadhaar XML format");
        }
    }
}
