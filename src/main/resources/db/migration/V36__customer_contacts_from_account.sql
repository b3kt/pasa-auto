-- ADR-0012: a customer linked to a portal account is reached through that account.
-- tb_customer_accounts.phone/email are authoritative for linked customers; tb_pelanggan.no_hp/email
-- remain only as the contact of walk-in customers without an account, and the landline
-- (tb_pelanggan.no_telepon, tlpn) is deprecated: no longer written or shown, dropped by the
-- contract migration together with the ADR-0011 columns.
--
-- Rollback-safe: data is only added to tb_customer_accounts.phone where it was empty, and nothing is
-- removed from tb_pelanggan.

-- 1. Carry a linked customer's mobile number over to an account that has no phone yet. Skipped when
--    another account already uses the number (idx_tb_customer_accounts_phone is unique) or when two
--    linked customers share it; those stay on tb_pelanggan for manual review.
WITH candidates AS (
    SELECT a.id AS account_id, p.no_hp
    FROM tb_customer_accounts a
    JOIN tb_pelanggan p ON p.id = a.id_pelanggan
    WHERE a.deleted_at IS NULL
      AND a.phone IS NULL
      AND p.no_hp IS NOT NULL
)
UPDATE tb_customer_accounts a
SET phone      = c.no_hp,
    updated_at = NOW()
FROM candidates c
WHERE a.id = c.account_id
  AND NOT EXISTS (SELECT 1 FROM tb_customer_accounts other WHERE other.phone = c.no_hp)
  AND NOT EXISTS (SELECT 1 FROM candidates c2 WHERE c2.no_hp = c.no_hp AND c2.account_id <> c.account_id);

-- 2. Reporting view: effective contact (account first), landline no longer exposed. Recreated rather
--    than replaced because column types change.
DROP VIEW IF EXISTS v_pelanggan_legacy;
CREATE VIEW v_pelanggan_legacy AS
SELECT p.id,
       p.nama_pelanggan,
       pv.nopol,
       pv.merk,
       pv.jenis,
       NULL::varchar(25)                                   AS tlpn,
       CASE WHEN a.id IS NOT NULL THEN a.phone ELSE p.no_hp END AS no_hp,
       NULL::varchar(25)                                   AS no_telepon,
       CASE WHEN a.id IS NOT NULL THEN a.email ELSE p.email END AS email,
       (a.id IS NOT NULL)                                  AS contact_from_account,
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
LEFT JOIN tb_customer_accounts a ON a.id_pelanggan = p.id AND a.deleted_at IS NULL
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

COMMENT ON VIEW v_pelanggan_legacy IS
    'Pre-ADR-0011 tb_pelanggan shape for reports: primary vehicle derived from ownership, contact '
    'resolved from the portal account when linked (ADR-0012), landline always NULL.';

-- 3. Document intent ------------------------------------------------------------------------------

COMMENT ON COLUMN tb_pelanggan.tlpn IS
    'DEPRECATED (ADR-0012): landline, no longer written or read by the application; dropped by the contract migration.';
COMMENT ON COLUMN tb_pelanggan.no_telepon IS
    'DEPRECATED (ADR-0012): landline, no longer written or read by the application; dropped by the contract migration.';
COMMENT ON COLUMN tb_pelanggan.no_hp IS
    'DEPRECATED for linked customers (ADR-0012): mobile phone of a walk-in customer only; tb_customer_accounts.phone is authoritative once linked.';
COMMENT ON COLUMN tb_pelanggan.email IS
    'DEPRECATED for linked customers (ADR-0012): email of a walk-in customer only; tb_customer_accounts.email is authoritative once linked.';
COMMENT ON COLUMN tb_customer_accounts.phone IS
    'Login phone and, for the linked customer, the authoritative mobile number (ADR-0012). Canonical national digits.';
COMMENT ON COLUMN tb_customer_accounts.email IS
    'Login email and, for the linked customer, the authoritative contact email (ADR-0012).';
