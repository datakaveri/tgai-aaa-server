ALTER TABLE organization_create_requests
ADD COLUMN manager_email VARCHAR NOT NULL UNIQUE;

ALTER TABLE compute_role ADD additional_info JSONB DEFAULT '{}';
