package org.cdpg.dx.email.service;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.email.models.EmailRequest;

public class EmailServiceImpl implements EmailService {
  private final MailClient mailClient;

  public EmailServiceImpl(MailClient mailClient) {
    this.mailClient = mailClient;
  }

  private final static Logger LOGGER = LogManager.getLogger(EmailServiceImpl.class);

  @Override
  public Future<Void> sendEmail(EmailRequest emailRequest) {

    Promise<Void> promise = Promise.promise();
    JsonObject emailJson = emailRequest.toJson();
    MailMessage message = new MailMessage();

    message.setFrom(emailJson.getString("from"));
    message.setTo(emailJson.getString("to"));
    message.setSubject(emailJson.getString("subject"));
    message.setHtml(emailJson.getString("htmlBody"));


    mailClient.sendMail(message, res -> {
      if (res.succeeded()) {
        LOGGER.info("Email sent: " + res.result());
        promise.complete();
      } else {
        LOGGER.error("Failed to send email", res.cause());
        promise.fail(res.cause());
      }
    });

    return promise.future();
  }

}
