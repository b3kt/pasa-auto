-- V27 added deleted_at to every existing table because BaseEntity maps it for all entities, but
-- customer_vehicle_claim_requests was created afterwards (V31) without it, so Hibernate schema
-- validation rejected the SessionFactory. BaseEntity requires deleted_at on every table.
ALTER TABLE customer_vehicle_claim_requests ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
