# Project Context - Pasa Auto

## Overview

**Pasa Auto** is a modern automotive workshop management system built with Quarkus (Java) and Vue.js (Quasar). It handles vehicle/customer management, service orders (SPK), employee/attendance tracking, inventory, billing, and RBAC.

---

## Technology Stack

### Backend
- **Framework**: Quarkus 3.29.3
- **Language**: Java 25 (LTS)
- **Database**: PostgreSQL with Hibernate ORM + Panache
- **Security**: JWT authentication with RBAC
- **Migrations**: Flyway
- **Build**: Maven with JaCoCo (80% coverage target)

### Frontend
- **Framework**: Vue.js 3 (via Quasar)
- **Build Tool**: Vite
- **State**: Pinia
- **i18n**: vue-i18n (en-US, id-ID)
- **HTTP**: Axios

### Infrastructure
- **Container**: Docker
- **CI/CD**: GitHub Actions

---

## Project Structure

```
src/main/java/com/github/b3kt/
├── domain/           # Domain models & exceptions
├── application/      # DTOs, services, mappers, helpers
├── infrastructure/   # Repositories, security, persistence entities
└── presentation/    # REST controllers, configs, exception handlers

src/main/resources/
├── application.properties
├── db/migration/     # Flyway migrations (V1-V6)
└── *.pem             # JWT keys

src/main/webui/       # Vue.js frontend (Quasar)
├── src/
│   ├── pages/        # pazaauto/*, master/*, IndexPage, LoginPage
│   ├── components/   # GenericTable, GenericDialog, SPK* components
│   ├── layouts/      # MainLayout, PublicLayout
│   ├── stores/       # Pinia stores (auth-store)
│   ├── boot/         # axios, auth, stores, i18n, offline
│   ├── composables/  # useCrud, useKeyboardShortcuts, useOfflineStorage
│   └── router/       # routes.js, index.js
└── package.json
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

---

## API Base Endpoints

| Module | Path |
|--------|------|
| Auth | `/api/auth` (login, refresh, logout) |
| Users | `/api/users` |
| Roles | `/api/roles` |
| Permissions | `/api/permissions` |
| RBAC | `/api/rbac` |
| Customers | `/api/pelanggan` |
| Vehicles | `/api/kendaraan` |
| Employees | `/api/karyawan`, `/api/karyawan-posisi` |
| Services | `/api/jasa` |
| Parts | `/api/barang`, `/api/sparepart` |
| Suppliers | `/api/supplier` |
| Service Orders | `/api/spk`, `/api/spk-detail` |
| Sales | `/api/penjualan`, `/api/penjualan-detail` |
| Purchases | `/api/pembelian`, `/api/pembelian-detail` |
| Reports | `/api/rekap-penjualan`, `/api/rekap-pembelian` |
| Attendance | `/api/absensi`, `/api/absensi-config` |
| System Params | `/api/system-parameter` |
| Health | `/health` |

---

## Authentication Flow

1. **Login**: `POST /api/auth/login` with username/password
2. **Response**: JWT token + refresh token (expires in 24h)
3. **Subsequent requests**: `Authorization: Bearer <token>`
4. **Refresh**: `POST /api/auth/refresh` with refresh token

JWT keys stored in `src/main/resources/*.pem`

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
JWT_PRIVATE_KEY=src/main/resources/privateKey.pem
JWT_PUBLIC_KEY=src/main/resources/publicKey.pem
```

### application.properties
- Quarkus config (datasource, Hibernate, Flyway)
- JWT settings (issuer, expiration)
- CORS allowed origins
- OpenAPI/Swagger endpoints

---

## Running the Project

```bash
# Dev
./mvnw quarkus:dev

# Test
./mvnw test

# Package
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar
```

---

## Code Conventions

- **Architecture**: Clean Architecture (domain → application → infrastructure → presentation)
- **Naming**: PascalCase (Java), camelCase (JS/Vue)
- **Layers**: Domain has no dependencies; Application depends only on Domain
- **DTOs**: Separate Request/Response DTOs in `application/dto/`
- **Tests**: JaCoCo 80% minimum coverage

---

## Documentation Files

| File | Content |
|------|---------|
| `README.md` | Quick start, tech stack, project structure |
| `docs/overview.md` | Business features, domain model |
| `docs/architecture.md` | Clean architecture details |
| `docs/api.md` | REST API reference |
| `docs/jwt_setup.md` | JWT configuration |
| `docs/swagger.md` | Swagger usage |
| `docs/development.md` | Dev guidelines |
| `docs/deployment.md` | Docker & production |

---

## Common Tasks (Quick Ref)

| Task | How |
|------|-----|
| Add new entity | Create Entity → Repository → Service → Resource + frontend page |
| Add migration | Add SQL to `src/main/resources/db/migration/V*.sql` |
| Add role permission | Update RBAC service + frontend RolePage |
| Run tests | `./mvnw test` |
| View API docs | Swagger at `/swagger-ui` |