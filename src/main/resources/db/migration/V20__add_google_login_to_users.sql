-- V20: Sign in with Google
--
-- Adds the three columns the Google flow needs to the users table. Existing rows are APPROVED, so
-- nothing about the current accounts changes: approval only ever applies to accounts the Google
-- callback creates on its own.
--
-- password_hash stays NOT NULL. Google-created rows carry '!', the sentinel V18 already uses for
-- an unusable password: PasswordEncoderImpl only matches strings starting with '$2', so no
-- password can ever authenticate such a row.

ALTER TABLE users ADD COLUMN IF NOT EXISTS google_sub VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS google_login_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS approval_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED';

COMMENT ON COLUMN users.google_sub IS 'Google account identifier (the id token "sub"), bound on the first successful Google sign-in';
COMMENT ON COLUMN users.google_login_enabled IS 'Whether an admin has allowed this account to be claimed by a matching verified Google email';
COMMENT ON COLUMN users.approval_status IS 'PENDING until an Owner approves a self-created Google account; APPROVED for every account created by an admin';

-- One user per Google account. Partial, so the many rows with no google_sub are unaffected.
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_google_sub ON users (google_sub) WHERE google_sub IS NOT NULL;

-- The Google flow looks users up by email, which until now was only ever read back, never searched
CREATE INDEX IF NOT EXISTS idx_users_email_lower ON users (lower(email));

-- Reject anything outside the three known states, whatever writes it
ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_users_approval_status;
ALTER TABLE users ADD CONSTRAINT chk_users_approval_status
    CHECK (approval_status IN ('PENDING', 'APPROVED', 'REJECTED'));
