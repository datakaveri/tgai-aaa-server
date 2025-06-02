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
import org.cdpg.dx.common.exception.DxBadRequestException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.cdpg.dx.common.config.ServiceProxyAddressConstants.EMAIL_SERVICE_ADDRESS;

@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EmailVerticleTest {

  private MailClient mailClient;

  @BeforeAll
  void setup(Vertx vertx,VertxTestContext testContext) {

    JsonObject config = new JsonObject()
      .put("host", "email-smtp.ap-south-1.amazonaws.com")
      .put("port", 2587)
      .put("username", "AKIAROAMFPEM2M4QITED")
      .put("password", "BE6v8Dw3LC/snQ/JEJh4aKwo2gWkCGvtNPh9axF6F86r");

    vertx.deployVerticle(new EmailVerticle(), new DeploymentOptions().setConfig(config))
      .onSuccess(id -> testContext.completeNow())
      .onFailure(testContext::failNow);
  }

  @Test
  void testSendEmail(Vertx vertx,VertxTestContext testContext) {
    EmailService emailService = EmailService.createProxy(vertx, EMAIL_SERVICE_ADDRESS);

    EmailRequest request = new EmailRequest();
    request.setFrom("no-reply.dev@iudx.io"); // Must be verified in SES
    request.setTo("sample_email@gmail.com");
    request.setSubject("Vert.x Email Test");
    request.setText("This is a test email sent from a deployed EmailVerticle.");


    String htmlTemplate = "";
    try {
      htmlTemplate = Files.readString(
              Path.of("src/main/java/org/cdpg/dx/templates/request-create-organization.html"),
              StandardCharsets.UTF_8
      );
    } catch (IOException e) {
      e.printStackTrace();
    }


    try {
      htmlTemplate = htmlTemplate.replace("${username}", "username")
              .replace("${orgname}", "orgname");
    }
    catch(DxBadRequestException e)
    {
      throw new DxBadRequestException("Parameters replacement in html invalid !!");
    }

    emailService.sendEmail(request,htmlTemplate)
      .onSuccess(res -> {
        System.out.println("Email sent successfully");
        testContext.completeNow();
      })
      .onFailure(err -> {
        err.printStackTrace();
        testContext.failNow(err);
      });
  }
}
