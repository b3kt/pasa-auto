# ADR-0009: Customer Self-Service Portal

## Status
**Accepted** - 2026-10-11

## Context
Customers (owners of vehicles serviced by the workshop) currently have no access to the system: every SPK, pembelian, and penjualan record is created and viewed by staff. We want customers to see the status and history of their own vehicles and to raise a service-booking request, without exposing the staff ERP.

This raises four decisions that are not obvious from the existing RBAC + JWT model (ADR-0004, ADR-0007):

1. **How is the customer app delivered?** The staff SPA already exists (`src/main/webui`, Quasar). Customers are untrusted, public-internet users with a different workflow (status lookup, booking) and a much smaller surface.
2. **How is a customer authenticated?** `User` is the staff identity, keyed by a globally-unique username. A customer is not a staff user and must never appear in staff user administration.
3. **How is a customer linked to their data?** `tb_pelanggan` is the business record for a customer; a login identity must map to at most one such record, and that mapping is an authorization boundary.
4. **How is the whole feature rolled out safely?** An unvetted public login is the largest newly-exposed surface in the app.

### Options Considered
- **Same SPA with role-gated routes**: Add customer routes to the existing staff bundle and gate on a new role. Rejected — the untrusted customer bundle would ship all staff assets/JS to the public, and route guards are client-side only.
- **Separate backend service**: Full isolation, but a second deployable, its own DB connection pool, and duplicated auth/queries. Rejected as disproportionate for the scope.
- **Separate frontend bundle on the same backend** (chosen): a second Quasar target served at `/app/`, backed by dedicated `/api/customer/**` endpoints in the same Quarkus app.
- **Reuse `User`/`tb_pelanggan` as the login**: Rejected — blurs the staff/customer identity boundary and risks privilege leakage.

## Decision

### 1. Separate frontend bundle at `/app/`
The customer portal is a second Quasar application (`src/main/appui`) served under `/app/`, using hash routing so the backend does not need a fallback rewrite rule. It shares no route guard or store with the staff SPA; the two bundles have independent asset graphs.

### 2. Dedicated customer identity, not `User`
Customers are stored in a new `customer_accounts` table (email + phone + password hash + `full_name`), and the role `Customer` (`Roles.CUSTOMER`) is added to the RBAC vocabulary. Customer endpoints live under `/api/customer/**` and are `@RolesAllowed("Customer")`. The staff `User` table and staff user-administration screens are untouched.

### 3. Strict 1:1 link to `tb_pelanggan`
A `customer_accounts` row may point at at most one `tb_pelanggan` (`id_pelanggan` is a single nullable column), and at most one account may point at a given pelanggan — enforced by a partial unique index (`idx_customer_accounts_pelanggan ... WHERE id_pelanggan IS NOT NULL`). Auto-linking matches on plate number or contact; a match that is already claimed by another account is queued in `customer_pelanggan_link_requests` for Admin/Owner approval instead of auto-linking.

### 4. Reduced DTOs
Customer-facing responses use dedicated DTOs that expose only customer-relevant fields; internal cost, margin, supplier, and staff-only fields (mechanic, km) are never serialized to the portal.

### 5. In-memory access token + HttpOnly refresh cookie
Matching the staff hardening, the customer access token is held in memory only and restored on reload by exchanging an `HttpOnly` `customer_refresh_token` cookie. Customer refresh tokens are stored in a separate `customer_refresh_tokens` table keyed by `customer_account_id` (a different identity space from the staff `refresh_tokens` table's username key), avoiding collision. Login is rate-limited through the existing `LoginAttemptService`.

### 6. Feature-gated, off by default
A DB-backed master switch `customer.access.enabled` (default `false`, `app_settings`) is enforced by `CustomerAccessGateFilter` at `@PreMatching`, before `@RolesAllowed` runs, so a disabled portal returns `503` rather than a `401` that would reveal the endpoint exists. The feature is enabled per-deployment once it is vetted.

### Migrations
`V21` app_settings, `V22` customer_accounts, `V23` customer_pelanggan_link_requests, `V24` customer_bookings, `V25` customer_refresh_tokens.

## Consequences
- **Positive**: The untrusted customer surface is a separate, smaller bundle and a separate identity space — no staff asset or credential path is shared.
- **Positive**: The 1:1 link is enforced in the database, so authorization cannot drift on a bad code path.
- **Positive**: The master switch lets the feature ship dark and be enabled deliberately.
- **Positive**: Bookings are a lightweight `customer_bookings` table (not `tb_spk`), so staff-only work-order fields are never populated prematurely; staff convert a booking into a real SPK on arrival.
- **Negative**: Two frontends and two auth stacks to maintain; some duplication of build/config and of token-handling logic.
- **Negative**: A second refresh-token table and a link-request approval queue add operational concepts staff must understand.
- **Trade-off**: Same-backend deployment keeps ops simple at the cost of not being able to scale or isolate the customer API independently.
