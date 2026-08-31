-- Customer registration/login profile fields

ALTER TABLE customer
    ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255),
    ADD COLUMN IF NOT EXISTS address_line1 TEXT,
    ADD COLUMN IF NOT EXISTS address_line2 TEXT,
    ADD COLUMN IF NOT EXISTS city VARCHAR(100),
    ADD COLUMN IF NOT EXISTS state VARCHAR(100) DEFAULT 'Maharashtra',
    ADD COLUMN IF NOT EXISTS pincode VARCHAR(20),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

UPDATE customer
SET address_line1 = COALESCE(address_line1, '123 MG Road'),
    city = COALESCE(city, 'Pune'),
    state = COALESCE(state, 'Maharashtra'),
    pincode = COALESCE(pincode, '411001'),
    updated_at = now()
WHERE id = '44444444-4444-4444-4444-444444444401';

CREATE UNIQUE INDEX IF NOT EXISTS idx_customer_email_lower ON customer (LOWER(email)) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_customer_phone ON customer (phone) WHERE phone IS NOT NULL;
