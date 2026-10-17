-- ADR-0011 expand step: normalize customer data across tb_pelanggan, tb_pelanggan_kendaraan and
-- tb_customer_accounts without breaking the previous application build.
--
-- Everything here is additive, a relaxation, a NOT VALID constraint, or a backfill the previous build
-- can still read. The destructive half (validating constraints, dropping tb_pelanggan.nopol/merk/
-- jenis/tlpn) is the contract step, V36, shipped at least one release later.
--
-- Ownership:
--   tb_customer_accounts   - credentials only (login email/phone, password, active, 1:1 link)
--   tb_pelanggan           - who the customer is (name, contacts, address, provenance)
--   tb_pelanggan_kendaraan - who owned which plate, and when (authoritative for vehicles)
--
-- Canonical formats: plate = UPPERCASE without whitespace (V30 / NopolNormalizer);
-- phone = digits only in national form, a leading 62/620 becomes 0 (PhoneNormalizer).

-- 1. tb_pelanggan: provenance + merge pointer; vehicle columns become nullable -----------------

ALTER TABLE tb_pelanggan
    ADD COLUMN IF NOT EXISTS sumber         VARCHAR(10) NOT NULL DEFAULT 'STAFF',
    ADD COLUMN IF NOT EXISTS merged_into_id BIGINT REFERENCES tb_pelanggan(id),
    ALTER COLUMN nopol DROP NOT NULL,
    ALTER COLUMN merk  DROP NOT NULL;

ALTER TABLE tb_pelanggan
    ADD CONSTRAINT chk_pelanggan_sumber CHECK (sumber IN ('STAFF', 'PORTAL'));

CREATE INDEX IF NOT EXISTS idx_pelanggan_merged_into
    ON tb_pelanggan (merged_into_id) WHERE merged_into_id IS NOT NULL;

-- Portal-originated records: the '-' placeholder (only ever created by the customer portal), or a
-- record written by a customer request (the audit listener stores the customer JWT subject, i.e.
-- the account id, in created_by).
UPDATE tb_pelanggan p
SET sumber = 'PORTAL'
WHERE btrim(coalesce(p.nopol, '')) IN ('', '-')
   OR EXISTS (SELECT 1 FROM tb_customer_accounts a WHERE p.created_by = a.id::text);

-- 2. Preserve legacy plates in the ownership history before tb_pelanggan.nopol becomes derived --
--    A live customer whose legacy plate has no ownership row of their own would otherwise lose
--    that plate when the column is recomputed below (and dropped in V36).

INSERT INTO tb_kendaraan (merk, merk_id, jenis, model, keterangan, created_at)
SELECT DISTINCT p.merk,
       (SELECT m.id FROM tb_merk_kendaraan m WHERE m.nama = p.merk ORDER BY m.id LIMIT 1),
       COALESCE(p.jenis, ''), NULL, NULL, NOW()
FROM tb_pelanggan p
WHERE p.deleted_at IS NULL
  AND p.merk IS NOT NULL
  AND regexp_replace(upper(coalesce(p.nopol, '')), '[[:space:]]', '', 'g') NOT IN ('', '-')
  AND NOT EXISTS (SELECT 1 FROM tb_pelanggan_kendaraan k
                  WHERE k.id_pelanggan = p.id AND k.deleted_at IS NULL
                    AND k.nopol = regexp_replace(upper(p.nopol), '[[:space:]]', '', 'g'))
  AND NOT EXISTS (SELECT 1 FROM tb_kendaraan kd
                  WHERE kd.merk = p.merk AND kd.jenis = COALESCE(p.jenis, ''));

CREATE TEMP TABLE tmp_v35_orphan_plates AS
SELECT p.id AS id_pelanggan,
       regexp_replace(upper(p.nopol), '[[:space:]]', '', 'g') AS nopol,
       (SELECT kd.id FROM tb_kendaraan kd
        WHERE kd.merk = p.merk AND kd.jenis = COALESCE(p.jenis, '')
        ORDER BY kd.id LIMIT 1) AS id_kendaraan,
       LEAST(COALESCE(p.tanggal_join, CURRENT_DATE), CURRENT_DATE) AS tanggal_mulai
FROM tb_pelanggan p
WHERE p.deleted_at IS NULL
  AND regexp_replace(upper(coalesce(p.nopol, '')), '[[:space:]]', '', 'g') NOT IN ('', '-')
  AND NOT EXISTS (SELECT 1 FROM tb_pelanggan_kendaraan k
                  WHERE k.id_pelanggan = p.id AND k.deleted_at IS NULL
                    AND k.nopol = regexp_replace(upper(p.nopol), '[[:space:]]', '', 'g'));

-- 2a. Plates nobody currently owns: the earliest such customer becomes the current owner.
INSERT INTO tb_pelanggan_kendaraan (id_pelanggan, id_kendaraan, nopol, tanggal_mulai, is_current, keterangan, created_at)
SELECT DISTINCT ON (o.nopol)
       o.id_pelanggan, o.id_kendaraan, o.nopol, o.tanggal_mulai, true, 'nopol lama tb_pelanggan (V35)', NOW()
FROM tmp_v35_orphan_plates o
WHERE o.id_kendaraan IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tb_pelanggan_kendaraan k
                  WHERE k.nopol = o.nopol AND k.is_current = true AND k.deleted_at IS NULL)
ORDER BY o.nopol, o.tanggal_mulai, o.id_pelanggan;

-- 2b. Everyone else keeps the plate as a closed (historical) ownership row.
INSERT INTO tb_pelanggan_kendaraan (id_pelanggan, id_kendaraan, nopol, tanggal_mulai, tanggal_akhir, is_current, keterangan, created_at)
SELECT o.id_pelanggan, o.id_kendaraan, o.nopol, o.tanggal_mulai, CURRENT_DATE, false,
       'nopol lama tb_pelanggan (V35)', NOW()
FROM tmp_v35_orphan_plates o
WHERE o.id_kendaraan IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tb_pelanggan_kendaraan k
                  WHERE k.id_pelanggan = o.id_pelanggan AND k.nopol = o.nopol AND k.deleted_at IS NULL);

DROP TABLE tmp_v35_orphan_plates;

-- 3. Sentinels become NULL (the previous build treats NULL and '-' alike) ----------------------

UPDATE tb_pelanggan SET nopol = NULL WHERE btrim(nopol) IN ('', '-');
UPDATE tb_pelanggan SET merk  = NULL WHERE btrim(merk)  IN ('', '-');
UPDATE tb_pelanggan SET jenis = NULL WHERE btrim(jenis) IN ('', '-');

-- 4. Phones -------------------------------------------------------------------------------------

-- 4a. Fold the legacy tlpn into the typed columns where they are empty (tlpn itself is untouched).
UPDATE tb_pelanggan SET no_hp = tlpn
WHERE btrim(coalesce(no_hp, '')) = ''
  AND tlpn ~ '^[[:space:]]*(\+?62|0)[[:space:]-]*8';

UPDATE tb_pelanggan SET no_telepon = tlpn
WHERE btrim(coalesce(no_telepon, '')) = ''
  AND btrim(coalesce(tlpn, '')) <> ''
  AND tlpn IS DISTINCT FROM no_hp;

-- 4b. Canonicalize. tlpn keeps its legacy value; the application mirrors it from no_telepon on
--     every write from now on, and V36 drops it.
UPDATE tb_pelanggan
SET no_hp      = NULLIF(regexp_replace(regexp_replace(no_hp,      '[^0-9]', '', 'g'), '^620?', '0'), ''),
    no_telepon = NULLIF(regexp_replace(regexp_replace(no_telepon, '[^0-9]', '', 'g'), '^620?', '0'), '')
WHERE no_hp IS NOT NULL OR no_telepon IS NOT NULL;

-- 4c. Account phones: canonicalize only where it does not collide with another account under
--     idx_tb_customer_accounts_phone. Colliding accounts are left as-is for manual resolution
--     before V36 adds the canonical-format CHECK.
WITH canon AS (
    SELECT id, NULLIF(regexp_replace(regexp_replace(phone, '[^0-9]', '', 'g'), '^620?', '0'), '') AS phone_norm
    FROM tb_customer_accounts
    WHERE phone IS NOT NULL
)
UPDATE tb_customer_accounts a
SET phone = c.phone_norm
FROM canon c
WHERE a.id = c.id
  AND a.phone IS DISTINCT FROM c.phone_norm
  AND NOT EXISTS (SELECT 1 FROM canon c2 WHERE c2.phone_norm = c.phone_norm AND c2.id <> c.id);

CREATE INDEX IF NOT EXISTS idx_pelanggan_no_hp       ON tb_pelanggan (no_hp)        WHERE no_hp IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_pelanggan_no_telepon  ON tb_pelanggan (no_telepon)   WHERE no_telepon IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_pelanggan_email_lower ON tb_pelanggan (lower(email)) WHERE email IS NOT NULL;

-- 5. Ownership integrity ------------------------------------------------------------------------

-- Closed rows always carry an end date (V34 step 1a closed some without one).
UPDATE tb_pelanggan_kendaraan
SET tanggal_akhir = GREATEST(tanggal_mulai, COALESCE(updated_at::date, CURRENT_DATE))
WHERE is_current = false AND tanggal_akhir IS NULL;

-- Checked for new writes now, validated against existing rows in V36. The period check
-- (tanggal_akhir >= tanggal_mulai) is deferred to V36 entirely: the previous build closes a
-- same-day ownership with "start - 1 day", which it would reject.
ALTER TABLE tb_pelanggan_kendaraan
    ADD CONSTRAINT chk_pk_nopol_canonical
        CHECK (nopol = upper(regexp_replace(nopol, '[[:space:]]', '', 'g'))) NOT VALID,
    ADD CONSTRAINT chk_pk_current_open
        CHECK (NOT is_current OR tanggal_akhir IS NULL) NOT VALID;

-- Soft deletes everywhere: a hard delete of a customer must not silently erase ownership history.
DO $$
DECLARE
    fk_name text;
BEGIN
    FOR fk_name IN
        SELECT c.conname
        FROM pg_constraint c
        JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = ANY (c.conkey)
        WHERE c.conrelid = 'tb_pelanggan_kendaraan'::regclass
          AND c.contype = 'f'
          AND c.confrelid = 'tb_pelanggan'::regclass
          AND a.attname = 'id_pelanggan'
    LOOP
        EXECUTE format('ALTER TABLE tb_pelanggan_kendaraan DROP CONSTRAINT %I', fk_name);
    END LOOP;
END $$;

ALTER TABLE tb_pelanggan_kendaraan
    ADD CONSTRAINT tb_pelanggan_kendaraan_id_pelanggan_fkey
        FOREIGN KEY (id_pelanggan) REFERENCES tb_pelanggan(id) ON DELETE RESTRICT;

-- 6. Referential integrity for transactional tables (new rows now, existing rows in V36) -------

ALTER TABLE tb_spk
    ADD CONSTRAINT fk_spk_pelanggan FOREIGN KEY (id_pelanggan) REFERENCES tb_pelanggan(id) NOT VALID;
ALTER TABLE tb_penjualan
    ADD CONSTRAINT fk_penjualan_pelanggan FOREIGN KEY (id_pelanggan) REFERENCES tb_pelanggan(id) NOT VALID;

-- 7. Single writer for the deprecated vehicle columns until V36 drops them ----------------------
--    Primary vehicle = the current ownership row with the latest tanggal_mulai, ties to the highest
--    id. PelangganVehicleResolver and v_pelanggan_legacy use the same rule.

CREATE OR REPLACE FUNCTION fn_pk_sync_legacy_vehicle() RETURNS trigger AS $$
DECLARE
    pid BIGINT;
BEGIN
    FOR pid IN
        SELECT DISTINCT x
        FROM unnest(ARRAY[
                CASE WHEN TG_OP <> 'INSERT' THEN OLD.id_pelanggan END,
                CASE WHEN TG_OP <> 'DELETE' THEN NEW.id_pelanggan END]) AS x
        WHERE x IS NOT NULL
    LOOP
        UPDATE tb_pelanggan p
        SET nopol = pv.nopol,
            merk  = pv.merk,
            jenis = pv.jenis
        FROM (SELECT v.nopol, v.merk, v.jenis
              FROM (SELECT 1) AS one
              LEFT JOIN LATERAL (
                  SELECT k.nopol,
                         COALESCE(NULLIF(kd.merk, ''), m.nama) AS merk,
                         NULLIF(kd.jenis, '')                  AS jenis
                  FROM tb_pelanggan_kendaraan k
                  JOIN tb_kendaraan kd ON kd.id = k.id_kendaraan
                  LEFT JOIN tb_merk_kendaraan m ON m.id = kd.merk_id
                  WHERE k.id_pelanggan = pid AND k.is_current = true AND k.deleted_at IS NULL
                  ORDER BY k.tanggal_mulai DESC NULLS LAST, k.id DESC
                  LIMIT 1) v ON true) pv
        WHERE p.id = pid
          AND (p.nopol, p.merk, p.jenis) IS DISTINCT FROM (pv.nopol, pv.merk, pv.jenis);
    END LOOP;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_pk_sync_legacy_vehicle
    AFTER INSERT OR UPDATE OR DELETE ON tb_pelanggan_kendaraan
    FOR EACH ROW EXECUTE FUNCTION fn_pk_sync_legacy_vehicle();

-- One-time recompute so every live customer follows the rule from day one. Customers without a
-- current vehicle end up with NULL nopol/merk/jenis; their plates are kept in the history (step 2).
UPDATE tb_pelanggan p
SET nopol = pv.nopol,
    merk  = pv.merk,
    jenis = pv.jenis
FROM (SELECT p2.id, v.nopol, v.merk, v.jenis
      FROM tb_pelanggan p2
      LEFT JOIN LATERAL (
          SELECT k.nopol,
                 COALESCE(NULLIF(kd.merk, ''), m.nama) AS merk,
                 NULLIF(kd.jenis, '')                  AS jenis
          FROM tb_pelanggan_kendaraan k
          JOIN tb_kendaraan kd ON kd.id = k.id_kendaraan
          LEFT JOIN tb_merk_kendaraan m ON m.id = kd.merk_id
          WHERE k.id_pelanggan = p2.id AND k.is_current = true AND k.deleted_at IS NULL
          ORDER BY k.tanggal_mulai DESC NULLS LAST, k.id DESC
          LIMIT 1) v ON true
      WHERE p2.deleted_at IS NULL) pv
WHERE p.id = pv.id
  AND (p.nopol, p.merk, p.jenis) IS DISTINCT FROM (pv.nopol, pv.merk, pv.jenis);

-- 8. Stable contract for SQL/report consumers; does not reference the columns V36 drops ---------

CREATE OR REPLACE VIEW v_pelanggan_legacy AS
SELECT p.id,
       p.nama_pelanggan,
       pv.nopol,
       pv.merk,
       pv.jenis,
       p.no_telepon AS tlpn,
       p.no_hp,
       p.no_telepon,
       p.email,
       p.contact_person,
       p.alamat,
       p.kota,
       p.kode_pos,
       p.jenis_kelamin,
       p.tanggal_join,
       p.keterangan,
       p.sumber,
       p.merged_into_id,
       p.created_at,
       p.created_by,
       p.updated_at,
       p.updated_by,
       p.deleted_at
FROM tb_pelanggan p
LEFT JOIN LATERAL (
    SELECT k.nopol,
           COALESCE(NULLIF(kd.merk, ''), m.nama) AS merk,
           NULLIF(kd.jenis, '')                  AS jenis
    FROM tb_pelanggan_kendaraan k
    JOIN tb_kendaraan kd ON kd.id = k.id_kendaraan
    LEFT JOIN tb_merk_kendaraan m ON m.id = kd.merk_id
    WHERE k.id_pelanggan = p.id AND k.is_current = true AND k.deleted_at IS NULL
    ORDER BY k.tanggal_mulai DESC NULLS LAST, k.id DESC
    LIMIT 1) pv ON true;

-- 9. Document intent -----------------------------------------------------------------------------

COMMENT ON COLUMN tb_pelanggan.nopol IS
    'DEPRECATED (ADR-0010/0011): derived from tb_pelanggan_kendaraan by trg_pk_sync_legacy_vehicle; dropped in V36.';
COMMENT ON COLUMN tb_pelanggan.merk IS
    'DEPRECATED (ADR-0010/0011): derived from tb_pelanggan_kendaraan by trg_pk_sync_legacy_vehicle; dropped in V36.';
COMMENT ON COLUMN tb_pelanggan.jenis IS
    'DEPRECATED (ADR-0010/0011): derived from tb_pelanggan_kendaraan by trg_pk_sync_legacy_vehicle; dropped in V36.';
COMMENT ON COLUMN tb_pelanggan.tlpn IS
    'DEPRECATED (ADR-0011): alias of no_telepon, mirrored by the application on every write; dropped in V36.';
COMMENT ON COLUMN tb_pelanggan.no_hp IS 'Mobile phone, canonical national digits (e.g. 081234567890).';
COMMENT ON COLUMN tb_pelanggan.no_telepon IS 'Landline phone, canonical national digits (e.g. 0215551234).';
COMMENT ON COLUMN tb_pelanggan.sumber IS 'Record origin: STAFF (workshop data entry) or PORTAL (customer self-registration).';
COMMENT ON COLUMN tb_pelanggan.merged_into_id IS 'Set when this record was merged into another; resolve stale references through it.';
COMMENT ON COLUMN tb_customer_accounts.full_name IS
    'Display name before the account is linked; tb_pelanggan.nama_pelanggan is authoritative once linked.';
COMMENT ON COLUMN tb_spk.nopol IS 'Snapshot of the plate at SPK time; not kept in sync with tb_pelanggan_kendaraan.';
COMMENT ON COLUMN tb_spk.nama_pelanggan IS 'Snapshot of the customer name at SPK time.';
COMMENT ON VIEW v_pelanggan_legacy IS
    'Pre-ADR-0011 tb_pelanggan shape (nopol/merk/jenis/tlpn derived) for reports and ad-hoc SQL.';
