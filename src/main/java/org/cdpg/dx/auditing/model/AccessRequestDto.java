package org.cdpg.dx.auditing.model;


import io.vertx.core.json.JsonObject;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.aaa.credit.models.Status;
import org.cdpg.dx.common.util.DateTimeHelper;
import org.cdpg.dx.database.postgres.base.entity.BaseEntity;
import org.cdpg.dx.database.postgres.util.EntityUtil;
import static org.cdpg.dx.auditing.util.DbConstants.*;

public class AccessRequestDto implements BaseEntity<AccessRequestDto> {
  private static final Logger LOGGER = LogManager.getLogger(AccessRequestDto.class);

  private String requestId;
  private Status status;
  private RequestType requestType;
  private JsonObject additionalInfo;
  private String providerId;
  private String consumerId;
  private String itemId;
  private String assetName;
  private String assetType;
  private String consumerOrganization;
  private LocalDateTime expiryAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private String consumerEmail;
  private String consumerFirstName;
  private String consumerLastName;

  public AccessRequestDto() {
  }

  public AccessRequestDto(JsonObject request) {
    setRequestId(request.getString(DB_REQUEST_ID));
    setStatus(Status.fromString(request.getString(DB_STATUS)));
    setRequestType(RequestType.valueOf(request.getString(DB_REQUEST_TYPE)));
    setAdditionalInfo(new JsonObject(request.getString(DB_ADDITIONAL_INFO)));
    setProviderId(request.getString(DB_PROVIDER_ID));
    setConsumerId(request.getString(DB_CONSUMER_ID));
    setItemId(request.getString(DB_ITEM_ID));
    setAssetName(request.getString(DB_ASSET_NAME));
    setAssetType(request.getString(DB_ASSET_TYPE));
    setConsumerOrganization(request.getString(DB_CONSUMER_ORGANIZATION));
    setConsumerEmail(request.getString(DB_CONSUMER_EMAIL));
    setConsumerFirstName(request.getString(DB_CONSUMER_FIRST_NAME));
    setConsumerLastName(request.getString(DB_CONSUMER_LAST_NAME));
    if(request.getString(DB_EXPIRY_AT) != null){
      setExpiryAt(LocalDateTime.parse(request.getString(DB_EXPIRY_AT)));
    }
    else {
      setExpiryAt(null);
    }
    setCreatedAt(LocalDateTime.parse(request.getString(DB_CREATED_AT)));
    setUpdatedAt(LocalDateTime.parse(request.getString(DB_UPDATED_AT)));
  }

  public static AccessRequestDto fromJson(JsonObject request) {
    AccessRequestDto dto = new AccessRequestDto();
    LOGGER.info("AccessRequestDto fromJson: " + request.encodePrettily());
    JsonObject entries = request.getJsonArray("rows").getJsonObject(0);
    dto.setRequestId(entries.getString(DB_REQUEST_ID));
    dto.setStatus(Status.fromString(entries.getString(DB_STATUS)));
    dto.setRequestType(RequestType.valueOf(entries.getString(DB_REQUEST_TYPE)));
    dto.setAdditionalInfo(new JsonObject(entries.getString(DB_ADDITIONAL_INFO)));
    dto.setProviderId(entries.getString(DB_PROVIDER_ID));
    dto.setConsumerId(entries.getString(DB_CONSUMER_ID));
    dto.setItemId(entries.getString(DB_ITEM_ID));
    dto.setAssetName(entries.getString(DB_ASSET_NAME));
    dto.setAssetType(entries.getString(DB_ASSET_TYPE));
    dto.setConsumerOrganization(entries.getString(DB_CONSUMER_ORGANIZATION));
    dto.setConsumerEmail(entries.getString(DB_CONSUMER_EMAIL));
    dto.setConsumerFirstName(entries.getString(DB_CONSUMER_FIRST_NAME));
    dto.setConsumerLastName(entries.getString(DB_CONSUMER_LAST_NAME));
    if(entries.getString(DB_EXPIRY_AT) != null){
      dto.setExpiryAt(LocalDateTime.parse(entries.getString(DB_EXPIRY_AT)));
    }
    else {
      dto.setExpiryAt(null);
    }
    dto.setCreatedAt(LocalDateTime.parse(entries.getString(DB_CREATED_AT)));
    dto.setUpdatedAt(LocalDateTime.parse(entries.getString(DB_UPDATED_AT)));
    return dto;
  }


  @Override
  public Map<String, Object> toNonEmptyFieldsMap() {
    Map<String, Object> fieldsMap = new HashMap<>();

    EntityUtil.putIfPresent(fieldsMap, DB_STATUS, getStatus());
    EntityUtil.putIfPresent(fieldsMap, DB_REQUEST_TYPE, getRequestType());
    EntityUtil.putIfPresent(fieldsMap, DB_ADDITIONAL_INFO, getAdditionalInfo());
    EntityUtil.putIfPresent(fieldsMap, DB_PROVIDER_ID, getProviderId());
    EntityUtil.putIfPresent(fieldsMap, DB_ASSET_NAME, getAssetName());
    EntityUtil.putIfPresent(fieldsMap, DB_EXPIRY_AT, getExpiryAt());
    EntityUtil.putIfPresent(fieldsMap, DB_CONSUMER_ORGANIZATION, getConsumerOrganization());
    EntityUtil.putIfPresent(fieldsMap, DB_CONSUMER_EMAIL, getConsumerEmail());
    EntityUtil.putIfPresent(fieldsMap, DB_CONSUMER_FIRST_NAME, getConsumerFirstName());
    EntityUtil.putIfPresent(fieldsMap, DB_CONSUMER_LAST_NAME, getConsumerLastName());
    EntityUtil.putIfPresent(fieldsMap, DB_CONSUMER_ID, getConsumerId());
    EntityUtil.putIfPresent(fieldsMap, DB_ASSET_TYPE, getAssetType());
    EntityUtil.putIfPresent(fieldsMap, DB_ITEM_ID, getItemId());

    return fieldsMap;
  }

  public JsonObject toJson() {
    JsonObject request =
        new JsonObject()
            .put(REQUEST_ID, getRequestId())
            .put(REQUEST_STATUS, getStatus())
            .put(REQUEST_TYPE, getRequestType())
            .put(ADDITIONAL_INFO, getAdditionalInfo())
            .put(
                USER,
                new JsonObject()
                    //.put(PROVIDER_ID, getProviderId())
                    .put(CONSUMER_ID, getConsumerId())
                    .put(CONSUMER_FIRST_NAME, getConsumerFirstName())
                    .put(CONSUMER_LAST_NAME, getConsumerLastName())
                    .put(CONSUMER_EMAIL, getConsumerEmail())
                    .put(CONSUMER_ORGANIZATION, getConsumerOrganization()))
            .put(
                ASSET,
                new JsonObject()
                    .put(ITEM_ID, getItemId())
                    .put(ASSET_NAME, getAssetName())
                    .put(ASSET_TYPE, getAssetType())
                    .put(EXPIRY_AT, getExpiryAt())
                    .put(CREATED_AT, getCreatedAt())
                    .put(UPDATED_AT, getUpdatedAt()));
    return request;
  }

  @Override
  public String getTableName() {
    return REQUEST_TABLE;
  }

  public String getRequestId() {
    return requestId;
  }

  public AccessRequestDto setRequestId(String requestId) {
    this.requestId = requestId;
    return this;
  }

  public Status getStatus() {
    return status;
  }

  public AccessRequestDto setStatus(Status status) {
    this.status = status;
    return this;
  }

  public String getConsumerEmail() {
    return consumerEmail;
  }

  public AccessRequestDto setConsumerEmail(String consumerEmail) {
    this.consumerEmail = consumerEmail;
    return this;
  }

  public String getConsumerFirstName() {
    return consumerFirstName;
  }

  public AccessRequestDto setConsumerFirstName(String consumerFirstName) {
    this.consumerFirstName = consumerFirstName;
    return this;
  }

  public String getConsumerLastName() {
    return consumerLastName;
  }

  public AccessRequestDto setConsumerLastName(String consumerLastName) {
    this.consumerLastName = consumerLastName;
    return this;
  }

  public RequestType getRequestType() {
    return requestType;
  }

  public AccessRequestDto setRequestType(RequestType requestType) {
    this.requestType = requestType;
    return this;
  }

  public JsonObject getAdditionalInfo() {
    return additionalInfo;
  }

  public AccessRequestDto setAdditionalInfo(JsonObject additionalInfo) {
    this.additionalInfo = additionalInfo;
    return this;
  }

  public String getProviderId() {
    return providerId;
  }

  public AccessRequestDto setProviderId(String providerId) {
    this.providerId = providerId;
    return this;
  }

  public String getConsumerId() {
    return consumerId;
  }

  public AccessRequestDto setConsumerId(String consumerId) {
    this.consumerId = consumerId;
    return this;
  }

  public String getItemId() {
    return itemId;
  }

  public AccessRequestDto setItemId(String itemId) {
    this.itemId = itemId;
    return this;
  }

  public String getAssetName() {
    return assetName;
  }

  public AccessRequestDto setAssetName(String assetName) {
    this.assetName = assetName;
    return this;
  }

  public String getAssetType() {
    return assetType;
  }

  public AccessRequestDto setAssetType(String assetType) {
    this.assetType = assetType;
    return this;
  }


  public String getConsumerOrganization() {
    return consumerOrganization;
  }

  public AccessRequestDto setConsumerOrganization(String consumerOrganization) {
    this.consumerOrganization = consumerOrganization;
    return this;
  }

  public LocalDateTime getExpiryAt() {
    return expiryAt;
  }

  public AccessRequestDto setExpiryAt(LocalDateTime expiryAt) {
    this.expiryAt = expiryAt;
    return this;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public AccessRequestDto setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public AccessRequestDto setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  @Override
  public String toString() {
    return "AccessRequestDto{" +
        "requestId='" + requestId + '\'' +
        ", status=" + status +
        ", requestType=" + requestType +
        ", additionalInfo=" + additionalInfo +
        ", providerId='" + providerId + '\'' +
        ", consumerId='" + consumerId + '\'' +
        ", itemId='" + itemId + '\'' +
        ", assetName='" + assetName + '\'' +
        ", assetType='" + assetType + '\'' +
        ", consumerOrganization='" + consumerOrganization + '\'' +
        ", expiryAt=" + expiryAt +
        ", createdAt=" + createdAt +
        ", updatedAt=" + updatedAt +
        ", consumerEmail='" + consumerEmail + '\'' +
        ", consumerFirstName='" + consumerFirstName + '\'' +
        ", consumerLastName='" + consumerLastName + '\'' +
        '}';
  }
}