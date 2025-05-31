package org.cdpg.dx.aaa.email;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.*;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.serviceproxy.ProxyHelper;
import org.cdpg.dx.aaa.email.models.EmailRequest;
import org.cdpg.dx.aaa.email.service.EmailService;
import org.cdpg.dx.aaa.email.service.EmailServiceImpl;
import org.cdpg.dx.aaa.email.verticle.EmailVerticle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.cdpg.dx.common.config.ServiceProxyAddressConstants.EMAIL_SERVICE_ADDRESS;

@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EmailVerticleTest {

  private MailClient mailClient;

  @BeforeAll
  void setup(Vertx vertx,VertxTestContext testContext) {

    JsonObject config = new JsonObject()
      .put("host", "hostname")
      .put("port", 2587)  // or 2587 based on SES
      .put("username", "username")
      .put("password", "password");

    vertx.deployVerticle(new EmailVerticle(), new DeploymentOptions().setConfig(config))
      .onSuccess(id -> testContext.completeNow())
      .onFailure(testContext::failNow);
  }

  @Test
  void testSendEmail(Vertx vertx,VertxTestContext testContext) {
    EmailService emailService = EmailService.createProxy(vertx, EMAIL_SERVICE_ADDRESS);

    EmailRequest request = new EmailRequest();
    request.setFrom("no-reply.dev@iudx.io"); // ✅ Must be verified in SES
    request.setTo("sample_email@gmail.com");
    request.setSubject("Vert.x Email Test");
    request.setText("This is a test email sent from a deployed EmailVerticle.");

    emailService.sendEmail(request)
      .onSuccess(res -> {
        System.out.println("✅ Email sent successfully");
        testContext.completeNow();
      })
      .onFailure(err -> {
        err.printStackTrace();
        testContext.failNow(err);
      });
  }
}
