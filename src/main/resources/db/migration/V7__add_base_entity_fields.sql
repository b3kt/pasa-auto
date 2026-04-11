-- V7: Add BaseEntity audit fields to tables that are missing them.
--
-- Field semantics
--   version    : optimistic-lock counter, default 0
--   created_at : set once on INSERT via column DEFAULT – never touched on UPDATE
--   updated_at : set on INSERT via column DEFAULT; auto-refreshed on every UPDATE
--                by trigger fn_set_updated_at (mirrors Hibernate @UpdateTimestamp)
--   created_by : username from application session, set on INSERT, never updated
--   updated_by : username from application session, set on UPDATE; NULL until first update
--
-- All ALTER TABLE statements use ADD COLUMN IF NOT EXISTS so the script is
-- safe to re-run against a database that already has some of these columns.

-- ── Shared trigger function ────────────────────────────────────────────────
-- Refreshes updated_at on every UPDATE row. Created once, reused by all tables.

CREATE OR REPLACE FUNCTION fn_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ── Helper macro (applied per table below) ────────────────────────────────
--
-- Pattern for each table:
--   1. ADD COLUMN IF NOT EXISTS for each missing field
--   2. Back-fill created_at for existing rows that would get NULL
--   3. DROP + CREATE trigger so the script is idempotent

-- ── roles ──────────────────────────────────────────────────────────────────
ALTER TABLE roles
    ADD COLUMN IF NOT EXISTS version    INTEGER                     NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITHOUT TIME ZONE          DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS created_by CHARACTER VARYING(255),
    ADD COLUMN IF NOT EXISTS updated_by CHARACTER VARYING(255);

UPDATE roles SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL;

DROP TRIGGER IF EXISTS trg_roles_set_updated_at ON roles;
CREATE TRIGGER trg_roles_set_updated_at
    BEFORE UPDATE ON roles
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- ── permissions ────────────────────────────────────────────────────────────
ALTER TABLE permissions
    ADD COLUMN IF NOT EXISTS version    INTEGER                     NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITHOUT TIME ZONE          DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS created_by CHARACTER VARYING(255),
    ADD COLUMN IF NOT EXISTS updated_by CHARACTER VARYING(255);

UPDATE permissions SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL;

DROP TRIGGER IF EXISTS trg_permissions_set_updated_at ON permissions;
CREATE TRIGGER trg_permissions_set_updated_at
    BEFORE UPDATE ON permissions
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- ── tb_spk_detail ──────────────────────────────────────────────────────────
-- Audit columns were present in V1 but dropped by V4; restored here.
ALTER TABLE tb_spk_detail
    ADD COLUMN IF NOT EXISTS version    INTEGER                     NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITHOUT TIME ZONE          DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS created_by CHARACTER VARYING(255),
    ADD COLUMN IF NOT EXISTS updated_by CHARACTER VARYING(255);

UPDATE tb_spk_detail SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL;

DROP TRIGGER IF EXISTS trg_tb_spk_detail_set_updated_at ON tb_spk_detail;
CREATE TRIGGER trg_tb_spk_detail_set_updated_at
    BEFORE UPDATE ON tb_spk_detail
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
