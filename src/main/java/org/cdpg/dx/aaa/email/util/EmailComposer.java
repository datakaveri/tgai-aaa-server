package org.cdpg.dx.aaa.email.util;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.mail.MailMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.aaa.credit.models.ComputeRole;
import org.cdpg.dx.aaa.organization.models.OrganizationCreateRequest;
import org.cdpg.dx.aaa.organization.models.OrganizationJoinRequest;
import org.cdpg.dx.aaa.organization.models.OrganizationUser;
import org.cdpg.dx.aaa.organization.service.OrganizationService;
import org.cdpg.dx.aaa.user.service.UserService;
import org.cdpg.dx.email.service.EmailService;
import org.cdpg.dx.keycloak.service.KeycloakUserService;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

public class EmailComposer {
  private static final Logger LOGGER = LogManager.getLogger(EmailComposer.class);
  private final EmailService emailService;
  private final KeycloakUserService keycloakUserService;
  private final JsonObject config;
  private final OrganizationService  organizationService;
  private final UserService userService;

  public EmailComposer(EmailService emailService, KeycloakUserService keycloakUserService, JsonObject config,OrganizationService organizationService,UserService userService) {
    this.emailService = emailService;
    this.keycloakUserService = keycloakUserService;
    this.config = config;
    this.organizationService = organizationService;
    this.userService = userService;
  }
  /**
   * Loads an HTML email template from the resources folder.
   *
   * @param resourcePath The path to the HTML template file in the resources folder.
   * @return The content of the HTML template as a String.
   */
  public static String loadTemplate(String resourcePath) {
    try (InputStream inputStream = EmailComposer.class.getClassLoader().getResourceAsStream(resourcePath);
         Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8)) {
      return scanner.useDelimiter("\\A").next();
    } catch (Exception e) {
      throw new RuntimeException("Failed to load template: " + resourcePath, e);
    }
  }

  public Future<Void> sendEmailForCreatingOrg(OrganizationCreateRequest organizationCreateRequest, User user) {

    LOGGER.info("Sending email for organization creation request: {}", organizationCreateRequest);
    String orgName = organizationCreateRequest.name();
    String orgSector = organizationCreateRequest.orgSector();
    String orgEntityType = organizationCreateRequest.entityType();
    String orgWebsite = organizationCreateRequest.websiteLink();
    String userName = organizationCreateRequest.userName();
    String emailId = user.principal().getString("email");


    String senderEmail = config.getString("emailSender"); // e.g., no-reply@domain.com
    String emailTemplate = loadTemplate("templates/request-create-organization.html"); // Path to HTML template
    String adminPortalUrl = config.getString("TGDxUrl"); // Admin portal URL
    String cosAdminEmailId = config.getString("cosAdminEmailId"); // Email of COS admin


    Map<String, String> emailDetails = Map.of(
      "USER_FIRST_NAME", userName,
      "USER_EMAIL_ID", emailId,
      "ORGANIZATION_NAME", orgName,
      "ORGANIZATION_SECTOR", orgSector,
      "ORGANIZATION_ENTITY_TYPE" , orgEntityType,
      "ORGANIZATION_WEBSITE" ,orgWebsite,
      "ADMIN_FIRST_NAME", "Admin",
      "ADMIN_LAST_NAME", "",
      "ADMIN_PORTAL_URL", adminPortalUrl,
      "SENDER_NAME", "TGDeX Team"
    );

    String htmlBody = getHtmlBody(emailTemplate, emailDetails);

    MailMessage mailMessage = createMailMessage(
      senderEmail,
      cosAdminEmailId,
      htmlBody,
      "Organization Creation Request"
    );

    return emailService.sendEmail(mailMessage).onComplete(res -> {
      if (res.succeeded()) {
        LOGGER.info("Organization creation request email sent to {}", cosAdminEmailId);
      } else {
        LOGGER.error("Failed to send organization creation email: {}", res.cause().getMessage());
      }
    }).recover(failure -> {
      LOGGER.error("Failed to handle email for organization creation: {}", failure.getMessage());
      return Future.failedFuture(failure);
    });
  }


  public Future<Void> sendEmailForJoiningOrg(OrganizationJoinRequest organizationJoinRequest,User user) {

    UUID orgId = organizationJoinRequest.organizationId();
    String userName = organizationJoinRequest.userName();
    String employeeId = organizationJoinRequest.empId();
    String jobTitle = organizationJoinRequest.jobTitle();
    String emailId = user.principal().getString("email");

    String senderEmail = config.getString("emailSender"); // no-org-reply
    String emailTemplate = loadTemplate("templates/request-join-organization.html");
    String adminPortalUrl = config.getString("TGDxUrl");

    Map<String, String> emailDetails = Map.of(
      "ADMIN_FIRST_NAME", "Admin",
      "ADMIN_LAST_NAME", "",
      "USER_FIRST_NAME", userName,
      "USER_EMAIL_ID", emailId,
      "EMPLOYEE_ID", employeeId,
      "JOB_TITLE", jobTitle,
      "ADMIN_PORTAL_URL", adminPortalUrl,
      "SENDER_NAME", "TGDeX Team"
    );


    return getOrgAdminEmail(orgId).compose(orgAdminEmail -> {
      String htmlBody = getHtmlBody(emailTemplate, emailDetails);
      LOGGER.info("Org Admin Email Id is : {}", orgAdminEmail);

      MailMessage mailMessage = createMailMessage(senderEmail, orgAdminEmail, htmlBody,"Join Organization Request");
      return emailService.sendEmail(mailMessage).onComplete(res -> {
        if (res.succeeded()) {
          LOGGER.info("Email sent successfully to {}", orgAdminEmail);
        } else {
          LOGGER.error("Failed to send email: {}", res.cause().getMessage());
        }
      }).recover(failure -> {
        LOGGER.error("Failed to retrieve provider user details for user {}: {}", userName, failure.getMessage());
        return Future.failedFuture(failure);
      });

    });
  }

  public Future<Void> sendEmailForComputeRole(ComputeRole computeRole,User user) {


    UUID userId = computeRole.userId();
    String userName = computeRole.userName();
    String emailId = user.principal().getString("email");

    String senderEmail = config.getString("emailSender"); // no-org-reply
    String emailTemplate = loadTemplate("templates/request-compute-role.html");
    String adminPortalUrl = config.getString("TGDxUrl");
    String cosAdminEmailId = config.getString("cosAdminEmailId"); // Email of COS admin


    Map<String, String> emailDetails = Map.of(
      "ADMIN_FIRST_NAME", "Admin",
      "ADMIN_LAST_NAME", "",
      "USER_FIRST_NAME", userName,
      "USER_EMAIL_ID", emailId,
      "USER_ID", userId.toString(),
      "ADMIN_PORTAL_URL", adminPortalUrl,
      "SENDER_NAME", "TGDeX Team"
    );

    String htmlBody = getHtmlBody(emailTemplate, emailDetails);

    MailMessage mailMessage = createMailMessage(
      senderEmail,
      cosAdminEmailId,
      htmlBody,
      "Compute Role Request"
    );

    return emailService.sendEmail(mailMessage).onComplete(res -> {
          if (res.succeeded()) {
            LOGGER.info("Compute Role request email sent to {}", cosAdminEmailId);
          } else {
            LOGGER.error("Failed to send compute role email: {}", res.cause().getMessage());
          }
        }).recover(failure -> {
          LOGGER.error("Failed to handle email for compute role creation: {}", failure.getMessage());
          return Future.failedFuture(failure);
        });
      }

  /**
   * Replaces placeholders in the HTML template with actual values.
   *
   * @param template     The HTML template containing placeholders.
   * @param replacements A map of placeholder names to their replacement values.
   * @return The HTML body with placeholders replaced by actual values.
   */
  public String getHtmlBody(String template, Map<String, String> replacements) {
    String result = template;
    for (Map.Entry<String, String> entry : replacements.entrySet()) {
      result = result.replace("${" + entry.getKey() + "}", entry.getValue());
    }
    return result;
  }

  public MailMessage createMailMessage(String senderEmail, String receiverEmail, String body,String subject) {
    MailMessage message = new MailMessage();
    message.setFrom(senderEmail);
    message.setTo(receiverEmail);
    message.setSubject(subject);
    message.setHtml(body);
    return message;
  }

  public Future<String> getOrgAdminEmail(UUID orgId) {
    if (orgId == null) {
      return Future.failedFuture("Organization ID cannot be null");
    }

    return organizationService.getOrganisationAdminId(orgId)
      .compose(res -> {
        if (res == null || res.size() == 0) {
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
