# Architecture Guide

## Clean Architecture

Pasa Auto follows Clean Architecture with strict layer separation and dependency inversion.

```
src/main/java/com/github/b3kt/
├── domain/              # Business logic (zero dependencies)
├── application/         # Use cases (depends on domain)
├── infrastructure/      # Technical details (implements app interfaces)
└── presentation/        # HTTP layer (depends on application)
```

### Dependency Rule

```
Presentation ──> Application ──> Domain <── Infrastructure
```

**Inner layers know nothing about outer layers.**

---

## Layer Details

### 1. Domain Layer

**Purpose**: Core business rules and entities.

**Contents**:
- `model/` - Domain entities (`User`, `Role`, `Permission`)
- `exception/` - Domain exceptions (`AuthenticationException`, `UserNotFoundException`)

**Rules**:
- No framework dependencies
- No database annotations
- Pure Java business logic

```java
// domain/model/User.java
public class User {
    private String username;
    private String password;

    public boolean canAccess(String permission) {
        // Business logic here
    }
}
```

### 2. Application Layer

**Purpose**: Orchestrates use cases.

**Contents**:
- `dto/` - Request/Response DTOs
- `service/` - Service interfaces and implementations
- `mapper/` - Entity ↔ DTO converters
- `helper/` - Shared utilities (`QueryFilterBuilder`)

**Rules**:
- Depends only on domain layer
- Defines interfaces that infrastructure implements
- Contains application-level business logic

```java
// application/service/AuthService.java
public interface AuthService {
    LoginResponse login(String username, String password);
    TokenResponse refreshToken(String refreshToken);
    void logout(String token);
}

// application/service/AuthServiceImpl.java
@ApplicationScoped
public class AuthServiceImpl implements AuthService {
    @Inject UserRepository userRepository;
    @Inject PasswordEncoder passwordEncoder;
    @Inject JwtTokenService jwtService;

    public LoginResponse login(String username, String password) {
        // Orchestrate: validate user → encode check → generate token
    }
}
```

### 3. Infrastructure Layer

**Purpose**: Technical implementations.

**Contents**:
- `persistence/entity/` - JPA entities with `@Entity` annotations
- `repository/` - Repository implementations
- `security/` - JWT, password encoding, interceptors
- `audit/` - Audit trail implementation

**Rules**:
- Implements interfaces defined in application layer
- Contains framework-specific code (JPA, JWT, etc.)
- Can be swapped without changing business logic

```java
// infrastructure/repository/UserRepository.java
@ApplicationScoped
public class UserRepository {
    @Inject PanacheQuery<TbUserEntity> findAll();

    public Optional<TbUserEntity> findByUsername(String username) {
        return find("username", username).firstResultOptional();
    }
}
```

### 4. Presentation Layer

**Purpose**: HTTP request/response handling.

**Contents**:
- `rest/` - JAX-RS REST resources
- `rest/pazaauto/` - Business domain resources
- `exception/` - Exception mappers for HTTP responses
- `config/` - CORS, OpenAPI configuration

**Rules**:
- Depends on application layer (services, DTOs)
- Handles HTTP concerns only (status codes, headers)
- No business logic

```java
// presentation/rest/AuthResource.java
@Path("/api/auth")
public class AuthResource {
    @Inject AuthService authService;

    @POST
    @Path("/login")
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(LoginRequest request) {
        LoginResponse response = authService.login(request.username(), request.password());
        return Response.ok(response).build();
    }
}
```

---

## Standard Patterns

### CRUD Pattern

All business entities follow a standard CRUD pattern:

```
AbstractCrudService<T>        ← Base service class
  └── TbSpkService            ← Entity-specific service

AbstractCrudResource<T>       ← Base REST resource
  └── TbSpkResource           ← Entity-specific resource
```

**Service provides**:
- `findAll()`, `findById()`, `create()`, `update()`, `delete()`
- `findPaginated()` with search/filter support

**Resource provides**:
- `GET /`, `GET /{id}`, `POST /`, `PUT /{id}`, `DELETE /{id}`
- `GET /paginated` with query parameters

### Entity Pattern

```java
@Entity
@Table(name = "tb_spk")
@EntityListeners(AuditTrailListener.class)
public class TbSpkEntity extends AbstractAuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public String noSpk;
    public String status;

    @ManyToOne
    public TbPelangganEntity pelanggan;
}
```

### DTO Pattern

```java
// Request DTO
public class SpkRequest {
    @NotBlank
    public String noSpk;

    @NotNull
    public Long pelangganId;
}

// Response DTO
public class SpkResponse {
    public Long id;
    public String noSpk;
    public String status;
    public String pelangganName;
}
```

---

## Security Architecture

```
HTTP Request
    │
    ▼
┌─────────────────────────┐
│  JWT Token Validation   │  (SmallRye JWT)
│  - Signature check      │
│  - Expiration check     │
│  - Issuer check         │
└─────────────────────────┘
    │
    ▼
┌─────────────────────────┐
│  Role Check             │  (@RolesAllowed)
│  - ADMIN, USER, etc.    │
└─────────────────────────┘
    │
    ▼
┌─────────────────────────┐
│  Business Logic         │  (Service layer)
└─────────────────────────┘
    │
    ▼
┌─────────────────────────┐
│  Data Access            │  (Repository layer)
└─────────────────────────┘
    │
    ▼
┌─────────────────────────┐
│  Audit Trail            │  (Entity listener)
└─────────────────────────┘
```

---

## Database Architecture

### Migrations

Flyway manages schema changes:

```
db/migration/
├── V1__initial_schema.sql
├── V2__add_audit_trail.sql
├── V3__add_rbac_tables.sql
├── ...
└── V13__latest_change.sql
```

Migrations run automatically at startup (`quarkus.flyway.migrate-at-start=true`).

### Entity Relationships

```
tb_pelanggan ──1:N──> tb_kendaraan ──1:N──> tb_spk
tb_spk ──1:N──> tb_spk_detail
tb_spk ──1:1──> tb_penjualan
tb_karyawan ──1:N──> tb_absensi
tb_supplier ──1:N──> tb_pembelian ──1:N──> tb_pembelian_detail
tb_barang ──1:N──> tb_sparepart
```

### Auditing

All entities extend `AbstractAuditableEntity`:

```java
public abstract class AbstractAuditableEntity {
    public String createdBy;
    public LocalDateTime createdAt;
    public String updatedBy;
    public LocalDateTime updatedAt;
}
```

---

## Adding a New Feature

### Step 1: Domain

```java
// domain/model/NewEntity.java
public class NewEntity {
    // Business rules
}
```

### Step 2: Infrastructure (Entity)

```java
// infrastructure/persistence/entity/NewEntityEntity.java
@Entity
@Table(name = "tb_new_entity")
public class NewEntityEntity extends AbstractAuditableEntity {
    @Id
    @GeneratedValue
    public Long id;

    public String name;
}
```

### Step 3: Infrastructure (Repository)

```java
// infrastructure/repository/NewEntityRepository.java
@ApplicationScoped
public class NewEntityRepository {
    // Panache-based data access
}
```

### Step 4: Application (DTOs + Service)

```java
// application/dto/NewEntityRequest.java
public record NewEntityRequest(@NotBlank String name) {}

// application/service/NewEntityService.java
@ApplicationScoped
public class NewEntityService extends AbstractCrudService<NewEntityEntity> {
    // Business logic
}
```

### Step 5: Presentation (Resource)

```java
// presentation/rest/NewEntityResource.java
@Path("/api/new-entity")
public class NewEntityResource extends AbstractCrudResource<NewEntityEntity, NewEntityService> {
    // REST endpoints
}
```

### Step 6: Database Migration

```sql
-- db/migration/V14__add_new_entity.sql
CREATE TABLE tb_new_entity (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_by VARCHAR(255),
    created_at TIMESTAMP,
    updated_by VARCHAR(255),
    updated_at TIMESTAMP
);
```

### Step 7: Frontend

Add page in `src/main/webui/src/pages/pazaauto/NewEntityPage.vue`.
