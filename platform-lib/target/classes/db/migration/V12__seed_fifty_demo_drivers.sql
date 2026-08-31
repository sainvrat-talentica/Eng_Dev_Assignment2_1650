-- Expand demo fleet to 50 drivers for Assignment 1 GPS simulator (50 × 1 update/5s ≈ 10 evt/s).

INSERT INTO driver (id, name, phone, license_number, partner_company, city, state, status)
SELECT
    ('55555555-5555-5555-5555-' || lpad((555555555500 + gs.i)::text, 12, '0'))::uuid,
    '[T] Demo Driver ' || (500 + gs.i),
    '9000' || lpad((500 + gs.i)::text, 6, '0'),
    'MH-DEMO-' || lpad(gs.i::text, 4, '0'),
    'SwiftFleet',
    CASE WHEN gs.i % 2 = 0 THEN 'Pune' ELSE 'Mumbai' END,
    'Maharashtra',
    'AVAILABLE'
FROM generate_series(6, 50) AS gs(i)
ON CONFLICT (id) DO NOTHING;

UPDATE driver
SET api_token = 'demo-driver-token-' || lpad(gs.i::text, 3, '0')
FROM generate_series(6, 50) AS gs(i)
WHERE driver.id = ('55555555-5555-5555-5555-' || lpad((555555555500 + gs.i)::text, 12, '0'))::uuid
  AND driver.api_token IS NULL;
