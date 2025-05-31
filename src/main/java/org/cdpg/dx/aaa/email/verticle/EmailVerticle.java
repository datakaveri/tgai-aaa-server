package org.cdpg.dx.aaa.email.verticle;

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
import org.cdpg.dx.aaa.email.service.EmailService;
import org.cdpg.dx.aaa.email.service.EmailServiceImpl;

import static org.cdpg.dx.common.config.ServiceProxyAddressConstants.EMAIL_SERVICE_ADDRESS;

public class EmailVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LogManager.getLogger(EmailVerticle.class);
  private MessageConsumer<JsonObject> consumer;
  private ServiceBinder binder;
  private Pool pool;


  @Override
  public void start(Promise<Void> startPromise) {

    String host = config().getString("host");
    int port = config().getInteger("port");
    String username = config().getString("username");
    String password = config().getString("password");

    MailConfig config = new MailConfig();
    config.setHostname(host);
    config.setPort(port);
    config.setStarttls(StartTLSOptions.REQUIRED);
    config.setUsername(username);
    config.setPassword(password);

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
