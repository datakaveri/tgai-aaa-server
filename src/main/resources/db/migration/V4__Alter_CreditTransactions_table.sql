ALTER TABLE credit_transactions
DROP COLUMN user_name,
ADD COLUMN requested_at TIMESTAMP WITHOUT TIME ZONE;
