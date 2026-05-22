# Project Overview

## What is Pasa Auto?

Pasa Auto is an automotive workshop management system ("bengkel") built with Quarkus (Java) backend and Quasar (Vue.js) frontend. It digitizes workshop operations from customer intake to billing.

**Version**: `0.0.28`

---

## Business Problem

Traditional automotive workshops struggle with:
- Manual tracking of vehicles and services
- Inefficient inventory management
- Poor customer experience due to disorganized processes
- Inaccurate billing and invoicing
- Lack of real-time business insights

Pasa Auto addresses these through a unified digital platform.

---

## Core Features

| Module | Description |
|--------|-------------|
| **SPK** | Digital service work orders (Surat Perintah Kerja) |
| **Penjualan** | Sales tracking and invoicing |
| **Pembelian** | Purchase orders and supplier management |
| **Pelanggan** | Customer database with vehicle records |
| **Kendaraan** | Vehicle information and service history |
| **Karyawan** | Employee profiles and position management |
| **Absensi** | Attendance tracking |
| **Barang/Sparepart** | Parts inventory and stock monitoring |
| **Jasa** | Service catalog with pricing |
| **RBAC** | Role-based access control |
| **Audit Trail** | Change tracking for all entities |

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| **Backend** | Java 25, Quarkus 3.34.0 |
| **Frontend** | Vue 3.5, Quasar 2.18, Vite |
| **Desktop** | Electron 28 |
| **Database** | PostgreSQL 15 |
| **ORM** | Hibernate + Panache |
| **Auth** | JWT (RSA asymmetric) |
| **Migrations** | Flyway (V1-V13) |
| **API Docs** | OpenAPI 3.0 / Swagger |
| **Monitoring** | OpenTelemetry |

---

## Architecture

Clean Architecture with 4 layers:

```
┌─────────────────────────────────────────────┐
│  Presentation (REST Resources, Handlers)    │
├─────────────────────────────────────────────┤
│  Application (Services, DTOs, Mappers)      │
├─────────────────────────────────────────────┤
│  Domain (Business Models, Rules)            │
├─────────────────────────────────────────────┤
│  Infrastructure (Repositories, Security)    │
└─────────────────────────────────────────────┘
```

Dependency flow: **Presentation → Application → Domain ← Infrastructure**

See [architecture.md](architecture.md) for details.

---

## Domain Model

### Core Entities

```
Customer (tb_pelanggan)
  └── Vehicle (tb_kendaraan)
        └── ServiceOrder (tb_spk)
              ├── SPKDetail (tb_spk_detail)
              ├── Sale (tb_penjualan)
              └── SalesRecap (tb_rekap_penjualan)

Employee (tb_karyawan)
  └── Position (tb_karyawan_posisi)
  └── Attendance (tb_absensi)

Part (tb_barang)
  └── SparePart (tb_sparepart)

Supplier (tb_supplier)
  └── Purchase (tb_pembelian)
        └── PurchaseDetail (tb_pembelian_detail)

User → Role → Permission (RBAC)
```

### Key Relationships

- Customers have multiple Vehicles
- Vehicles have multiple Service Orders (SPK)
- SPKs contain multiple Details (services + parts used)
- SPKs generate Sales records
- Employees are assigned to SPKs and have Attendance records
- Suppliers provide Parts via Purchases

---

## Security Model

### Authentication
- JWT with RSA asymmetric signing (PKCS#8)
- Token expiration: 2400 hours (~100 days)
- Refresh token support

### Authorization
- Role-Based Access Control (RBAC)
- Configurable via feature flag (`RBAC_ENABLED`)
- Granular permissions per role

### Data Protection
- All secrets via environment variables
- Password hashing with configurable salt
- SSL/TLS support for HTTPS

---

## API

- **Base path**: `/api/`
- **Swagger UI**: `/swagger-ui` (disabled by default)
- **OpenAPI JSON**: `/openapi`

See [api.md](api.md) for full endpoint documentation.

---

## Deployment

Two modes available:

1. **Web App** - Quarkus serves Quasar SPA via Quinoa
2. **Electron Desktop** - Quarkus native binary + Electron wrapper

See [deployment.md](deployment.md) for details.

---

## Roadmap

### Short Term
- [ ] Mobile app for technicians
- [ ] Advanced reporting dashboard
- [ ] Customer self-service portal

### Medium Term
- [ ] Predictive maintenance scheduling
- [ ] Multi-branch support
- [ ] Integration with accounting software

### Long Term
- [ ] IoT vehicle diagnostics integration
- [ ] ML-powered demand forecasting
- [ ] Parts marketplace integration
