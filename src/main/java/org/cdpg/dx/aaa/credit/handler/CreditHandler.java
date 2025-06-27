package org.cdpg.dx.aaa.credit.handler;

import io.vertx.core.Future;
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
import org.cdpg.dx.aaa.email.util.EmailComposer;
import org.cdpg.dx.aaa.user.service.UserService;
import org.cdpg.dx.common.HttpStatusCode;
import org.cdpg.dx.common.exception.DxBadRequestException;
import org.cdpg.dx.common.exception.DxNotFoundException;
import org.cdpg.dx.common.exception.DxValidationException;
import org.cdpg.dx.common.request.PaginatedRequest;
import org.cdpg.dx.common.request.PaginationRequestBuilder;
import org.cdpg.dx.common.response.ResponseBuilder;
import org.cdpg.dx.common.util.PaginationInfo;
import org.cdpg.dx.common.util.RequestHelper;

import java.util.*;

import static org.cdpg.dx.aaa.organization.config.Constants.*;
import static org.cdpg.dx.database.postgres.util.Constants.DEFAULT_SORTING_ORDER;

public class CreditHandler {

  private static final Logger LOGGER = LogManager.getLogger(CreditHandler.class);
    private final CreditService creditService;
    private final EmailComposer emailComposer;
    private final UserService userService;


  public CreditHandler(CreditService creditService, EmailComposer emailComposer, UserService userService) {
    this.creditService = creditService;
    this.emailComposer = emailComposer;
    this.userService = userService;
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


  public void getBalance(RoutingContext ctx) {

    User user = ctx.user();
    UUID userId = UUID.fromString(user.subject());

    creditService.getBalance(userId)
      .onSuccess(balance -> {
        ResponseBuilder.sendSuccess(ctx,new JsonObject(Map.of("balance", balance)));
      })
      .onFailure(ctx::fail);
  }

    public void getBalanceofUser(RoutingContext ctx) {
        UUID userId = RequestHelper.getPathParamAsUUID(ctx, "id");
        creditService.getBalance(userId)
                .onSuccess(balance -> {
                    ResponseBuilder.sendSuccess(ctx,new JsonObject(Map.of("user_id", userId, "balance", balance)));
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

    Double amount = null;

    if (status == Status.GRANTED && creditRequestJson.getValue("amount") == null) {
      throw new DxBadRequestException("Amount is required for GRANTED status");
    }


    amount = creditRequestJson.getDouble("amount");

    creditService.updateCreditRequestStatus( requestId, status, transactedBy,amount)
      .onSuccess(transaction -> {
          ResponseBuilder.sendSuccess(ctx,  transaction);
        // Send email notification
        Future<Void> future = emailComposer.sendUserEmailForCreditApproval(requestId);

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

  public void addCredits(RoutingContext ctx) {
    JsonObject creditAdditionJson = ctx.body().asJsonObject();

    User user = ctx.user();
    UUID transactedBy = UUID.fromString(user.subject());
    creditAdditionJson.put("transacted_by", transactedBy.toString());

    JsonObject responseObject = creditAdditionJson.copy();

    // pass userId and userName from json
    CreditTransaction creditTransaction = CreditTransaction.fromJson(creditAdditionJson);
    creditService.addCredits(creditTransaction)
      .onSuccess(res -> {
        ResponseBuilder.sendSuccess(ctx, res);
      })
      .onFailure(ctx::fail);
  }

  public void createComputeRoleRequest(RoutingContext ctx) {

    User user = ctx.user();
    String userID = user.subject();
    String userName = user.principal().getString("name");

    JsonObject computeRoleJsonBody = ctx.body().asJsonObject();
      System.out.println("Additional Info: " + computeRoleJsonBody);

    JsonObject additionalInfo = computeRoleJsonBody.getJsonObject("additional_info");

    ComputeRole computeRoleRequest = ComputeRole.fromJson(new JsonObject().put("user_id", userID).put("user_name", userName).put("additional_info", additionalInfo));

    creditService.getComputeRoleRequestByUserId(UUID.fromString(userID))
            .recover(err -> {
                if (err instanceof DxNotFoundException) {
                    return Future.succeededFuture(null);
                }
                // For other errors, propagate
                return Future.failedFuture(err);
            })
            .compose(existingComputeRole -> {
                System.out.println("here in compose block");
              if (existingComputeRole != null && existingComputeRole.status().equalsIgnoreCase(Status.REJECTED.getStatus())) {
                return creditService.updateComputeRoleStatus(existingComputeRole.id(), Status.PENDING, existingComputeRole.approvedBy())
                        .map(updated -> true);
              } else {
                // Otherwise, create a new compute role request
                return Future.succeededFuture(false);
              }
            })
            .compose(updated -> {
                System.out.println("here in compose block after update" + updated);
              if (!updated) {
                return creditService.createComputeRoleRequest(computeRoleRequest)
                        .onSuccess(requests -> {
                          ResponseBuilder.sendSuccess(ctx, requests);
                          emailComposer.sendEmailForComputeRole(computeRoleRequest, user);
                        })
                        .onFailure(ctx::fail)
                        .mapEmpty();
              } else {
                return Future.succeededFuture();
              }
            })
            .onSuccess(v -> {
                System.out.println("here in onSuccess block");
              ResponseBuilder.sendSuccess(ctx, "Compute Role Request created successfully");
            })
            .onFailure(ctx::fail);

  }

  public void getAllComputeRequests(RoutingContext ctx) {

    PaginatedRequest request = PaginationRequestBuilder.from(ctx)
            .allowedFiltersDbMap(ALLOWED_FILTER_MAP_FOR_COMPUTE_ROLE)
            .apiToDbMap(ALLOWED_FILTER_MAP_FOR_COMPUTE_ROLE)
            .additionalFilters(Map.of())
            .allowedTimeFields(Set.of(CREATED_AT))
            .defaultTimeField(CREATED_AT)
            .defaultSort(CREATED_AT, DEFAULT_SORTING_ORDER)
            .allowedSortFields(ALLOWED_FILTER_MAP_FOR_COMPUTE_ROLE.keySet())
            .build();

    creditService.getAllComputeRequests(request)
      .compose(result->{
        List<Future<JsonObject>> enrichedFutures =result.data().stream()
          .map(computeRequest->userService.getUserInfoByID(computeRequest.userId())
            .map(user->{
              JsonObject enriched = computeRequest.toJson();
              enriched.put("roles",user.roles());
              enriched.put("account_enabled", user.account_enabled());
              return enriched;
            })
            .recover(err->{
              System.out.println("In recover block!");
              JsonObject enriched = computeRequest.toJson();
              enriched.put("roles", List.of());
              return Future.succeededFuture(enriched);
            })
          )
          .toList();


        return Future.all(enrichedFutures).map(cf -> {
          List<JsonObject> enrichedList = new ArrayList<>();
          for (int i = 0; i < cf.size(); i++) {
            enrichedList.add((JsonObject) cf.resultAt(i));
          }
          return Map.entry(enrichedList, result.paginationInfo());
        });
      })
      .onSuccess(entry -> {
        List<JsonObject> enrichedList = entry.getKey();
        PaginationInfo paginationInfo = entry.getValue();
        ResponseBuilder.sendSuccess(ctx, enrichedList, paginationInfo);
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
              ResponseBuilder.sendSuccess(ctx, "Compute Role Status " + status.getStatus());
              if (updated) {
                // Send email notification
                Future<Void> future = emailComposer.sendUserEmailForComputeRoleApproval(requestId);
              }
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


