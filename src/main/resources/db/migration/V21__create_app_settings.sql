-- Generic, app-wide key/value settings table, runtime-toggleable without a redeploy.
-- Same shape as tb_absensi_config (attendance-domain-owned); this one is intentionally
-- generic so other features besides the customer portal can reuse it later.

CREATE TABLE IF NOT EXISTS app_settings (
    id            BIGSERIAL PRIMARY KEY,
    config_key    VARCHAR(100) NOT NULL,
    config_value  VARCHAR(500),
    description   VARCHAR(500),
    config_type   VARCHAR(50), -- TIME, INTEGER, BOOLEAN, STRING, IP_LIST
    created_at    TIMESTAMP,
    created_by    VARCHAR(255),
    updated_at    TIMESTAMP,
    updated_by    VARCHAR(255),
    version       INTEGER DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_app_settings_config_key ON app_settings (config_key);

-- Master switch for the customer self-service portal. Defaults to disabled: the feature
-- ships dark and an Owner turns it on once the rollout is ready.
INSERT INTO app_settings (config_key, config_value, description, config_type, created_at)
SELECT 'customer.access.enabled', 'false', 'Master switch for the customer self-service portal', 'BOOLEAN', NOW()
WHERE NOT EXISTS (SELECT 1 FROM app_settings WHERE config_key = 'customer.access.enabled');
