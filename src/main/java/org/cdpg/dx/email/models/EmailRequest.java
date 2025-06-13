package org.cdpg.dx.email.models;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@DataObject
@JsonGen
public class EmailRequest {
  private String from; //config
  private String to; //provider email
  private String subject; // Access Requested for the asset
  private String htmlBody; // consumer details (from token), asset details

  public EmailRequest() {
    // default constructor
  }

  public EmailRequest(JsonObject json) {
    // Use generated converter to initialize fields
    EmailRequestConverter.fromJson(json, this);
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    EmailRequestConverter.toJson(this, json);
    return json;
  }

  // getters and setters for all fields
  public String getFrom() { return from; }
  public void setFrom(String from) { this.from = from; }

  public String getTo() { return to; }
  public void setTo(String to) { this.to = to; }

  public String getSubject() { return subject; }
  public void setSubject(String subject) { this.subject = subject; }


  public String getHtmlBody() {
    return htmlBody;
  }

  public void setHtmlBody(String htmlBody) {
    this.htmlBody = htmlBody;
  }
}
