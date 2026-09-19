# Improvement Plan - Pasa Auto

> Generated: 2026-08-15
> Based on codebase analysis of the Quarkus + Quasar automotive workshop management system.

---

## Executive Summary

The Pasa Auto project has a solid foundation with clean architecture, proper security practices (JWT, RBAC, env-based secrets), and CI/CD pipelines. However, there are significant gaps in **test coverage**, **code maintainability**, **security hardening**, and **observability** that need addressing before scaling.

---

## Current State Assessment

| Category | Score | Notes |
|----------|-------|-------|
| Architecture | ✅ Good | Clean 4-layer DDD structure |
| Security | ⚠️ Partial | RBAC disabled, no rate limiting |
| Testing | ❌ Low | ~10% JaCoCo threshold, no frontend tests |
| Code Quality | ⚠️ Fair | Large services, entity exposure in APIs |
| Performance | ⚠️ Fair | N+1 queries, no caching strategy |
| Observability | ⚠️ Partial | OTEL configured but no dashboards |
| Documentation | ✅ Good | Comprehensive docs + ADRs |
| DevOps | ⚠️ Fair | Native build works, standalone deployment |

---

## Priority 1: Critical (Do First)

### 1.1 Increase Test Coverage Threshold
**File**: `pom.xml` (line 256)
**Change**: `minimum>0.10` → `minimum>0.60`
**Effort**: 5 minutes
**Impact**: Enforces minimum 60% coverage on all new code

### 1.2 Add Integration Tests for Core Flows
**Target Flows**:
- SPK creation → details → completion → penjualan
- Pembelian → barang stock update
- Pelanggan + kendaraan management
- Auth login/refresh/token validation

**Location**: `src/test/java/com/github/b3kt/integration/`
**Effort**: 2-3 days
**Dependencies**: Testcontainers (already configured)

### 1.3 Decouple REST Resources from JPA Entities
**Problem**: Resources return `TbSpkEntity`, `TbPelangganEntity` directly
**Solution**: Create DTOs + MapStruct mappers for all resources
**Files Affected**: All `*Resource.java` in `presentation/rest/pazaauto/`
**Effort**: 3-4 days
**Example**:
```java
// Before
@GET @Path("/{id}") public Response getById(...) {
    return Response.ok(ApiResponse.success(service.findById(id))).build(); // Returns Entity
}

// After
@GET @Path("/{id}") public Response getById(...) {
    SpkDto dto = spkMapper.toDto(service.findById(id));
    return Response.ok(ApiResponse.success(dto)).build();
}
```

### 1.4 Enable RBAC & Add Permission Checks
**Config**: `application.properties` line 74 → `app.rbac.enabled=true`
**Implementation**: Add `@RolesAllowed` / custom permissions to all resources
**Effort**: 1-2 days
**Testing**: Verify all endpoints respect permissions

### 1.5 Add Frontend Testing Infrastructure
**Tools**: Vitest (unit), Playwright (e2e)
**Config**: `src/main/webui/package.json`
```json
"scripts": {
  "test:unit": "vitest run",
  "test:e2e": "playwright test",
  "test": "npm run test:unit && npm run test:e2e"
}
```
**Effort**: 1 day setup + ongoing test writing

---

## Priority 2: High (Next Sprint)

### 2.1 Refactor Large Services (SRP Violation)
**Target**: `TbSpkService` (327 lines) → Split into:
- `SpkCrudService` - Basic CRUD operations
- `SpkReportService` - RekapPenjualan, paginated reports
- `SpkEnrichmentService` - enrich(), fillRequiredFields()
- `SpkNumberService` - generateNextSpkNumber(), getNextSpkNumber()

**Pattern**: Apply to `TbPenjualanService`, `TbPembelianService` similarly
**Effort**: 3-4 days

### 2.2 Implement MapStruct for Entity↔DTO Mapping
**Dependency**: Add to `pom.xml`
```xml
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.6.3</version>
</dependency>
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct-processor</artifactId>
    <version>1.6.3</version>
    <scope>provided</scope>
</dependency>
```

**Create Mappers**:
- `SpkMapper`, `PelangganMapper`, `BarangMapper`, `KaryawanMapper`, etc.
**Effort**: 2 days

### 2.3 Add Caching for Reference Data
**Target Entities**: Pelanggan, Barang, Sparepart, Jasa, Karyawan, Supplier
**Annotations**: `@CacheResult`, `@CacheInvalidate` on repository methods
**Config**: `quarkus.cache.caffeine.*` in application.properties
**Effort**: 1 day

### 2.4 Standardize Pagination/Filtering
**Current**: Each service reimplements `findPaginated()` with `QueryFilterBuilder`
**Solution**: Enhance `AbstractCrudService` with:
- Generic filter/sort/pagination
- Specification pattern for complex queries
- Default search across configurable fields

**Effort**: 2 days

### 2.5 Add API Rate Limiting
**Extension**: `quarkus-smallrye-rate-limiting`
**Config**: Per-endpoint or per-user limits
**Example**:
```java
@RateLimit(value = 100, window = 60, unit = TimeUnit.SECONDS) // 100 req/min
@GET @Path("/list") public Response list() { ... }
```
**Effort**: 4 hours

---

## Priority 3: Medium (Technical Debt)

### 3.1 Consolidate Flyway Migrations
**Current**: 13 migration files (V1-V13)
**Action**: Create baseline migration `V14__baseline.sql` for fresh installs
**Keep**: Historical migrations for existing deployments
**Effort**: 4 hours

### 3.2 Add Health/Readiness Checks
**Dependencies**: `quarkus-smallrye-health`
**Implement**: Custom `HealthCheck` for DB connectivity, external services
**Note**: Standalone app — no Kubernetes manifest generation
**Effort**: 1 day

### 3.3 Define OpenTelemetry Dashboards & Alerts
**Current**: OTEL exports to Uptrace
**Missing**: 
- Service-level dashboards (latency, error rate, throughput)
- Business metrics (SPK created/day, sales conversion)
- Alert rules (error rate > 5%, p99 latency > 2s)
**Effort**: 1-2 days (mostly Uptrace/Grafana config)

### 3.4 Frontend Architecture Improvements
- **API Layer**: Create `src/services/api/*.js` with axios interceptors
- **State**: Pinia stores with persistence plugin for auth/user preferences
- **SSR**: Evaluate Quasar SSR mode for SEO/performance
- **Components**: Storybook for component documentation
**Effort**: 3-5 days

### 3.5 Document Migration Rollback Procedures
**Create**: `docs/database-rollback.md`
**Content**: 
- How to rollback each migration
- Backup/restore procedures
- Blue-green deployment strategy for DB changes
**Effort**: 4 hours

---

## Priority 4: Low (Nice to Have)

### 4.1 Add Input Validation to All DTOs
**Current**: Inconsistent validation annotations
**Standardize**: `@NotNull`, `@Size`, `@Pattern`, `@Email`, `@Positive`, etc.
**Custom Validators**: For business rules (e.g., `@ValidNoSpk`, `@ValidNopol`)
**Effort**: 1 day

### 4.2 Implement Audit Logging at Application Level
**Current**: DB triggers only
**Add**: `@Audited` on entities + `AuditTrailService` for business events
**Events**: SPK status change, price override, user permission changes
**Effort**: 2 days

### 4.3 Add API Documentation Examples
**OpenAPI**: Add `@Schema(example = "...")` to all DTO fields
**Swagger UI**: Enable `quarkus.swagger-ui.enabled=true` in dev
**Effort**: 4 hours

### 4.4 Create Automated Setup Script
**Script**: `scripts/setup-dev.sh`
**Actions**:
- Check prerequisites (Java, Node, Docker)
- Start PostgreSQL container
- Generate JWT keys if missing
- Copy `.env.example` → `.env` with prompts
- Run migrations
- Install frontend deps
**Effort**: 4 hours

### 4.5 Evaluate Native Image Optimizations
**Current**: Native profile configured
**Check**: Build time, image size, startup time
**Optimize**: 
- `@RegisterForReflection` only where needed
- GraalVM reachability metadata
- `quarkus.native.additional-build-args`
**Effort**: 1 day

---

## Effort Summary

| Priority | Items | Estimated Effort |
|----------|-------|------------------|
| Critical | 5 | ~5-7 days |
| High | 5 | ~8-10 days |
| Medium | 5 | ~5-7 days |
| Low | 5 | ~4-5 days |
| **Total** | **20** | **~22-29 days** |

---

## Quick Wins (Do Today)

- [ ] Update JaCoCo threshold to 60% in `pom.xml`
- [ ] Add `@NotNull`/`@Size` to all DTO fields
- [ ] Enable `quarkus.swagger-ui.enabled=true` for dev profile
- [ ] Document all required env vars in `.env.example`
- [ ] Add `quarkus-smallrye-health` dependency
- [ ] Create `.editorconfig` for consistent formatting (if missing)

---

## Tracking

| Item | Status | Assignee | Target Date |
|------|--------|----------|-------------|
| JaCoCo threshold | 📋 Planned | - | - |
| Integration tests | 📋 Planned | - | - |
| DTO/Mapper extraction | 📋 Planned | - | - |
| RBAC enablement | 📋 Planned | - | - |
| Frontend test setup | 📋 Planned | - | - |
| Service refactoring | 📋 Planned | - | - |
| MapStruct integration | 📋 Planned | - | - |
| Caching strategy | 📋 Planned | - | - |
| Rate limiting | 📋 Planned | - | - |
| Health probes | 📋 Planned | - | - |

---

## Notes

- This plan should be reviewed and prioritized with the team
- Items marked 📋 Planned can be moved to GitHub Issues/Projects
- Consider sprint capacity when committing to items
- Re-assess after completing Priority 1 items

---

*Last updated: 2026-08-15*