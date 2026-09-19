-- Originally v6__add_index_pelanggan.sql; the lowercase "v" prefix made Flyway skip it,
-- so tb_spk.nama_pelanggan was never created. Idempotent so it is safe on any database.
alter table tb_spk add column if not exists nama_pelanggan VARCHAR DEFAULT NULL;
create index if not exists tb_spk_nama_pelanggan_index
    on tb_spk (nama_pelanggan);

create index if not exists tb_pelanggan_nopol_index
    on tb_pelanggan (nopol);
create index if not exists tb_pelanggan_nama_index
    on tb_pelanggan (nama_pelanggan, nopol);
