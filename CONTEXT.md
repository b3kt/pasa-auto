# Pasa Auto — Domain Context

## Core domain concepts

### Pelanggan (Customer / Client)
A workshop customer, identified by their vehicle plate number (nopol). Every Pelanggan has at least one vehicle (identified by merk + jenis) and a name. The same person may return with different vehicles — the canonical record is the latest-updated nopol match.

Invariants:
- `nopol` is required and non-blank
- `namaPelanggan` is required and non-blank
- Lookup by `nopol` returns the most recently updated record

### Spk (Surat Perintah Kerja — Service Work Order)
The core transactional unit. Status lifecycle: `OPEN` → `PROSES` → `SELESAI`. Contains detail lines for services (jasa) and parts (barang).

### Pembelian (Purchase)
Procurement of goods, with types SPAREPART, BARANG, OPERASIONAL. Has stock integration: when a SPAREPART-type purchase is created, sparepart stock increases.

### Penjualan (Sales)
Sale created from a completed SPK. Invariant: noSpk must be unique across penjualan records.
