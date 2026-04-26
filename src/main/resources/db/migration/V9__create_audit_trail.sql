-- V9: Create centralized audit trail table
--
-- This table captures all changes across all application tables with before/after values.

CREATE SEQUENCE IF NOT EXISTS tb_audit_trail_id_seq;

CREATE TABLE tb_audit_trail (
    id BIGINT NOT NULL DEFAULT nextval('tb_audit_trail_id_seq'),
    table_name VARCHAR(255) NOT NULL,
    field_name VARCHAR(255),
    action VARCHAR(50) NOT NULL,
    user_id BIGINT,
    username VARCHAR(255),
    timestamp TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    before_value JSONB,
    after_value JSONB,
    record_id BIGINT,
    version INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT pk_tb_audit_trail PRIMARY KEY (id)
);

COMMENT ON TABLE tb_audit_trail IS 'Centralized audit trail - captures all database changes with before/after values';
COMMENT ON COLUMN tb_audit_trail.table_name IS 'Name of the table that was changed';
COMMENT ON COLUMN tb_audit_trail.field_name IS 'Name of the specific field that changed (null for insert/delete of entire record)';
COMMENT ON COLUMN tb_audit_trail.action IS 'Type of operation: INSERT, UPDATE, DELETE';
COMMENT ON COLUMN tb_audit_trail.user_id IS 'ID of the user who made the change';
COMMENT ON COLUMN tb_audit_trail.username IS 'Username for display purposes';
COMMENT ON COLUMN tb_audit_trail.timestamp IS 'When the change occurred';
COMMENT ON COLUMN tb_audit_trail.before_value IS 'JSON representation of the value BEFORE the change';
COMMENT ON COLUMN tb_audit_trail.after_value IS 'JSON representation of the value AFTER the change';
COMMENT ON COLUMN tb_audit_trail.record_id IS 'Primary key ID of the affected record';

CREATE INDEX idx_audit_trail_table_name ON tb_audit_trail(table_name);
CREATE INDEX idx_audit_trail_timestamp ON tb_audit_trail(timestamp);
CREATE INDEX idx_audit_trail_user_id ON tb_audit_trail(user_id);
CREATE INDEX idx_audit_trail_record_id ON tb_audit_trail(record_id);