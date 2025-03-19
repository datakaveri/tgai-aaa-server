ALTER TABLE organization ADD CONSTRAINT organization_pkey PRIMARY KEY (id);

CREATE TABLE IF NOT EXISTS org_requests (
    id uuid DEFAULT public.gen_random_uuid() NOT NULL,
    org_id UUID NOT NULL,
    user_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    created_at TIMESTAMP without time zone NOT NULL DEFAULT NOW(),
    FOREIGN KEY (org_id) REFERENCES organization(id) ON DELETE CASCADE
);
