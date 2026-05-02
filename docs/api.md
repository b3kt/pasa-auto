# API Documentation

## Overview

Pasa Auto provides a RESTful API for managing automotive workshop operations. The API follows OpenAPI 3.0 specifications and uses JSON for all request/response payloads.

## Base URL

| Environment | URL |
|-------------|-----|
| Development | `http://localhost:8080` |
| Production | `https://your-domain.com` |

## Authentication

All endpoints (except `/api/auth/login`) require JWT Bearer authentication.

### Header Format
```
Authorization: Bearer <jwt_token>
```

### Auth Endpoints

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/api/auth/login` | Login with username/password | No |
| POST | `/api/auth/refresh` | Refresh JWT token | No |
| POST | `/api/auth/logout` | Logout current session | Yes |
| GET | `/api/auth/me` | Get current user info | Yes |

### Login Request
```json
{
  "username": "admin",
  "password": "admin123"
}
```

### Login Response
```json
{
  "token": "eyJhbGci...",
  "refreshToken": "eyJhbGci...",
  "expiresIn": 8640000,
  "user": {
    "id": 1,
    "username": "admin",
    "email": "admin@example.com",
    "roles": ["ADMIN"]
  }
}
```

---

## API Endpoints

### User Management (`/api/users`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/users` | List all users |
| GET | `/api/users/{id}` | Get user by ID |
| POST | `/api/users` | Create user |
| PUT | `/api/users/{id}` | Update user |
| DELETE | `/api/users/{id}` | Delete user |

### Role Management (`/api/roles`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/roles` | List all roles |
| GET | `/api/roles/{id}` | Get role by ID |
| POST | `/api/roles` | Create role |
| PUT | `/api/roles/{id}` | Update role |
| DELETE | `/api/roles/{id}` | Delete role |

### Permission Management (`/api/permissions`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/permissions` | List all permissions |
| GET | `/api/permissions/{id}` | Get permission by ID |
| POST | `/api/permissions` | Create permission |
| PUT | `/api/permissions/{id}` | Update permission |
| DELETE | `/api/permissions/{id}` | Delete permission |

### RBAC (`/api/rbac`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/rbac/roles/{roleId}/permissions` | Get permissions for role |
| POST | `/api/rbac/roles/{roleId}/permissions` | Assign permissions to role |
| DELETE | `/api/rbac/roles/{roleId}/permissions/{permissionId}` | Remove permission from role |

### Customers (`/api/pazaauto/pelanggan`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/pazaauto/pelanggan` | List all customers |
| GET | `/api/pazaauto/pelanggan/{id}` | Get customer by ID |
| POST | `/api/pazaauto/pelanggan` | Create customer |
| PUT | `/api/pazaauto/pelanggan/{id}` | Update customer |
| DELETE | `/api/pazaauto/pelanggan/{id}` | Delete customer |
| GET | `/api/pazaauto/pelanggan/paginated` | Paginated list with search |

### Vehicles (`/api/pazaauto/kendaraan`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/pazaauto/kendaraan` | List all vehicles |
| GET | `/api/pazaauto/kendaraan/{id}` | Get vehicle by ID |
| POST | `/api/pazaauto/kendaraan` | Create vehicle |
| PUT | `/api/pazaauto/kendaraan/{id}` | Update vehicle |
| DELETE | `/api/pazaauto/kendaraan/{id}` | Delete vehicle |
| GET | `/api/pazaauto/kendaraan/merk/distinct` | Get distinct vehicle brands |
| GET | `/api/pazaauto/kendaraan/jenis/distinct` | Get distinct vehicle types |
| GET | `/api/pazaauto/kendaraan/jenis/by-merk` | Get types by brand |

### Employees (`/api/pazaauto/karyawan`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/pazaauto/karyawan` | List all employees |
| GET | `/api/pazaauto/karyawan/{id}` | Get employee by ID |
| POST | `/api/pazaauto/karyawan` | Create employee |
| PUT | `/api/pazaauto/karyawan/{id}` | Update employee |
| DELETE | `/api/pazaauto/karyawan/{id}` | Delete employee |
| GET | `/api/pazaauto/karyawan/paginated` | Paginated list with search |

### Employee Positions (`/api/pazaauto/karyawan-posisi`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/pazaauto/karyawan-posisi` | List all positions |
| GET | `/api/pazaauto/karyawan-posisi/{id}` | Get position by ID |
| POST | `/api/pazaauto/karyawan-posisi` | Create position |
| PUT | `/api/pazaauto/karyawan-posisi/{id}` | Update position |
| DELETE | `/api/pazaauto/karyawan-posisi/{id}` | Delete position |

### Service Orders / SPK (`/api/pazaauto/spk`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/pazaauto/spk` | List all service orders |
| GET | `/api/pazaauto/spk/{id}` | Get service order by ID |
| POST | `/api/pazaauto/spk` | Create service order |
| PUT | `/api/pazaauto/spk/{id}` | Update service order |
| DELETE | `/api/pazaauto/spk/{id}` | Delete service order |
| GET | `/api/pazaauto/spk/paginated` | Paginated list with filters |
| GET | `/api/pazaauto/spk/next-number` | Get next SPK number |

### SPK Details (`/api/pazaauto/spk-detail`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/pazaauto/spk-detail` | List all SPK details |
| GET | `/api/pazaauto/spk-detail/{id}` | Get detail by ID |
| POST | `/api/pazaauto/spk-detail` | Create SPK detail |
| PUT | `/api/pazaauto/spk-detail/{id}` | Update SPK detail |
| DELETE | `/api/pazaauto/spk-detail/{id}` | Delete SPK detail |

### Services / Jasa (`/api/pazaauto/jasa`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/pazaauto/jasa` | List all services |
| GET | `/api/pazaauto/jasa/{id}` | Get service by ID |
| POST | `/api/pazaauto/jasa` | Create service |
| PUT | `/api/pazaauto/jasa/{id}` | Update service |
| DELETE | `/api/pazaauto/jasa/{id}` | Delete service |

### Parts / Barang (`/api/pazaauto/barang`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/pazaauto/barang` | List all parts |
| GET | `/api/pazaauto/barang/{id}` | Get part by ID |
| POST | `/api/pazaauto/barang` | Create part |
| PUT | `/api/pazaauto/barang/{id}` | Update part |
| DELETE | `/api/pazaauto/barang/{id}` | Delete part |

### Spare Parts (`/api/pazaauto/sparepart`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/pazaauto/sparepart` | List all spare parts |
| GET | `/api/pazaauto/sparepart/{id}` | Get spare part by ID |
| POST | `/api/pazaauto/sparepart` | Create spare part |
| PUT | `/api/pazaauto/sparepart/{id}` | Update spare part |
| DELETE | `/api/pazaauto/sparepart/{id}` | Delete spare part |
| GET | `/api/pazaauto/sparepart/paginated` | Paginated list with search |

### Suppliers (`/api/pazaauto/supplier`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/pazaauto/supplier` | List all suppliers |
| GET | `/api/pazaauto/supplier/{id}` | Get supplier by ID |
| POST | `/api/pazaauto/supplier` | Create supplier |
| PUT | `/api/pazaauto/supplier/{id}` | Update supplier |
| DELETE | `/api/pazaauto/supplier/{id}` | Delete supplier |

### Sales / Penjualan (`/api/pazaauto/penjualan`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/pazaauto/penjualan` | List all sales |
| GET | `/api/pazaauto/penjualan/{id}` | Get sale by ID |
| POST | `/api/pazaauto/penjualan` | Create sale |
| PUT | `/api/pazaauto/penjualan/{id}` | Update sale |
| DELETE | `/api/pazaauto/penjualan/{id}` | Delete sale |
| POST | `/api/pazaauto/penjualan/{noPenjualan}/print` | Generate print data |
| POST | `/api/pazaauto/penjualan/cancel-by-no-spk/{noSpk}` | Cancel sale by SPK number |

### Sales Details (`/api/pazaauto/penjualan-detail`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/pazaauto/penjualan-detail` | List all sale details |
| GET | `/api/pazaauto/penjualan-detail/{id}` | Get detail by ID |
| POST | `/api/pazaauto/penjualan-detail` | Create sale detail |
| PUT | `/api/pazaauto/penjualan-detail/{id}` | Update sale detail |
| DELETE | `/api/pazaauto/penjualan-detail/{id}` | Delete sale detail |

### Purchases / Pembelian (`/api/pazaauto/pembelian`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/pazaauto/pembelian` | List all purchases |
| GET | `/api/pazaauto/pembelian/{id}` | Get purchase by ID |
| POST | `/api/pazaauto/pembelian` | Create purchase |
| PUT | `/api/pazaauto/pembelian/{id}` | Update purchase |
| DELETE | `/api/pazaauto/pembelian/{id}` | Delete purchase |

### Purchase Details (`/api/pazaauto/pembelian-detail`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/pazaauto/pembelian-detail` | List all purchase details |
| GET | `/api/pazaauto/pembelian-detail/{id}` | Get detail by ID |
| POST | `/api/pazaauto/pembelian-detail` | Create purchase detail |
| PUT | `/api/pazaauto/pembelian-detail/{id}` | Update purchase detail |
| DELETE | `/api/pazaauto/pembelian-detail/{id}` | Delete purchase detail |

### Sales Recap (`/api/pazaauto/rekap-penjualan`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/pazaauto/rekap-penjualan` | List all sales recaps |
| GET | `/api/pazaauto/rekap-penjualan/{id}` | Get recap by ID |
| POST | `/api/pazaauto/rekap-penjualan` | Create sales recap |
| PUT | `/api/pazaauto/rekap-penjualan/{id}` | Update sales recap |
| DELETE | `/api/pazaauto/rekap-penjualan/{id}` | Delete sales recap |
| GET | `/api/pazaauto/rekap-penjualan/by-no-spk/{noSpk}` | Get recap by SPK number |
| GET | `/api/pazaauto/rekap-penjualan/unprocessed` | Get unprocessed recaps |
| GET | `/api/pazaauto/rekap-penjualan/get-next-spk-number` | Get next SPK number |
| GET | `/api/pazaauto/rekap-penjualan/paginated` | Paginated list with filters |

### Attendance (`/api/pazaauto/absensi`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/pazaauto/absensi` | List all attendance records |
| GET | `/api/pazaauto/absensi/{id}` | Get record by ID |
| POST | `/api/pazaauto/absensi` | Create attendance record |
| PUT | `/api/pazaauto/absensi/{id}` | Update attendance record |
| DELETE | `/api/pazaauto/absensi/{id}` | Delete attendance record |

### Summary (`/api/pazaauto/summary`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/pazaauto/summary` | Get dashboard summary data |

### Audit Trail (`/api/audit-trail`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/audit-trail` | List all audit entries |
| GET | `/api/audit-trail/paginated` | Paginated list with filters |
| GET | `/api/audit-trail/table/{tableName}` | Get audit entries for table |
| GET | `/api/audit-trail/record/{recordId}` | Get audit entries for record |
| GET | `/api/audit-trail/table/{tableName}/record/{recordId}` | Get audit for specific record |
| GET | `/api/audit-trail/user/{userId}` | Get audit entries by user |

### System Parameters (`/api/system-parameter`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/system-parameter` | List all system parameters |
| GET | `/api/system-parameter/{id}` | Get parameter by ID |
| POST | `/api/system-parameter` | Create parameter |
| PUT | `/api/system-parameter/{id}` | Update parameter |
| DELETE | `/api/system-parameter/{id}` | Delete parameter |

### Health Check

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/health` | Application health status | No |

---

## Request/Response Patterns

### Standard CRUD Response
```json
{
  "id": 1,
  "createdAt": "2024-01-01T00:00:00Z",
  "updatedAt": "2024-01-01T00:00:00Z",
  "createdBy": "admin",
  "updatedBy": "admin"
}
```

### Paginated Response
```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8
}
```

### Error Response
```json
{
  "code": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "details": [
    {
      "field": "email",
      "message": "Email harus diisi"
    }
  ]
}
```

---

## Pagination

List endpoints support pagination via query parameters:

| Parameter | Default | Max | Description |
|-----------|---------|-----|-------------|
| `page` | 0 | - | Page number |
| `size` | 20 | 100 | Items per page |
| `sort` | - | - | Sort field (e.g., `createdAt,desc`) |
| `search` | - | - | Text search across fields |

Example:
```
GET /api/pazaauto/pelanggan?page=0&size=10&sort=createdAt,desc&search=John
```

---

## Error Codes

| Status | Meaning |
|--------|---------|
| 200 | Success |
| 201 | Created |
| 400 | Bad Request (validation error) |
| 401 | Unauthorized (invalid/missing token) |
| 403 | Forbidden (insufficient permissions) |
| 404 | Not Found |
| 409 | Conflict (duplicate entry) |
| 500 | Internal Server Error |

---

## Interactive Documentation

- **Swagger UI**: `http://localhost:8080/swagger-ui` (enable with `SWAGGER_ENABLED=true`)
- **OpenAPI JSON**: `http://localhost:8080/openapi`

See [swagger.md](swagger.md) for usage instructions.
