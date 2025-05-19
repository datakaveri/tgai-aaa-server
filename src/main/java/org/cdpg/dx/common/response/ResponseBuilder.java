package org.cdpg.dx.common.response;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.cdpg.dx.common.HttpStatusCode;

public class ResponseBuilder {

  public static <T> DxResponse<T> success(String detail, T result) {
    HttpStatusCode code = HttpStatusCode.SUCCESS;
    return new DxResponse<>(code.getUrn(), code.getDescription(), detail, result);
  }

  public static DxResponse<Void> success(String detail) {
    return success(detail, null);
  }

  public static <T> void send(RoutingContext ctx, HttpStatusCode status, String detail, T result) {
    if (status == HttpStatusCode.NO_CONTENT) {
      ctx.response().setStatusCode(status.getValue()).end();
      return;
    }
    DxResponse<T> response =
        new DxResponse<>(status.getUrn(), status.getDescription(), detail, result);
    ctx.response()
        .setStatusCode(status.getValue())
        .putHeader("Content-Type", "application/json")
        .end(JsonObject.mapFrom(response).encode());
  }

  public static void sendSuccess(RoutingContext ctx, String detail) {
    send(ctx, HttpStatusCode.SUCCESS, detail, null);
  }

  public static <T> void sendSuccess(RoutingContext ctx, T result) {
    send(ctx, HttpStatusCode.SUCCESS, null, result);
  }

  public static <T> void sendCreated(RoutingContext ctx, String detail, T result) {
    send(ctx, HttpStatusCode.CREATED, detail, result);
  }

  public static void sendCreated(RoutingContext ctx, String detail) {
    send(ctx, HttpStatusCode.CREATED, detail, null);
  }

  public static void sendNoContent(RoutingContext ctx) {
    send(ctx, HttpStatusCode.NO_CONTENT, null, null);
  }

  public static void sendAccepted(RoutingContext ctx, String detail) {
    send(ctx, HttpStatusCode.ACCEPTED, detail, null);
  }

  public static void sendProcessing(RoutingContext ctx, String detail) {
    send(ctx, HttpStatusCode.PROCESSING, detail, null);
  }
}
