-- Align the customer-portal tables with the tb_ prefix used by every other domain table.
-- The original CREATE statements (V22-V31) are already applied and their checksums frozen, so
-- the rename is done here rather than by editing them. Foreign keys reference the tables by OID,
-- so child constraints keep working automatically; sequences and indexes are renamed explicitly
-- to avoid orphaned legacy names.

-- Tables -------------------------------------------------------------------------------------
ALTER TABLE customer_accounts                RENAME TO tb_customer_accounts;
ALTER TABLE customer_bookings                RENAME TO tb_customer_bookings;
ALTER TABLE customer_pelanggan_link_requests RENAME TO tb_customer_pelanggan_link_requests;
ALTER TABLE customer_refresh_tokens          RENAME TO tb_customer_refresh_tokens;
ALTER TABLE customer_vehicle_claim_requests  RENAME TO tb_customer_vehicle_claim_requests;

-- Primary keys -------------------------------------------------------------------------------
ALTER INDEX customer_accounts_pkey                RENAME TO tb_customer_accounts_pkey;
ALTER INDEX customer_bookings_pkey                RENAME TO tb_customer_bookings_pkey;
ALTER INDEX customer_pelanggan_link_requests_pkey RENAME TO tb_customer_pelanggan_link_requests_pkey;
ALTER INDEX customer_refresh_tokens_pkey          RENAME TO tb_customer_refresh_tokens_pkey;
ALTER INDEX customer_vehicle_claim_requests_pkey  RENAME TO tb_customer_vehicle_claim_requests_pkey;

-- Secondary indexes --------------------------------------------------------------------------
ALTER INDEX idx_customer_accounts_email       RENAME TO idx_tb_customer_accounts_email;
ALTER INDEX idx_customer_accounts_phone       RENAME TO idx_tb_customer_accounts_phone;
ALTER INDEX idx_customer_accounts_pelanggan   RENAME TO idx_tb_customer_accounts_pelanggan;

ALTER INDEX idx_customer_bookings_pelanggan   RENAME TO idx_tb_customer_bookings_pelanggan;
ALTER INDEX idx_customer_bookings_account     RENAME TO idx_tb_customer_bookings_account;
ALTER INDEX idx_customer_bookings_status      RENAME TO idx_tb_customer_bookings_status;

ALTER INDEX idx_link_requests_status              RENAME TO idx_tb_link_requests_status;
ALTER INDEX idx_link_requests_pending_account     RENAME TO idx_tb_link_requests_pending_account;

ALTER INDEX idx_customer_refresh_tokens_account   RENAME TO idx_tb_customer_refresh_tokens_account;
ALTER INDEX idx_customer_refresh_tokens_family_id RENAME TO idx_tb_customer_refresh_tokens_family_id;

ALTER INDEX idx_vehicle_claim_requests_status            RENAME TO idx_tb_vehicle_claim_requests_status;
ALTER INDEX idx_vehicle_claim_requests_nopol             RENAME TO idx_tb_vehicle_claim_requests_nopol;
ALTER INDEX idx_vehicle_claim_requests_pending_account   RENAME TO idx_tb_vehicle_claim_requests_pending_account;
ALTER INDEX idx_vehicle_claim_requests_pending_nopol     RENAME TO idx_tb_vehicle_claim_requests_pending_nopol;

-- Identity sequences -------------------------------------------------------------------------
ALTER SEQUENCE customer_accounts_id_seq                RENAME TO tb_customer_accounts_id_seq;
ALTER SEQUENCE customer_bookings_id_seq                RENAME TO tb_customer_bookings_id_seq;
ALTER SEQUENCE customer_pelanggan_link_requests_id_seq RENAME TO tb_customer_pelanggan_link_requests_id_seq;
ALTER SEQUENCE customer_vehicle_claim_requests_id_seq  RENAME TO tb_customer_vehicle_claim_requests_id_seq;
