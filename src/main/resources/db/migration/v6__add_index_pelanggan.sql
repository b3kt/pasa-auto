alter table tb_spk add column if not exists nama_pelanggan VARCHAR DEFAULT NULL;
create index CONCURRENTLY if not exists tb_spk_nama_pelanggan_index
    on tb_spk (nama_pelanggan);

create index CONCURRENTLY if not exists tb_pelanggan_nopol_index
    on tb_pelanggan (nopol);
create index CONCURRENTLY if not exists tb_pelanggan_nama_index
    on tb_pelanggan (nama_pelanggan, nopol);

