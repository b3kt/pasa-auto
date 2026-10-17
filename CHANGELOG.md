# CHANGELOG

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed
- **Added**: Configurable request/response payload logging: `LOG_PAYLOAD_ENABLED` (off by default) logs JSON bodies at INFO independent of the log level, `LOG_PAYLOAD_MAX_LENGTH` sets the truncation length (`0` = whole body) and `LOG_PAYLOAD_EXCLUDED_PATHS` adds path prefixes whose bodies are never logged. Response bodies are now logged as the JSON the client receives instead of the entity's `toString()`. `APP_LOG_LEVEL=DEBUG` no longer logs bodies, and with payload logging off, request bodies are no longer buffered

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
- **Changed**: Customer data normalization, expand step (ADR-0011, migration `V35`). `tb_pelanggan_kendaraan` is the only source of vehicle data. `tb_pelanggan.nopol/merk/jenis` are nullable, maintained only by the `trg_pk_sync_legacy_vehicle` trigger (primary vehicle = most recently acquired current one), and read-only in the application. Legacy plates without an ownership row are first preserved as ownership history. The `'-'` portal placeholder is replaced by a `sumber = PORTAL` record that owns no vehicle. New `merged_into_id` column and `PelangganMergeService` for traceable duplicate merges. `v_pelanggan_legacy` keeps the old column shape for reports. Stricter integrity: canonical-plate and current-row checks, `ON DELETE RESTRICT` on ownership, FKs from `tb_spk`/`tb_penjualan` to `tb_pelanggan` (all `NOT VALID` until the V36 contract step)
- **Changed**: Phone numbers on `tb_pelanggan` and `tb_customer_accounts` are stored in canonical national digits (`PhoneNormalizer`, e.g. `+62 812-3456` becomes `08123456`). Portal contact matching and the duplicate-phone check at registration now ignore formatting. `tlpn` is folded into `no_hp`/`no_telepon` and is now a deprecated alias of `no_telepon` (the `telepon` API field echoes `noTelepon`)
- **Changed**: `PelangganDto.nopol` and `merk` are no longer required, since a customer may own no vehicle. `PelangganDto.sumber` is exposed read-only. Staff customer search matches plates through current ownership
- **Changed**: Once a portal account is linked, the login response and the profile endpoint use the customer record's name instead of the registration name
- **Security**: Plate lookups in the customer portal (`GET /api/customer/vehicles/check-nopol`) are rate limited, since they reveal a vehicle's brand/type and a masked owner name. The limit is on distinct plates: 20 per account and 60 per client IP per 15-minute window (`NOPOL_LOOKUP_MAX_PLATES_PER_ACCOUNT`, `NOPOL_LOOKUP_MAX_PLATES_PER_IP`, `NOPOL_LOOKUP_WINDOW_MINUTES`). Re-checking a plate is free; going over returns 429 with `Retry-After`, and the portal shows how long to wait
- **Added**: Adding a vehicle in the customer portal now says when the plate is already in the workshop records ("{merk} {jenis}, registered to T*****3 - is this your car?", ADR-0013). A button beside the plate field prefills brand and type, which stay editable. Answering yes links that customer record to the account, unless its phone/email contradict the account's; then the link waits for staff approval. Answering no (or not answering) moves only the vehicle to the customer's own record. Approving a link request now also merges the customer's empty self-registered record into the approved one
- **Added**: Customers can register with an email, a mobile number, or both, and sign in with either (migration `V37`: `tb_customer_accounts.email` is nullable, at least one identifier is required). The login endpoint takes `identifier` (the old `email` key is still accepted), phone numbers match in any format, the phone must be a plausible national number, and login lockout counts all spellings of one identifier together. The portal login form has a single "Email or phone number" field
- **Changed**: A customer linked to a portal account is reached through that account (ADR-0012, migration `V36`). Their phone and email come from `tb_customer_accounts`, the portal profile saves the mobile number on the account (it stays unique) and shows the login email read-only, and staff see those fields read-only (`contactFromAccount`). Walk-in customers without an account keep `tb_pelanggan.no_hp`/`email`. The landline (`no_telepon`, `tlpn`) is deprecated for everyone: no longer written, returned or matched in contact search
- **Fixed**: Saving the customer portal profile returned, and kept showing for up to 10 minutes, the previous values (and the previous display name after the token refresh). `invalidatePelangganCache()` used a parameterless `@CacheInvalidate`, which never evicted the per-id `pelanggan-cache` entries; it now clears the whole cache. Profile updates and staff edits also evict the `pelanggan-by-nopol` entries for every plate of that customer (`JpaPelangganRepository.invalidatePelanggan`), so the staff SPK and sales lookups show the new name and contacts
- **Fixed**: Deleting a customer's last vehicle in the portal soft-deleted their customer record, leaving the linked account pointing at a deleted row
- **Fixed**: Closing an ownership period that began the same day produced an end date before the start date
- **Added**: The customer portal works offline. Besides the precached app shell, its service worker now caches the customer's own read-only API responses (`GET /api/customer/*`, network-first with a 5 s timeout, 7-day expiry) and the public portal switch, plus any other same-origin static resource. A signed-in customer whose device is offline, or whose backend cannot be reached, keeps an offline read-only session with a banner instead of being signed out or sent to "unavailable"; the session is renewed on reconnect or on the next navigation. Auth endpoints and the rate-limited plate lookups are never cached, and the cached data is deleted on every sign-in, registration and sign-out. Changes still need a connection
- **Fixed**: After a portal redeploy, an open tab could fail to load its next page ("Failed to fetch dynamically imported module"), because the new service worker activates at once and drops the old build's precache. The page now reloads once when a new worker takes over. The PWA manifest also pointed every icon size at the 41 px favicon; it now lists the real 128-512 px icons
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
- **Fixed**: Saving a customer on the staff Pelanggan page always failed with HTTP 400 ("This record was changed by someone else"). The update path rebuilds the entity from the `Pelanggan` domain model, which carries no row version, and `merge()` rejected the version-less entity as stale. `JpaPelangganRepository.save` now carries the stored version (and soft-delete marker) forward, like `AbstractCrudService.update` (last write wins). Covered by `TbPelangganServiceUpdateIntegrationTest`
- **Added**: Every non-2xx REST response that comes from an exception is logged with its stack trace the request it answered and the client IP (`PUT /api/pazaauto/pelanggan/12 from 10.0.0.7 -> 400 Bad Request`; behind a reverse proxy the real client needs `PROXY_ADDRESS_FORWARDING`/`TRUSTED_PROXIES`). This covers the global exception mappers and the 60 resource `catch` blocks that build error responses themselves. 4xx is logged at WARN and 5xx at ERROR, under the category `com.github.b3kt.rest.errors`; set `quarkus.log.category."com.github.b3kt.rest.errors".level=ERROR` to keep only server errors
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
