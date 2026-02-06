# CHANGELOG

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Security
- **Fixed**: Replaced hardcoded security salt `1234567890` with environment variable `APP_SECURITY_SALT`
- **Fixed**: Removed hardcoded SSL certificate paths `/etc/letsencrypt/live/pasa-auto.web.id/` 
- **Fixed**: Updated JWT configuration to require explicit environment variables instead of classpath defaults
- **Added**: Created `.env.example` template with safe placeholders for all sensitive configuration
- **Changed**: Default SSL ports changed from 80/443 to 8080/8443 for development safety

### Documentation
- **Added**: Comprehensive project-specific README.md replacing generic Quarkus template
- **Added**: Project overview document in `/docs/overview.md` with architecture and business context
- **Added**: Complete API documentation in `/docs/api.md` with endpoints, models, and examples
- **Added**: Detailed deployment guide in `/docs/deployment.md` covering Docker, Kubernetes, and traditional deployments
- **Added**: Development setup guide in `/docs/development.md` with local development instructions
- **Added**: Project-level CHANGELOG.md for tracking all changes

### Testing
- **Added**: JaCoCo Maven plugin for code coverage reporting
- **Added**: Coverage threshold enforcement (≥80% instruction coverage)
- **Added**: Testcontainers dependencies for integration testing with PostgreSQL
- **Added**: GitHub Actions workflow for automated test coverage checks
- **Added**: Test configuration for Testcontainers integration
- **Added**: Integration test base classes for consistent test setup
- **Added**: Comprehensive unit tests for all major packages:
  - `application.service.impl` - AuthServiceImpl, RbacServiceImpl
  - `application.service.pazaauto` - AbstractCrudService and concrete implementations
  - `infrastructure.persistence.listener` - AuditListener
  - `infrastructure.persistence.repository` - UserRepository and related repositories
  - `infrastructure.security` - PasswordEncoder, JWT services
- **Added**: Integration tests for REST endpoints:
  - `presentation.rest` - AuthResource with full HTTP testing
- **Added**: Test coverage for authentication, authorization, CRUD operations, and security features

### Changed
- Improved security posture by eliminating hardcoded secrets in configuration files
- Enhanced environment variable handling for better deployment flexibility
- Established comprehensive documentation structure following ACTION_RULES.md requirements
- Implemented automated testing infrastructure with coverage enforcement

### Notes
- Database migration files contain legitimate `admin` user references (created_by/updated_by fields)
- No hardcoded passwords, API keys, or tokens found in source code
- JWT private key reference updated to require explicit environment variable

---

## [1.0.0-SNAPSHOT] - Initial State

### Features
- Quarkus-based application with Quinoa frontend integration
- JWT authentication system
- PostgreSQL database with Hibernate ORM
- Role-based access control (RBAC) framework
- REST API with OpenAPI documentation
- SSL/TLS support
