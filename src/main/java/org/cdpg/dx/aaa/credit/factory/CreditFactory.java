package org.cdpg.dx.aaa.credit.factory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.cdpg.dx.aaa.credit.dao.CreditDAOFactory;
import org.cdpg.dx.aaa.credit.handler.CreditHandler;
import org.cdpg.dx.aaa.credit.service.CreditService;
import org.cdpg.dx.aaa.credit.service.CreditServiceImpl;
import org.cdpg.dx.database.postgres.service.PostgresService;

public class CreditFactory {
  private static final Logger LOGGER = LogManager.getLogger(CreditFactory.class);

  private CreditFactory() {}

  public static CreditHandler createHandler(PostgresService pgService) {

    CreditDAOFactory creditDAOFactory = new CreditDAOFactory(pgService);
    CreditService creditService = new CreditServiceImpl(creditDAOFactory);

    return new CreditHandler(creditService);
  }
}
