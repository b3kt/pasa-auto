-- Vehicle claim requests: a customer wants a plate (nopol) that is currently owned by another
-- customer. Default flow queues the claim for Admin/Owner approval (see app.customer.vehicle-claim.mode);
-- on approval the ownership is transferred to the claiming customer's pelanggan.

CREATE TABLE IF NOT EXISTS customer_vehicle_claim_requests (
    id                    BIGSERIAL PRIMARY KEY,
    id_customer_account   BIGINT NOT NULL REFERENCES customer_accounts(id) ON DELETE CASCADE,
    id_pelanggan          BIGINT NOT NULL REFERENCES tb_pelanggan(id), -- claimant's own pelanggan
    nopol                 VARCHAR(10) NOT NULL,
    id_current_owner      BIGINT REFERENCES tb_pelanggan(id),          -- previous owner at request time
    status                VARCHAR(20) NOT NULL DEFAULT 'PENDING',      -- PENDING, APPROVED, REJECTED
    reviewed_by           VARCHAR(50),
    reviewed_at           TIMESTAMP,
    review_note           VARCHAR(500),
    created_at            TIMESTAMP,
    created_by            VARCHAR(255),
    updated_at            TIMESTAMP,
    updated_by            VARCHAR(255),
    version               INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_vehicle_claim_requests_status ON customer_vehicle_claim_requests (status);
CREATE INDEX IF NOT EXISTS idx_vehicle_claim_requests_nopol ON customer_vehicle_claim_requests (nopol);

-- One outstanding request per customer account at a time.
CREATE UNIQUE INDEX IF NOT EXISTS idx_vehicle_claim_requests_pending_account
    ON customer_vehicle_claim_requests (id_customer_account) WHERE status = 'PENDING';

-- One outstanding request per plate at a time.
CREATE UNIQUE INDEX IF NOT EXISTS idx_vehicle_claim_requests_pending_nopol
    ON customer_vehicle_claim_requests (nopol) WHERE status = 'PENDING';
