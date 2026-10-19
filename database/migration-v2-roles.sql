ALTER TABLE users
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'ADMIN',
    ADD COLUMN customer_id VARCHAR(20) NULL,
    ADD CONSTRAINT chk_user_role CHECK (role IN ('ADMIN', 'CUSTOMER')),
    ADD CONSTRAINT chk_user_customer_link CHECK (
        (role = 'ADMIN' AND customer_id IS NULL) OR (role = 'CUSTOMER' AND customer_id IS NOT NULL)
    ),
    ADD CONSTRAINT fk_user_customer FOREIGN KEY (customer_id) REFERENCES customers(id);

UPDATE users SET role = 'ADMIN', password = '$2a$10$16pMrBGZR4clgnvGkSGsau8HMWVnru8iubwKv791G85vgLNsrfZ1a' WHERE username = 'admin';

ALTER TABLE users ALTER COLUMN role SET DEFAULT 'CUSTOMER';

-- Existing accounts other than the seeded admin remain ADMIN to preserve v1 behavior.
-- Their plaintext passwords must be reset to BCrypt hashes before those accounts can log in.
