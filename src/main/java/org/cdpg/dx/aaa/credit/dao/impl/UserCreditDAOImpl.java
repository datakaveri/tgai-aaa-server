package org.cdpg.dx.aaa.credit.dao.impl;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.aaa.credit.dao.UserCreditDAO;
import org.cdpg.dx.aaa.credit.models.CreditRequest;
import org.cdpg.dx.aaa.credit.models.CreditTransaction;
import org.cdpg.dx.aaa.credit.models.UserCredit;
import org.cdpg.dx.aaa.credit.models.UserCreditDTO;
import org.cdpg.dx.aaa.credit.util.Constants;
import org.cdpg.dx.aaa.organization.dao.impl.OrganizationCreateRequestDAOImpl;
import org.cdpg.dx.aaa.organization.models.OrganizationCreateRequest;
import org.cdpg.dx.database.postgres.base.dao.AbstractBaseDAO;
import org.cdpg.dx.database.postgres.models.Condition;
import org.cdpg.dx.database.postgres.models.InsertQuery;
import org.cdpg.dx.database.postgres.models.SelectQuery;
import org.cdpg.dx.database.postgres.models.UpdateQuery;
import org.cdpg.dx.database.postgres.service.PostgresService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.cdpg.dx.aaa.credit.util.Constants.*;

public class UserCreditDAOImpl extends AbstractBaseDAO<UserCredit>implements UserCreditDAO {
  private static final Logger LOGGER = LogManager.getLogger(UserCreditDAOImpl.class);

  public UserCreditDAOImpl(PostgresService postgresService) {
    super(postgresService,USER_CREDIT_TABLE, USER_CREDIT_ID, UserCredit::fromJson);
  }

}



