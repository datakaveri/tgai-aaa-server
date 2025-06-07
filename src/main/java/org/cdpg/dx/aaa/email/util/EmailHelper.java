package org.cdpg.dx.aaa.email.util;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.ext.mail.MailMessage;
import org.cdpg.dx.email.models.EmailRequest;
import org.cdpg.dx.email.service.EmailService;

import static org.cdpg.dx.common.config.ServiceProxyAddressConstants.EMAIL_SERVICE_ADDRESS;

public class EmailHelper {
  private final EmailService emailService;

  public EmailHelper(Vertx vertx) {
    this.emailService = EmailService.createProxy(vertx, EMAIL_SERVICE_ADDRESS);
  }

  public Future<Void> sendMail(String from, String to, String subject, String content) {

    EmailRequest req=new EmailRequest();
    req.setFrom(from);
    req.setSubject(subject);
    req.setTo(to);
    req.setHtmlBody(content);

    return emailService.sendEmail(req)
      .onSuccess(v -> {
        System.out.println("Email sent from helper");
      })
      .onFailure(err -> {
        System.err.println("Email send failed from helper: " + err.getMessage());
      });
  }
}
