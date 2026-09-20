# JWT Setup Guide

## Overview

Pasa Auto uses JWT (JSON Web Tokens) with RSA asymmetric signing for authentication.

---

## Key Files

| File | Purpose | Secret? |
|------|---------|---------|
| `privateKey-pkcs8.pem` | Signs tokens (PKCS#8 format) | Yes |
| `publicKey.pem` | Verifies tokens | No |
| `privateKey.pem` | Original private key (can be deleted after conversion) | Yes |

Keys are stored in `src/main/resources/` for development.

---

## Configuration

### application.properties

```properties
# Signing key (for generating tokens)
smallrye.jwt.sign.key.location=${JWT_PRIVATE_KEY:classpath:privateKey-pkcs8.pem}

# Verification key (for validating tokens)
mp.jwt.verify.publickey.location=${JWT_PUBLIC_KEY:classpath:publicKey.pem}

# Token issuer
mp.jwt.verify.issuer=${JWT_ISSUER:https://quarkus-quasar.example.com}
jwt.issuer=${JWT_ISSUER:https://quarkus-quasar.example.com}

# Access token lifetime (minutes) - keep short; sessions are extended with refresh tokens
jwt.expiration.minutes=${JWT_EXPIRATION_MINUTES:30}
# Refresh token lifetime (days) - sliding: every refresh issues a new token
jwt.refresh.expiration.days=${JWT_REFRESH_EXPIRATION_DAYS:7}
# How long a just-rotated refresh token is still accepted (concurrent refresh from two tabs)
jwt.refresh.reuse-grace-seconds=${JWT_REFRESH_REUSE_GRACE_SECONDS:30}
```

### Refresh Token Rotation & Revocation

Refresh tokens are tracked server-side in the `refresh_tokens` table (keyed by the token's `jti`):

- **Rotation**: each `POST /api/auth/refresh` marks the presented token as used and returns a new pair.
- **Reuse detection**: presenting an already-rotated token after the grace period revokes the whole
  session (token family), since it indicates the token was copied.
- **Logout**: `POST /api/auth/logout` with `{"refreshToken": "..."}` revokes that session; without a body,
  all of the user's sessions are revoked.
- **Deactivated users**: a refresh by an inactive user revokes all of their sessions.

Access tokens are not tracked; after logout or a role change they stay valid until they expire
(at most `jwt.expiration.minutes`).

### Environment Variables

Both key locations are **required** and must point outside the source tree: keys are excluded from the
packaged jar and native image, so there is no bundled fallback. Without them the application refuses to
start (`Failed to load config value ... mp.jwt.verify.publickey.location`); if a key is set but unreadable,
startup fails with a message naming the variable.

Set in `.env`:

```bash
JWT_PRIVATE_KEY=file:/etc/pasa-auto/keys/privateKey-pkcs8.pem
JWT_PUBLIC_KEY=file:/etc/pasa-auto/keys/publicKey.pem
JWT_ISSUER=https://your-domain.com
```

If you still keep keys in `src/main/resources`, move them out (they are no longer packaged, so
`classpath:` locations will not resolve in a built application):

```bash
sudo mkdir -p /etc/pasa-auto/keys
sudo mv src/main/resources/*.pem /etc/pasa-auto/keys/
sudo chmod 600 /etc/pasa-auto/keys/*.pem
```

---

## Generating Keys

### Development

```bash
# Generate RSA key pair
openssl genpkey -algorithm RSA \
  -out src/main/resources/privateKey.pem \
  -pkeyopt rsa_keygen_bits:2048

# Extract public key
openssl rsa -pubout \
  -in src/main/resources/privateKey.pem \
  -out src/main/resources/publicKey.pem

# Convert to PKCS#8 (required by Quarkus/SmallRye JWT)
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt \
  -in src/main/resources/privateKey.pem \
  -out src/main/resources/privateKey-pkcs8.pem
```

### Production

```bash
# Generate stronger key
openssl genpkey -algorithm RSA \
  -out private.pem \
  -pkeyopt rsa_keygen_bits:4096

# Convert and store securely
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt \
  -in private.pem -out private-pkcs8.pem

# Store in secrets manager (AWS Secrets Manager, Vault, etc.)
```

---

## Security

### Rules

- **Never** commit private keys to version control
- Use different keys per environment (dev, staging, production)
- Rotate keys periodically
- Store production keys in a secrets manager

### Git Ignore

Private keys are excluded in `.gitignore`:

```
*.pem
!src/main/resources/publicKey.pem
```

---

## Authentication Flow

```
Client                          Server
  |                               |
  |-- POST /api/auth/login ------>|
  |   {username, password}        |
  |                               |
  |<-- {token, refreshToken} -----|
  |                               |
  |-- GET /api/users ------------>|
  |   Authorization: Bearer <tok> |
  |                               |
  |<-- [user data] ---------------|
```

1. Client sends credentials to `/api/auth/login`
2. Server validates and returns JWT + refresh token
3. Client includes `Authorization: Bearer <token>` in subsequent requests
4. Server verifies token using public key
5. On expiration, client refreshes via `POST /api/auth/refresh`

---

## Troubleshooting

### Error: SRJWT05009 (Missing Signing Key)

**Cause**: Private key file not found or wrong format.

**Fix**:
1. Verify file exists: `ls src/main/resources/privateKey-pkcs8.pem`
2. Verify PKCS#8 format: `head -1 src/main/resources/privateKey-pkcs8.pem`
   - Should show: `-----BEGIN PRIVATE KEY-----`
3. Check config: `grep smallrye.jwt.sign.key src/main/resources/application.properties`

### Error: Invalid Key Format

**Cause**: Key not in PKCS#8 format.

**Fix**:
```bash
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt \
  -in privateKey.pem -out privateKey-pkcs8.pem
```

### Token Verification Failed

**Cause**: Public key doesn't match private key, or issuer mismatch.

**Fix**:
1. Regenerate both keys from same source
2. Verify `JWT_ISSUER` matches the configured issuer
3. Check token payload: https://jwt.io

---

## Token Structure

```json
{
  "iss": "https://your-domain.com",
  "sub": "admin",
  "upn": "admin",
  "groups": ["ADMIN", "USER"],
  "iat": 1700000000,
  "exp": 1700100000
}
```

| Claim | Description |
|-------|-------------|
| `iss` | Issuer (must match `mp.jwt.verify.issuer`) |
| `sub` | Subject (username) |
| `upn` | User principal name |
| `groups` | User roles |
| `iat` | Issued at (timestamp) |
| `exp` | Expiration (timestamp) |
