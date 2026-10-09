-- Only used for the "conflict" linking path: the pelanggan a customer wants to claim is already
-- linked to a different customer account, so it needs Admin/Owner approval instead of auto-linking.

CREATE TABLE IF NOT EXISTS customer_pelanggan_link_requests (
    id                    BIGSERIAL PRIMARY KEY,
    id_customer_account   BIGINT NOT NULL REFERENCES customer_accounts(id) ON DELETE CASCADE,
    id_pelanggan          BIGINT NOT NULL REFERENCES tb_pelanggan(id),
    match_type            VARCHAR(20) NOT NULL, -- NOPOL, CONTACT
    match_value           VARCHAR(100),
    status                VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED
    reviewed_by           VARCHAR(50),
    reviewed_at           TIMESTAMP,
    review_note           VARCHAR(500),
    created_at            TIMESTAMP,
    created_by            VARCHAR(255),
    updated_at            TIMESTAMP,
    updated_by            VARCHAR(255),
    version               INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_link_requests_status ON customer_pelanggan_link_requests (status);

-- One outstanding request per customer account at a time, so a customer can't queue several
-- simultaneous claims against different pelanggan targets.
CREATE UNIQUE INDEX IF NOT EXISTS idx_link_requests_pending_account
    ON customer_pelanggan_link_requests (id_customer_account) WHERE status = 'PENDING';
