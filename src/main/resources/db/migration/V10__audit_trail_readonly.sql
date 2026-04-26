-- V10: Add read-only policy on tb_audit_trail
--
-- This prevents UPDATE and DELETE operations on tb_audit_trail
-- making it an append-only audit log

-- Function to block updates
CREATE OR REPLACE FUNCTION fn_audit_trail_block_update()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'UPDATE operation not allowed on tb_audit_trail. This table is append-only.';
END;
$$ LANGUAGE plpgsql;

-- Function to block deletes
CREATE OR REPLACE FUNCTION fn_audit_trail_block_delete()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'DELETE operation not allowed on tb_audit_trail. This table is append-only.';
END;
$$ LANGUAGE plpgsql;

-- Trigger to block updates
DROP TRIGGER IF EXISTS trg_audit_trail_block_update ON tb_audit_trail;
CREATE TRIGGER trg_audit_trail_block_update
    BEFORE UPDATE ON tb_audit_trail
    FOR EACH ROW EXECUTE FUNCTION fn_audit_trail_block_update();

-- Trigger to block deletes
DROP TRIGGER IF EXISTS trg_audit_trail_block_delete ON tb_audit_trail;
CREATE TRIGGER trg_audit_trail_block_delete
    BEFORE DELETE ON tb_audit_trail
    FOR EACH ROW EXECUTE FUNCTION fn_audit_trail_block_delete();

-- Remove UPDATE and DELETE permissions from public role
-- (Execute only if these permissions exist)
-- This is handled by the triggers above, as PostgreSQL doesn't support 
-- removing DML permissions at role level in the same way as Oracle