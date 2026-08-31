-- Demo restaurants for local validation (Maharashtra cities)

INSERT INTO cuisine (id, name) VALUES
    ('11111111-1111-1111-1111-111111111101', 'Maharashtrian'),
    ('11111111-1111-1111-1111-111111111102', 'Street Food'),
    ('11111111-1111-1111-1111-111111111103', 'Biryani'),
    ('11111111-1111-1111-1111-111111111104', 'Mughlai')
ON CONFLICT (name) DO NOTHING;

INSERT INTO restaurant (
    id, name, address_line1, city, state, pincode, rating, is_open,
    estimated_wait_mins, capacity, status, contact_email, opening_time, closing_time
) VALUES
    (
        '22222222-2222-2222-2222-222222222201',
        'Misal House',
        'JM Road, Shivajinagar',
        'Pune',
        'Maharashtra',
        '411005',
        4.5,
        true,
        25,
        120,
        'ACTIVE',
        'owner@misalhouse.example',
        '08:00',
        '22:00'
    ),
    (
        '22222222-2222-2222-2222-222222222202',
        'Pune Biryani Co',
        'FC Road',
        'Pune',
        'Maharashtra',
        '411004',
        4.2,
        true,
        35,
        80,
        'ACTIVE',
        'hello@punebiryani.example',
        '11:00',
        '23:00'
    ),
    (
        '22222222-2222-2222-2222-222222222203',
        'Marine Drive Dosa',
        'Marine Drive',
        'Mumbai',
        'Maharashtra',
        '400020',
        4.7,
        true,
        20,
        60,
        'ACTIVE',
        'contact@marinedosa.example',
        '07:00',
        '21:00'
    )
ON CONFLICT (id) DO NOTHING;

INSERT INTO restaurant_cuisine (restaurant_id, cuisine_id) VALUES
    ('22222222-2222-2222-2222-222222222201', '11111111-1111-1111-1111-111111111101'),
    ('22222222-2222-2222-2222-222222222201', '11111111-1111-1111-1111-111111111102'),
    ('22222222-2222-2222-2222-222222222202', '11111111-1111-1111-1111-111111111103'),
    ('22222222-2222-2222-2222-222222222202', '11111111-1111-1111-1111-111111111104'),
    ('22222222-2222-2222-2222-222222222203', '11111111-1111-1111-1111-111111111102')
ON CONFLICT DO NOTHING;

INSERT INTO menu_item (id, restaurant_id, name, description, category, price, available) VALUES
    ('33333333-3333-3333-3333-333333333301', '22222222-2222-2222-2222-222222222201', 'Kolhapuri Misal', 'Spicy misal with farsan', 'Main', 120.00, true),
    ('33333333-3333-3333-3333-333333333302', '22222222-2222-2222-2222-222222222201', 'Poha', 'Classic breakfast poha', 'Breakfast', 60.00, true),
    ('33333333-3333-3333-3333-333333333303', '22222222-2222-2222-2222-222222222202', 'Chicken Biryani', 'Hyderabadi style', 'Main', 280.00, true),
    ('33333333-3333-3333-3333-333333333304', '22222222-2222-2222-2222-222222222202', 'Veg Biryani', 'Fragrant basmati rice', 'Main', 220.00, true),
    ('33333333-3333-3333-3333-333333333305', '22222222-2222-2222-2222-222222222203', 'Mumbai Masala Dosa', 'Crisp dosa with chutney', 'Main', 90.00, true)
ON CONFLICT (id) DO NOTHING;
