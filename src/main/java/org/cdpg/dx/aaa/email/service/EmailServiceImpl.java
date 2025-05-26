package org.cdpg.dx.aaa.email.service;

import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailMessage;
import org.cdpg.dx.aaa.email.models.EmailRequest;

public class EmailServiceImpl implements EmailService {
  private final MailClient mailClient;

  public EmailServiceImpl(MailClient mailClient) {
    this.mailClient = mailClient;
  }

  @Override
  public Future<EmailRequest> sendEmail(EmailRequest emailRequest) {
    JsonObject emailJson = emailRequest.toJson();
    MailMessage message = new MailMessage()
      .setFrom(emailJson.getString("from"))
      .setTo(emailJson.getString("to"))
      .setSubject(emailJson.getString("subject"))
      .setText(emailJson.getString("text"));

    return mailClient.sendMail(message)
      .map(mailResult -> emailRequest); // return the same object for confirmation
  }

}
