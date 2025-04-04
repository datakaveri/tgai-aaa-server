package org.cdpg.dx.database.postgres.service;

import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Tuple;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Row;
import org.cdpg.dx.database.postgres.models.*;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostgresServiceImpl implements PostgresService {
    private final PgPool client;
    private static final Logger LOG = LoggerFactory.getLogger(PostgresServiceImpl.class);

    public PostgresServiceImpl(PgPool client) {
        LOG.info("inside the constructor : hereee");
        this.client = client;
    }

    private QueryResult convertToQueryResult(RowSet<Row> rowSet) {
        JsonArray jsonArray = new JsonArray();

        for (Row row : rowSet) {
            JsonObject json = new JsonObject();
            for (int i = 0; i < row.size(); i++) {
                json.put(row.getColumnName(i), row.getValue(i));
            }
            jsonArray.add(json);
        }

        boolean rowsAffected = rowSet.rowCount() > 0; // Check if any rows were affected

        return new QueryResult(jsonArray, jsonArray.size(), false, rowsAffected);
    }

    private Future<QueryResult> executeQuery(String sql, List<Object> params) {
        return client.preparedQuery(sql).execute(Tuple.from(params))
            .map(this::convertToQueryResult);
    }

//    @Override
//    public Future<QueryResult> execute(Query query) {
//        return executeQuery(query.toSQL(), query.getQueryParams());
//    }

    @Override
    public Future<QueryResult> insert(InsertQuery query) {
        return executeQuery(query.toSQL(), query.getQueryParams());
    }

    @Override
    public Future<QueryResult> update(UpdateQuery query) {
        return executeQuery(query.toSQL(), query.getQueryParams());
    }

    @Override
    public Future<QueryResult> delete(DeleteQuery query) {
        return executeQuery(query.toSQL(), query.getQueryParams());
    }

    @Override
    public Future<QueryResult> select(SelectQuery query) {
        return executeQuery(query.toSQL(), query.getQueryParams());
    }
}
