# ADR-0011: Customer Data Normalization (tb_pelanggan / tb_pelanggan_kendaraan / tb_customer_accounts)

## Status
**Accepted** - 2026-10-17 (V35 and release N implemented; contract migration pending)

## Context
`tb_pelanggan` began as a flat "customer + their one car" record. Since then, vehicle ownership moved to
`tb_pelanggan_kendaraan` (V14, ADR-0010) and a separate login identity appeared in `tb_customer_accounts`
(V22, ADR-0009). The three tables now overlap, and nothing in the schema says which one owns which fact.

### Current shape

```
tb_customer_accounts            tb_pelanggan                         tb_pelanggan_kendaraan        tb_kendaraan ── tb_merk_kendaraan
 email*  (login)                 nama_pelanggan*                      id_pelanggan ─┐ ON DELETE     merk (deprecated)
 phone   (login)                 nopol*  ┐ duplicate of the            id_kendaraan  │ CASCADE       merk_id
 full_name*                      merk*   │ junction (ADR-0010,         nopol*        │               jenis*, model
 id_pelanggan ──(1:1, nullable)─► jenis   ┘ phases 2-3 pending)         tanggal_mulai/akhir, is_current
                                 tlpn, no_hp, no_telepon   ← 3 phones  
                                 email, alamat, kota, kode_pos, ...
tb_spk.id_pelanggan        ──► (no FK)       tb_spk.nopol, tb_spk.nama_pelanggan  (snapshots)
tb_penjualan.id_pelanggan  ──► (no FK)
```

### Problems
1. **Vehicle data still lives on the customer.** `tb_pelanggan.nopol`/`merk` are `NOT NULL`, and the domain
   `Pelanggan` refuses a blank plate or brand. That is the only reason the `'-'` placeholder exists
   (`PelangganPlaceholder`, V34): a customer with no car cannot be stored honestly. ADR-0010 already made the
   junction the source of truth, but its Phase 2 (stop writing) and Phase 3 (drop the columns) are not done.
2. **Three phone columns with no clear meaning.** `tlpn`, `no_hp` and `no_telepon` are all free text. The
   placeholder writes the same number into both `tlpn` and `no_hp`. Contact matching
   (`TbPelangganRepository.findByContact`) compares the raw strings, so `0812-3456-7890` never matches
   `+6281234567890`. The JPA lengths (15/20) also don't match the DB columns (25).
3. **Account and customer duplicate the profile.** `full_name`/`email`/`phone` on the account and
   `nama_pelanggan`/`email`/`no_hp` on the customer are copied into each other on several paths (link, skip,
   add vehicle, V34). Nothing defines which one wins. `tb_customer_accounts.phone` is unique on the raw
   string, so the same number in two formats gets past it.
4. **Weak referential integrity.** `tb_spk.id_pelanggan` and `tb_penjualan.id_pelanggan` have no FK.
   `tb_pelanggan_kendaraan.id_pelanggan` is `ON DELETE CASCADE`, so a hard delete of a customer silently
   erases ownership history, in a system that otherwise soft-deletes.
5. **Ownership rows aren't fully constrained.** Canonical plate format (ADR-0010) is enforced only in Java.
   `is_current = false` rows can have a null `tanggal_akhir` (V34 step 1a), and nothing stops
   `tanggal_akhir < tanggal_mulai`.
6. **Duplicate customers can't be merged traceably.** V34 "adopts" placeholders by soft-deleting them, and
   ADR-0010 found 11 plates split across customers. A merged-away record leaves nothing behind that points to
   the surviving one, so old `tb_spk`/`tb_customer_bookings` references end at a deleted row.

### Options Considered
- **Big-bang rewrite** (drop the columns and rewrite every reader in one release): rejected for the same reasons
  as in ADR-0010. A Flyway migration that drops columns cannot be rolled back with the previous build, because
  Hibernate validation fails on the missing columns.
- **Split contacts into a 1:N `tb_pelanggan_kontak` table**: rejected as over-engineering for a workshop. Two
  typed phone columns plus email cover every screen we have.
- **Make every account own a `tb_pelanggan` (`id_pelanggan NOT NULL`) and drop `full_name`**: deferred. It
  changes what "linked" means for the link flow and can't be rolled back cleanly. Revisit once the link flow
  is redesigned around merging.
- **Add a vehicle-instance table** (`tb_unit_kendaraan`: plate, chassis number, year, colour) between the model
  catalogue and ownership: deferred. It only matters once a re-plated vehicle (mutasi) has to keep its service
  history. The current junction can be split later without changing its API.
- **Expand → migrate → contract, with every step backward compatible (chosen).**

## Decision

### Target model: one fact, one owner

| Table | Owns | Does not own |
|---|---|---|
| `tb_customer_accounts` | **Credentials**: login email, login phone, password hash, active flag, the 1:1 link to a customer | Profile data. `full_name` is only the display name *before* linking; once linked, `tb_pelanggan.nama_pelanggan` is authoritative |
| `tb_pelanggan` | **Who the customer is**: name, contacts (`no_hp` mobile, `no_telepon` landline, `email`), address, provenance (`sumber`), merge pointer (`merged_into_id`) | Vehicles (`nopol`, `merk`, `jenis` → dropped). `tlpn` → folded into `no_hp`/`no_telepon`, then dropped |
| `tb_pelanggan_kendaraan` | **Who owned which plate, and when**: canonical `nopol`, model link, validity period | Customer or contact data |
| `tb_spk.nopol`, `tb_spk.nama_pelanggan`, `tb_customer_bookings.id_pelanggan`/`nopol` | **Historical snapshots**, kept on purpose and documented with `COMMENT ON COLUMN` | Live data. Never "fixed up" from the master |

Rules:
- **A customer may have zero vehicles.** `tb_pelanggan.nopol`/`merk` become nullable, the domain invariant on
  `Pelanggan` is removed, and the `'-'` sentinel goes away. Portal-created customers are marked
  `sumber = 'PORTAL'` instead.
- **Primary vehicle**, for APIs that still expose a single `nopol`: the current ownership row with the latest
  `tanggal_mulai` (ties go to the highest `id`). The DB trigger, the compatibility view and
  `PelangganVehicleResolver` all use this one rule (the resolver drops its "prefer the legacy plate"
  tie-break).
- **Canonical phone format**: digits only, national form. A leading `62`/`+62` becomes `0`, so
  `+62 812-3456-7890` is stored as `081234567890`. A new `PhoneNormalizer` (alongside `NopolNormalizer`) applies
  it on write and on contact lookup, for both `tb_pelanggan` and `tb_customer_accounts`.
- **Account to customer copying** happens only to seed a newly created `tb_pelanggan`, and only into blank
  fields (as V34 step 1d already does). Profile edits write to `tb_pelanggan` only.
- **Merging customers** (`PelangganMergeService`) moves the duplicate's ownership history, pending vehicle claims,
  pending link requests and portal account link to the survivor, then sets `merged_into_id` and soft-deletes the
  duplicate. Historical snapshots (`tb_spk`, `tb_penjualan`, `tb_customer_bookings`) are not rewritten; they
  resolve to the survivor through `merged_into_id`.
- **Removing a customer's last vehicle keeps the customer record** (it used to be soft-deleted, which left a
  linked portal account pointing at a deleted row).

### Backward-compatibility guarantees
1. **REST/DTO shape doesn't change.** `PelangganDto.nopol/merk/jenis/telepon` stay (marked deprecated in
   OpenAPI) and `sumber` is added as a read-only field. Vehicle fields come from the primary vehicle. `telepon` is a
   strict alias of `noTelepon`: on read it echoes `noTelepon`, and a write that sets `telepon` while `noTelepon` is
   absent (`null`) is applied to `noTelepon`. It deliberately does not fall back to `noHp`, so a read-modify-write
   round trip can't copy the mobile number into the landline field. `nopol`/`merk` are no longer `@NotBlank`. Both
   SPAs keep working unchanged.
2. **Raw SQL/report consumers** move to `v_pelanggan_legacy`, which keeps the old column names and is created
   in the expand step, *before* anything is dropped.
3. **Rolling back one release is always safe.** The expand migration only adds columns, relaxes constraints or
   adds `NOT VALID` constraints, and backfills data the previous build can still read. Destructive DDL happens
   only in the contract migration, at least one release later, once no running build maps the dropped columns.

### Migration plan

#### Pre-flight audit (read-only; run on a production snapshot, results go into the PR)
```sql
-- a) placeholder customers ('-' sentinel) and how many own vehicles
SELECT count(*) FILTER (WHERE btrim(coalesce(nopol,'')) IN ('','-')) AS placeholders,
       count(*) FILTER (WHERE btrim(coalesce(nopol,'')) IN ('','-')
                        AND EXISTS (SELECT 1 FROM tb_pelanggan_kendaraan k
                                    WHERE k.id_pelanggan = p.id AND k.is_current AND k.deleted_at IS NULL)) AS placeholders_with_vehicle
FROM tb_pelanggan p WHERE deleted_at IS NULL;

-- b) tlpn values that are not already present in no_hp / no_telepon
SELECT id, tlpn, no_hp, no_telepon FROM tb_pelanggan
WHERE deleted_at IS NULL AND btrim(coalesce(tlpn,'')) <> ''
  AND tlpn IS DISTINCT FROM no_hp AND tlpn IS DISTINCT FROM no_telepon;

-- c) account phones that collide once normalized (would violate idx_tb_customer_accounts_phone)
SELECT n, array_agg(id) FROM (
  SELECT id, regexp_replace(regexp_replace(phone, '[^0-9]', '', 'g'), '^62', '0') AS n
  FROM tb_customer_accounts WHERE phone IS NOT NULL AND deleted_at IS NULL) x
GROUP BY n HAVING count(*) > 1;

-- d) orphaned customer references that would block FK validation
SELECT 'tb_spk' t, count(*) FROM tb_spk s
 WHERE s.id_pelanggan IS NOT NULL AND NOT EXISTS (SELECT 1 FROM tb_pelanggan p WHERE p.id = s.id_pelanggan)
UNION ALL
SELECT 'tb_penjualan', count(*) FROM tb_penjualan j
 WHERE j.id_pelanggan IS NOT NULL AND NOT EXISTS (SELECT 1 FROM tb_pelanggan p WHERE p.id = j.id_pelanggan);

-- e) ownership rows that would fail the new checks
SELECT count(*) FILTER (WHERE NOT is_current AND tanggal_akhir IS NULL)   AS closed_without_end,
       count(*) FILTER (WHERE is_current AND tanggal_akhir IS NOT NULL)   AS current_with_end,
       count(*) FILTER (WHERE tanggal_akhir < tanggal_mulai)              AS negative_period,
       count(*) FILTER (WHERE nopol <> upper(regexp_replace(nopol, '[[:space:]]', '', 'g'))) AS non_canonical
FROM tb_pelanggan_kendaraan;

-- f) multi-vehicle customers whose displayed plate will change under the single "primary vehicle" rule
SELECT p.id, p.nopol AS legacy, pv.nopol AS primary_by_rule
FROM tb_pelanggan p
JOIN LATERAL (SELECT k.nopol FROM tb_pelanggan_kendaraan k
              WHERE k.id_pelanggan = p.id AND k.is_current AND k.deleted_at IS NULL
              ORDER BY k.tanggal_mulai DESC NULLS LAST, k.id DESC LIMIT 1) pv ON true
WHERE p.deleted_at IS NULL AND p.nopol IS DISTINCT FROM pv.nopol;
```
Collisions from (c) and orphans from (d) are resolved by hand (or by a targeted data migration) before the
contract step. They don't block the expand step.

#### V35: expand (ships with release N; rollback-safe)
Implemented in `src/main/resources/db/migration/V35__normalize_customer_data_expand.sql`:

1. `tb_pelanggan` gets `sumber` (`STAFF`/`PORTAL`, CHECKed) and `merged_into_id`; `nopol`/`merk` become nullable.
   Rows are marked `PORTAL` when they are the `'-'` placeholder or were created by a customer request (the audit
   listener stores the customer JWT subject, the account id, in `created_by`).
2. **Legacy plates are preserved before the column becomes derived.** A live customer whose legacy plate has no
   ownership row of their own gets one: a current row when nobody owns the plate (earliest customer wins), and
   a closed historical row otherwise. Otherwise the recompute in step 7, and the contract migration's drop, would lose the plate.
3. The `'-'`/blank sentinels in `nopol`/`merk`/`jenis` become `NULL`.
4. Phones: `tlpn` is folded into an empty `no_hp` (mobile-looking) or `no_telepon`. `tlpn` itself is left
   untouched until the contract migration, and `no_hp`/`no_telepon` are canonicalized. Account phones are canonicalized only where
   that doesn't collide with another account (audit c); colliding ones stay as they are for manual resolution.
   Indexes are added on `no_hp`, `no_telepon` and `lower(email)`.
5. Ownership: closed rows without an end date get one; `chk_pk_nopol_canonical` and `chk_pk_current_open` are
   added `NOT VALID`; the `id_pelanggan` FK becomes `ON DELETE RESTRICT`. **`chk_pk_period` is deferred to the contract migration**,
   because the previous build closes a same-day ownership with "start - 1 day", which the constraint would reject
   even as `NOT VALID`. Release N clamps the end date instead.
6. `fk_spk_pelanggan` and `fk_penjualan_pelanggan` are added `NOT VALID`.
7. Trigger `trg_pk_sync_legacy_vehicle` becomes the only writer of `tb_pelanggan.nopol/merk/jenis`, followed by
   a one-time recompute for every live customer. Customers without a current vehicle end up with `NULL`.
8. View `v_pelanggan_legacy` keeps the old column names (`tlpn` = `no_telepon`) and doesn't reference any column
   the contract migration drops.
9. `COMMENT ON` documents deprecated columns, snapshots and the semantics of the new columns.

Verified against a scratch PostgreSQL 16 with V1–V34 applied and seeded edge cases: multiple vehicles,
transferred-away owner, duplicate legacy plate, unowned legacy plate, placeholder, portal-promoted record,
`tlpn` folding, and colliding account phones. Also checked: trigger behaviour on transfer and on soft-delete,
and that each new constraint and FK rejects bad writes.

#### Release N: application changes (same deploy as V35)
- `Pelanggan` domain: `nopol`/`merk` are optional (blank becomes `null`); adds `sumber`, `mergedIntoId`.
- `TbPelangganEntity`: `nopol`/`merk`/`jenis` are mapped `insertable = false, updatable = false`; phone column
  lengths are 25; adds `sumber` (defaults to `STAFF`) and `mergedIntoId`. `@PrePersist/@PreUpdate` applies the
  `telepon` write alias, canonicalizes phones and mirrors `tlpn = noTelepon`.
- `PhoneNormalizer` (mirrors the SQL in V35) is used by the entity, `TbPelangganRepository.findByContact`
  (which now compares canonical `no_hp`/`no_telepon` only), `CustomerAuthService.register` and
  `CustomerAccountRepository.findByPhone` (canonical first, raw value as a fallback for collided legacy rows).
- `PelangganPlaceholder` is replaced by `PortalPelanggan`, which builds a `sumber = PORTAL` record with no
  vehicle. `CustomerVehicleService` treats a caller as provisioned when their record is `STAFF` or owns a current
  vehicle. Its legacy-column lookup is gone, it no longer writes vehicle fields onto `tb_pelanggan`, and adopting
  an on-record customer merges the caller's own record into it.
- `PelangganVehicleResolver` uses the single primary-vehicle rule.
- Legacy-column reads removed: `JpaPelangganRepository.findByNopol` fallback, staff search on `nopol` (now a
  subquery on current ownership, canonical plate), the `UNION` arm of `findVehicleOptions`,
  `TbPelangganRepository.findByNopolCached`/`findByNormalizedNopol`.
- `TbPelangganKendaraanRepository.closeOwnership` clamps the end date to the period start.
- The login response and the profile endpoint return `nama_pelanggan` as the name once the account is linked.
  The `fullName` JWT claim still carries `full_name`; the portal prefers the response value.
- `KendaraanOwnershipService.deleteVehicle` no longer soft-deletes the customer when the last vehicle goes.

Still reading the trigger-derived columns in release N (they are correct, but have to move before the contract migration):
the bound-customer branch and legacy fallback in `SpkEnrichmentService`, `TbPenjualanService` (merk/nopol
for receipts), `CustomerLinkService` (plate shown in search results), and `TbPelangganService` update/patch
(which pass the primary plate through to `PelangganOwnershipSyncService`).

V36 became ADR-0012's contact migration, so the contract step takes the next free number. V35's own
comments still say "V36"; they refer to this step and stay as they are because V35 is applied.

#### Contract migration (V37 or later; ships with release N+1, at least one release after N, once audits (c)/(d) are clean)
```sql
-- Negative periods from pre-release-N closes (audit e) must be fixed first.
ALTER TABLE tb_pelanggan_kendaraan
    ADD CONSTRAINT chk_pk_period CHECK (tanggal_akhir IS NULL OR tanggal_akhir >= tanggal_mulai);
ALTER TABLE tb_pelanggan_kendaraan VALIDATE CONSTRAINT chk_pk_nopol_canonical;
ALTER TABLE tb_pelanggan_kendaraan VALIDATE CONSTRAINT chk_pk_current_open;
ALTER TABLE tb_spk       VALIDATE CONSTRAINT fk_spk_pelanggan;
ALTER TABLE tb_penjualan VALIDATE CONSTRAINT fk_penjualan_pelanggan;

ALTER TABLE tb_pelanggan
    ADD CONSTRAINT chk_pelanggan_no_hp_canonical CHECK (no_hp ~ '^0[0-9]{6,15}$');
ALTER TABLE tb_customer_accounts
    ADD CONSTRAINT chk_customer_accounts_phone_canonical CHECK (phone ~ '^0[0-9]{6,15}$');

DROP TRIGGER  IF EXISTS trg_pk_sync_legacy_vehicle ON tb_pelanggan_kendaraan;
DROP FUNCTION IF EXISTS fn_pk_sync_legacy_vehicle();

DROP INDEX IF EXISTS tb_pelanggan_nopol_index;
DROP INDEX IF EXISTS tb_pelanggan_nama_index;
CREATE INDEX IF NOT EXISTS idx_pelanggan_nama ON tb_pelanggan (nama_pelanggan) WHERE deleted_at IS NULL;
DROP INDEX IF EXISTS idx_pk_nopol_norm;     -- redundant once chk_pk_nopol_canonical is validated

-- no_telepon is dropped too: the landline is deprecated by ADR-0012.
ALTER TABLE tb_pelanggan DROP COLUMN nopol, DROP COLUMN merk, DROP COLUMN jenis, DROP COLUMN tlpn,
                         DROP COLUMN no_telepon;
-- v_pelanggan_legacy (as redefined by V36) does not reference the dropped columns, so it survives.
```
In release N+1, `TbPelangganEntity` stops mapping `nopol`/`merk`/`jenis`/`tlpn`. The DTO keeps them, marked
`@Deprecated`, until the staff SPA's customer page moves to the multi-vehicle view.

### Rollback matrix
| State | Roll back to | Safe? | Why |
|---|---|---|---|
| V35 + release N | release N-1 | Yes | Columns still exist; NULL plate is treated like `'-'`; trigger keeps `nopol` current; `NOT VALID` constraints only affect writes N-1 already does correctly |
| Contract + release N+1 | release N | Only if N+1 has been the oldest deployable build for a full release | N still maps the columns read-only, so Hibernate validation fails once they're gone. Ship the entity change (N+1) first and the contract migration in N+2, or accept that N is out of the rollback window |
| Contract + release N+1 | release N-1 | No | Expected for a contract step. Don't ship the contract migration until N-1 is out of the rollback window |

## Consequences
- **Positive**: Each fact has one owner. Vehicles live only in ownership, profile only in `tb_pelanggan`,
  credentials only in `tb_customer_accounts`.
- **Positive**: The `'-'` placeholder and the domain invariant that forced it are gone. A customer without a car
  is a valid, honest row.
- **Positive**: Normalized phones make contact-based linking work whatever the number's formatting, and close
  the duplicate-account hole in `idx_tb_customer_accounts_phone`.
- **Positive**: FKs and CHECKs move integrity from Java into the database. That is the same approach ADR-0009
  took for the 1:1 account link.
- **Positive**: `merged_into_id` makes duplicate cleanup reversible and auditable.
- **Negative**: A trigger becomes a hidden writer of three columns for one release cycle, and it adds audit-trail
  rows on every ownership change.
- **Negative**: Multi-vehicle customers may show a different "primary" plate after V35 (audit f lists them).
- **Trade-off**: Two releases and a compatibility view instead of one. In exchange, every deploy can be rolled
  back.
