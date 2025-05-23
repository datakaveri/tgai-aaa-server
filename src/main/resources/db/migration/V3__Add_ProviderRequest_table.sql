CREATE TABLE IF NOT EXISTS provider_requests (
    id UUID DEFAULT public.gen_random_uuid() PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    user_id UUID NOT NULL REFERENCES organization_users(id),
    status VARCHAR NOT NULL CHECK (status IN ('pending', 'approved', 'rejected')),
    requested_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITHOUT TIME ZONE
);