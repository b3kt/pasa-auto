-- Normalize vehicle brand out of tb_kendaraan into its own master table.
-- tb_merk_kendaraan: HONDA, TOYOTA, KIA ...
-- tb_kendaraan:      Mobilio, Brio, Yaris ... linked via merk_id.
-- tb_kendaraan.merk is kept (deprecated) and synced by the application
-- until all consumers read through merk_id; it will be dropped later.

CREATE TABLE IF NOT EXISTS tb_merk_kendaraan (
    id          BIGSERIAL PRIMARY KEY,
    nama        VARCHAR(50) NOT NULL,
    keterangan  VARCHAR(500),
    created_at  TIMESTAMP,
    created_by  VARCHAR(255),
    updated_at  TIMESTAMP,
    updated_by  VARCHAR(255),
    version     INTEGER DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_merk_kendaraan_nama
    ON tb_merk_kendaraan (lower(nama));

-- 1. Seed one merk per distinct brand (case/whitespace-insensitive), stored uppercase.
INSERT INTO tb_merk_kendaraan (nama, created_at)
SELECT DISTINCT UPPER(TRIM(k.merk)), NOW()
FROM tb_kendaraan k
WHERE k.merk IS NOT NULL
  AND TRIM(k.merk) <> ''
  AND NOT EXISTS (
      SELECT 1 FROM tb_merk_kendaraan m WHERE lower(m.nama) = lower(TRIM(k.merk))
  );

-- 2. Link tb_kendaraan to the merk master.
ALTER TABLE tb_kendaraan ADD COLUMN IF NOT EXISTS merk_id BIGINT REFERENCES tb_merk_kendaraan(id);
CREATE INDEX IF NOT EXISTS idx_kendaraan_merk_id ON tb_kendaraan (merk_id);

-- 3. Backfill merk_id and normalize the deprecated merk string to the master name.
UPDATE tb_kendaraan k
SET merk_id = m.id,
    merk    = m.nama
FROM tb_merk_kendaraan m
WHERE lower(TRIM(k.merk)) = lower(m.nama)
  AND k.merk_id IS NULL;

COMMENT ON COLUMN tb_kendaraan.merk IS 'DEPRECATED: use merk_id -> tb_merk_kendaraan. Kept in sync by the application; will be removed.';

-- 4. Audit trail.
DROP TRIGGER IF EXISTS trg_audit_trail_insert ON tb_merk_kendaraan;
DROP TRIGGER IF EXISTS trg_audit_trail_update ON tb_merk_kendaraan;
DROP TRIGGER IF EXISTS trg_audit_trail_delete ON tb_merk_kendaraan;
CREATE TRIGGER trg_audit_trail_insert AFTER INSERT ON tb_merk_kendaraan FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();
CREATE TRIGGER trg_audit_trail_update AFTER UPDATE ON tb_merk_kendaraan FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();
CREATE TRIGGER trg_audit_trail_delete AFTER DELETE ON tb_merk_kendaraan FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();
