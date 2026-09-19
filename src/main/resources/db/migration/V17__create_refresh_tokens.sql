-- Server-side refresh token store: enables rotation on use, reuse detection and revocation on logout
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id         VARCHAR(36) NOT NULL PRIMARY KEY,
    username   VARCHAR(50) NOT NULL,
    family_id  VARCHAR(36) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    rotated_at TIMESTAMP(6),
    revoked_at TIMESTAMP(6)
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_username ON refresh_tokens (username);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_family_id ON refresh_tokens (family_id);
