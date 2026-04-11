# CHANGELOG

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Security
- **Fixed**: Password verification in `AuthServiceImpl` now uses `PasswordEncoder.matches()` with backward compatibility for plain-text passwords
- **Enhanced**: `PasswordEncoderImpl` supports both bcrypt-hashed and plain-text passwords for seamless migration
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
