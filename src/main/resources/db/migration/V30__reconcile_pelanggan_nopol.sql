-- Phase 0 of the tb_pelanggan.nopol removal: reconcile the vehicle plate data so that
-- tb_pelanggan_kendaraan can become the single source of truth.
--
-- Canonical plate format: UPPERCASE with all whitespace removed, e.g. 'B1019CYM'.
-- This is a data-only migration; the deprecated tb_pelanggan.nopol column is kept in sync
-- so the compatibility read layer (Phase 1) keeps returning the same values.

-- 1. Demote duplicate *current* owners that would collide once the plate is normalized.
--    Keep the row whose plate best matches the customer's own plate (after normalization),
--    breaking ties by earliest ownership start and then lowest id. This resolves the
--    whitespace/junk variants (e.g. 'B .' vs 'B.', 'B 1074 CMD' vs 'B1074CMD').
WITH ranked AS (
    SELECT k.id,
           row_number() OVER (
               PARTITION BY regexp_replace(upper(k.nopol), '[[:space:]]', '', 'g')
               ORDER BY
                   (CASE
                        WHEN regexp_replace(upper(k.nopol), '[[:space:]]', '', 'g')
                             = regexp_replace(upper(coalesce(p.nopol, '')), '[[:space:]]', '', 'g')
                        THEN 0 ELSE 1
                    END),
                   k.tanggal_mulai ASC NULLS LAST,
                   k.id ASC
           ) AS rn
    FROM tb_pelanggan_kendaraan k
    JOIN tb_pelanggan p ON p.id = k.id_pelanggan
    WHERE k.is_current = true AND k.deleted_at IS NULL
)
UPDATE tb_pelanggan_kendaraan k
SET is_current   = false,
    tanggal_akhir = COALESCE(k.tanggal_akhir, CURRENT_DATE),
    keterangan   = COALESCE(NULLIF(k.keterangan, ''), 'duplikat nopol dinormalisasi'),
    updated_at   = NOW()
FROM ranked r
WHERE k.id = r.id AND r.rn > 1;

-- 2. Normalize the stored plate to the canonical compact uppercase form.
UPDATE tb_pelanggan_kendaraan
SET nopol      = regexp_replace(upper(nopol), '[[:space:]]', '', 'g'),
    updated_at = NOW()
WHERE nopol <> regexp_replace(upper(nopol), '[[:space:]]', '', 'g');

-- 3. Keep the deprecated tb_pelanggan.nopol aligned with the authoritative junction value
--    (still read by the Phase 1 compatibility layer and by legacy queries).
UPDATE tb_pelanggan p
SET nopol      = k.nopol,
    updated_at = NOW()
FROM tb_pelanggan_kendaraan k
WHERE k.id_pelanggan = p.id
  AND k.is_current = true
  AND k.deleted_at IS NULL
  AND p.nopol IS DISTINCT FROM k.nopol;

-- 4. Backfill a current ownership row for legacy customers that still carry a real plate but
--    have no current ownership row. Skip the self-service placeholder ('-') and any plate that
--    is already owned by another customer (those are handled by the de-duplication above).
INSERT INTO tb_kendaraan (merk, jenis, model, keterangan, created_at)
SELECT DISTINCT p.merk, COALESCE(p.jenis, ''), NULL, NULL, NOW()
FROM tb_pelanggan p
WHERE p.deleted_at IS NULL
  AND p.merk IS NOT NULL
  AND btrim(p.nopol) <> '' AND btrim(p.nopol) <> '-'
  AND NOT EXISTS (SELECT 1 FROM tb_pelanggan_kendaraan k
                  WHERE k.id_pelanggan = p.id AND k.is_current = true AND k.deleted_at IS NULL)
  AND NOT EXISTS (SELECT 1 FROM tb_kendaraan kd
                  WHERE kd.merk = p.merk AND kd.jenis = COALESCE(p.jenis, ''));

INSERT INTO tb_pelanggan_kendaraan (id_pelanggan, id_kendaraan, nopol, tanggal_mulai, is_current, created_at)
SELECT p.id,
       (SELECT kd.id FROM tb_kendaraan kd
        WHERE kd.merk = p.merk AND kd.jenis = COALESCE(p.jenis, '')
        ORDER BY kd.id LIMIT 1),
       regexp_replace(upper(p.nopol), '[[:space:]]', '', 'g'),
       COALESCE(p.tanggal_join, CURRENT_DATE),
       true,
       NOW()
FROM tb_pelanggan p
WHERE p.deleted_at IS NULL
  AND btrim(p.nopol) <> '' AND btrim(p.nopol) <> '-'
  AND NOT EXISTS (SELECT 1 FROM tb_pelanggan_kendaraan k
                  WHERE k.id_pelanggan = p.id AND k.is_current = true AND k.deleted_at IS NULL)
  AND NOT EXISTS (SELECT 1 FROM tb_pelanggan_kendaraan k2
                  WHERE regexp_replace(upper(k2.nopol), '[[:space:]]', '', 'g')
                        = regexp_replace(upper(p.nopol), '[[:space:]]', '', 'g')
                    AND k2.is_current = true AND k2.deleted_at IS NULL);

-- 5. Index the canonical form so normalized lookups stay fast.
CREATE INDEX IF NOT EXISTS idx_pk_nopol_norm
    ON tb_pelanggan_kendaraan (regexp_replace(upper(nopol), '[[:space:]]', '', 'g'));
