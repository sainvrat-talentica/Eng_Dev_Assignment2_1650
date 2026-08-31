-- Demo drivers for tracking / assignment (Maharashtra)

INSERT INTO driver (id, name, phone, license_number, partner_company, city, state, status)
VALUES
    ('55555555-5555-5555-5555-555555555501', 'Aarav Patil', '9000000001', 'MH-12-AB-1001', 'SwiftFleet', 'Pune', 'Maharashtra', 'AVAILABLE'),
    ('55555555-5555-5555-5555-555555555502', 'Neha Kulkarni', '9000000002', 'MH-12-CD-1002', 'SwiftFleet', 'Pune', 'Maharashtra', 'AVAILABLE'),
    ('55555555-5555-5555-5555-555555555503', 'Rohan Desai', '9000000003', 'MH-12-EF-1003', 'CityRiders', 'Pune', 'Maharashtra', 'AVAILABLE'),
    ('55555555-5555-5555-5555-555555555504', 'Priya Shah', '9000000004', 'MH-01-GH-1004', 'CityRiders', 'Mumbai', 'Maharashtra', 'AVAILABLE'),
    ('55555555-5555-5555-5555-555555555505', 'Vikram Joshi', '9000000005', 'MH-01-IJ-1005', 'SwiftFleet', 'Mumbai', 'Maharashtra', 'AVAILABLE')
ON CONFLICT (id) DO NOTHING;
