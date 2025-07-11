CREATE TABLE KYC_Transactions (
    user_id UUID NOT NULL,
    txn_id VARCHAR(255) NOT NULL,
    code_verifier VARCHAR(255) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_flag BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (user_id),
    UNIQUE (txn_id)
);
