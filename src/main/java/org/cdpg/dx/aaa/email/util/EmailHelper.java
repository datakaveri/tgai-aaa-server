package org.cdpg.dx.aaa.email.util;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.ext.mail.MailMessage;
import jakarta.validation.constraints.Email;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.aaa.organization.handler.OrganizationHandler;
import org.cdpg.dx.aaa.organization.models.OrganizationUser;
import org.cdpg.dx.aaa.organization.service.OrganizationService;
import org.cdpg.dx.aaa.user.service.UserService;
import org.cdpg.dx.database.postgres.base.dao.AbstractBaseDAO;
import org.cdpg.dx.email.models.EmailRequest;
import org.cdpg.dx.email.service.EmailService;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static io.vertx.core.Vertx.vertx;
import static org.cdpg.dx.common.config.ServiceProxyAddressConstants.EMAIL_SERVICE_ADDRESS;

public class EmailHelper{

  private static final Logger LOGGER = LogManager.getLogger(EmailHelper.class);
  private final OrganizationService organizationService;
  private final UserService userService;

  public EmailHelper(OrganizationService organizationService, UserService userService)
  {
    this.organizationService=organizationService;
    this.userService=userService;
  }

  public Future<String> readTemplate(String type)
  {

    StringBuilder sb = new StringBuilder();

    sb.append("src/main/resources/templates/");
    sb.append(type);
    sb.append(".html");

    String path = sb.toString();


    return vertx().fileSystem()
        .readFile(path)
      .map(buffer -> buffer.toString(StandardCharsets.UTF_8));
  }


  public Future<EmailRequest> sendEmailRequestForCreating(String userName,String emailId) {

    return readTemplate("request-create-organization")
      .compose(res -> {

      String adminPortalUrl = "https://staging.catalogue.tgdex.iudx.io/";
      String htmlTemplate = res.replace("${adminPortalUrl}", adminPortalUrl)
                                .replace("${userName}",userName)
                                .replace("${emailId}",emailId);

      EmailRequest emailRequest = new EmailRequest();

      String sender = "no-reply.dev@iudx.io";
      String reciever="cos_admin@gmail.com"; //cosAdmin

      emailRequest.setFrom(sender);
      emailRequest.setTo(reciever);
      emailRequest.setSubject("Request to create an organisation !");
      emailRequest.setHtmlBody(htmlTemplate);

      return Future.succeededFuture(emailRequest);
    })
      .recover(throwable -> {
        LOGGER.error("Failed to create email request for user: {}", userName, throwable);
        return Future.failedFuture("Failed to create email request");
      });
  }

  public Future<EmailRequest> sendEmailRequestForJoining(String userName, String emailId, UUID orgId) {

    return getOrgAdminEmail(orgId)
      .compose(adminEmail -> {
        return readTemplate("request-join-organization")
          .compose(templateContent -> {
            try {
              String adminPortalUrl = "https://staging.catalogue.tgdx.iudx.io/";
              String htmlTemplate = templateContent
                .replace("${adminPortalUrl}", adminPortalUrl)
                .replace("${userName}", userName)
                .replace("${emailId}", emailId);

              EmailRequest emailRequest = new EmailRequest();
              emailRequest.setFrom("no-reply.dev@iudx.io");
              emailRequest.setTo(adminEmail);
              emailRequest.setSubject("Request to join your organisation");
              emailRequest.setHtmlBody(htmlTemplate);

              return Future.succeededFuture(emailRequest);

            } catch (Exception e) {
              return Future.failedFuture("Failed to process email template: " + e.getMessage());
            }
          });
      })
      .recover(throwable -> {
        LOGGER.error("Failed to create email request for user: {} to join org: {}", userName, orgId, throwable);
        return Future.failedFuture("Failed to create email request: " + throwable.getMessage());
      });
  }

  public Future<String> getOrgAdminEmail(UUID orgId) {
    if (orgId == null) {
      return Future.failedFuture("Organization ID cannot be null");
    }

    return organizationService.getOrganisationAdminId(orgId)
      .compose(res -> {
        if (res == null || res.isEmpty()) {
          return Future.failedFuture("No admin found for organization: " + orgId);
        }

        OrganizationUser organizationUser = res.get(0);
        UUID userId = organizationUser.userId();

        if (userId == null) {
          return Future.failedFuture("Invalid user ID for organization admin");
        }

        return userService.getUserInfoByID(userId)
          .compose(user -> {
            if (user == null || user.email() == null || user.email().trim().isEmpty()) {
              return Future.failedFuture("No valid email found for admin user");
            }
            return Future.succeededFuture(user.email());
          });
      })
      .recover(throwable -> {
        LOGGER.error("Failed to get organization admin email for orgId: {}", orgId, throwable);
        return Future.failedFuture("Failed to retrieve organization admin email: " + throwable.getMessage());
      });
  }

}
