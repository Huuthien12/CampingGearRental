CREATE TABLE users (
    id VARCHAR(20) PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);

CREATE TABLE customers (
    id VARCHAR(20) PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(255),
    address VARCHAR(255)
);

CREATE TABLE categories (
    id VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE equipment (
    id VARCHAR(20) PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    category_id VARCHAR(20) NOT NULL,
    price_per_day DECIMAL(12, 2) NOT NULL,
    total_quantity INT NOT NULL,
    available_quantity INT NOT NULL,
    status VARCHAR(30) NOT NULL,
    CONSTRAINT fk_equipment_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT chk_equipment_price CHECK (price_per_day > 0),
    CONSTRAINT chk_equipment_total_quantity CHECK (total_quantity >= 0),
    CONSTRAINT chk_equipment_available_quantity CHECK (available_quantity BETWEEN 0 AND total_quantity)
);

CREATE TABLE rental_orders (
    id VARCHAR(20) PRIMARY KEY,
    customer_id VARCHAR(20) NOT NULL,
    rental_date DATE NOT NULL,
    expected_return_date DATE NOT NULL,
    actual_return_date DATE,
    status VARCHAR(20) NOT NULL,
    subtotal DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    discount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    total DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    payment_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rental_order_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT chk_rental_dates CHECK (expected_return_date >= rental_date),
    CONSTRAINT chk_rental_amounts CHECK (subtotal >= 0 AND discount >= 0 AND total >= 0)
);

CREATE TABLE rental_details (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rental_order_id VARCHAR(20) NOT NULL,
    equipment_id VARCHAR(20) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(12, 2) NOT NULL,
    CONSTRAINT uq_rental_detail_equipment UNIQUE (rental_order_id, equipment_id),
    CONSTRAINT fk_rental_detail_order FOREIGN KEY (rental_order_id) REFERENCES rental_orders(id),
    CONSTRAINT fk_rental_detail_equipment FOREIGN KEY (equipment_id) REFERENCES equipment(id),
    CONSTRAINT chk_rental_detail_quantity CHECK (quantity > 0),
    CONSTRAINT chk_rental_detail_unit_price CHECK (unit_price > 0)
);
