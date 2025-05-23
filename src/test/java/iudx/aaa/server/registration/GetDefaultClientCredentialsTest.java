package iudx.aaa.server.registration;

import static iudx.aaa.server.apiserver.util.Urn.URN_ALREADY_EXISTS;
import static iudx.aaa.server.apiserver.util.Urn.URN_SUCCESS;
import static iudx.aaa.server.registration.Constants.CLIENT_SECRET_BYTES;
import static iudx.aaa.server.registration.Constants.CONFIG_COS_URL;
import static iudx.aaa.server.registration.Constants.CONFIG_OMITTED_SERVERS;
import static iudx.aaa.server.registration.Constants.DEFAULT_CLIENT;
import static iudx.aaa.server.registration.Constants.ERR_DETAIL_DEFAULT_CLIENT_EXISTS;
import static iudx.aaa.server.registration.Constants.ERR_DETAIL_NO_APPROVED_ROLES;
import static iudx.aaa.server.registration.Constants.ERR_TITLE_DEFAULT_CLIENT_EXISTS;
import static iudx.aaa.server.registration.Constants.ERR_TITLE_NO_APPROVED_ROLES;
import static iudx.aaa.server.registration.Constants.RESP_CLIENT_ID;
import static iudx.aaa.server.registration.Constants.RESP_CLIENT_NAME;
import static iudx.aaa.server.registration.Constants.RESP_CLIENT_SC;
import static iudx.aaa.server.registration.Constants.SUCC_TITLE_CREATED_DEFAULT_CLIENT;
import static iudx.aaa.server.registration.Constants.UUID_REGEX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Tuple;
import iudx.aaa.server.apiserver.models.Roles;
import iudx.aaa.server.apiserver.models.User;
import iudx.aaa.server.apiserver.models.User.UserBuilder;
import iudx.aaa.server.configuration.Configuration;
import iudx.aaa.server.token.TokenService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

/** Unit tests for getting default client credentials. */
@ExtendWith(VertxExtension.class)
public class GetDefaultClientCredentialsTest {
  private static Logger LOGGER = LogManager.getLogger(GetDefaultClientCredentialsTest.class);

  private static Configuration config;

  /* Database Properties */
  private static String databaseIP;
  private static int databasePort;
  private static String databaseName;
  private static String databaseSchema;
  private static String databaseUserName;
  private static String databasePassword;
  private static int poolSize;
  private static PgPool pool;
  private static PoolOptions poolOptions;
  private static PgConnectOptions connectOptions;
  private static RegistrationService registrationService;
  private static Vertx vertxObj;

  private static KcAdmin kc = Mockito.mock(KcAdmin.class);
  private static TokenService tokenService = Mockito.mock(TokenService.class);
  private static JsonObject options = new JsonObject();

  private static Utils utils;

  private static final String DUMMY_SERVER_1 =
      "dummy" + RandomStringUtils.randomAlphabetic(5).toLowerCase() + ".iudx.io";

  private static final int CLIENT_SECRET_HEX_LEN = CLIENT_SECRET_BYTES * 2;
  private static final String CLIENT_SECRET_REGEX = "^[0-9a-f]{" + CLIENT_SECRET_HEX_LEN + "}$";

  @BeforeAll
  @DisplayName("Deploying Verticle")
  static void startVertx(Vertx vertx, VertxTestContext testContext) {
    Configuration config = new Configuration();
    vertxObj = vertx;
    JsonObject dbConfig = config.configLoader(1, vertx);

    /* Read the configuration and set the postgres client properties. */
    LOGGER.debug("Info : Reading config file");

    databaseIP = dbConfig.getString("databaseIP");
    databasePort = Integer.parseInt(dbConfig.getString("databasePort"));
    databaseName = dbConfig.getString("databaseName");
    databaseSchema = dbConfig.getString("databaseSchema");
    databaseUserName = dbConfig.getString("databaseUserName");
    databasePassword = dbConfig.getString("databasePassword");
    poolSize = Integer.parseInt(dbConfig.getString("poolSize"));

    /* Set Connection Object and schema */
    if (connectOptions == null) {
      Map<String, String> schemaProp = Map.of("search_path", databaseSchema);

      connectOptions =
          new PgConnectOptions()
              .setPort(databasePort)
              .setHost(databaseIP)
              .setDatabase(databaseName)
              .setUser(databaseUserName)
              .setPassword(databasePassword)
              .setProperties(schemaProp);
    }

    if (poolOptions == null) {
      poolOptions = new PoolOptions().setMaxSize(poolSize);
    }

    options
        .put(CONFIG_OMITTED_SERVERS, dbConfig.getJsonArray(CONFIG_OMITTED_SERVERS))
        .put(CONFIG_COS_URL, dbConfig.getString(CONFIG_COS_URL));
    pool = PgPool.pool(vertx, connectOptions, poolOptions);

    utils = new Utils(pool);

    utils
        .createFakeResourceServer(
            DUMMY_SERVER_1, new UserBuilder().userId(UUID.randomUUID()).build())
        .onSuccess(
            res -> {
              registrationService = new RegistrationServiceImpl(pool, kc, tokenService, options);
              testContext.completeNow();
            })
        .onFailure(err -> testContext.failNow(err.getMessage()));
  }

  @AfterAll
  public static void finish(VertxTestContext testContext) {
    LOGGER.info("Finishing and resetting DB");

    utils
        .deleteFakeResourceServer()
        .compose(res -> utils.deleteFakeUser())
        .onComplete(
            x -> {
              if (x.failed()) {
                LOGGER.warn(x.cause().getMessage());
              }
              vertxObj.close(testContext.succeeding(response -> testContext.completeNow()));
            });
  }

  @Test
  @DisplayName("Test user does not have any roles")
  void userDoesNotHaveRoles(VertxTestContext testContext) {
    User user = new UserBuilder().userId(UUID.randomUUID()).name("Foo", "Bar").build();

    Mockito.when(kc.getEmailId(any())).thenReturn(Future.succeededFuture("email@gmail.com"));

    registrationService
        .getDefaultClientCredentials(user)
        .onComplete(
            testContext.succeeding(
                response ->
                    testContext.verify(
                        () -> {
                          assertEquals(404, response.getInteger("status"));
                          assertEquals(ERR_TITLE_NO_APPROVED_ROLES, response.getString("title"));
                          assertEquals(ERR_DETAIL_NO_APPROVED_ROLES, response.getString("detail"));
                          testContext.completeNow();
                        })));
  }

  @Test
  @DisplayName("Successfully get default client creds")
  void clientRegenSuccess(VertxTestContext testContext) {

    User user =
        new UserBuilder()
            .userId(UUID.randomUUID())
            .roles(List.of(Roles.CONSUMER))
            .rolesToRsMapping(
                Map.of(Roles.CONSUMER.toString(), new JsonArray().add(DUMMY_SERVER_1)))
            .name("aa", "bb")
            .build();

    Future<Void> created = utils.createFakeUser(user, false, false);

    created.onSuccess(
        userJson -> {
          Mockito.when(kc.getEmailId(any()))
              .thenReturn(Future.succeededFuture(utils.getDetails(user).email));

          registrationService
              .getDefaultClientCredentials(user)
              .onComplete(
                  testContext.succeeding(
                      response ->
                          testContext.verify(
                              () -> {
                                assertEquals(201, response.getInteger("status"));
                                assertEquals(
                                    SUCC_TITLE_CREATED_DEFAULT_CLIENT, response.getString("title"));
                                assertEquals(URN_SUCCESS.toString(), response.getString("type"));

                                JsonObject result = response.getJsonObject("results");

                                assertTrue(result.containsKey(RESP_CLIENT_NAME));
                                assertTrue(
                                    result.getString(RESP_CLIENT_NAME).equals(DEFAULT_CLIENT));
                                assertTrue(result.containsKey(RESP_CLIENT_ID));
                                String clientId = result.getString(RESP_CLIENT_ID);
                                assertTrue(clientId.matches(UUID_REGEX));

                                assertTrue(result.containsKey(RESP_CLIENT_SC));
                                String clientSec = result.getString(RESP_CLIENT_SC);
                                assertTrue(clientSec.matches(CLIENT_SECRET_REGEX));

                                testContext.completeNow();
                              })));
        });
  }

  @Test
  @DisplayName("Cannot get default credentials if already obtained")
  void alreadyGotDefaultClientCreds(VertxTestContext testContext) {

    User user =
        new UserBuilder()
            .userId(UUID.randomUUID())
            .roles(List.of(Roles.CONSUMER))
            .rolesToRsMapping(
                Map.of(Roles.CONSUMER.toString(), new JsonArray().add(DUMMY_SERVER_1)))
            .name("aa", "bb")
            .build();

    Future<Void> created = utils.createFakeUser(user, false, false);

    created.onSuccess(
        userJson -> {
          Mockito.when(kc.getEmailId(any()))
              .thenReturn(Future.succeededFuture(utils.getDetails(user).email));

          registrationService
              .getDefaultClientCredentials(user)
              .onComplete(
                  testContext.succeeding(
                      response ->
                          testContext.verify(
                              () -> {
                                assertEquals(201, response.getInteger("status"));
                                assertEquals(
                                    SUCC_TITLE_CREATED_DEFAULT_CLIENT, response.getString("title"));
                                assertEquals(URN_SUCCESS.toString(), response.getString("type"));

                                JsonObject result = response.getJsonObject("results");

                                assertTrue(result.containsKey(RESP_CLIENT_NAME));
                                assertTrue(
                                    result.getString(RESP_CLIENT_NAME).equals(DEFAULT_CLIENT));
                                assertTrue(result.containsKey(RESP_CLIENT_ID));
                                String clientId = result.getString(RESP_CLIENT_ID);
                                assertTrue(clientId.matches(UUID_REGEX));

                                assertTrue(result.containsKey(RESP_CLIENT_SC));
                                String clientSec = result.getString(RESP_CLIENT_SC);
                                assertTrue(clientSec.matches(CLIENT_SECRET_REGEX));

                                registrationService
                                    .getDefaultClientCredentials(user)
                                    .onComplete(
                                        testContext.succeeding(
                                            failResponse ->
                                                testContext.verify(
                                                    () -> {
                                                      assertEquals(
                                                          409, failResponse.getInteger("status"));
                                                      assertEquals(
                                                          ERR_TITLE_DEFAULT_CLIENT_EXISTS,
                                                          failResponse.getString("title"));
                                                      assertEquals(
                                                          ERR_DETAIL_DEFAULT_CLIENT_EXISTS,
                                                          failResponse.getString("detail"));
                                                      assertEquals(
                                                          URN_ALREADY_EXISTS.toString(),
                                                          failResponse.getString("type"));

                                                      assertTrue(
                                                          failResponse.containsKey("context"));
                                                      JsonObject context =
                                                          failResponse.getJsonObject("context");
                                                      assertTrue(
                                                          context
                                                              .getString(RESP_CLIENT_ID)
                                                              .equals(clientId));
                                                      testContext.completeNow();
                                                    })));
                              })));
        });
  }

  @Test
  @DisplayName(
      "Check if COS admin is added to user table in DB when getting creds if not inserted before")
  void cosAdminEdgeCase(VertxTestContext testContext) {

    User cosAdminUser =
        new UserBuilder()
            .userId(UUID.randomUUID())
            .roles(List.of(Roles.COS_ADMIN))
            .name("aa", "bb")
            .build();

    String cosAdminEmail = RandomStringUtils.randomAlphabetic(10) + "@gmail.com";

    Mockito.when(kc.findUserByEmail(cosAdminEmail))
        .thenReturn(
            Future.succeededFuture(
                new JsonObject()
                    .put("keycloakId", cosAdminUser.getUserId())
                    .put("email", cosAdminEmail)
                    .put(
                        "name",
                        new JsonObject().put("firstName", "cos").put("lastName", "admin"))));

    Checkpoint cosAdminNotInDb = testContext.checkpoint();
    Checkpoint clientCreated = testContext.checkpoint();
    Checkpoint entryMadeInDb = testContext.checkpoint();

    pool.withConnection(
            conn ->
                conn.preparedQuery("SELECT id FROM users WHERE id = $1::UUID")
                    .execute(Tuple.of(cosAdminUser.getUserId())))
        .compose(
            row -> {
              if (row.rowCount() == 0) {
                cosAdminNotInDb.flag();
              } else {
                testContext.failNow("failed");
              }
              return Future.succeededFuture();
            })
        .map(
            next ->
                registrationService
                    .getDefaultClientCredentials(cosAdminUser)
                    .onComplete(
                        testContext.succeeding(
                            response ->
                                testContext.verify(
                                    () -> {
                                      assertEquals(201, response.getInteger("status"));
                                      assertEquals(
                                          SUCC_TITLE_CREATED_DEFAULT_CLIENT,
                                          response.getString("title"));
                                      assertEquals(
                                          URN_SUCCESS.toString(), response.getString("type"));

                                      JsonObject result = response.getJsonObject("results");

                                      assertTrue(result.containsKey(RESP_CLIENT_NAME));
                                      assertTrue(
                                          result
                                              .getString(RESP_CLIENT_NAME)
                                              .equals(DEFAULT_CLIENT));
                                      assertTrue(result.containsKey(RESP_CLIENT_ID));
                                      String clientId = result.getString(RESP_CLIENT_ID);
                                      assertTrue(clientId.matches(UUID_REGEX));

                                      assertTrue(result.containsKey(RESP_CLIENT_SC));
                                      String clientSec = result.getString(RESP_CLIENT_SC);
                                      assertTrue(clientSec.matches(CLIENT_SECRET_REGEX));

                                      clientCreated.flag();
                                      pool.withConnection(
                                              conn ->
                                                  conn.preparedQuery(
                                                          "SELECT id FROM users WHERE id = $1::UUID")
                                                      .execute(Tuple.of(cosAdminUser.getUserId())))
                                          .onSuccess(
                                              rows -> {
                                                if (rows.rowCount() == 1) {
                                                  entryMadeInDb.flag();
                                                } else {
                                                  testContext.failNow("failed");
                                                }
                                              })
                                          .onFailure(
                                              fail -> testContext.failNow(fail.getMessage()));
                                    }))));
  }
}
