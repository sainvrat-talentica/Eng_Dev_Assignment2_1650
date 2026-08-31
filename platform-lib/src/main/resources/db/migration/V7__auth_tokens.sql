-- Per-actor API tokens for customer and driver authentication (local demo seeds).

ALTER TABLE customer ADD COLUMN IF NOT EXISTS api_token VARCHAR(255);
ALTER TABLE driver ADD COLUMN IF NOT EXISTS api_token VARCHAR(255);

UPDATE customer
SET api_token = 'demo-customer-token-local-only'
WHERE id = '44444444-4444-4444-4444-444444444401'
  AND api_token IS NULL;

UPDATE driver SET api_token = 'demo-driver-token-001' WHERE id = '55555555-5555-5555-5555-555555555501' AND api_token IS NULL;
UPDATE driver SET api_token = 'demo-driver-token-002' WHERE id = '55555555-5555-5555-5555-555555555502' AND api_token IS NULL;
UPDATE driver SET api_token = 'demo-driver-token-003' WHERE id = '55555555-5555-5555-5555-555555555503' AND api_token IS NULL;
UPDATE driver SET api_token = 'demo-driver-token-004' WHERE id = '55555555-5555-5555-5555-555555555504' AND api_token IS NULL;
UPDATE driver SET api_token = 'demo-driver-token-005' WHERE id = '55555555-5555-5555-5555-555555555505' AND api_token IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_customer_api_token ON customer (api_token) WHERE api_token IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_driver_api_token ON driver (api_token) WHERE api_token IS NOT NULL;
