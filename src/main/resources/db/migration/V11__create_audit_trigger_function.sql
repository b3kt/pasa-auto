-- V11: Create audit trail trigger function at database layer
--
-- This creates trigger functions that automatically capture all changes
-- to any table when INSERT/UPDATE/DELETE occurs.
--
-- User info is captured from:
-- 1. PostgreSQL session variables (app.username, app.user_id) set by application
-- 2. Fallback to created_by/updated_by in the entity if available

-- Drop existing function if exists
DROP FUNCTION IF EXISTS fn_record_audit_trail() CASCADE;

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
    v_session_user_id TEXT;
BEGIN
    -- Get table name from TG_TABLE_NAME
    v_table_name = TG_TABLE_NAME;
    
    -- Determine action type
    IF TG_OP = 'INSERT' THEN
        v_action = 'INSERT';
        v_after_value = to_jsonb(NEW);
        v_record_id = NEW.id;
        
        -- Try to get username from created_by field if not in session
        IF NEW.created_by IS NOT NULL AND NEW.created_by != '' THEN
            v_username := NEW.created_by;
        END IF;
        
    ELSIF TG_OP = 'UPDATE' THEN
        v_action = 'UPDATE';
        v_before_value = to_jsonb(OLD);
        v_after_value = to_jsonb(NEW);
        v_record_id = NEW.id;
        
        -- Try to get username from updated_by field if not in session
        IF NEW.updated_by IS NOT NULL AND NEW.updated_by != '' THEN
            v_username := NEW.updated_by;
        END IF;
        
    ELSIF TG_OP = 'DELETE' THEN
        v_action = 'DELETE';
        v_before_value = to_jsonb(OLD);
        v_record_id = OLD.id;
        
        -- Try to get username from updated_by field (last modifier)
        IF OLD.updated_by IS NOT NULL AND OLD.updated_by != '' THEN
            v_username := OLD.updated_by;
        END IF;
    END IF;

    -- Get current user from session (set by application) or use entity fields
    v_username := COALESCE(
        NULLIF(current_setting('app.username', true), ''),
        v_username,
        'system'
    );
    
    v_session_user_id := current_setting('app.user_id', true);
    
    -- Convert user_id from string to bigint if present
    IF v_session_user_id IS NOT NULL AND v_session_user_id != '' THEN
        BEGIN
            v_user_id := v_session_user_id::BIGINT;
        EXCEPTION WHEN OTHERS THEN
            v_user_id := NULL;
        END;
    END IF;

    -- Insert audit record
    INSERT INTO tb_audit_trail (
        table_name,
        action,
        before_value,
        after_value,
        record_id,
        user_id,
        username,
        timestamp
    ) VALUES (
        v_table_name,
        v_action,
        v_before_value,
        v_after_value,
        v_record_id,
        v_user_id,
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