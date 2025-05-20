package org.cdpg.dx.aaa.credit.dao.impl;

import io.vertx.core.Future;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.aaa.credit.dao.CreditTransactionDAO;
import org.cdpg.dx.aaa.credit.models.CreditTransaction;
import org.cdpg.dx.aaa.credit.models.CreditTransactionDTO;
import org.cdpg.dx.aaa.credit.models.TransactionType;
import org.cdpg.dx.aaa.organization.models.OrganizationCreateRequest;
import org.cdpg.dx.aaa.organization.util.Constants;
import org.cdpg.dx.database.postgres.base.dao.AbstractBaseDAO;
import org.cdpg.dx.database.postgres.models.InsertQuery;
import org.cdpg.dx.database.postgres.service.PostgresService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.cdpg.dx.aaa.credit.util.Constants.CREDIT_TRANSACTION_ID;
import static org.cdpg.dx.aaa.credit.util.Constants.CREDIT_TRANSACTION_TABLE;
import static org.cdpg.dx.aaa.organization.util.Constants.ORG_ID;

public class CreditTransactionDAOImpl extends AbstractBaseDAO<CreditTransaction> implements CreditTransactionDAO {

  private static final Logger LOGGER = LogManager.getLogger(CreditTransactionDAOImpl.class);

  public CreditTransactionDAOImpl(PostgresService postgresService) {
    super(postgresService,CREDIT_TRANSACTION_TABLE, CREDIT_TRANSACTION_ID, CreditTransaction::fromJson);
  }

}
