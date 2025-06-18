UPDATE organizations
SET organisation_documents = '-na-'  -- or '{}' or '' depending on data type
WHERE organisation_documents IS NULL;

UPDATE organization_create_requests
SET organisation_documents = '-na-'  -- or '{}' or '' depending on data type
WHERE organisation_documents IS NULL;


ALTER TABLE organizations
ALTER COLUMN organisation_documents SET NOT NULL;

ALTER TABLE organization_create_requests
ALTER COLUMN organisation_documents SET NOT NULL;
