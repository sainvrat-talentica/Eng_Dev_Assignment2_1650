-- Customer activate/deactivate for admin management
ALTER TABLE customer
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

CREATE INDEX idx_customer_status ON customer (status);

-- Payment completion time (payment state lives on order; no separate payment table)
ALTER TABLE "order"
    ADD COLUMN payment_processed_at TIMESTAMPTZ;
