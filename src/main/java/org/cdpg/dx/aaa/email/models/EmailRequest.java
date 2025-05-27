package org.cdpg.dx.aaa.email.models;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import org.cdpg.dx.aaa.email.models.EmailRequestConverter;

@DataObject(generateConverter = true)
public class EmailRequest {
  private String from;
  private String to;
  private String subject;
  private String text;

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

  public String getText() { return text; }
  public void setText(String text) { this.text = text; }

//  public String getCreatedAt() {
//    return createdAt;
//  }
//
//  public void setId(String id) {
//    this.id = id;
//  }
//
//  public String getId() {
//    return id;
//  }
//
//  public void setCreatedAt(String createdAt) {
//    this.createdAt = createdAt;
//  }
}
