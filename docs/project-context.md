# Project Context - Pasa Auto

> Automotive workshop management application ("bengkel")

**Version**: `0.0.28` | **Group/Artifact**: `com.github.b3kt:pasa-auto`

## Overview

Full-stack application for managing auto repair shop operations including service work orders (SPK), sales, purchases, inventory, customer management, employee attendance, and more.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| **Backend** | Java 25, Quarkus 3.34.0 |
| **Frontend** | Vue 3.5, Quasar 2.18, Vite, Pinia 3.0 |
| **Desktop** | Electron 28 |
| **Database** | PostgreSQL 15 |
| **Build** | Maven (backend), npm (frontend) |

### Backend Dependencies
- `quarkus-resteasy-jackson` - REST + JSON
- `quarkus-smallrye-jwt` - JWT auth
- `quarkus-jdbc-postgresql` - PostgreSQL driver
- `quarkus-hibernate-orm-panache` - ORM
- `quarkus-flyway` - DB migrations (V1-V13)
- `quarkus-quinoa` - Frontend integration
- `quarkus-smallrye-openapi` - Swagger/OpenAPI
- `lombok` - Boilerplate reduction

### Frontend Dependencies
- `vue` 3.5, `quasar` 2.18, `pinia` 3.0
- `axios` - HTTP client
- `vue-i18n` - i18n (en-US, id-ID)
- `chart.js` + `vue-chartjs` - Charts
- `jwt-decode` - Token decoding

---

## Architecture

Clean Architecture with 4 layers:

```
src/main/java/com/github/b3kt/
├── domain/           # Core business models, domain exceptions
├── application/      # Services, DTOs, mappers, helpers
├── infrastructure/   # JPA repos, security (JWT), entities, interceptors
└── presentation/     # REST resources, exception handlers, config
```

- **ORM**: Hibernate + Panache
- **Migrations**: Flyway (V1-V13 in `src/main/resources/db/migration/`)
- **Security**: JWT with RSA key pair (PKCS#8/PEM)
- **API**: JAX-RS REST, OpenAPI/Swagger via SmallRye

---

## Directory Structure

```
pasa-auto/
├── .github/workflows/       # CI/CD (native build, test coverage, secret scanning)
├── docs/                    # Architecture, API, deployment, ADRs
│   └── decisions/           # Architecture Decision Records
├── electron/                # Electron desktop app wrapper
├── src/
│   ├── main/
│   │   ├── java/            # Java backend (Clean Architecture)
│   │   ├── resources/       # application.properties, Flyway migrations, JWT keys
│   │   ├── webui/           # Quasar/Vue frontend
│   │   │   └── src/
│   │   │       ├── pages/   # 23+ Vue pages (pazaauto/, admin/, master/)
│   │   │       ├── components/  # GenericTable, GenericDialog, SPK* components
│   │   │       ├── services/    # API service layer
│   │   │       ├── stores/      # Pinia state management (auth-store)
│   │   │       ├── composables/ # useCrud, useKeyboardShortcuts, useOfflineStorage
│   │   │       ├── layouts/     # MainLayout, PublicLayout
│   │   │       ├── boot/        # axios, auth, stores, i18n, offline
│   │   │       ├── router/      # routes.js, index.js
│   │   │       └── i18n/        # en-US, id-ID locales
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

## Key Entities (Database Tables)

| Table | Description |
|-------|-------------|
| `users` | System users for authentication |
| `roles`, `permissions` | RBAC |
| `tb_pelanggan` | Customers |
| `tb_kendaraan` | Vehicles |
| `tb_spk` | Service orders (Surat Perintah Kerja) |
| `tb_spk_detail` | Service order details |
| `tb_karyawan` | Employees |
| `tb_karyawan_posisi` | Employee positions |
| `tb_jasa` | Services catalog |
| `tb_barang` | Parts/goods |
| `tb_sparepart` | Spare parts |
| `tb_supplier` | Suppliers |
| `tb_penjualan` | Sales |
| `tb_penjualan_detail` | Sales details |
| `tb_pembelian` | Purchases |
| `tb_pembelian_detail` | Purchase details |
| `tb_absensi` | Attendance |
| `tb_absensi_config` | Attendance config |
| `tb_system_parameter` | System settings |
| `audit_trail` | Audit log |

---

## API

- **Base path**: `/api/pazaauto/` for business modules, `/api/` for auth/users/RBAC
- **OpenAPI**: `/openapi`
- **Swagger UI**: `/swagger-ui` (disabled by default, enable via `SWAGGER_ENABLED=true`)
- **Auth**: JWT Bearer tokens, refresh token support
- **CORS**: Enabled, configurable origins

### Main Endpoints

| Module | Path |
|--------|------|
| Auth | `/api/auth/*` (login, logout, refresh) |
| Users | `/api/users/*` |
| Roles | `/api/roles/*` |
| Permissions | `/api/permissions/*` |
| RBAC | `/api/rbac/*` |
| Customers | `/api/pazaauto/pelanggan/*` |
| Vehicles | `/api/pazaauto/kendaraan/*` |
| Employees | `/api/pazaauto/karyawan/*`, `/api/pazaauto/karyawan-posisi/*` |
| Services | `/api/pazaauto/jasa/*` |
| Parts | `/api/pazaauto/barang/*`, `/api/pazaauto/sparepart/*` |
| Suppliers | `/api/pazaauto/supplier/*` |
| Service Orders | `/api/pazaauto/spk/*`, `/api/pazaauto/spk-detail/*` |
| Sales | `/api/pazaauto/penjualan/*`, `/api/pazaauto/penjualan-detail/*` |
| Purchases | `/api/pazaauto/pembelian/*`, `/api/pazaauto/pembelian-detail/*` |
| Reports | `/api/pazaauto/rekap-penjualan/*` |
| Attendance | `/api/pazaauto/absensi/*` |
| System Params | `/api/system-parameter/*` |
| Audit | `/api/audit-trail/*` |
| Health | `/health` |

See [api.md](api.md) for full documentation.

---

## Authentication Flow

1. **Login**: `POST /api/auth/login` with username/password
2. **Response**: JWT token + refresh token (expires in 2400h / ~100 days)
3. **Subsequent requests**: `Authorization: Bearer <token>`
4. **Refresh**: `POST /api/auth/refresh` with refresh token

JWT keys configured via environment variables (`JWT_PRIVATE_KEY`, `JWT_PUBLIC_KEY`) with classpath PEM fallback.

---

## Key Services

- `AuthService` - Authentication logic
- `UserService`, `RoleService`, `PermissionService`, `RbacService` - RBAC
- `TbPelangganService`, `TbKendaraanService`, `TbKaryawanService` - CRUD
- `TbSpkService` - Service order management
- `TbPenjualanService`, `TbPembelianService` - Sales/purchase
- `TbAbsensiService` - Attendance tracking

All business entities follow pattern: `AbstractCrudService<T>` base class.

---

## Frontend Patterns

- **Layouts**: `MainLayout` (authenticated), `PublicLayout` (login)
- **State**: Pinia `auth-store.js` for JWT/user management
- **HTTP**: Axios with interceptors in `boot/axios.js`
- **CRUD**: `useCrud()` composable for generic operations
- **Offline**: Service worker + local storage for offline support
- **i18n**: `boot/i18n.js` loads en-US, id-ID locales

---

## Configuration

### Environment Variables (.env)

```
DB_URL=jdbc:postgresql://localhost:5432/pasa_auto
DB_USERNAME=
DB_PASSWORD=
APP_SECURITY_SALT=
JWT_PRIVATE_KEY=
JWT_PUBLIC_KEY=
JWT_ISSUER=
SWAGGER_ENABLED=false
RBAC_ENABLED=false
```

### application.properties

- Quarkus config (datasource, Hibernate, Flyway)
- JWT settings (issuer, expiration)
- CORS allowed origins
- OpenAPI/Swagger endpoints
- OpenTelemetry tracing and logging

---

## Testing

| Framework | Purpose |
|-----------|---------|
| JUnit 5 | Unit testing |
| Mockito | Mocking |
| REST Assured | REST API testing |
| Testcontainers | Integration tests (PostgreSQL 15) |
| JaCoCo | Code coverage (80% minimum) |

**Run tests**: `./mvnw test`  
**Run with coverage**: `./mvnw clean verify jacoco:report jacoco:check`

---

## Linting & Formatting

| Tool | Scope | Commands |
|------|-------|----------|
| ESLint v9 (flat config) | Vue/JS | `cd src/main/webui && npm run lint` |
| Prettier v3 | Frontend | `cd src/main/webui && npm run format` |

No Java linting configured.

---

## CI/CD (GitHub Actions)

| Workflow | Trigger | Purpose |
|----------|---------|---------|
| `quarkus-native-build.yml` | Push to `release` or manual | Build native images (Linux + Windows), Electron, GitHub release |
| `test-coverage.yml` | PRs to `feature/*` or manual | Run tests with PostgreSQL, upload coverage to Codecov |
| `secret-scanning.yml` | Push/PR to main/release or manual | Detect secrets/credentials with Gitleaks |

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

## Code Conventions

- **Architecture**: Clean Architecture (domain → application → infrastructure → presentation)
- **Naming**: PascalCase (Java), camelCase (JS/Vue)
- **Layers**: Domain has no dependencies; Application depends only on Domain
- **DTOs**: Separate Request/Response DTOs in `application/dto/`
- **Tests**: JaCoCo 80% minimum coverage

---

## Deployment Modes

1. **Web app** - Quarkus serves Quasar SPA via Quinoa
2. **Electron desktop** - Wraps the Quarkus native binary with Electron

---

## Documentation Files

| File | Content |
|------|---------|
| [README.md](../README.md) | Quick start, tech stack, project structure |
| [overview.md](overview.md) | Business features, domain model, roadmap |
| [architecture.md](architecture.md) | Clean architecture details |
| [api.md](api.md) | REST API reference |
| [application-flow.md](application-flow.md) | Menu journeys, sequence diagrams, auth flow |
| [project-context.md](project-context.md) | Comprehensive project context (this file) |
| [jwt_setup.md](jwt_setup.md) | JWT configuration |
| [swagger.md](swagger.md) | Swagger usage |
| [development.md](development.md) | Dev guidelines |
| [deployment.md](deployment.md) | Docker & production |
| [decisions/](decisions/) | Architecture Decision Records (ADRs) |

---

## Common Tasks (Quick Ref)

| Task | How |
|------|-----|
| Add new entity | Create Entity → Repository → Service → Resource + frontend page |
| Add migration | Add SQL to `src/main/resources/db/migration/V*.sql` |
| Add role permission | Update RBAC service + frontend RolePage |
| Run tests | `./mvnw test` |
| View API docs | Swagger at `/swagger-ui` |
