-- Normalize self-service customer data created before the add-vehicle flow learned to adopt an
-- existing on-record customer.
--
-- Historically a customer who registered (or skipped linking) got a placeholder tb_pelanggan
-- (nopol = '-') and any plate they added was attached to that placeholder, even when a matching
-- on-record tb_pelanggan already existed. The registration flow now reconciles against the
-- ownership junction and the legacy tb_pelanggan.nopol, adopting the on-record customer instead of
-- duplicating it. This is a data-only migration that replays that behaviour for existing accounts:
--
--   1. Adopt: an account linked to a placeholder that owns a plate which also exists on another,
--      unclaimed on-record customer is moved onto that record (its ownership history follows it)
--      and the emptied placeholder is soft-deleted.
--   2. Promote: a placeholder that owns vehicles but matches no on-record customer is turned into a
--      real record by copying its primary (earliest) vehicle's plate and vehicle master.
-- Placeholders that own no vehicles are left as-is.
--
-- Canonical plate format: UPPERCASE with all whitespace removed (matches V30 / NopolNormalizer).
-- The deprecated tb_pelanggan.nopol column is kept aligned with the authoritative junction.

-- 1. Adopt -------------------------------------------------------------------------------------

CREATE TABLE tmp_v34_adopt (account_id bigint, placeholder_id bigint, target_id bigint);

INSERT INTO tmp_v34_adopt (account_id, placeholder_id, target_id)
WITH placeholder_accounts AS (
    SELECT a.id AS account_id, p.id AS placeholder_id
    FROM tb_customer_accounts a
    JOIN tb_pelanggan p ON p.id = a.id_pelanggan
    WHERE a.deleted_at IS NULL
      AND p.deleted_at IS NULL
      AND regexp_replace(upper(coalesce(p.nopol, '')), '[[:space:]]', '', 'g') IN ('', '-')
),
candidates AS (
    SELECT pa.account_id,
           pa.placeholder_id,
           lp.id AS target_id,
           row_number() OVER (
               PARTITION BY pa.account_id
               ORDER BY k.tanggal_mulai ASC NULLS LAST, lp.id ASC
           ) AS rn
    FROM placeholder_accounts pa
    JOIN tb_pelanggan_kendaraan k
      ON k.id_pelanggan = pa.placeholder_id
     AND k.is_current = true
     AND k.deleted_at IS NULL
    JOIN tb_pelanggan lp
      ON lp.deleted_at IS NULL
     AND lp.id <> pa.placeholder_id
     AND regexp_replace(upper(coalesce(lp.nopol, '')), '[[:space:]]', '', 'g')
         = regexp_replace(upper(k.nopol), '[[:space:]]', '', 'g')
    WHERE regexp_replace(upper(coalesce(lp.nopol, '')), '[[:space:]]', '', 'g') NOT IN ('', '-')
      AND NOT EXISTS (SELECT 1 FROM tb_customer_accounts a2
                      WHERE a2.id_pelanggan = lp.id AND a2.deleted_at IS NULL)
)
SELECT account_id, placeholder_id, target_id
FROM candidates
WHERE rn = 1;

-- 1a. Close placeholder ownership rows that would duplicate a plate already current on the target.
UPDATE tb_pelanggan_kendaraan k
SET is_current = false,
    deleted_at = NOW(),
    updated_at = NOW()
FROM tmp_v34_adopt t
WHERE k.id_pelanggan = t.placeholder_id
  AND k.deleted_at IS NULL
  AND k.is_current = true
  AND EXISTS (SELECT 1 FROM tb_pelanggan_kendaraan k2
              WHERE k2.id_pelanggan = t.target_id
                AND k2.deleted_at IS NULL
                AND k2.is_current = true
                AND regexp_replace(upper(k2.nopol), '[[:space:]]', '', 'g')
                    = regexp_replace(upper(k.nopol), '[[:space:]]', '', 'g'));

-- 1b. Move the remaining placeholder ownership rows to the adopted record.
UPDATE tb_pelanggan_kendaraan k
SET id_pelanggan = t.target_id,
    updated_at = NOW()
FROM tmp_v34_adopt t
WHERE k.id_pelanggan = t.placeholder_id
  AND k.deleted_at IS NULL;

-- 1c. Repoint the accounts onto the adopted record.
UPDATE tb_customer_accounts a
SET id_pelanggan = t.target_id,
    linked_at = NOW(),
    updated_at = NOW()
FROM tmp_v34_adopt t
WHERE a.id = t.account_id
  AND a.deleted_at IS NULL;

-- 1d. Fill blanks on the adopted record from the account (never overwrite on-record data).
UPDATE tb_pelanggan p
SET no_hp = COALESCE(NULLIF(p.no_hp, ''), a.phone),
    tlpn = COALESCE(NULLIF(p.tlpn, ''), a.phone),
    email = COALESCE(NULLIF(p.email, ''), a.email),
    updated_at = NOW()
FROM tmp_v34_adopt t
JOIN tb_customer_accounts a ON a.id = t.account_id
WHERE p.id = t.target_id;

-- 1e. Soft-delete placeholders left without any ownership row.
UPDATE tb_pelanggan p
SET deleted_at = NOW(),
    updated_at = NOW()
FROM tmp_v34_adopt t
WHERE p.id = t.placeholder_id
  AND NOT EXISTS (SELECT 1 FROM tb_pelanggan_kendaraan k
                  WHERE k.id_pelanggan = p.id AND k.deleted_at IS NULL);

DROP TABLE tmp_v34_adopt;

-- 2. Promote ------------------------------------------------------------------------------------

WITH primary_vehicle AS (
    SELECT DISTINCT ON (k.id_pelanggan)
           k.id_pelanggan,
           k.nopol,
           k.id_kendaraan
    FROM tb_pelanggan_kendaraan k
    JOIN tb_pelanggan p ON p.id = k.id_pelanggan AND p.deleted_at IS NULL
    WHERE k.is_current = true
      AND k.deleted_at IS NULL
      AND regexp_replace(upper(coalesce(p.nopol, '')), '[[:space:]]', '', 'g') IN ('', '-')
    ORDER BY k.id_pelanggan, k.tanggal_mulai ASC NULLS LAST, k.id ASC
)
UPDATE tb_pelanggan p
SET nopol = pv.nopol,
    merk = COALESCE(NULLIF(kd.merk, ''), NULLIF(p.merk, '-'), p.merk),
    jenis = COALESCE(NULLIF(kd.jenis, ''), NULLIF(p.jenis, '-'), p.jenis),
    updated_at = NOW()
FROM primary_vehicle pv
LEFT JOIN tb_kendaraan kd ON kd.id = pv.id_kendaraan
WHERE p.id = pv.id_pelanggan;
