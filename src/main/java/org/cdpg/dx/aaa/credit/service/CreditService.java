package org.cdpg.dx.aaa.credit.service;

import io.vertx.core.Future;
import org.cdpg.dx.aaa.credit.models.*;

import java.util.List;
import java.util.UUID;

public interface CreditService {

  // ************ CREDIT REQUEST *********
  Future<CreditRequest> createCreditRequest(CreditRequest creditRequest); // done -> creates a new entry in credit request table

  Future<List<CreditRequest>> getAllPendingCreditRequests();

  Future<CreditTransaction> updateCreditRequestStatus(UUID requestId, Status status,UUID transactedBy); //done -> on approval will create a new entry in userCredit table

  // ************ USER CREDIT *********

  Future<CreditTransaction> deductCredits(CreditTransaction creditTransaction);

  Future<Double> getBalance(UUID userId);

  // ************ COMPUTE ROLE **********

  Future<ComputeRole> createComputeRoleRequest(ComputeRole computeRole);

  Future<List<ComputeRole>> getAllComputeRequests();

  Future<Boolean> hasPendingComputeRequest(UUID userId);

  Future<Boolean> updateComputeRoleStatus(UUID requestId, Status status,UUID approvedBy);

  Future<Boolean> hasUserComputeAccess(UUID userId);
}
