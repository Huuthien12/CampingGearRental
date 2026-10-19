INSERT IGNORE INTO customers (id, full_name, phone, email, address) VALUES
    ('CUS001', 'Nguyễn Văn A', '0900000001', 'a@example.com', 'Đà Lạt');

INSERT IGNORE INTO users (id, username, password, role, customer_id) VALUES
    ('USR001', 'admin', '$2a$10$16pMrBGZR4clgnvGkSGsau8HMWVnru8iubwKv791G85vgLNsrfZ1a', 'ADMIN', NULL),
    ('USR002', 'cus1', '$2a$10$oDJXmduNDKRvQpsh8cpzkOKT5RCnrWBamLBqwbrxPgjn0rn.ZHnke', 'CUSTOMER', 'CUS001');

INSERT IGNORE INTO categories (id, name) VALUES
    ('CAT001', 'Tent'),
    ('CAT002', 'Sleeping Bag'),
    ('CAT003', 'Chair'),
    ('CAT004', 'Lamp'),
    ('CAT005', 'Stove');

INSERT IGNORE INTO equipment (id, name, category_id, price_per_day, total_quantity, available_quantity, status) VALUES
    ('EQ001', 'Lều 2 người', 'CAT001', 100000.00, 5, 5, 'AVAILABLE'),
    ('EQ002', 'Túi ngủ', 'CAT002', 40000.00, 10, 10, 'AVAILABLE'),
    ('EQ003', 'Ghế camping', 'CAT003', 30000.00, 10, 10, 'AVAILABLE'),
    ('EQ004', 'Đèn camping', 'CAT004', 20000.00, 8, 8, 'AVAILABLE'),
    ('EQ005', 'Bếp mini', 'CAT005', 50000.00, 4, 4, 'AVAILABLE');
