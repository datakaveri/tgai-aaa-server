package org.cdpg.dx.aaa.credit.service;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import org.cdpg.dx.aaa.credit.dao.*;
import org.cdpg.dx.aaa.credit.models.*;
import org.cdpg.dx.aaa.organization.service.OrganizationServiceImpl;
import org.cdpg.dx.aaa.organization.util.Constants;
import org.cdpg.dx.auth.authorization.model.DxRole;
import org.cdpg.dx.common.exception.BaseDxException;
import org.cdpg.dx.common.exception.DxConflictException;
import org.cdpg.dx.common.exception.DxNotFoundException;
import org.cdpg.dx.common.exception.NoRowFoundException;
import org.cdpg.dx.keyclock.service.KeycloakUserService;
import org.postgresql.core.TransactionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.cdpg.dx.aaa.credit.util.Constants.*;

public class CreditServiceImpl implements CreditService {

  private static final Logger LOG = LoggerFactory.getLogger(CreditServiceImpl.class);

  private final CreditRequestDAO creditRequestDAO;
  private final UserCreditDAO userCreditDAO;
  private final CreditTransactionDAO creditTransactionDAO;
  private final ComputeRoleDAO computeRoleDAO;
  private final KeycloakUserService keycloakUserService;


  public CreditServiceImpl(CreditDAOFactory factory, KeycloakUserService keycloakUserService) {
    this.creditRequestDAO = factory.creditRequestDAO();
    this.userCreditDAO = factory.userCreditDAO();
    this.creditTransactionDAO = factory.creditTransactionDAO();
    this.computeRoleDAO = factory.computeRoleDAO();
    this.keycloakUserService = keycloakUserService;
  }

  @Override
  public Future<CreditRequest> createCreditRequest(CreditRequest creditRequest) {
    return creditRequestDAO.create(creditRequest);
  }


  @Override
  public Future<List<CreditRequest>> getAllPendingCreditRequests() {
    Map<String,Object> conditionMap = Map.of(Constants.STATUS, Status.PENDING.getStatus());

    return creditRequestDAO.getAllWithFilters(conditionMap);
  }


  // ***************************************************************************************
  @Override
  public Future<Boolean> updateCreditRequestStatus(UUID requestId, Status status, UUID transactedBy) {
    LOG.info("Updating credit request status for requestId: {} to: {}", requestId, status);
    return creditRequestDAO.update(Map.of(CREDIT_REQUEST_ID, requestId.toString()), Map.of(STATUS, status.getStatus()))
      .compose(updated -> {
        if (!updated) return Future.succeededFuture(false);
        if (status != Status.GRANTED) return Future.succeededFuture(true);

        return processCreditGrant(requestId, transactedBy);
      })
      .recover(err -> {
        BaseDxException dxEx = BaseDxException.from(err);
        if (dxEx instanceof NoRowFoundException) {
          return Future.failedFuture(new DxNotFoundException("No request found with given ID", dxEx));
        }
        return Future.failedFuture(dxEx);
      });
  }

  private Future<Boolean> processCreditGrant(UUID requestId, UUID transactedBy) {
    return creditRequestDAO.get(requestId).compose(cr -> {
      UUID userId = cr.userId();
      double amount = cr.amount();
      ZonedDateTime indiaTime = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));


      return userCreditDAO.get(userId)
        .recover(err -> {
          if (err.getMessage() != null && err.getMessage().toLowerCase().contains("no rows")) {
            LOG.info("No entry in user_credits for userId: {}, creating with 0 balance", userId);
            UserCredit newCredit = new UserCredit(null, userId, 0.0, null);
            return userCreditDAO.create(newCredit).map(newCredit);
          } else {
            return Future.failedFuture(err); // genuine failure
          }
        })
        .compose(ignored -> getBalance(userId))
        .compose(balance -> {
          double newBalance = balance + amount;
          LOG.info("Current balance: {}, New balance after crediting {}: {}", balance, amount, newBalance);

          Map<String, Object> updateMap = Map.of(BALANCE,newBalance);
          Map<String, Object> conditionMap = Map.of(USER_ID, userId.toString());

          return userCreditDAO.update(conditionMap, updateMap).compose(updated -> {
            if (!updated) {
              return Future.failedFuture("Failed to update balance.");
            }
            LOG.info("Updated balance for userId: {} to {}", userId, newBalance);
            return Future.succeededFuture();
          });
        })
      .compose(v -> createCreditTransaction(userId, amount, transactedBy,indiaTime.toLocalDateTime())
        .map(t -> true));
    });
  }



  private Future<Boolean> createCreditTransaction(UUID userId, double amount, UUID transactedBy,LocalDateTime reqAt) {
    CreditTransaction creditTransaction = new CreditTransaction(
      null,
      userId,
      amount,
      transactedBy,
      TransactionStatus.SUCCESS.getStatus(),
      TransactionType.CREDIT.getType(),
      null,
      reqAt
    );
    return creditTransactionDAO.create(creditTransaction).map(true);
  }


  //**************************************************************************************


  @Override
  public Future<Boolean> deductCredits(CreditTransaction creditTransaction) {
    LocalDateTime reqAt = creditTransaction.requestedAt();

    UUID userId = creditTransaction.userId();

    if (creditTransaction.amount() == null) {
      return Future.failedFuture("Amount is missing in CreditTransaction");
    }

    Double amount = creditTransaction.amount();

    // Check for duplicate request using requestedAt
    Map<String, Object> filter = Map.of(REQUESTED_AT, reqAt.toString());
    return creditTransactionDAO.getAllWithFilters(filter).compose(existingTransactions -> {
      if (existingTransactions != null && !existingTransactions.isEmpty()) {
        // Duplicate request — already handled
        return Future.failedFuture(new DxConflictException("Credit Transaction not valid"));
      }

      // Proceed with balance check and deduction
      return getBalance(userId).compose(balance -> {
        if (balance < amount) {
          return Future.succeededFuture(false);
        }

        double updatedBalance = balance - amount;

        Map<String, Object> conditionMap = Map.of(USER_ID, userId.toString());
        Map<String, Object> updatedMap = Map.of(BALANCE, updatedBalance);

        return userCreditDAO.update(conditionMap, updatedMap).compose(v -> {
          CreditTransaction completeTransaction = new CreditTransaction(
            null,
            userId,
            amount,
            creditTransaction.transactedBy(),
            TransactionStatus.SUCCESS.getStatus(),
            TransactionType.DEBIT.getType(),
            null,
            reqAt
          );

          System.out.println("Reached at line 190");

          return creditTransactionDAO.create(completeTransaction);
        }).map(true);
      });
    }).recover(err -> {
      BaseDxException dxEx = BaseDxException.from(err);
      if (dxEx instanceof NoRowFoundException) {
        return Future.failedFuture(new DxNotFoundException("User or balance entry not found", dxEx));
      }
      return Future.failedFuture(dxEx);
    });
  }


  @Override
  public Future<ComputeRole> createComputeRoleRequest(ComputeRole computeRole) {
    return computeRoleDAO.create(computeRole);
  }

  @Override
  public Future<List<ComputeRole>> getAllPendingComputeRequests() {
    Map<String, Object> filterMap = Map.of(Constants.STATUS, Status.PENDING.getStatus());
    return computeRoleDAO.getAllWithFilters(filterMap);
  }

  @Override
  public Future<Boolean> updateComputeRoleStatus(UUID requestId, Status status,UUID approvedBy) {
    Map<String, Object> conditionMap = Map.of(
      COMPUTE_ROLE_ID, requestId.toString());


    Map<String, Object> updateDataMap = Map.of(
      Constants.STATUS, status.getStatus(),
      APPROVED_BY, approvedBy.toString()
    );

    return computeRoleDAO.update(conditionMap, updateDataMap).compose(updated ->{
                if (!updated) {
                    return Future.failedFuture(new DxNotFoundException("Request not found"));
                }
                //Update in KC
                if (Status.GRANTED.getStatus().equals(status.getStatus())) {
                    return computeRoleDAO.get(requestId)
                            .compose(req -> {
                                // Update role in Keycloak
                                return keycloakUserService.addRoleToUser(
                                                req.userId(),
                                                DxRole.COMPUTE
                                        )
                                        .compose(success -> {
                                            if (!success) {
                                                return Future.failedFuture("Failed to assign Computer role in Keycloak");
                                            }
                                            return Future.succeededFuture(true);
                                        });
                            });
                }

                return Future.succeededFuture(true);
            })
      .recover(err -> {
        BaseDxException dxEx = BaseDxException.from(err);
        if (dxEx instanceof NoRowFoundException) {
          return Future.failedFuture(
            new DxNotFoundException("No matching requestId found in computeRole table", dxEx)
          );
        }
        return Future.failedFuture(dxEx);
      });
  }

  @Override
  public Future<Boolean> hasUserComputeAccess(UUID userId) {
    return computeRoleDAO.hasUserComputeAccess(userId);
  }

  @Override
  public Future<Double> getBalance(UUID userId) {
    LOG.info("Fetching balance for userId: {}", userId);
    return userCreditDAO.get(userId)
      .map(result -> {
        JsonObject userCredit = result.toJson();
        Double balance = userCredit.getDouble("balance");
        return balance != null ? balance : 0.0;
      })
      .recover(err -> {
        // Check if it's a "no rows" error
        if (err.getMessage() != null && err.getMessage().toLowerCase().contains("no rows")) {
          return Future.succeededFuture(0.0);
        }
        return Future.failedFuture(err);
      });
  }

  public Future<Boolean> hasPendingComputeRequest(UUID userId){
      Map<String, Object> filterMap = Map.of(
              Constants.STATUS, Status.PENDING.getStatus(), Constants.USER_ID, userId.toString()
      );
      return computeRoleDAO.getAllWithFilters(filterMap)
              .map(list -> !list.isEmpty());
  }
}

