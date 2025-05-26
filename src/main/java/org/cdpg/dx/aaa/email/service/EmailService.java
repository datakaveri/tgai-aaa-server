package org.cdpg.dx.aaa.email.service;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.mail.MailMessage;
import org.cdpg.dx.aaa.email.models.EmailRequest;

@VertxGen
@ProxyGen
public interface EmailService {
  static EmailService createProxy(Vertx vertx, String address) {
    return new EmailServiceVertxEBProxy(vertx, address);
  }

  Future<EmailRequest> sendEmail(EmailRequest emailRequest);}
