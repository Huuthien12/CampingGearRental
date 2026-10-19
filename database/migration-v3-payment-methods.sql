ALTER TABLE rental_orders
    ADD COLUMN payment_method VARCHAR(20) NULL AFTER total,
    ADD COLUMN paid_at DATETIME NULL AFTER payment_status,
    ADD CONSTRAINT chk_payment_method CHECK (
        payment_method IS NULL OR payment_method IN ('CASH', 'BANK_TRANSFER', 'VIETQR')
    ),
    ADD CONSTRAINT chk_payment_status CHECK (
        payment_status IN ('UNPAID', 'TRANSFER_REPORTED', 'REVIEW_REQUIRED', 'REJECTED', 'PAID')
    );

-- Existing orders keep their payment_status and totals. Their payment method and paid_at remain unknown (NULL).
