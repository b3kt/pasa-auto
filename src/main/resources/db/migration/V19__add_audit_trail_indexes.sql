-- V19: Indexes for the audit trail page
--
-- The page lists tb_audit_trail newest first, searches it with a "contains" match over three
-- columns, and drills into the history of one record. idx_audit_trail_timestamp already covers
-- the listing; this migration adds what the search and the drill-downs need, and drops the two
-- indexes whose access paths the new composites now provide. Every row in this table is written
-- by the audit triggers, so each index costs on the write path of the whole application.
--
-- On a large table, build these with CREATE INDEX CONCURRENTLY outside Flyway instead: the plain
-- form below holds a lock that blocks audit inserts, and therefore application writes, until done.

-- "lower(col) like '%x%'" can never use a btree index, which is why the search scans the table
-- today. Trigram indexes do support it, and Postgres can bitmap-OR the three the way the query
-- ORs the columns. pg_trgm needs privileges the application user may not have, so its absence
-- leaves the rest of the migration intact.
DO $$
BEGIN
    CREATE EXTENSION IF NOT EXISTS pg_trgm;
EXCEPTION
    WHEN insufficient_privilege THEN
        RAISE NOTICE 'pg_trgm is not installed and could not be created; audit trail search keeps scanning the table';
END
$$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_trgm') THEN
        CREATE INDEX IF NOT EXISTS idx_audit_trail_table_name_trgm
            ON tb_audit_trail USING gin (lower(table_name) gin_trgm_ops);
        CREATE INDEX IF NOT EXISTS idx_audit_trail_username_trgm
            ON tb_audit_trail USING gin (lower(username) gin_trgm_ops);
        CREATE INDEX IF NOT EXISTS idx_audit_trail_action_trgm
            ON tb_audit_trail USING gin (lower(action) gin_trgm_ops);
    END IF;
END
$$;

-- The history of one record, newest first (findByRecordId, findLatestByRecordId)
CREATE INDEX IF NOT EXISTS idx_audit_trail_record_id_timestamp
    ON tb_audit_trail (record_id, timestamp DESC);

-- One table's rows, and one record inside it (findByTableName, findByTableNameAndRecordId)
CREATE INDEX IF NOT EXISTS idx_audit_trail_table_name_record_id
    ON tb_audit_trail (table_name, record_id);

-- Redundant: each is the leading column of one of the composites above
DROP INDEX IF EXISTS idx_audit_trail_record_id;
DROP INDEX IF EXISTS idx_audit_trail_table_name;

ANALYZE tb_audit_trail;
