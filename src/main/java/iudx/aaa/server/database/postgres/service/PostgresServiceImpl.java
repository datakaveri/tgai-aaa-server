package iudx.aaa.server.database.postgres.service;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import iudx.aaa.server.apiserver.util.RespBuilder;
import iudx.aaa.server.common.models.Query;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static iudx.aaa.server.util.Constants.*;

public class PostgresServiceImpl implements PostgresService {
  private static final Logger LOGGER = LogManager.getLogger(PostgresServiceImpl.class);
  private final PgPool client;

  public PostgresServiceImpl(final PgPool pgclient) {
    this.client = pgclient;
  }

  @Override
  public Future<JsonObject> executeQuery(final String query) {
    Promise<JsonObject> promise = Promise.promise();

    Collector<Row, ?, List<JsonObject>> rowCollector =
      Collectors.mapping(row -> row.toJson(), Collectors.toList());

    client
      .withConnection(
        connection ->
          connection.query(query).collecting(rowCollector).execute().map(row -> row.value())
            .onSuccess(
              successHandler -> {
                JsonArray result = new JsonArray(successHandler);
                RespBuilder respBuilder =
                  new RespBuilder().withType(TYPE_SUCCESS).withTitle(SUCCESS).withResult(result);

                promise.complete(respBuilder.getJsonResponse());
              })
            .onFailure(
              failureHandler -> {
                LOGGER.debug(failureHandler);
                RespBuilder respBuilder =
                  new RespBuilder()
                    .withType(TYPE_DB_ERROR)
                    .withTitle(TITLE_DB_ERROR)
                    .withDetail(failureHandler.getLocalizedMessage());

                promise.fail(respBuilder.getResponse());
              })
      );
    return promise.future();

  }

  @Override
  public Future<JsonObject> executeInsert(final String query, JsonArray params) {
    Promise<JsonObject> promise = Promise.promise();

    client.withConnection(connection -> {
        // Convert JsonArray params into a Tuple dynamically
        Tuple tuple = Tuple.tuple();
        for (int i = 0; i < params.size(); i++) {
          tuple.addValue(params.getValue(i)); // Adds value dynamically
        }

        return connection.preparedQuery(query).execute(tuple);
      })
      .onSuccess(successHandler -> {
        RespBuilder respBuilder = new RespBuilder().withType(TYPE_SUCCESS).withTitle(SUCCESS);
        promise.complete(respBuilder.getJsonResponse());
      })
      .onFailure(failureHandler -> {
        LOGGER.debug("Database insertion failed", failureHandler);
        RespBuilder respBuilder = new RespBuilder()
          .withType(TYPE_DB_ERROR)
          .withTitle(TITLE_DB_ERROR)
          .withDetail(failureHandler.getLocalizedMessage());
        promise.fail(respBuilder.getResponse());
      });

    return promise.future();
  }

}
