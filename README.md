# Pasa Auto

[![Java](https://img.shields.io/badge/Java-25-007396?logo=openjdk)](https://www.oracle.com/java/)
[![Quarkus](https://img.shields.io/badge/Quarkus-3.34.0-4695EB?logo=quarkus)](https://quarkus.io/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-336791?logo=postgresql)](https://www.postgresql.org/)
[![Quasar](https://img.shields.io/badge/Quasar-Vue.js-0576bd?logo=quasar)](https://quasar.dev/)
[![License](https://img.shields.io/badge/License-proprietary-blue.svg)]()

Automotive workshop management system with Quarkus backend and Quasar/Vue.js frontend.

## Features

- **Service Orders (SPK)**: Digital work order creation and tracking
- **Inventory Management**: Parts and spareparts stock monitoring
- **Sales & Purchases**: Penjualan and pembelian tracking
- **Customer Management**: Pelanggan database and vehicle records
- **Employee Management**: Karyawan profiles, positions, and attendance
- **Supplier Management**: Supplier records and purchase orders
- **RBAC**: Role-based access control with permissions
- **JWT Auth**: Stateless authentication with token refresh
- **OpenAPI Docs**: Auto-generated API documentation

## Quick Start

### Prerequisites

- Java 25+
- Maven 3.9+
- Node.js 20+
- PostgreSQL 15+

### Setup

1. Clone and configure:
   ```bash
   git clone <repo-url>
   cd pasa-auto
   cp .env.example .env
   # Edit .env with your values
   ```

2. Run the database:
   ```bash
   docker run -d --name postgres -e POSTGRES_PASSWORD=dev \
     -e POSTGRES_USER=dev -e POSTGRES_DB=pasa_auto \
     -p 5432:5432 postgres:15
   ```

3. Start the app:
   ```bash
   ./mvnw quarkus:dev
   ```

4. Open:
   - API: http://localhost:8080
   - Swagger: http://localhost:8080/swagger-ui
   - Frontend: http://localhost:5173 (dev mode)

## Configuration

All sensitive config goes through environment variables. See `.env.example` for required variables:

| Variable | Description | Required |
|----------|-------------|----------|
| `DB_URL` | PostgreSQL JDBC URL | Yes |
| `DB_USERNAME` | Database user | Yes |
| `DB_PASSWORD` | Database password | Yes |
| `APP_SECURITY_SALT` | Password hashing salt | Yes |
| `JWT_PRIVATE_KEY` | Path to JWT private key | Yes |
| `JWT_PUBLIC_KEY` | Path to JWT public key | Yes |

## Project Structure

```
src/main/java/com/github/b3kt/
├── domain/              # Business logic and models
├── application/         # Services and DTOs
├── infrastructure/      # Repositories and security
└── presentation/        # REST resources and exception handlers

src/main/webui/          # Quasar/Vue.js frontend
```

See [docs/architecture.md](docs/architecture.md) for detailed architecture.

## Testing

```bash
# Unit tests
./mvnw test

# Integration tests
./mvnw verify

# Coverage report
./mvnw jacoco:report
```

## Building

```bash
# JVM mode
./mvnw clean package

# Native image
./mvnw clean package -Pnative -Dquarkus.native.enabled=true

# Docker
docker build -f src/main/docker/Dockerfile.jvm -t pasa-auto .
```

## Documentation

- [Overview](docs/overview.md) - Business context and roadmap
- [Architecture](docs/architecture.md) - Clean architecture details
- [API Docs](docs/api.md) - Endpoint documentation
- [Development](docs/development.md) - Local setup guide
- [Deployment](docs/deployment.md) - Production deployment
- [JWT Setup](docs/jwt_setup.md) - Authentication configuration
- [Swagger](docs/swagger.md) - OpenAPI/Swagger usage

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for release history.
