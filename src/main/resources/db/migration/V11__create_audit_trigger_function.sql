-- V11: Create audit trail trigger function at database layer
--
-- This creates trigger functions that automatically capture all changes
-- to any table when INSERT/UPDATE/DELETE occurs.

-- Drop existing functions if they exist
DROP FUNCTION IF EXISTS fn_audit_trail_insert() CASCADE;
DROP FUNCTION IF EXISTS fn_audit_trail_update() CASCADE;
DROP FUNCTION IF EXISTS fn_audit_trail_delete() CASCADE;

-- Create the common audit recording function
CREATE OR REPLACE FUNCTION fn_record_audit_trail()
RETURNS TRIGGER AS $$
DECLARE
    v_table_name TEXT;
    v_action TEXT;
    v_before_value JSONB;
    v_after_value JSONB;
    v_record_id BIGINT;
    v_username TEXT;
    v_user_id BIGINT;
BEGIN
    -- Get table name from TG_TABLE_NAME
    v_table_name = TG_TABLE_NAME;
    
    -- Determine action type
    IF TG_OP = 'INSERT' THEN
        v_action = 'INSERT';
        v_after_value = to_jsonb(NEW);
        v_record_id = NEW.id;
    ELSIF TG_OP = 'UPDATE' THEN
        v_action = 'UPDATE';
        v_before_value = to_jsonb(OLD);
        v_after_value = to_jsonb(NEW);
        v_record_id = NEW.id;
    ELSIF TG_OP = 'DELETE' THEN
        v_action = 'DELETE';
        v_before_value = to_jsonb(OLD);
        v_record_id = OLD.id;
    END IF;

    -- Get current user from session (if available)
    SELECT current_setting('app.username', true) INTO v_username;
    IF v_username IS NULL OR v_username = '' THEN
        v_username = 'system';
    END IF;

    -- Insert audit record
    INSERT INTO tb_audit_trail (
        table_name,
        action,
        before_value,
        after_value,
        record_id,
        username,
        timestamp
    ) VALUES (
        v_table_name,
        v_action,
        v_before_value,
        v_after_value,
        v_record_id,
        v_username,
        CURRENT_TIMESTAMP
    );

    RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Comment on function
COMMENT ON FUNCTION fn_record_audit_trail() IS 'Records all changes to tb_audit_trail table';

-- Grant execute permission
GRANT EXECUTE ON FUNCTION fn_record_audit_trail() TO PUBLIC;