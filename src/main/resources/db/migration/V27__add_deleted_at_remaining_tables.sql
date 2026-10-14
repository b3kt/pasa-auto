-- V26 added deleted_at only to tb_pelanggan / tb_pelanggan_kendaraan, but BaseEntity maps
-- deleted_at for every entity, so Hibernate schema validation fails on all other tables.
-- Idempotent: only adds the column where it is still missing.

ALTER TABLE app_settings                      ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE customer_accounts                 ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE customer_bookings                 ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE customer_pelanggan_link_requests  ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE system_parameters                 ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE tb_absensi                        ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE tb_absensi_config                 ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE tb_barang                         ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE tb_jasa                           ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE tb_karyawan                       ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE tb_karyawan_posisi                ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE tb_kendaraan                      ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE tb_merk_kendaraan                 ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE tb_pembelian                      ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE tb_pembelian_detail               ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE tb_penjualan                      ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE tb_penjualan_detail               ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE tb_sparepart                      ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE tb_spk                            ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE users                             ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
