package org.cdpg.dx.aaa.credit.handler;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.aaa.credit.models.ComputeRole;
import org.cdpg.dx.aaa.credit.models.CreditRequest;
import org.cdpg.dx.aaa.credit.models.CreditTransaction;
import org.cdpg.dx.aaa.credit.models.Status;
import org.cdpg.dx.aaa.credit.service.CreditService;
import org.cdpg.dx.common.exception.DxNotFoundException;
import org.cdpg.dx.common.request.PaginatedRequest;
import org.cdpg.dx.common.request.PaginationRequestBuilder;
import org.cdpg.dx.common.response.ResponseBuilder;
import org.cdpg.dx.common.util.RequestHelper;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.cdpg.dx.aaa.organization.config.Constants.*;
import static org.cdpg.dx.database.postgres.util.Constants.DEFAULT_SORTIMG_ORDER;

public class CreditHandler {

  private static final Logger LOGGER = LogManager.getLogger(CreditHandler.class);
    private final CreditService creditService;


  public CreditHandler(CreditService creditService) {
    this.creditService = creditService;
  }


  public void createCreditRequest(RoutingContext ctx) {
    JsonObject creditRequestJson = ctx.body().asJsonObject();

    CreditRequest creditRequest;
    User user = ctx.user();
    creditRequestJson.put("user_id", user.subject());

    String userName = user.principal().getString("name");
    creditRequestJson.put("user_name", userName);

    creditRequest = CreditRequest.fromJson(creditRequestJson);

    creditService.createCreditRequest(creditRequest)
      .onSuccess(requests -> {
        ResponseBuilder.sendSuccess(ctx, requests);

      })
      .onFailure(ctx::fail);
  }

  public void getAllPendingCreditRequests(RoutingContext ctx) {

    creditService.getAllPendingCreditRequests()
      .onSuccess(orgs -> {
        ResponseBuilder.sendSuccess(ctx, orgs);
      })
      .onFailure(ctx::fail);

  }


  public void updateCreditRequestStatus(RoutingContext ctx) {

    JsonObject creditRequestJson = ctx.body().asJsonObject();

    JsonObject responseObject = creditRequestJson.copy();
    responseObject.remove("status");

    User user = ctx.user();
    UUID transactedBy = UUID.fromString(user.subject());
    Status status = Status.fromString(creditRequestJson.getString("status"));
    UUID requestId = UUID.fromString(creditRequestJson.getString("id"));


    creditService.updateCreditRequestStatus( requestId, status, transactedBy)
      .onSuccess(transaction -> {
          ResponseBuilder.sendSuccess(ctx,  transaction);

      }).onFailure(ctx::fail);

  }


  public void deductCredits(RoutingContext ctx) {
    JsonObject creditDeductionJson = ctx.body().asJsonObject();

    User user = ctx.user();
    UUID transactedBy = UUID.fromString(user.subject());
    creditDeductionJson.put("transacted_by", transactedBy.toString());

    JsonObject responseObject = creditDeductionJson.copy();

    // pass userId and userName from json
    CreditTransaction creditTransaction = CreditTransaction.fromJson(creditDeductionJson);
    creditService.deductCredits(creditTransaction)
      .onSuccess(res -> {
        ResponseBuilder.sendSuccess(ctx, res);
      })
      .onFailure(ctx::fail);
  }

  public void createComputeRoleRequest(RoutingContext ctx) {

    User user = ctx.user();
    String userID = user.subject();
    String userName = user.principal().getString("name");

    ComputeRole computeRoleRequest = ComputeRole.fromJson(new JsonObject().put("user_id", userID).put("user_name", userName));

    creditService.createComputeRoleRequest(computeRoleRequest)
      .onSuccess(requests -> {
        ResponseBuilder.sendSuccess(ctx, requests);
      })
      .onFailure(ctx::fail);
  }

  public void getAllComputeRequests(RoutingContext ctx) {

    PaginatedRequest request = PaginationRequestBuilder.from(ctx)
            .allowedFiltersDbMap(ALLOWED_FILTER_MAP_FOR_COMPUTE_ROLE)
            .additionalFilters(Map.of())
            .allowedTimeFields(Set.of(CREATED_AT))
            .defaultTimeField(CREATED_AT)
            .defaultSort(CREATED_AT, DEFAULT_SORTIMG_ORDER)
            .allowedSortFields(ALLOWED_SORT_FIELDS_COMPUTE_ROLE)
            .build();

    creditService.getAllComputeRequests(request)
      .onSuccess(reqs -> {
        ResponseBuilder.sendSuccess(ctx, reqs.data(), reqs.paginationInfo());
      })
      .onFailure(ctx::fail);

  }

  public void updateComputeRoleStatus(RoutingContext ctx) {

    JsonObject creditRequestJson = ctx.body().asJsonObject();

    User user = ctx.user();
    UUID approvedBy = UUID.fromString(user.subject());
    Status status = Status.fromString(creditRequestJson.getString("status"));
    UUID requestId = RequestHelper.getPathParamAsUUID(ctx,"id");


    creditService.updateComputeRoleStatus( requestId, status, approvedBy)
            .onSuccess(updated -> {
              ResponseBuilder.sendSuccess(ctx, "Updated Resquest ");
            })
            .onFailure(ctx::fail);
  }

  public void hasUserComputeAccess(RoutingContext ctx) {

    User user = ctx.user();
    UUID userId = UUID.fromString(user.subject());

    creditService.hasUserComputeAccess(userId)
      .onSuccess(requests -> {
        ResponseBuilder.sendSuccess(ctx, requests);

      })
      .onFailure(ctx::fail);
  }

}


