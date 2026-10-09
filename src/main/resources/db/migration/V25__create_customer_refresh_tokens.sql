-- Refresh tokens for customer sessions, kept separate from the staff refresh_tokens table
-- (which is keyed by username, a global string). Customer sessions are keyed by
-- customer_account_id, a different identity space, so mixing them risks accidental collisions.

CREATE TABLE IF NOT EXISTS customer_refresh_tokens (
    id                    VARCHAR(36) PRIMARY KEY,
    id_customer_account   BIGINT NOT NULL REFERENCES customer_accounts(id) ON DELETE CASCADE,
    family_id             VARCHAR(36) NOT NULL,
    created_at            TIMESTAMP NOT NULL,
    expires_at            TIMESTAMP NOT NULL,
    rotated_at            TIMESTAMP,
    revoked_at            TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_customer_refresh_tokens_account ON customer_refresh_tokens (id_customer_account);
CREATE INDEX IF NOT EXISTS idx_customer_refresh_tokens_family_id ON customer_refresh_tokens (family_id);
