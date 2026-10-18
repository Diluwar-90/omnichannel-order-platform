ALTER TABLE payment ADD COLUMN idempotency_key VARCHAR(100);

CREATE UNIQUE INDEX idx_payment_idempotency_key ON payment(idempotency_key);
