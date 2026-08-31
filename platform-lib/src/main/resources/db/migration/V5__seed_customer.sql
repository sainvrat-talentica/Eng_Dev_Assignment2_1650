-- Demo customer for order placement

INSERT INTO customer (id, name, phone, email)
VALUES (
    '44444444-4444-4444-4444-444444444401',
    'Demo Customer',
    '9876543210',
    'demo.customer@example.com'
)
ON CONFLICT (id) DO NOTHING;
