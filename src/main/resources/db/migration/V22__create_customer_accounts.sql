-- Customer self-service portal: login identity, separate from tb_pelanggan (the business
-- record). A customer registers with just email/phone + password; linking to their existing
-- tb_pelanggan record (by plate number or contact match) is a distinct, later step.

CREATE TABLE IF NOT EXISTS customer_accounts (
    id             BIGSERIAL PRIMARY KEY,
    email          VARCHAR(150) NOT NULL,
    phone          VARCHAR(20),
    password_hash  VARCHAR(255) NOT NULL,
    full_name      VARCHAR(150) NOT NULL,
    active         BOOLEAN NOT NULL DEFAULT true,
    id_pelanggan   BIGINT REFERENCES tb_pelanggan(id),
    linked_at      TIMESTAMP,
    created_at     TIMESTAMP,
    created_by     VARCHAR(255),
    updated_at     TIMESTAMP,
    updated_by     VARCHAR(255),
    version        INTEGER DEFAULT 0
);

-- Case-insensitive: email is the login identifier.
CREATE UNIQUE INDEX IF NOT EXISTS idx_customer_accounts_email ON customer_accounts (lower(email));

-- Only enforced when a phone was actually given.
CREATE UNIQUE INDEX IF NOT EXISTS idx_customer_accounts_phone ON customer_accounts (phone) WHERE phone IS NOT NULL;

-- Strict 1:1 with tb_pelanggan: at most one customer account per pelanggan. Combined with
-- id_pelanggan being a single nullable column (not a join table), this also guarantees the
-- reverse direction - one account can point at only one pelanggan by construction.
CREATE UNIQUE INDEX IF NOT EXISTS idx_customer_accounts_pelanggan ON customer_accounts (id_pelanggan) WHERE id_pelanggan IS NOT NULL;
