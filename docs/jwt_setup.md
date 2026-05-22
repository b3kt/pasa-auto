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

# Token expiration (in hours)
jwt.expiration.hours=2400
```

### Environment Variables

Set in `.env`:

```bash
JWT_PRIVATE_KEY=/path/to/privateKey-pkcs8.pem
JWT_PUBLIC_KEY=/path/to/publicKey.pem
JWT_ISSUER=https://your-domain.com
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
