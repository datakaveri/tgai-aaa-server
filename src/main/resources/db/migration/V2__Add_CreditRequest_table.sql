CREATE TABLE IF NOT EXISTS credit_requests (
    id UUID DEFAULT public.gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES organization_users(id),
    user_name VARCHAR NOT NULL,
    amount	DECIMAL,
    status VARCHAR NOT NULL CHECK (status IN ('pending', 'approved', 'rejected')),
    requested_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE TABLE IF NOT EXISTS credit_transactions (
    id UUID DEFAULT public.gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES organization_users(id),
    user_name VARCHAR NOT NULL,
    amount DECIMAL NOT NULL,
    transacted_by UUID NOT NULL,
    transaction_status VARCHAR NOT NULL CHECK (transaction_status IN ('success', 'failure')),
    transaction_type VARCHAR NOT NULL CHECK (transaction_type IN ('credit','debit')),
    created_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE TABLE IF NOT EXISTS user_credits (
    id UUID DEFAULT public.gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES organization_users(id),
    balance	DECIMAL DEFAULT 0,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE TABLE IF NOT EXISTS compute_role (
    id UUID DEFAULT public.gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES organization_users(id),
    user_name VARCHAR NOT NULL,
    status VARCHAR NOT NULL CHECK (status IN ('pending', 'approved', 'rejected')),
    approved_by UUID,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);
