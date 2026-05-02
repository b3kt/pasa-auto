# Project Context: PasaAuto (PazaAuto)

> Automotive workshop management application ("bengkel")

## Overview

Full-stack application for managing auto repair shop operations including service work orders (SPK), sales, purchases, inventory, customer management, employee attendance, and more.

**Version**: `0.0.28` | **Group/Artifact**: `com.github.b3kt:pasa-auto`

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| **Backend** | Java 25, Quarkus 3.34.0 |
| **Frontend** | Vue 3, Quasar Framework 2.x, Vite |
| **Desktop** | Electron 28 |
| **Database** | PostgreSQL |
| **Build** | Maven (backend), npm (frontend) |

---

## Architecture

Clean Architecture with 4 layers:

```
src/main/java/com/github/b3kt/
├── domain/           # Core business models, domain exceptions
├── application/      # Services, DTOs, mappers
├── infrastructure/   # JPA repos, security (JWT), entities, interceptors
└── presentation/     # REST resources, exception handlers, config
```

- **ORM**: Hibernate + Panache
- **Migrations**: Flyway (V1-V13 in `src/main/resources/db/migration/`)
- **Security**: JWT with RSA key pair (PKCS8/PEM)
- **API**: JAX-RS REST, OpenAPI/Swagger via SmallRye

---

## Directory Structure

```
pasa-auto/
├── .github/workflows/       # CI/CD (native build, test coverage)
├── docs/                    # Architecture, API, deployment docs
├── electron/                # Electron desktop app
├── src/
│   ├── main/
│   │   ├── java/            # Java backend (Clean Architecture)
│   │   ├── resources/       # application.properties, Flyway migrations
│   │   ├── webui/           # Quasar/Vue frontend
│   │   │   └── src/
│   │   │       ├── pages/   # 23+ Vue pages (pazaauto/, admin/, master/)
│   │   │       ├── services/  # API service layer
│   │   │       ├── stores/    # Pinia state management
│   │   │       ├── composables/
│   │   │       ├── router/
│   │   │       └── i18n/
│   │   └── docker/
│   └── test/
│       ├── java/            # JUnit 5, Mockito, REST Assured, Testcontainers
│       └── resources/       # application-test.properties
├── pom.xml
├── .env.example
├── release.sh
└── update-to-next-version.sh
```

---

## Key Dependencies

### Backend (pom.xml)
- `quarkus-resteasy-jackson` - REST + JSON
- `quarkus-smallrye-jwt` - JWT auth
- `quarkus-jdbc-postgresql` - PostgreSQL driver
- `quarkus-hibernate-orm-panache` - ORM
- `quarkus-flyway` - DB migrations
- `quarkus-quinoa` - Frontend integration
- `quarkus-smallrye-openapi` - Swagger/OpenAPI
- `lombok` - Boilerplate reduction

### Frontend (src/main/webui/package.json)
- `vue` 3.5, `quasar` 2.18, `pinia` 3.0
- `axios` - HTTP client
- `vue-i18n` - i18n
- `chart.js` + `vue-chartjs` - Charts
- `jwt-decode` - Token decoding

---

## Domain Modules

- **SPK** - Service Work Orders (Surat Perintah Kerja)
- **Penjualan** - Sales
- **Pembelian** - Purchases
- **Pelanggan** - Customers
- **Kendaraan** - Vehicles
- **Karyawan** - Employees
- **Absensi** - Attendance
- **Barang/Sparepart** - Parts/Inventory
- **Jasa** - Services
- **Supplier** - Suppliers

---

## API

- **Base path**: `/api/`
- **OpenAPI**: `/openapi`
- **Swagger UI**: `/swagger-ui` (disabled by default, enable via `SWAGGER_ENABLED=true`)
- **Auth**: JWT Bearer tokens, refresh token support
- **CORS**: Enabled, configurable origins

### Main Endpoints
- `/api/auth/*` - Login, logout, refresh
- `/api/users/*` - User CRUD
- `/api/roles/*` - Role CRUD
- `/api/permissions/*` - Permission CRUD
- `/api/audit-trail/*` - Audit queries
- Plus business domain resources

---

## Testing

| Framework | Purpose |
|-----------|---------|
| JUnit 5 | Unit testing |
| Mockito | Mocking |
| REST Assured | REST API testing |
| Testcontainers | Integration tests (PostgreSQL 15) |
| JaCoCo | Code coverage (10% minimum) |

**Run tests**: `./mvnw test`  
**Run with coverage**: `./mvnw clean verify jacoco:report jacoco:check`

---

## Linting & Formatting

| Tool | Scope | Commands |
|------|-------|----------|
| ESLint v9 (flat config) | Vue/JS | `npm run lint` |
| Prettier v3 | Frontend | `npm run format` |

No Java linting configured.

---

## CI/CD (GitHub Actions)

### quarkus-native-build.yml
- **Trigger**: Push to `release` branch or manual
- Builds Quarkus native images (Linux + Windows, GraalVM Java 25)
- Builds Electron Windows installer
- Creates GitHub release with artifacts

### test-coverage.yml
- **Trigger**: PRs to `feature/*` or manual
- Runs tests with PostgreSQL 15 service
- Uploads coverage to Codecov

---

## Environment Variables

See `.env.example` for template. Key vars:
- Database connection settings
- JWT configuration (RSA keys)
- `SWAGGER_ENABLED` - Toggle Swagger UI
- CORS origins

---

## Development Commands

```bash
# Backend dev mode
./mvnw quarkus:dev

# Frontend dev (if working separately)
cd src/main/webui && npm run dev

# Build
./mvnw clean package

# Run tests
./mvnw test

# Lint frontend
cd src/main/webui && npm run lint

# Format frontend
cd src/main/webui && npm run format
```

---

## Deployment Modes

1. **Web app** - Quarkus serves Quasar SPA via Quinoa
2. **Electron desktop** - Wraps the Quarkus native binary
