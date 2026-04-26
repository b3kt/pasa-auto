-- V12: Add audit trail triggers to all tables
--
-- This adds AFTER INSERT, UPDATE, DELETE triggers to all tables
-- that inherit from BaseEntity or are user-facing tables.

-- Drop existing audit triggers (if any)
DROP TRIGGER IF EXISTS trg_audit_trail_insert ON tb_jasa;
DROP TRIGGER IF EXISTS trg_audit_trail_update ON tb_jasa;
DROP TRIGGER IF EXISTS trg_audit_trail_delete ON tb_jasa;

DROP TRIGGER IF EXISTS trg_audit_trail_insert ON tb_barang;
DROP TRIGGER IF EXISTS trg_audit_trail_update ON tb_barang;
DROP TRIGGER IF EXISTS trg_audit_trail_delete ON tb_barang;

DROP TRIGGER IF EXISTS trg_audit_trail_insert ON tb_sparepart;
DROP TRIGGER IF EXISTS trg_audit_trail_update ON tb_sparepart;
DROP TRIGGER IF EXISTS trg_audit_trail_delete ON tb_sparepart;

DROP TRIGGER IF EXISTS trg_audit_trail_insert ON tb_supplier;
DROP TRIGGER IF EXISTS trg_audit_trail_update ON tb_supplier;
DROP TRIGGER IF EXISTS trg_audit_trail_delete ON tb_supplier;

DROP TRIGGER IF EXISTS trg_audit_trail_insert ON tb_pelanggan;
DROP TRIGGER IF EXISTS trg_audit_trail_update ON tb_pelanggan;
DROP TRIGGER IF EXISTS trg_audit_trail_delete ON tb_pelanggan;

DROP TRIGGER IF EXISTS trg_audit_trail_insert ON tb_karyawan;
DROP TRIGGER IF EXISTS trg_audit_trail_update ON tb_karyawan;
DROP TRIGGER IF EXISTS trg_audit_trail_delete ON tb_karyawan;

DROP TRIGGER IF EXISTS trg_audit_trail_insert ON tb_karyawan_posisi;
DROP TRIGGER IF EXISTS trg_audit_trail_update ON tb_karyawan_posisi;
DROP TRIGGER IF EXISTS trg_audit_trail_delete ON tb_karyawan_posisi;

DROP TRIGGER IF EXISTS trg_audit_trail_insert ON tb_kendaraan;
DROP TRIGGER IF EXISTS trg_audit_trail_update ON tb_kendaraan;
DROP TRIGGER IF EXISTS trg_audit_trail_delete ON tb_kendaraan;

DROP TRIGGER IF EXISTS trg_audit_trail_insert ON tb_spk;
DROP TRIGGER IF EXISTS trg_audit_trail_update ON tb_spk;
DROP TRIGGER IF EXISTS trg_audit_trail_delete ON tb_spk;

DROP TRIGGER IF EXISTS trg_audit_trail_insert ON tb_spk_detail;
DROP TRIGGER IF EXISTS trg_audit_trail_update ON tb_spk_detail;
DROP TRIGGER IF EXISTS trg_audit_trail_delete ON tb_spk_detail;

DROP TRIGGER IF EXISTS trg_audit_trail_insert ON tb_penjualan;
DROP TRIGGER IF EXISTS trg_audit_trail_update ON tb_penjualan;
DROP TRIGGER IF EXISTS trg_audit_trail_delete ON tb_penjualan;

DROP TRIGGER IF EXISTS trg_audit_trail_insert ON tb_penjualan_detail;
DROP TRIGGER IF EXISTS trg_audit_trail_update ON tb_penjualan_detail;
DROP TRIGGER IF EXISTS trg_audit_trail_delete ON tb_penjualan_detail;

DROP TRIGGER IF EXISTS trg_audit_trail_insert ON tb_pembelian;
DROP TRIGGER IF EXISTS trg_audit_trail_update ON tb_pembelian;
DROP TRIGGER IF EXISTS trg_audit_trail_delete ON tb_pembelian;

DROP TRIGGER IF EXISTS trg_audit_trail_insert ON tb_pembelian_detail;
DROP TRIGGER IF EXISTS trg_audit_trail_update ON tb_pembelian_detail;
DROP TRIGGER IF EXISTS trg_audit_trail_delete ON tb_pembelian_detail;

DROP TRIGGER IF EXISTS trg_audit_trail_insert ON tb_pembelian_barang_detail;
DROP TRIGGER IF EXISTS trg_audit_trail_update ON tb_pembelian_barang_detail;
DROP TRIGGER IF EXISTS trg_audit_trail_delete ON tb_pembelian_barang_detail;

DROP TRIGGER IF EXISTS trg_audit_trail_insert ON tb_absensi;
DROP TRIGGER IF EXISTS trg_audit_trail_update ON tb_absensi;
DROP TRIGGER IF EXISTS trg_audit_trail_delete ON tb_absensi;

DROP TRIGGER IF EXISTS trg_audit_trail_insert ON tb_absensi_config;
DROP TRIGGER IF EXISTS trg_audit_trail_update ON tb_absensi_config;
DROP TRIGGER IF EXISTS trg_audit_trail_delete ON tb_absensi_config;

-- Create triggers for each table
-- tb_jasa
CREATE TRIGGER trg_audit_trail_insert AFTER INSERT ON tb_jasa FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();
CREATE TRIGGER trg_audit_trail_update AFTER UPDATE ON tb_jasa FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();
CREATE TRIGGER trg_audit_trail_delete AFTER DELETE ON tb_jasa FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();

-- tb_barang
CREATE TRIGGER trg_audit_trail_insert AFTER INSERT ON tb_barang FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();
CREATE TRIGGER trg_audit_trail_update AFTER UPDATE ON tb_barang FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();
CREATE TRIGGER trg_audit_trail_delete AFTER DELETE ON tb_barang FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();

-- tb_sparepart
CREATE TRIGGER trg_audit_trail_insert AFTER INSERT ON tb_sparepart FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();
CREATE TRIGGER trg_audit_trail_update AFTER UPDATE ON tb_sparepart FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();
CREATE TRIGGER trg_audit_trail_delete AFTER DELETE ON tb_sparepart FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();

-- tb_supplier
CREATE TRIGGER trg_audit_trail_insert AFTER INSERT ON tb_supplier FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();
CREATE TRIGGER trg_audit_trail_update AFTER UPDATE ON tb_supplier FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();
CREATE TRIGGER trg_audit_trail_delete AFTER DELETE ON tb_supplier FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();

-- tb_pelanggan
CREATE TRIGGER trg_audit_trail_insert AFTER INSERT ON tb_pelanggan FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();
CREATE TRIGGER trg_audit_trail_update AFTER UPDATE ON tb_pelanggan FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();
CREATE TRIGGER trg_audit_trail_delete AFTER DELETE ON tb_pelanggan FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();

-- tb_karyawan
CREATE TRIGGER trg_audit_trail_insert AFTER INSERT ON tb_karyawan FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();
CREATE TRIGGER trg_audit_trail_update AFTER UPDATE ON tb_karyawan FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();
CREATE TRIGGER trg_audit_trail_delete AFTER DELETE ON tb_karyawan FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();

-- tb_karyawan_posisi
CREATE TRIGGER trg_audit_trail_insert AFTER INSERT ON tb_karyawan_posisi FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();
CREATE TRIGGER trg_audit_trail_update AFTER UPDATE ON tb_karyawan_posisi FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();
CREATE TRIGGER trg_audit_trail_delete AFTER DELETE ON tb_karyawan_posisi FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();

-- tb_kendaraan
CREATE TRIGGER trg_audit_trail_insert AFTER INSERT ON tb_kendaraan FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();
CREATE TRIGGER trg_audit_trail_update AFTER UPDATE ON tb_kendaraan FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();
CREATE TRIGGER trg_audit_trail_delete AFTER DELETE ON tb_kendaraan FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();

-- tb_spk
CREATE TRIGGER trg_audit_trail_insert AFTER INSERT ON tb_spk FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();
CREATE TRIGGER trg_audit_trail_update AFTER UPDATE ON tb_spk FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();
CREATE TRIGGER trg_audit_trail_delete AFTER DELETE ON tb_spk FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();

-- tb_spk_detail
CREATE TRIGGER trg_audit_trail_insert AFTER INSERT ON tb_spk_detail FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();
CREATE TRIGGER trg_audit_trail_update AFTER UPDATE ON tb_spk_detail FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();
CREATE TRIGGER trg_audit_trail_delete AFTER DELETE ON tb_spk_detail FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();

-- tb_penjualan
CREATE TRIGGER trg_audit_trail_insert AFTER INSERT ON tb_penjualan FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();
CREATE TRIGGER trg_audit_trail_update AFTER UPDATE ON tb_penjualan FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();
CREATE TRIGGER trg_audit_trail_delete AFTER DELETE ON tb_penjualan FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();

-- tb_penjualan_detail
CREATE TRIGGER trg_audit_trail_insert AFTER INSERT ON tb_penjualan_detail FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();
CREATE TRIGGER trg_audit_trail_update AFTER UPDATE ON tb_penjualan_detail FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();
CREATE TRIGGER trg_audit_trail_delete AFTER DELETE ON tb_penjualan_detail FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();

-- tb_pembelian
CREATE TRIGGER trg_audit_trail_insert AFTER INSERT ON tb_pembelian FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();
CREATE TRIGGER trg_audit_trail_update AFTER UPDATE ON tb_pembelian FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();
CREATE TRIGGER trg_audit_trail_delete AFTER DELETE ON tb_pembelian FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();

-- tb_pembelian_detail
CREATE TRIGGER trg_audit_trail_insert AFTER INSERT ON tb_pembelian_detail FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();
CREATE TRIGGER trg_audit_trail_update AFTER UPDATE ON tb_pembelian_detail FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();
CREATE TRIGGER trg_audit_trail_delete AFTER DELETE ON tb_pembelian_detail FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();

-- tb_pembelian_barang_detail
CREATE TRIGGER trg_audit_trail_insert AFTER INSERT ON tb_pembelian_barang_detail FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();
CREATE TRIGGER trg_audit_trail_update AFTER UPDATE ON tb_pembelian_barang_detail FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();
CREATE TRIGGER trg_audit_trail_delete AFTER DELETE ON tb_pembelian_barang_detail FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();

-- tb_absensi
CREATE TRIGGER trg_audit_trail_insert AFTER INSERT ON tb_absensi FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();
CREATE TRIGGER trg_audit_trail_update AFTER UPDATE ON tb_absensi FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();
CREATE TRIGGER trg_audit_trail_delete AFTER DELETE ON tb_absensi FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();

-- tb_absensi_config
CREATE TRIGGER trg_audit_trail_insert AFTER INSERT ON tb_absensi_config FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();
CREATE TRIGGER trg_audit_trail_update AFTER UPDATE ON tb_absensi_config FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();
CREATE TRIGGER trg_audit_trail_delete AFTER DELETE ON tb_absensi_config FOR EACH ROW EXECUTE FUNCTION fn_record_audit_trail();