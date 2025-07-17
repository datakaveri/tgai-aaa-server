CREATE TABLE KYC_Transactions (
    id UUID DEFAULT public.gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL,
    txn_id VARCHAR(512) NOT NULL,
    code_verifier VARCHAR(512) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_flag BOOLEAN NOT NULL DEFAULT FALSE
);
