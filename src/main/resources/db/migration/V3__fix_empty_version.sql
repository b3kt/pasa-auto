-- FIX EMPTY VERSION (first add column if not exists, then update)
-- Add version column to tables that might not have it
ALTER TABLE tb_absensi ADD COLUMN IF NOT EXISTS version integer DEFAULT 0;
ALTER TABLE tb_absensi_config ADD COLUMN IF NOT EXISTS version integer DEFAULT 0;
ALTER TABLE tb_barang ADD COLUMN IF NOT EXISTS version integer DEFAULT 0;
ALTER TABLE tb_jasa ADD COLUMN IF NOT EXISTS version integer DEFAULT 0;
ALTER TABLE tb_karyawan ADD COLUMN IF NOT EXISTS version integer DEFAULT 0;
ALTER TABLE tb_karyawan_posisi ADD COLUMN IF NOT EXISTS version integer DEFAULT 0;
ALTER TABLE tb_kendaraan ADD COLUMN IF NOT EXISTS version integer DEFAULT 0;
ALTER TABLE tb_pelanggan ADD COLUMN IF NOT EXISTS version integer DEFAULT 0;
ALTER TABLE tb_pembelian ADD COLUMN IF NOT EXISTS version integer DEFAULT 0;
ALTER TABLE tb_pembelian_barang_detail ADD COLUMN IF NOT EXISTS version integer DEFAULT 0;
ALTER TABLE tb_pembelian_detail ADD COLUMN IF NOT EXISTS version integer DEFAULT 0;
ALTER TABLE tb_penjualan ADD COLUMN IF NOT EXISTS version integer DEFAULT 0;
ALTER TABLE tb_penjualan_detail ADD COLUMN IF NOT EXISTS version integer DEFAULT 0;
ALTER TABLE tb_sparepart ADD COLUMN IF NOT EXISTS version integer DEFAULT 0;
ALTER TABLE tb_spk ADD COLUMN IF NOT EXISTS version integer DEFAULT 0;
ALTER TABLE tb_spk_detail ADD COLUMN IF NOT EXISTS version integer DEFAULT 0;
ALTER TABLE tb_supplier ADD COLUMN IF NOT EXISTS version integer DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS version integer DEFAULT 0;

-- Set version to 0 where null
UPDATE tb_absensi set version = 0 WHERE version IS null;
UPDATE tb_absensi_config set version = 0 WHERE version IS null;
UPDATE tb_barang set version = 0 WHERE version IS null;
UPDATE tb_jasa set version = 0 WHERE version IS null;
UPDATE tb_karyawan set version = 0 WHERE version IS null;
UPDATE tb_karyawan_posisi set version = 0 WHERE version IS null;
UPDATE tb_kendaraan set version = 0 WHERE version IS null;
UPDATE tb_pelanggan set version = 0 WHERE version IS null;
UPDATE tb_pembelian set version = 0 WHERE version IS null;
UPDATE tb_pembelian_barang_detail set version = 0 WHERE version IS null;
UPDATE tb_pembelian_detail set version = 0 WHERE version IS null;
UPDATE tb_penjualan set version = 0 WHERE version IS null;
UPDATE tb_penjualan_detail set version = 0 WHERE version IS null;
UPDATE tb_sparepart set version = 0 WHERE version IS null;
UPDATE tb_spk set version = 0 WHERE version IS null;
UPDATE tb_spk_detail set version = 0 WHERE version IS null;
UPDATE tb_supplier set version = 0 WHERE version IS null;
UPDATE users set version = 0 WHERE version IS null;

-- FIX CONFLICTED SEQUENCES
DO $$
DECLARE
  r RECORD;
BEGIN
  FOR r IN
    SELECT
      pg_get_serial_sequence(
        quote_ident(t.schemaname) || '.' || quote_ident(t.tablename),
        c.column_name
      ) AS seq,
      quote_ident(t.schemaname) || '.' || quote_ident(t.tablename) AS tbl,
      quote_ident(c.column_name) AS col
    FROM pg_tables t
    JOIN information_schema.columns c
      ON c.table_name   = t.tablename
     AND c.table_schema = t.schemaname
    WHERE c.column_default LIKE 'nextval%'
      AND t.tablename IN (
        'tb_absensi',
        'tb_absensi_config',
        'tb_barang',
        'tb_jasa',
        'tb_karyawan',
        'tb_karyawan_posisi',
        'tb_kendaraan',
        'tb_pelanggan',
        'tb_pembelian',
        'tb_pembelian_barang_detail',
        'tb_pembelian_detail',
        'tb_penjualan',
        'tb_penjualan_detail',
        'tb_sparepart',
        'tb_spk',
        'tb_spk_detail',
        'tb_supplier',
        'user_roles',
        'users'
      )
  LOOP
    EXECUTE format(
      'SELECT setval(%L, (SELECT COALESCE(MAX(%s), 0) FROM %s))',
      r.seq, r.col, r.tbl
    );
  END LOOP;
END $$;

-- FIX CONSTRAINTS
ALTER TABLE public.tb_pembelian DROP CONSTRAINT IF EXISTS chk_jenis_pembelian;
ALTER TABLE public.tb_pembelian ADD CONSTRAINT chk_jenis_pembelian CHECK (((jenis_pembelian)::text = ANY (ARRAY[('SPAREPART'::character varying)::text, ('OPERASIONAL'::character varying)::text, ('BARANG'::character varying)::text])))
