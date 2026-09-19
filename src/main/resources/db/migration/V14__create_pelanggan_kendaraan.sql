-- Normalize customer-vehicle relationship with ownership history.
-- tb_kendaraan stays a vehicle master (merk/jenis/model, no nopol).
-- tb_pelanggan_kendaraan tracks which customer owns which nopol (vehicle instance)
-- over time, enabling vehicle transfer and historical queries.

CREATE TABLE IF NOT EXISTS tb_pelanggan_kendaraan (
    id            BIGSERIAL PRIMARY KEY,
    id_pelanggan  BIGINT NOT NULL REFERENCES tb_pelanggan(id) ON DELETE CASCADE,
    id_kendaraan  BIGINT NOT NULL REFERENCES tb_kendaraan(id),
    nopol         VARCHAR(10) NOT NULL,
    tanggal_mulai DATE NOT NULL DEFAULT CURRENT_DATE,
    tanggal_akhir DATE,
    is_current    BOOLEAN NOT NULL DEFAULT true,
    keterangan    VARCHAR(500),
    created_at    TIMESTAMP,
    created_by    VARCHAR(255),
    updated_at    TIMESTAMP,
    updated_by    VARCHAR(255),
    version       INTEGER DEFAULT 0
);

-- At most one current owner per nopol.
CREATE UNIQUE INDEX IF NOT EXISTS idx_pk_current_nopol
    ON tb_pelanggan_kendaraan (nopol) WHERE is_current = true;

CREATE INDEX IF NOT EXISTS idx_pk_pelanggan
    ON tb_pelanggan_kendaraan (id_pelanggan);
CREATE INDEX IF NOT EXISTS idx_pk_kendaraan
    ON tb_pelanggan_kendaraan (id_kendaraan);

-- 1. Ensure a master exists for each distinct (merk, jenis) in tb_pelanggan.
--    jenis is NOT NULL on tb_kendaraan; normalize NULL jenis to '' (matches
--    findByMerkAndJenis's normalization).
INSERT INTO tb_kendaraan (merk, jenis, model, keterangan, created_at)
SELECT DISTINCT p.merk, COALESCE(p.jenis, ''), NULL, NULL, NOW()
FROM tb_pelanggan p
WHERE p.merk IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM tb_kendaraan k
      WHERE k.merk = p.merk AND k.jenis = COALESCE(p.jenis, '')
  );

-- 2. Seed ownership from existing pelanggan rows.
INSERT INTO tb_pelanggan_kendaraan (id_pelanggan, id_kendaraan, nopol, tanggal_mulai, is_current)
SELECT p.id, k.id, p.nopol, COALESCE(p.tanggal_join, CURRENT_DATE), true
FROM tb_pelanggan p
JOIN LATERAL (
    SELECT id FROM tb_kendaraan
    WHERE merk = p.merk AND jenis = COALESCE(p.jenis, '')
    ORDER BY id LIMIT 1
) k ON true
WHERE p.nopol IS NOT NULL;

-- 3. Add nullable link to vehicle master on tb_spk (mirrors tb_penjualan.id_kendaraan,
--    which already exists from V1).
ALTER TABLE tb_spk ADD COLUMN IF NOT EXISTS id_kendaraan BIGINT REFERENCES tb_kendaraan(id);
CREATE INDEX IF NOT EXISTS idx_spk_kendaraan ON tb_spk (id_kendaraan);

-- 4. Backfill tb_spk.id_kendaraan (nullable link to vehicle master).
UPDATE tb_spk s
SET id_kendaraan = pk.id_kendaraan
FROM tb_pelanggan_kendaraan pk
WHERE pk.nopol = s.nopol AND s.id_kendaraan IS NULL;

-- 5. Backfill tb_penjualan.id_kendaraan where missing.
UPDATE tb_penjualan pen
SET id_kendaraan = s.id_kendaraan
FROM tb_spk s
WHERE pen.no_spk = s.no_spk
  AND pen.id_kendaraan IS NULL
  AND s.id_kendaraan IS NOT NULL;