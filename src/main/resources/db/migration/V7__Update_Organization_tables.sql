ALTER TABLE organizations
ALTER COLUMN organisation_documents SET NOT NULL;

ALTER TABLE organization_create_requests
ALTER COLUMN organisation_documents SET NOT NULL;
