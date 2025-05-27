package org.cdpg.dx.aaa.email;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.cdpg.dx.aaa.email.models.EmailRequest;
import org.cdpg.dx.aaa.email.service.EmailService;
import org.cdpg.dx.aaa.email.verticle.EmailVerticle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.cdpg.dx.common.config.ServiceProxyAddressConstants.EMAIL_SERVICE_ADDRESS;


@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EmailVerticleTest {

  public String TEST_EMAIL = "sample_email@gmail.com";

  @BeforeEach
  void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
    JsonObject config = new JsonObject()
      .put("host", "smtp.mail.gov.in")
      .put("port", 465)
      .put("username", "support-tgdex-itc@telangana.gov.in")
      .put("password", "K1&dX7@oS3");

    vertx.deployVerticle(new EmailVerticle(), new DeploymentOptions().setConfig(config))
      .onSuccess(id -> testContext.completeNow())
      .onFailure(testContext::failNow);
  }

  @Test
  void testSendEmail(Vertx vertx, VertxTestContext testContext) {
    EmailService emailService = EmailService.createProxy(vertx, EMAIL_SERVICE_ADDRESS);

    EmailRequest request = new EmailRequest();

    request.setFrom("smtp.mail.gov.in");
    request.setTo(TEST_EMAIL);
    request.setSubject("<EMAIL>");
    request.setText("This is a test email");

    emailService.sendEmail(request)
      .onSuccess(resp -> {
        System.out.println("Email sent successfully");
        testContext.completeNow();  // ✅ Mark test complete
      })
      .onFailure(err -> {
        err.printStackTrace();
        testContext.failNow(err);   // ✅ Fail test
      });
  }
}

