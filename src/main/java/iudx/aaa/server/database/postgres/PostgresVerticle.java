package iudx.aaa.server.database.postgres;

import static io.vertx.pgclient.PgPool.*;
import static iudx.aaa.server.util.Constants.PG_SERVICE_ADDRESS;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.sqlclient.PoolOptions;
import iudx.aaa.server.database.postgres.service.PostgresService;
import iudx.aaa.server.database.postgres.service.PostgresServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class PostgresVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LogManager.getLogger(PostgresVerticle.class);
  private MessageConsumer<JsonObject> consumer;
  private ServiceBinder binder;

  private PoolOptions poolOptions;
  private PgPool pool;

  private String databaseIp;
  private int databasePort;
  private String databaseName;
  private String databaseUserName;
  private String databasePassword;
  private int poolSize;

  private PostgresService pgService;

  @Override
  public void start() throws Exception {

    databaseIp = config().getString("databaseIP");
    databasePort = config().getInteger("databasePort");
    databaseName = config().getString("databaseName");
    databaseUserName = config().getString("databaseUserName");
    databasePassword = config().getString("databasePassword");
    poolSize = config().getInteger("poolSize");

    LOGGER.info("DATABASE IP "+databaseIp);
    LOGGER.info("DATABASE PORT "+databasePort);
    LOGGER.info("DATABASE NAME "+databaseName);
    LOGGER.info("DATABASE USER NAME "+databaseUserName);
    LOGGER.info("DATABASE USER PASSWORD "+databasePassword);
    LOGGER.info("DATABASE Pool Size "+poolSize);



    PgConnectOptions connectOptions = new PgConnectOptions()
      .setPort(databasePort)
      .setHost(databaseIp)
      .setDatabase(databaseName)
      .setUser(databaseUserName)
      .setPassword(databasePassword)
      .setReconnectAttempts(2)
      .setReconnectInterval(1000L);

    this.poolOptions = new PoolOptions().setMaxSize(poolSize);
    this.pool = pool(vertx, connectOptions, poolOptions);

    pgService = new PostgresServiceImpl(this.pool);

    binder = new ServiceBinder(vertx);
    consumer = binder.setAddress(PG_SERVICE_ADDRESS).register(PostgresService.class, pgService);
    LOGGER.info("Postgres verticle started.");
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}
