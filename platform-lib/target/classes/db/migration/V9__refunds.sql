CREATE TABLE refund (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        UUID NOT NULL REFERENCES "order"(id),
    customer_id     UUID NOT NULL,
    amount          NUMERIC(12, 2) NOT NULL,
    status          VARCHAR(32) NOT NULL,
    reason          TEXT,
    failure_reason  TEXT,
    idempotency_key VARCHAR(128) UNIQUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMPTZ
);

CREATE INDEX idx_refund_order_id ON refund(order_id);
CREATE INDEX idx_refund_customer_id ON refund(customer_id);
CREATE INDEX idx_refund_status ON refund(status);
