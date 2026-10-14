-- Add deleted_at column for soft delete functionality
ALTER TABLE tb_pelanggan ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE tb_pelanggan_kendaraan ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

-- Create index on deleted_at for performance
CREATE INDEX IF NOT EXISTS idx_tb_pelanggan_deleted_at ON tb_pelanggan(deleted_at) WHERE deleted_at IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_tb_pelanggan_kendaraan_deleted_at ON tb_pelanggan_kendaraan(deleted_at) WHERE deleted_at IS NOT NULL;
