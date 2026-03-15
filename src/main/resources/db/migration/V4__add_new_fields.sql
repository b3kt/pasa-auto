alter table tb_penjualan
    drop if exists grand_total,
    drop if exists ppn;
alter table tb_penjualan add grand_total decimal(18,2) default 0;

alter table tb_spk_detail
    drop if exists harga_master,
    drop if exists created_at,
    drop if exists created_by,
    drop if exists updated_at,
    drop if exists updated_by,
    drop if exists version;
;
alter table tb_spk_detail add harga_master decimal(18,2) default null;

alter table tb_spk_detail drop if exists harga ;
alter table tb_spk_detail add harga decimal(18,2) default null;

alter table tb_spk
    drop if exists id_cs,
    drop if exists diskon,
    drop if exists ppn,
    drop if exists keluhan;

alter table tb_pembelian
    drop if exists ppn;
