-- Passwords used to be accepted in plaintext (seeded accounts, the "password" default for employee logins,
-- and passwords typed on the user page). Plaintext matching has been removed, so hash every remaining
-- plaintext value with bcrypt and make those users choose a new password at their next login, since the
-- old values are known or guessable (admin123, owner123, test123, password).
-- pgcrypto is a trusted extension on PostgreSQL 13+, so the database owner can create it.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE users ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE users
SET password_hash = crypt(password_hash, gen_salt('bf', 10)),
    must_change_password = TRUE
WHERE password_hash NOT LIKE '$2%'
  AND password_hash NOT IN ('', '!');

-- An empty password can't be hashed into anything usable; lock the account until the Owner sets one
UPDATE users
SET password_hash = '!',
    must_change_password = TRUE
WHERE password_hash = '';
