# ADR-0008: Domain Model Extraction with Per-Aggregate Repositories

## Status
**Accepted** - 2026-05-22

## Context
The application follows Clean Architecture (ADR-0002) but the pazaauto business domain has no domain layer — 19 JPA entities serve as domain models, DTOs, and HTTP payloads simultaneously. The `AbstractCrudService<T, ID>` generic base class hard-codes `PanacheRepositoryBase`, coupling every service to JPA infrastructure. This creates three problems:

1. **No seam for testing** — services cannot be unit-tested without mocking JPA infrastructure
2. **Shallow services** — 6 of 21 subclasses are pure pass-throughs (interface ≈ implementation)
3. **Leaky abstraction** — JPA entity fields are the REST API contract; changing a column name changes the API

The auth side of the application (User, Role, Permission) already demonstrates the correct pattern: domain models with `toDomain()` / `fromDomain()` entity methods and a repository interface returning domain objects.

### Options Considered
- **Keep AbstractCrudService with a repository interface adapter**: Loosen coupling but keep the generic pattern. Rejected because generic CRUD still exposes wide interfaces (`findAll()`, `listAll()`, `persist()`, `merge()`) that don't express domain intent.
- **Remove AbstractCrudService and AbstractCrudResource entirely**: Each aggregate gets its own repository interface with business-oriented methods. Resources become explicit per-resource with DTOs. **Chosen.**
- **Leave as-is**: The current pattern works but prevents testability and creates friction as the codebase grows. Rejected.

### Aggregate Boundaries
- **Spk** aggregate owns SpkDetail (was `TbSpkDetailService`)
- **Pembelian** aggregate owns PembelianDetail (was `TbPembelianDetailService`)
- **Penjualan** aggregate owns PenjualanDetail (was `TbPenjualanDetailService`)

Detail services are eliminated; their operations fold into the parent aggregate repository.

## Decision
Remove `AbstractCrudService` and `AbstractCrudResource`. Replace with per-aggregate repository interfaces in `infrastructure/repository/` (following the existing `UserRepository` pattern), business-domain services in `application/service/`, and per-resource REST endpoints with DTOs in `presentation/rest/`.

### Repository Interface Shape
Interfaces declare business-oriented methods, not CRUD generics:

```java
// Before (generic)
interface PanacheRepositoryBase<T, ID> { findAll(), findById(), persist(), merge(), deleteById(), ... }

// After (business-oriented)
interface SpkRepository {
    Optional<Spk> findById(Long id);
    Spk save(Spk spk);
    List<Spk> findUnprocessed();
    PageResponse<Spk> findPaginated(SpkFilter filter);
    String getNextSpkNumber();
}
```

### Migration Order
1. Create domain models for core concepts (Pelanggan, Spk, Pembelian, Penjualan)
2. Add `toDomain()` / `fromDomain()` to existing JPA entities
3. Create repository interfaces + JPA implementations (with cache logic at the seam)
4. Refactor services to depend on repository interfaces, not Panache
5. Create per-resource DTOs and refactor resources
6. Delete `AbstractCrudService`, `AbstractCrudResource`, and pure-pass-through services

## Consequences
- **Positive**: Each service is testable with an in-memory adapter behind its repository interface
- **Positive**: Aggregate boundaries are explicit — no orphan detail services
- **Positive**: 21 generic service subclasses → ~7 focused services with real business methods
- **Positive**: Pagination/filter logic concentrates in one place per aggregate instead of 9 inline variants
- **Negative**: More upfront code to write per aggregate (domain model + repo interface + JPA impl + mapper)
- **Trade-off**: The detail service elimination trades loose coupling for aggregate cohesion — callers must go through the aggregate root
