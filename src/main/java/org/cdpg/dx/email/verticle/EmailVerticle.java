package org.cdpg.dx.email.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.StartTLSOptions;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.sqlclient.Pool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.email.service.EmailService;
import org.cdpg.dx.email.service.EmailServiceImpl;

import static org.cdpg.dx.common.config.ServiceProxyAddressConstants.EMAIL_SERVICE_ADDRESS;

public class EmailVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LogManager.getLogger(EmailVerticle.class);
  private MessageConsumer<JsonObject> consumer;
  private ServiceBinder binder;
  private Pool pool;


  @Override
  public void start(Promise<Void> startPromise) {

    String emailhost = config().getString("emailHostName");
    int emailport = config().getInteger("emailPort");
    String emailusername = config().getString("emailUserName");
    String emailpassword = config().getString("emailPassword");

    MailConfig config = new MailConfig();
    config.setHostname(emailhost);
    config.setPort(emailport);
    config.setStarttls(StartTLSOptions.REQUIRED);
    config.setUsername(emailusername);
    config.setPassword(emailpassword);

    MailClient mailClient = MailClient.create(vertx, config);

      EmailService service = new EmailServiceImpl(mailClient);
      binder = new ServiceBinder(vertx);
      consumer = binder.setAddress(EMAIL_SERVICE_ADDRESS).register(EmailService.class, service);

      LOGGER.info("Email service registered on 'email.service'");
      startPromise.complete();
    }

    @Override
    public void stop() {
      if (binder != null && consumer != null) {
        binder.unregister(consumer);
      }
    }
  }
