# ADR-0010: Vehicle Plate Authority Moves to the Ownership Junction

## Status
**Accepted** - 2026-10-15

## Context
`tb_pelanggan` historically held a customer's vehicle details inline: `nopol` (NOT NULL), `merk` (NOT NULL) and `jenis`. This assumed **one vehicle per customer**. Vehicle ownership was later modelled properly in the junction table `tb_pelanggan_kendaraan` (`id_pelanggan`, `id_kendaraan`, `nopol`, `tanggal_mulai`/`tanggal_akhir`, `is_current`), which supports multiple vehicles, transfers between customers and history — but the deprecated columns on `tb_pelanggan` were kept in place, producing two sources of truth that drift:

- The junction stores plates verbatim (whitespace and casing preserved, e.g. `"B 1019 CYM"`), while `tb_pelanggan.nopol` is often the compact form — 35 current ownership rows disagree with the customer's own plate.
- **11 plates** are duplicated across different `tb_pelanggan` rows (`"B ."` vs `"B."`, `"B 1074 CMD"` vs `"B1074CMD"`), and the unique partial index `idx_pk_current_nopol` only guarantees one current owner per *stored* plate, so the duplicate is invisible until the plate is normalized.
- One customer (id 2506, `nopol = '-'`) exists only as a self-service portal placeholder and owns no vehicle.

Lookups are split: staff and SPK enrichment fall back to `tb_pelanggan.nopol`, customer endpoints resolve through ownership, and `tb_penjualan` carries no plate at all. Removing the duplicated columns is necessary to make the junction the single source of truth, but the whole codebase and the existing frontend depend on a single `nopol`/`merk`/`jenis` per customer.

### Options Considered
- **Keep the columns, sync on every write**: Rejected — perpetuates the drift and the "one vehicle" constraint the junction was built to remove.
- **Drop the columns in one migration and rewrite every reader at once**: Rejected — high-risk big-bang change touching SPK enrichment, customer portal, reporting and the staff SPA simultaneously.
- **Make the junction authoritative and keep the columns as a compatibility view, removed later (chosen)**: a data reconciliation migration plus a read-compatibility layer, then delete the columns once all readers are migrated.

## Decision

### 1. `tb_pelanggan_kendaraan` is the single source of truth for a vehicle's plate
`tb_pelanggan.nopol` is deprecated and will be dropped, along with `merk` and `jenis`. Vehicle identity (plate, brand, model) lives on the ownership row and its `tb_kendaraan` master. A customer may own zero, one or many vehicles.

### 2. Canonical plate format
Plates are stored **uppercase with all whitespace removed** (e.g. `"B1019CYM"`). A shared `NopolNormalizer` canonicalizes every input at the repository boundary, and `V30` rewrites the existing junction values and creates an expression index on the normalized form.

### 3. Duplicate current owners are reconciled deterministically
For each normalized plate with more than one `is_current` row, keep the row whose plate best matches the previous `tb_pelanggan.nopol` value, breaking ties by earliest `tanggal_mulai` then lowest id; demote the rest (`is_current = false`). Legacy customers with a meaningful plate but no ownership row get one backfilled; the `'-'` portal placeholder is excluded.

### 4. Compatibility read layer keeps the single-vehicle API stable
While the columns still exist, a `PelangganVehicleResolver` fills `nopol`/`merk`/`jenis` from the customer's current ownership row (preferring the vehicle that matches the legacy plate; otherwise the most recently acquired). When a customer owns several vehicles, `PelangganDto.nopol` reflects that same primary vehicle. `GET`/`PUT /api/pazaauto/pelanggan/by-nopol/{nopol}` are retained and reimplemented on top of ownership, so the staff SPA needs no change.

### 5. Phased removal
- **Phase 0** — `V30` reconciles and normalizes plate data (non-destructive; columns retained).
- **Phase 1** — ownership-based repository lookups + `PelangganVehicleResolver` enrichment (this ADR).
- **Phase 2** — migrate remaining writers of `merk`/`jenis` on `tb_pelanggan` to the junction.
- **Phase 3** — drop `tb_pelanggan.nopol`, `merk`, `jenis` and remove the compatibility layer.

## Consequences
- **Positive**: One authoritative plate per vehicle; transfers and multi-vehicle ownership are unambiguous.
- **Positive**: Normalized plates make lookups tolerant of spacing/casing, fixing the 35 mismatched and 11 duplicated records.
- **Positive**: The compatibility layer and retained by-nopol endpoints keep existing frontends working throughout the migration.
- **Negative**: The `tb_pelanggan.nopol` column temporarily remains as derived data, so writers must keep it consistent until Phase 3.
- **Negative**: The `pelanggan-by-nopol` cache is keyed on the canonical plate and invalidated by every ownership write (attach, transfer, detach, master change); missing an invalidation hook would surface a stale owner.
- **Trade-off**: Phasing avoids a big-bang rewrite at the cost of running two plate representations for the duration of the migration.
