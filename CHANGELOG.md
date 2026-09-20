# CHANGELOG

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Security
- **Removed**: `CorsFilter`, which sent `Access-Control-Allow-Origin: *` with credentials on every response and overrode the `quarkus.http.cors.*` origin allowlist
- **Fixed**: Attendance endpoints let employees edit/delete any record and clock in or read attendance for other employees; employees are now limited to their own records (karyawanId from the JWT)
- **Fixed**: Clock-in/out IP allowlist trusted the client-supplied `X-Forwarded-For`/`X-Real-IP` headers (and fell back to `127.0.0.1`); it now uses the connection address, with opt-in proxy support (`PROXY_ADDRESS_FORWARDING`, `TRUSTED_PROXIES`)
- **Fixed**: Request/response body logging wrote login passwords and access/refresh tokens to the logs; `/api/auth/*` bodies are no longer logged and password/token/secret fields are masked elsewhere
- **Changed**: App log level is now `INFO` by default (`APP_LOG_LEVEL`); the previous `quarkus.log.category."com.github.b3kt"=DEBUG` line was missing its `.level` suffix
- **Changed**: With IP restriction enabled, an empty `allowed.ips` list now rejects all clock-ins instead of allowing all
- **Changed**: Access tokens now expire after 30 minutes (`jwt.expiration.minutes`, was 2400 hours)
- **Added**: Server-side refresh token store (`refresh_tokens`, migration V17) with rotation on use, reuse detection and revocation on logout / for deactivated users
- **Changed**: The refresh token is now issued as an HttpOnly, SameSite=Strict cookie scoped to `/api/auth` instead of being returned in the response body and stored in `localStorage`, so page scripts (and injected ones) cannot read it. The body token is still accepted on refresh for sessions issued before this change
- **Fixed**: The `sortBy` query parameter went into ORDER BY unchecked; only plain field references are accepted now
- **Added**: Security headers on every response (CSP, X-Content-Type-Options, X-Frame-Options, Referrer-Policy, Permissions-Policy, HSTS)
- **Changed**: Flyway now validates applied migrations by default (`FLYWAY_VALIDATE_ON_MIGRATE`) and `flyway clean` is disabled
- **Fixed**: Error responses leaked internals (Hibernate/SQL text, entity names and ids, parse failures); unexpected failures now return a generic message with a reference id that is logged with the stack trace, missing records return "Data not found", and optimistic-lock conflicts return a retry message
- **Fixed**: Swagger UI and the OpenAPI schema were served by packaged builds by default (`/swagger-ui`, `/openapi`); both are now excluded from packaged builds and remain available in dev mode (`OPENAPI_ENABLED`, `SWAGGER_ENABLED`, `SWAGGER_ALWAYS_INCLUDE` are build-time settings)
- **Fixed**: JWT signing/verification keys and TLS certificates placed in `src/main/resources` were packaged into the jar and native image; `*.pem`, `*.p12`, `*.jks` and `*.key` are now excluded from packaging and `JWT_PRIVATE_KEY`/`JWT_PUBLIC_KEY` are required (no classpath fallback), validated at startup
- **Added**: Brute-force protection for login and change-password: 5 failures per account or 20 per client IP lock further attempts (HTTP 429 with `Retry-After`) until 15 minutes after the last failure (`LOGIN_MAX_FAILURES_PER_USER`, `LOGIN_MAX_FAILURES_PER_IP`, `LOGIN_LOCKOUT_MINUTES`)
- **Fixed**: Login revealed whether a username exists (a different message for inactive accounts before the password check, and faster rejection of unknown usernames)
- **Removed**: Plaintext password matching; only bcrypt hashes are accepted. Migration V18 (requires the `pgcrypto` extension) bcrypt-hashes remaining plaintext passwords and flags those users to change their password
- **Fixed**: New employee logins were created with the plaintext password `password`; they now get a random one-time password shown once to the Admin/Owner and must change it at first login
- **Fixed**: `/api/users` returned password hashes and stored the Owner-entered password in plaintext; hashes are never serialized, passwords are write-only and hashed, and an Owner-set password is temporary (the user's sessions end)
- **Added**: `POST /api/auth/change-password` and a Change Password page; users with a temporary password can do nothing else until they change it (enforced server-side via the `pwd_change` token claim)
- **Fixed**: `SecurityProperties` interface properly configured as SmallRye ConfigMapping (removed stub method)

### Code Quality
- **Improved**: Standardized dependency injection in `TbSpkService` (removed inconsistent `@Inject` alongside `@RequiredArgsConstructor`)
- **Improved**: Added `@Min`/`@Max` validation annotations to `PageRequest` DTO for pagination limits
- **Added**: `@NotBlank` validation to `RefreshTokenRequest` DTO
- **Fixed**: Made `DateTimeFormatter` static and final in `AbstractCrudResource` (was incorrectly as instance field)
- **Renamed**: Changed `spkNoformatter` to `SPK_DATE_FORMATTER` with proper static constant naming

### Refactoring
- **Added**: New `QueryFilterBuilder` helper class to reduce duplicate query filter logic
- **Refactored**: `TbSpkService.findPaginated()` now uses `QueryFilterBuilder` (reduced from ~90 to ~30 lines)
- **Refactored**: `TbSpkService.findPaginatedWithPenjualan()` now uses `QueryFilterBuilder` (reduced from ~110 to ~50 lines)

### Testing
- **Added**: `TbSpkServiceTest` with comprehensive unit tests covering:
  - Find by ID with details population
  - Find unprocessed SPK
  - Find by noSpk number
  - Get next SPK number
  - Paginated queries with search, status, and date filters

### Documentation
- **Added**: Comprehensive project-specific README.md replacing generic Quarkus template
- **Added**: Project overview document in `/docs/overview.md` with architecture and business context
- **Added**: Complete API documentation in `/docs/api.md` with endpoints, models, and examples
- **Added**: Detailed deployment guide in `/docs/deployment.md` covering Docker, Kubernetes, and traditional deployments
- **Added**: Development setup guide in `/docs/development.md` with local development instructions
- **Added**: Project-level CHANGELOG.md for tracking all changes

### Changed
- **Fixed**: Replaced hardcoded security salt `1234567890` with environment variable `APP_SECURITY_SALT`
- **Fixed**: Removed hardcoded SSL certificate paths `/etc/letsencrypt/live/pasa-auto.web.id/` 
- **Fixed**: Updated JWT configuration to require explicit environment variables instead of classpath defaults
- **Added**: Created `.env.example` template with safe placeholders for all sensitive configuration
- **Changed**: Default SSL ports changed from 80/443 to 8080/8443 for development safety
- Improved security posture by eliminating hardcoded secrets in configuration files
- Enhanced environment variable handling for better deployment flexibility
- Established comprehensive documentation structure following ACTION_RULES.md requirements
- Implemented automated testing infrastructure with coverage enforcement

### Notes
- Database migration files contain legitimate `admin` user references (created_by/updated_by fields)
- No hardcoded passwords, API keys, or tokens found in source code
- JWT private key reference updated to require explicit environment variable
- Plain-text password compatibility maintained for seamless migration to bcrypt hashing

---

## [1.0.0-SNAPSHOT] - Initial State

### Features
- Quarkus-based application with Quinoa frontend integration
- JWT authentication system
- PostgreSQL database with Hibernate ORM
- Role-based access control (RBAC) framework
- REST API with OpenAPI documentation
- SSL/TLS support
