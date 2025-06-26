package org.cdpg.dx.aaa.credit.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.cdpg.dx.aaa.credit.dao.*;
import org.cdpg.dx.aaa.credit.models.*;
import org.cdpg.dx.aaa.organization.config.Constants;
import org.cdpg.dx.auth.authorization.model.DxRole;
import org.cdpg.dx.common.exception.*;
import org.cdpg.dx.common.request.PaginatedRequest;
import org.cdpg.dx.database.postgres.models.PaginatedResult;
import org.cdpg.dx.keycloak.service.KeycloakUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.cdpg.dx.aaa.credit.models.Status.*;
import static org.cdpg.dx.aaa.credit.util.Constants.*;

public class CreditServiceImpl implements CreditService {

  private static final Logger LOGGER = LoggerFactory.getLogger(CreditServiceImpl.class);

  private final CreditRequestDAO creditRequestDAO;
  private final UserCreditDAO userCreditDAO;
  private final CreditTransactionDAO creditTransactionDAO;
  private final ComputeRoleDAO computeRoleDAO;
  private final KeycloakUserService keycloakUserService;
  private final JsonObject config;

  public CreditServiceImpl(CreditDAOFactory factory, KeycloakUserService keycloakUserService, JsonObject config) {
    this.creditRequestDAO = factory.creditRequestDAO();
    this.userCreditDAO = factory.userCreditDAO();
    this.creditTransactionDAO = factory.creditTransactionDAO();
    this.computeRoleDAO = factory.computeRoleDAO();
    this.keycloakUserService = keycloakUserService;
    this.config = config;
  }

  @Override
  public Future<CreditRequest> createCreditRequest(CreditRequest creditRequest) {
    return creditRequestDAO.create(creditRequest);
  }

  @Override
  public Future<List<CreditRequest>> getAllPendingCreditRequests() {
    Map<String, Object> conditionMap = Map.of(Constants.STATUS, PENDING.getStatus());
    return creditRequestDAO.getAllWithFilters(conditionMap);
  }

  @Override
  public Future<CreditTransaction> updateCreditRequestStatus(UUID requestId, Status status, UUID transactedBy) {
    LOGGER.info("Updating credit request status for requestId: {} to: {}", requestId, status);
    return creditRequestDAO.update(
                    Map.of(CREDIT_REQUEST_ID, requestId.toString()),
                    Map.of(STATUS, status.getStatus()))
            .compose(updated -> {
              if (status != GRANTED) {
                return Future.succeededFuture(null); // No transaction needed
              }
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

  private Future<CreditTransaction> processCreditGrant(UUID requestId, UUID transactedBy) {
    return creditRequestDAO.get(requestId).compose(cr -> {
      UUID userId = cr.userId();
      double amount = cr.amount();
      LocalDateTime requestedAt = cr.requestedAt();

      return userCreditDAO.get(userId)
              .compose(userCredit -> {
                double balance = userCredit.balance();
                double newBalance = balance + amount;

                LOGGER.info("Current balance: {}, New balance: {}", balance, newBalance);

                Map<String, Object> updateMap = Map.of(BALANCE, newBalance);
                Map<String, Object> conditionMap = Map.of(USER_ID, userId.toString());

                return userCreditDAO.update(conditionMap, updateMap)
                        .compose(updated -> {
                          CreditTransaction transaction = new CreditTransaction(
                                  null,
                                  userId,
                                  amount,
                                  transactedBy,
                                  TransactionStatus.SUCCESS.getStatus(),
                                  TransactionType.CREDIT.getType(),
                                  null,
                                  requestedAt,
                                  newBalance
                          );

                          return creditTransactionDAO.create(transaction);
                        });
              });
    }).recover(err -> {
        BaseDxException dxEx = BaseDxException.from(err);
       if (dxEx instanceof BaseDxException) {
        LOGGER.warn("No user_credit entry found for requestId: {}, attempting to create...", requestId);

        return creditRequestDAO.get(requestId).compose(cr -> {
          UserCredit newCredit = new UserCredit(null, cr.userId(), cr.amount(), LocalDateTime.now());

          return userCreditDAO.create(newCredit)
                  .recover(createErr -> {
                    BaseDxException createDxEx = BaseDxException.from(createErr);
                    if (createDxEx instanceof UniqueConstraintViolationException) {
                      LOGGER.info("UserCredit already exists, proceeding with balance update.");
                      return Future.succeededFuture(); // Already exists, proceed
                    }
                    return Future.failedFuture(createDxEx);
                  })
                  .compose(v -> processCreditGrant(requestId, transactedBy)); // retry credit grant after creation
        });
      }
      return Future.failedFuture(dxEx);
    });
  }

  @Override
  public Future<CreditTransaction> deductCredits(CreditTransaction creditTransaction) {
    LocalDateTime reqAt = creditTransaction.requestedAt();
    System.out.println("Request at: " + reqAt);
    UUID userId = creditTransaction.userId();

    if (creditTransaction.amount() == null) {
      return Future.failedFuture(new DxBadRequestException("Amount is missing in CreditTransaction"));
    }

    Double amount = creditTransaction.amount();
    Map<String, Object> filter = Map.of(REQUESTED_AT, reqAt.toString(),USER_ID, userId.toString());

    return creditTransactionDAO.getAllWithFilters(filter).compose(existing -> {
      if (existing != null && !existing.isEmpty()) {
        return Future.failedFuture(new DxConflictException("Duplicate transaction request"));
      }

      return getBalance(userId).compose(balance -> {
        if (balance < amount) {
          return Future.failedFuture(new DxValidationException("No sufficient balance"));
        }

        double updatedBalance = balance - amount;

        Map<String, Object> conditionMap = Map.of(USER_ID, userId.toString());
        Map<String, Object> updatedMap = Map.of(BALANCE, updatedBalance);

        return userCreditDAO.update(conditionMap, updatedMap).compose(v -> {
          CreditTransaction transaction = new CreditTransaction(
                  null,
                  userId,
                  amount,
                  creditTransaction.transactedBy(),
                  TransactionStatus.SUCCESS.getStatus(),
                  TransactionType.DEBIT.getType(),
                  null,
                  reqAt,
                  updatedBalance
          );
          return creditTransactionDAO.create(transaction);
        });
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
    // check if there is a pending or granted request for the same user
    // if yes then dont create a new request
    // if status is rejected then only allow to create a new request

    Map<String, Object> filterMap = Map.of(
            Constants.USER_ID, computeRole.userId().toString());
    return computeRoleDAO.getAllWithFilters(filterMap)
      .compose(requests -> {
        if (!requests.isEmpty()) {
          // If there is a pending or granted request, do not create a new one
          ComputeRole existingRequest = requests.get(0);
          if (PENDING.getStatus().equals(existingRequest.status()) ||
            GRANTED.getStatus().equals(existingRequest.status())) {
            return Future.failedFuture(new DxConflictException("A pending or granted compute role request already exists for this user"));
          }
          else if( REJECTED.getStatus().equals(existingRequest.status())) {
            // If the existing request is rejected, allow to create a new one
            LOGGER.info("Existing request is rejected, allowing to create a new compute role request");
            return computeRoleDAO.create(computeRole);
          } else {
            return Future.failedFuture(new DxConflictException("Provider role request is not in a state that allows creation of a new request"));
          }
        }
        else {
          // No existing requests found, proceed to create a new one
          return computeRoleDAO.create(computeRole);
        }
      });

  }

  @Override
  public Future<List<ComputeRole>> getAllComputeRequests() {
    return computeRoleDAO.getAll();
  }

  @Override
  public Future<ComputeRole> getComputeRoleRequestByUserId(UUID userId){
    Map<String, Object> filter = Map.of(Constants.USER_ID, userId.toString());
    return computeRoleDAO.getAllWithFilters(filter)
            .compose(requests -> {
                if (requests.isEmpty()) {
                    return Future.failedFuture(new DxNotFoundException("No compute request found for userId: " + userId));
                }
                return Future.succeededFuture(requests.get(0)); // Return the first request
            });
  }

    @Override
    public Future<PaginatedResult<ComputeRole>> getAllComputeRequests(PaginatedRequest paginatedRequest) {
        return computeRoleDAO.getAll(paginatedRequest);
    }

    @Override
  public Future<Boolean> updateComputeRoleStatus(UUID requestId, Status status, UUID approvedBy) {
    Map<String, Object> conditionMap = Map.of(COMPUTE_ROLE_ID, requestId.toString());
    Map<String, Object> updateMap = Map.of(
            Constants.STATUS, status.getStatus(),
            APPROVED_BY, approvedBy.toString()
    );

    return computeRoleDAO.update(conditionMap, updateMap).compose(updated -> {
      return computeRoleDAO.get(requestId).compose(req -> {
        UUID userId = req.userId();

        if (GRANTED.equals(status)) {
          //TODO Assigining 1000 when user get compute role
          return userCreditDAO.create(new UserCredit(null, userId, config.getInteger("initialCreditBalance"), LocalDateTime.now()))
                  .recover(dxEx -> {
                    if (dxEx instanceof UniqueConstraintViolationException) {
                      LOGGER.info("UserCredit already exists, continuing role assignment.");
                      return Future.succeededFuture();
                    }
                    return Future.failedFuture(dxEx);
                  })
                  .compose(v -> keycloakUserService.addRoleToUser(userId, DxRole.COMPUTE))
                  .compose(success -> {
                    if (!success) {
                      return Future.failedFuture("Failed to assign Compute role in Keycloak");
                    }
                    return Future.succeededFuture(true);
                  });

        } else if (REJECTED.equals(status)) {
          return keycloakUserService.removeRoleFromUser(userId, DxRole.COMPUTE)
                  .compose(success -> {
                    if (!success) {
                      return Future.failedFuture("Failed to remove Compute role in Keycloak");
                    }
                    return Future.succeededFuture(true);
                  });
        }

        return Future.succeededFuture(true);
      });
    }).recover(err -> {
      BaseDxException dxEx = BaseDxException.from(err);
      if (dxEx instanceof NoRowFoundException) {
        return Future.failedFuture(new DxNotFoundException("No matching requestId found in computeRole table", dxEx));
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
    LOGGER.info("Fetching balance for userId: {}", userId);
    return userCreditDAO.get(userId)
            .map(result -> {
              Double balance = result.toJson().getDouble("balance");
              return balance != null ? balance : 0.0;
            })
            .recover(err -> {
              if (err.getMessage() != null && err.getMessage().toLowerCase().contains("no rows")) {
                return Future.succeededFuture(0.0);
              }
              return Future.failedFuture(err);
            });
  }

  @Override
  public Future<ComputeRole> getComputeRequestById(UUID requestId)
  {
    return computeRoleDAO.get(requestId)
            .recover(err -> {
              BaseDxException dxEx = BaseDxException.from(err);
              if (dxEx instanceof NoRowFoundException) {
                return Future.failedFuture(new DxNotFoundException("No matching requestId found in computeRole table", dxEx));
              }
              return Future.failedFuture(dxEx);
            });
  }

  @Override
  public Future<CreditRequest> getCreditRequestById(UUID requestId)
  {
    return creditRequestDAO.get(requestId)
            .recover(err -> {
              BaseDxException dxEx = BaseDxException.from(err);
              if (dxEx instanceof NoRowFoundException) {
                return Future.failedFuture(new DxNotFoundException("No matching requestId found in creditRequest table", dxEx));
              }
              return Future.failedFuture(dxEx);
            });
  }


  @Override
  public Future<Boolean> hasPendingComputeRequest(UUID userId) {
    Map<String, Object> filter = Map.of(
            Constants.STATUS, PENDING.getStatus(),
            Constants.USER_ID, userId.toString()
    );

    return computeRoleDAO.getAllWithFilters(filter)
            .map(list -> !list.isEmpty());
  }
}
