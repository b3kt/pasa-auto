-- V13: Add discount column to tb_penjualan
ALTER TABLE tb_penjualan ADD COLUMN discount numeric(18, 2) DEFAULT 0.00;
