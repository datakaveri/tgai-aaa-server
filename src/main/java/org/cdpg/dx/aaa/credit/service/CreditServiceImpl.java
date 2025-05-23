package org.cdpg.dx.aaa.credit.service;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.cdpg.dx.aaa.credit.dao.*;
import org.cdpg.dx.aaa.credit.models.*;
import org.cdpg.dx.aaa.organization.service.OrganizationServiceImpl;
import org.cdpg.dx.aaa.organization.util.Constants;
import org.cdpg.dx.common.exception.BaseDxException;
import org.cdpg.dx.common.exception.DxNotFoundException;
import org.cdpg.dx.common.exception.NoRowFoundException;
import org.postgresql.core.TransactionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.cdpg.dx.aaa.credit.util.Constants.*;

public class CreditServiceImpl implements CreditService {

  private static final Logger LOG = LoggerFactory.getLogger(OrganizationServiceImpl.class);

  private final CreditRequestDAO creditRequestDAO;
  private final UserCreditDAO userCreditDAO;
  private final CreditTransactionDAO creditTransactionDAO;
  private final ComputeRoleDAO computeRoleDAO;


  public CreditServiceImpl(CreditDAOFactory factory) {
    this.creditRequestDAO = factory.creditRequestDAO();
    this.userCreditDAO = factory.userCreditDAO();
    this.creditTransactionDAO = factory.creditTransactionDAO();
    this.computeRoleDAO = factory.computeRoleDAO();
  }

  @Override
  public Future<CreditRequest> createCreditRequest(CreditRequest creditRequest) {
    return creditRequestDAO.create(creditRequest);
  }


  @Override
  public Future<List<CreditRequest>> getAllPendingCreditRequests() {
    Map<String,Object> conditionMap = Map.of(Constants.STATUS, Status.PENDING);

    return creditRequestDAO.getAllWithFilters(conditionMap);
  }


  // ***************************************************************************************
  @Override
  public Future<Boolean> updateCreditRequestStatus(UUID requestId, Status status, UUID transactedBy) {
    Map<String, Object> conditionMap = Map.of(
      CREDIT_REQUEST_ID, requestId.toString()
    );

    Map<String, Object> updateDataMap = Map.of(
      STATUS, status.getStatus()
    );

    return creditRequestDAO.update(conditionMap, updateDataMap)
      .compose(updated -> {
        if (!updated) return Future.succeededFuture(false);

        // Proceed only if status is APPROVED
        if (status == Status.APPROVED) {
          return creditRequestDAO.get(requestId)
            .compose(cr -> {
              UUID userId = cr.userId();
              String userName = cr.userName();
              double amount = cr.amount();

              return getBalance(userId)
                .compose(balance -> {
                  double newBalance = balance + amount;

                  Map<String, Object> balanceUpdateCondition = Map.of(
                    USER_ID, userId.toString()
                  );
                  Map<String, Object> balanceUpdateData = Map.of(
                    BALANCE, newBalance
                  );

                  return userCreditDAO.update(balanceUpdateCondition, balanceUpdateData)
                    .compose(updateResult -> {
                      CreditTransaction creditTransaction = new CreditTransaction(
                        null,
                        userId,
                        userName,
                        amount,
                        transactedBy,
                        TransactionStatus.SUCCESS.getStatus(),
                        TransactionType.CREDIT.getType(),
                        null
                      );

                      return creditTransactionDAO.create(creditTransaction);
                    })
                    .map(true);
                });
            });
        } else {
          return Future.succeededFuture(true);
        }
      })
      .recover(err -> {
        BaseDxException dxEx = BaseDxException.from(err);
        if (dxEx instanceof NoRowFoundException) {
          return Future.failedFuture(new DxNotFoundException("No request found with given ID", dxEx));
        }
        return Future.failedFuture(dxEx);
      });
  }

  //**************************************************************************************


  @Override
  public Future<Boolean> deductCredits(CreditTransaction creditTransaction) {
    UUID userId = creditTransaction.userId();
    String userName = creditTransaction.userName();
    if (creditTransaction.amount() == null) {
      return Future.failedFuture("Amount is missing in CreditTransaction");
    }

    Double amount = creditTransaction.amount();

    return getBalance(userId)
      .compose(balance -> {
        if (balance < amount) {
          return Future.succeededFuture(false);
        } else {
          double updatedBalance = balance - amount;

          Map<String, Object> conditionMap = Map.of(USER_ID, userId);
          Map<String, Object> updatedMap = Map.of(BALANCE, updatedBalance);

          return userCreditDAO.update(conditionMap, updatedMap)
            .compose(v -> {
              CreditTransaction completeTransaction = new CreditTransaction(
                null,
                userId,
                userName,
                amount,
                creditTransaction.transactedBy(),
                TransactionStatus.SUCCESS.getStatus(),
                TransactionType.DEBIT.getType(),
                null
              );

              return creditTransactionDAO.create(completeTransaction);
            })
            .map(true);
        }
      })
      .recover(err -> {
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
    Map<String, Object> filterMap = Map.of(Constants.STATUS, Status.PENDING);
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

    return computeRoleDAO.update(conditionMap, updateDataMap)
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
    Promise<Double> promise = Promise.promise();

    userCreditDAO.get(userId).onSuccess(result -> {
      if (result!=null) {
        JsonObject userCredit = result.toJson();
        Double balance = userCredit.getDouble("balance");
        promise.complete(balance);
      } else {
        promise.complete(0.0); // or fail if no record is found
      }
    }).onFailure(promise::fail);

    return promise.future();
  }
}

