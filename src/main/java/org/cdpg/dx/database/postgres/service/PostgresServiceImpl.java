package org.cdpg.dx.database.postgres.service;

import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Tuple;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Row;
import org.cdpg.dx.database.postgres.models.*;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostgresServiceImpl implements PostgresService {
    private final PgPool client;
    private static final Logger LOG = LoggerFactory.getLogger(PostgresServiceImpl.class);

    public PostgresServiceImpl(PgPool client) {
        System.out.println("inside the constructor : hereee");
        this.client = client;
    }

    private QueryResult convertToQueryResult(RowSet<Row> rowSet) {
      System.out.println("Inside convertToQueryResult");
      JsonArray jsonArray = new JsonArray();

        for (Row row : rowSet) {
          JsonObject json = new JsonObject();
            for (int i = 0; i < row.size(); i++) {
               // json.put(row.getColumnName(i), row.getValue(i));
              Object value = row.getValue(i);
              if (value instanceof UUID) {
                json.put(row.getColumnName(i), value.toString());
              } else if (value instanceof java.time.LocalDateTime) {
                json.put(row.getColumnName(i), value.toString());  // or format if needed
              } else {
                json.put(row.getColumnName(i), value);
              }
            }
          jsonArray.add(json);
        }



        boolean rowsAffected = rowSet.rowCount() > 0; // Check if any rows were affected
        if(rowsAffected)
      {
        System.out.println("Rows affected :"+rowSet.rowCount());
      }
      else
      {
        System.out.println("Rows unaffected");
      }
      System.out.println("Returned rows: " + jsonArray.encodePrettily());

      QueryResult queryResult = new QueryResult();
      queryResult.setRows(jsonArray);
      queryResult.setTotalCount(rowSet.rowCount());
      queryResult.setHasMore(false);
      queryResult.setRowsAffected(rowsAffected);
      //return new QueryResult(jsonArray, jsonArray.size(), false, rowsAffected);

      return queryResult;
    }

  private Future<QueryResult> executeQuery(String sql, List<Object> params) {
    System.out.println("Executing SQL: " + sql);
    System.out.println("With parameters: " + params);

    try {
      // Print param types for debug
      for (Object param : params) {
        System.out.println("Param type: " + (param != null ? param.getClass().getSimpleName() : "null") + ", value: " + param);
      }

      Tuple tuple = Tuple.from(params);

      return client
        .preparedQuery(sql)
        .execute(tuple)
        .map(rowSet -> {
          try {
            System.out.println("Query executed successfully.");
            return convertToQueryResult(rowSet);
          } catch (Exception ex) {
            System.out.println("Exception inside map(): " + ex.getMessage());
            ex.printStackTrace();
            throw ex;
          }
        })
        .onFailure(err -> {
          System.err.println("SQL execution error: " + err.getMessage());
          err.printStackTrace();
        });
    } catch (Exception e) {
      System.err.println("Exception while building Tuple or executing query: " + e.getMessage());
      e.printStackTrace();
      return Future.failedFuture("Error in PostgresServiceImpl: " + e.getMessage());
    }
  }


//    private Future<QueryResult> executeQuery(String sql, List<Object> params) {
//      System.out.println("SQL: "+sql);
//      try
//      {
//        System.out.println(Tuple.from(params));
//        return client.preparedQuery(sql).execute(Tuple.from(params))
//          .map(this::convertToQueryResult);
//      }
//      catch(Exception e)
//      {
//        return Future.failedFuture("Error found in postgresImpl"+e.getMessage());
//      }


//      return client.preparedQuery(sql).execute(Tuple.from(params))
//            .map(this::convertToQueryResult);
  //  }

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
