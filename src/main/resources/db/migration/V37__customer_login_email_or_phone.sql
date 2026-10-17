-- Customers register with an email, a phone number, or both, and log in with either (ADR-0012).
-- tb_customer_accounts.email becomes optional; an account must still carry at least one login
-- identifier. The existing unique indexes already allow several NULLs: idx_tb_customer_accounts_email
-- (lower(email)) treats NULLs as distinct, and idx_tb_customer_accounts_phone is partial on phone.
--
-- Every existing account has an email, so the CHECK is validated right away.

ALTER TABLE tb_customer_accounts ALTER COLUMN email DROP NOT NULL;

ALTER TABLE tb_customer_accounts
    ADD CONSTRAINT chk_tb_customer_accounts_login_identifier CHECK (email IS NOT NULL OR phone IS NOT NULL);

COMMENT ON COLUMN tb_customer_accounts.email IS
    'Login email (optional when phone is set) and, for the linked customer, the authoritative contact email (ADR-0012).';
COMMENT ON COLUMN tb_customer_accounts.phone IS
    'Login phone (optional when email is set) and, for the linked customer, the authoritative mobile number (ADR-0012). Canonical national digits.';
