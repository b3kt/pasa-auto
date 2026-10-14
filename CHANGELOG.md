# CHANGELOG

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Security
- **Fixed**: Upgraded Quarkus 3.36.3 -> 3.40.1, which clears all 29 Trivy findings on `pom.xml`, including the critical Netty `netty-handler` advisory. The platform BOM now manages Netty 4.1.138.Final, Jackson 2.21.7, the PostgreSQL driver 42.7.13 and OpenTelemetry 1.62.0. Quinoa moved 2.9.0 -> 2.9.2
- **Fixed**: Upgraded Quasar 2.18.6 -> 2.35.0 and `@quasar/app-vite` 2.1.0 -> 2.6.2, clearing the critical and high Quasar advisories. axios 1.15.2 -> 1.20.0, Vue 3.5.22 -> 3.5.43 (server-renderer advisory), and transitive postcss, nanoid and source-map-js now resolve to fixed releases. Trivy reports zero findings and `npm audit --omit=dev` reports zero runtime vulnerabilities. The advisories `npm audit` still lists are all in the build toolchain (vite, rollup, the dev-server express stack, eslint tooling); clearing the `@quasar/app-vite` one needs the 3.x major, which requires Node >= 22.22 and `vue-router` 5, and `@intlify/unplugin-vue-i18n` has no stable fixed release yet
- **Removed**: `CorsFilter`, which sent `Access-Control-Allow-Origin: *` with credentials on every response and overrode the `quarkus.http.cors.*` origin allowlist
- **Fixed**: Attendance endpoints let employees edit/delete any record and clock in or read attendance for other employees; employees are now limited to their own records (karyawanId from the JWT)
- **Fixed**: Clock-in/out IP allowlist trusted the client-supplied `X-Forwarded-For`/`X-Real-IP` headers (and fell back to `127.0.0.1`); it now uses the connection address, with opt-in proxy support (`PROXY_ADDRESS_FORWARDING`, `TRUSTED_PROXIES`)
- **Fixed**: Request/response body logging wrote login passwords and access/refresh tokens to the logs; `/api/auth/*` bodies are no longer logged and password/token/secret fields are masked elsewhere
- **Changed**: App log level is now `INFO` by default (`APP_LOG_LEVEL`); the previous `quarkus.log.category."com.github.b3kt"=DEBUG` line was missing its `.level` suffix
- **Changed**: With IP restriction enabled, an empty `allowed.ips` list now rejects all clock-ins instead of allowing all
- **Changed**: Access tokens now expire after 30 minutes (`jwt.expiration.minutes`, was 2400 hours)
- **Added**: Server-side refresh token store (`refresh_tokens`, migration V17) with rotation on use, reuse detection and revocation on logout / for deactivated users
- **Changed**: Container images now run on Java 25 (`ubi9/openjdk-25:1.24`); they were built on Java 21 while the project targets 25
- **Added**: Dependency scanning — Dependabot for Maven, npm (webui and electron) and GitHub Actions, plus a weekly `Dependency Audit` workflow running `npm audit` and a Trivy scan
- **Fixed**: Build-only packages (`@quasar/vite-plugin`, `sass`, `sass-loader`) were declared as runtime dependencies; moved to devDependencies
- **Changed**: The refresh token is now issued as an HttpOnly, SameSite=Strict cookie scoped to `/api/auth` instead of being returned in the response body and stored in `localStorage`, so page scripts (and injected ones) cannot read it. The body token is still accepted on refresh for sessions issued before this change
- **Changed**: The access token is now held in memory only and is no longer written to `localStorage`; a reload restores the session by exchanging the HttpOnly refresh cookie, and any legacy `auth_token` value is scrubbed on load
- **Changed**: The public `/health` endpoint no longer reports the application name/version to anonymous callers; the build identity is only returned to authenticated requests
- **Fixed**: The `sortBy` query parameter went into ORDER BY unchecked; only plain field references are accepted now, enforced both when binding the request DTO and again at the query sink (`SortFieldValidator`, plus a column allowlist for the rekap-penjualan report so an unknown field returns HTTP 400 instead of reaching the DB)
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
- **Fixed**: The employee list endpoint (`GET /api/pazaauto/karyawan`) returned full PII (address, phone, email, gender, joining date) to every staff member including `KARYAWAN`; it now returns only id + name, and the full record stays on the ADMIN/OWNER endpoints
- **Removed**: Dead `app.security.salt` config and its `SecurityProperties` interface, which were declared but never referenced (bcrypt salts internally); `APP_SECURITY_SALT` is no longer required to start

### Code Quality
- **Improved**: Standardized dependency injection in `TbSpkService` (removed inconsistent `@Inject` alongside `@RequiredArgsConstructor`)
- **Improved**: Added `@Min`/`@Max` validation annotations to `PageRequest` DTO for pagination limits
- **Added**: `@NotBlank` validation to `RefreshTokenRequest` DTO
- **Fixed**: Made `DateTimeFormatter` static and final in `AbstractCrudResource` (was incorrectly as instance field)
- **Renamed**: Changed `spkNoformatter` to `SPK_DATE_FORMATTER` with proper static constant naming

### Refactoring
- **Changed**: The customer portal is now a separate frontend project, `src/main/appui`, served at `/app/` instead of `/portal/*` routes of the staff app. The staff login page links to `/app/`; the old `/portal/*` routes are gone. The customer bundle is built by the staff app's `build:all` script and copied into its `dist/spa/app`, since Quinoa supports a single `ui-dir`
- **Renamed**: Portal pages dropped the redundant `Portal` prefix (`PortalLoginPage.vue` → `LoginPage.vue`, etc.) and the customer store is now `stores/auth-store.js`; the `customer_auth_user` localStorage key is unchanged so existing customer sessions survive
- **Changed**: The customer portal store (`src/main/appui`) also keeps its access token in memory only and restores the session from the HttpOnly `customer_refresh_token` cookie via a new `boot/auth.js`, matching the staff app; the legacy `customer_auth_token` value is scrubbed on load
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
- **Changed**: The "Convert to SPK" dialog on the staff booking queue now assigns mechanics with the same multi-select picker used on the SPK page (chips, one Utama/Pembantu task per mechanic) instead of a free-text mechanic name, and the resulting work order stores that `mekanik_list`
- **Added**: Customers must now enter the vehicle odometer reading (KM) when requesting a service booking. The value is stored on the booking, shown in the staff review queue, and used to pre-fill the intake KM when converting to a work order
- **Fixed**: Replaced hardcoded security salt `1234567890` with environment variable `APP_SECURITY_SALT`
- **Fixed**: Removed hardcoded SSL certificate paths `/etc/letsencrypt/live/pasa-auto.web.id/` 
- **Fixed**: Updated JWT configuration to require explicit environment variables instead of classpath defaults
- **Added**: Created `.env.example` template with safe placeholders for all sensitive configuration
- **Changed**: Default SSL ports changed from 80/443 to 8080/8443 for development safety
- Improved security posture by eliminating hardcoded secrets in configuration files
- Enhanced environment variable handling for better deployment flexibility
- Established comprehensive documentation structure following ACTION_RULES.md requirements
- Implemented automated testing infrastructure with coverage enforcement

### Fixed
- **Fixed**: Opening an SPK that was converted from a booking on the Penjualan (sales) page cleared the customer panel and flagged it as a new customer, because the nopol lookup only searched the deprecated `tb_pelanggan.nopol` and the booking's plate lives in `tb_pelanggan_kendaraan`. The page now resolves the customer, address and vehicle from the current-ownership vehicle list (same source as the SPK page), falling back to `by-nopol` before treating the plate as unknown
- **Fixed**: A booking converted to an SPK was created with status `MENUNGGU`, which the staff SPK list never queries, so the converted work order never appeared on the SPK page. Conversions now create an `OPEN` SPK (the documented `OPEN -> PROSES -> SELESAI` lifecycle), the customer portal labels `OPEN` as "Waiting"/"Menunggu", and migration `V28` normalizes existing `MENUNGGU` rows
- **Changed**: Converting a booking to an SPK now numbers the work order from the booking's requested service date (`requested_date_time`) instead of the conversion date, so it joins that day's SPK series

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
