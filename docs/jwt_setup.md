# JWT Configuration Guide

This project uses JWT (JSON Web Tokens) for authentication. The JWT signing requires a private key.

## Generated Keys

The following keys have been generated for JWT signing and verification:

- **privateKey-pkcs8.pem** - Private key for signing JWT tokens (PKCS#8 format)
- **publicKey.pem** - Public key for verifying JWT tokens
- **privateKey.pem** - Original private key (can be deleted)

## Configuration

The JWT configuration is in `application.properties`:

```properties
# Signing key (for generating tokens)
smallrye.jwt.sign.key.location=privateKey-pkcs8.pem
# Verification key (for validating tokens)
mp.jwt.verify.publickey.location=publicKey.pem
mp.jwt.verify.issuer=https://quarkus-quasar.example.com
jwt.issuer=https://quarkus-quasar.example.com
jwt.expiration.hours=24
```

## Security Notes

⚠️ **IMPORTANT:**
- The private key files (`privateKey*.pem`) are in `.gitignore` and should **NEVER** be committed to version control
- The public key (`publicKey.pem`) can be shared publicly
- In production, use environment variables or a secrets manager for key storage
- Rotate keys periodically for security

## Regenerating Keys

If you need to regenerate the keys:

```bash
# Generate new private key
openssl genrsa -out src/main/resources/privateKey.pem 2048

# Extract public key
openssl rsa -in src/main/resources/privateKey.pem -pubout -out src/main/resources/publicKey.pem

# Convert to PKCS#8 format (required by Quarkus)
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in src/main/resources/privateKey.pem -out src/main/resources/privateKey-pkcs8.pem
```

## Troubleshooting

### Error: SRJWT05009

This error indicates that the JWT signing key is missing or incorrectly configured.

**Solution:**
1. Verify `privateKey-pkcs8.pem` exists in `src/main/resources/`
2. Check that the file is in PKCS#8 format
3. Verify the configuration property `smallrye.jwt.sign.key.location` points to the correct file
4. Ensure the file is readable by the application

### Key Format Issues

If you get key format errors, ensure the private key is in PKCS#8 format:
```bash
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in privateKey.pem -out privateKey-pkcs8.pem
```

## Production Deployment

For production:

1. **Use environment variables:**
   ```properties
   smallrye.jwt.sign.key.location=${JWT_PRIVATE_KEY_PATH}
   mp.jwt.verify.publickey.location=${JWT_PUBLIC_KEY_PATH}
   ```

2. **Store keys securely:**
   - Use a secrets manager (AWS Secrets Manager, HashiCorp Vault, etc.)
   - Never commit keys to version control
   - Use different keys for different environments

3. **Key rotation:**
   - Implement a key rotation strategy
   - Support multiple keys during rotation period

