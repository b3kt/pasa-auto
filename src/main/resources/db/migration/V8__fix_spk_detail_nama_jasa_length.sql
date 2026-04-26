-- V8__fix_spk_detail_nama_jasa_length.sql
-- Increase length of nama_jasa column in tb_spk_detail from 40 to 100 characters

ALTER TABLE tb_spk_detail ALTER COLUMN nama_jasa TYPE VARCHAR(100);