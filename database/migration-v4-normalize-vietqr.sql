-- Safe and idempotent: old QR payments are bank transfers; payment status and paid_at are unchanged.
UPDATE rental_orders
SET payment_method = 'BANK_TRANSFER'
WHERE payment_method = 'VIETQR';
