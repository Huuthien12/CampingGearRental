INSERT INTO users (id, username, password) VALUES
    ('USR001', 'admin', 'admin123');

INSERT INTO customers (id, full_name, phone, email, address) VALUES
    ('CUS001', 'Nguyễn Văn A', '0900000001', 'a@example.com', 'Đà Lạt');

INSERT INTO categories (id, name) VALUES
    ('CAT001', 'Tent'),
    ('CAT002', 'Sleeping Bag'),
    ('CAT003', 'Chair'),
    ('CAT004', 'Lamp'),
    ('CAT005', 'Stove');

INSERT INTO equipment (id, name, category_id, price_per_day, total_quantity, available_quantity, status) VALUES
    ('EQ001', 'Lều 2 người', 'CAT001', 100000.00, 5, 5, 'AVAILABLE'),
    ('EQ002', 'Túi ngủ', 'CAT002', 40000.00, 10, 10, 'AVAILABLE'),
    ('EQ003', 'Ghế camping', 'CAT003', 30000.00, 10, 10, 'AVAILABLE'),
    ('EQ004', 'Đèn camping', 'CAT004', 20000.00, 8, 8, 'AVAILABLE'),
    ('EQ005', 'Bếp mini', 'CAT005', 50000.00, 4, 4, 'AVAILABLE');
