package org.cdpg.dx.aaa.email.service;

import io.vertx.core.Future;
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
  public Future<EmailRequest> sendEmail(EmailRequest emailRequest,String htmlbody) {
    JsonObject emailJson = emailRequest.toJson();
    MailMessage message = new MailMessage();



    message.setFrom(emailJson.getString("from"));
    message.setTo(emailJson.getString("to"));
    message.setSubject(emailJson.getString("subject"));
    message.setText(emailJson.getString("text"));
    message.setHtml(htmlbody);

    return mailClient.sendMail(message)
      .onSuccess(mailResult -> {
        System.out.println("Mail sent with messageId: " + mailResult.getMessageID());
      })
      .onFailure(err -> {
        System.err.println("Failed to send mail: " + err.getMessage());
      })
      .map(mailResult -> emailRequest);
  }

}
