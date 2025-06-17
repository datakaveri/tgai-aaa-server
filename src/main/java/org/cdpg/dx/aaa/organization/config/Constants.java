package org.cdpg.dx.aaa.organization.config;

import org.cdpg.dx.aaa.organization.models.Role;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Constants {

  private Constants()
  {

  }

  public static final String ORGANIZATION_TABLE="organizations";
  public static final String ORG_ID = "id";
  public static final String ORG_NAME = "name";
  public static final String CREATED_AT = "created_at";
  public static final String UPDATED_AT = "updated_at";



  public static final String ORG_JOIN_REQUEST_TABLE = "organization_join_requests";

  public static final String ORG_JOIN_ID = "id";
  public static final String ORGANIZATION_ID = "organization_id";
  public static final String USER_ID = "user_id";
  public static final String STATUS = "status";
  public static final String REQUESTED_AT = "requested_at";
  public static final String PROCESSED_AT = "processed_at";

  public static final String ORG_USER_ID="id";
  public static final String ROLE="role";

  //CreateOrganizationRequest
  public static final String ORG_CREATE_ID="id";
  public static final String ORG_CREATE_REQUEST_TABLE = "organization_create_requests";
  public static final String REQUESTED_BY="requested_by";
  public static final String ORG_LOGO="logo_path";
  public static final String ENTITY_TYPE="entity_type";
  public static final String ORG_SECTOR="org_sector";
  public static final String ORG_WEBSITE="website_link";
  public static final String ORG_ADDRESS="address";
  public static final String CERTIFICATE="certificate_path";
  public static final String PANCARD="pancard_path";
  public static final String RELEVANT_DOC="relevant_doc_path";
  public static final String EMP_ID="emp_id";
  public static final String JOB_TITLE="job_title";
  public static final String PHONE_NO="phone_no";
  public static final String USER_NAME = "user_name";
  public static final String ORG_DOCUMENTS = "organisation_documents";





  public static final List<String> ALL_ORG_CREATE_REQUEST_FIELDS = List.of(
    ORG_CREATE_ID,
    REQUESTED_BY,
    ORG_NAME,
    ENTITY_TYPE,
    ORG_SECTOR,
    ORG_WEBSITE,
    ORG_ADDRESS,
    CERTIFICATE,
    PANCARD,
    RELEVANT_DOC,
    EMP_ID,
    JOB_TITLE,
    PHONE_NO,
    ORG_DOCUMENTS,
    CREATED_AT,
    UPDATED_AT
  );

  public static final List<String> ALL_ORG_FIELDS = List.of(
    ORG_ID,
    ORG_NAME,
    ORG_LOGO,
    ENTITY_TYPE,
    ORG_SECTOR,
    ORG_WEBSITE,
    ORG_ADDRESS,
    CERTIFICATE,
    PANCARD,
    RELEVANT_DOC,
    ORG_DOCUMENTS,
    CREATED_AT,
    UPDATED_AT
  );


  public static final String ORG_USER_TABLE = "organization_users";

  //organization user fields
  public static final Map<String, String> ALLOWED_FILTER_MAP_FOR_ORG = Map.of(
          "orgName", ORG_NAME,
          "entityType", ENTITY_TYPE,
          "orgSector", ORG_SECTOR
  );

  public static final Map<String, String> ALLOWED_FILTER_MAP_FOR_ORG_JOIN_REQUEST = Map.of(
    "status", STATUS
  );

  public static final Map<String, String> ALLOWED_FILTER_MAP_FOR_ORG_USERS = Map.of(
    "organizationId", ORGANIZATION_ID,
    "userId", USER_ID,
    "userName", USER_NAME,
    "role", ROLE
  );
  public static final Map<String, String> ALLOWED_FILTER_MAP_FOR_ORG_CREATE_REQUEST = Map.of(
    "orgName", ORG_NAME,
    "entityType", ENTITY_TYPE,
    "orgSector", ORG_SECTOR,
    "status", STATUS
  );

  public static final Set<String> ALLOWED_SORT_FEILDS_ORG = Set.of("createdAt", "orgName", "entityType", "orgSector");



}
