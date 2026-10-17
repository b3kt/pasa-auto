-- Removing a vehicle soft-deleted its ownership row but left is_current = true, so the partial
-- unique index idx_pk_current_nopol kept the plate taken and re-adding it failed.

-- 1. Close the soft-deleted rows that are still flagged current.
UPDATE tb_pelanggan_kendaraan
SET is_current    = false,
    tanggal_akhir = GREATEST(tanggal_mulai, COALESCE(deleted_at::date, CURRENT_DATE))
WHERE is_current = true
  AND deleted_at IS NOT NULL;

-- 2. A deleted row never holds the plate, whatever its flag says.
DROP INDEX IF EXISTS idx_pk_current_nopol;
CREATE UNIQUE INDEX idx_pk_current_nopol
    ON tb_pelanggan_kendaraan (nopol) WHERE is_current = true AND deleted_at IS NULL;
