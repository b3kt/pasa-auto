package com.github.b3kt.infrastructure.persistence.entity.pazaauto;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

import com.github.b3kt.infrastructure.persistence.entity.BaseEntity;

@Entity
@Table(name = "tb_sparepart")
@Getter
@Setter
public class TbSparepartEntity extends BaseEntity {

    @Column(name = "kd_barang", length = 9)
    private String kodeBarang;

    @Column(name = "nama_barang", length = 40)
    private String namaBarang;

    @Column(name = "harga_beli", precision = 18, scale = 2)
    private BigDecimal hargaBeli;

    @Column(name = "id")
    private Long id;

    @Column(name = "is_active", nullable = false)
    private Boolean active;

    @Column(name = "harga_jual", precision = 18, scale = 2)
    private BigDecimal hargaJual;

    @Column(name = "keterangan", length = 500)
    private String keterangan;

    @Column(name = "kode_sparepart", length = 20, nullable = false)
    private String kodeSparepart;

    @Column(name = "merek", length = 50)
    private String merek;

    @Column(name = "nama_sparepart", length = 100, nullable = false)
    private String namaSparepart;

    @Column(name = "satuan", length = 20)
    private String satuan;

    @Column(name = "stok")
    private Integer stok;

    @Column(name = "stok_minimal")
    private Integer stokMinimal;

    @Column(name = "tipe_kendaraan", length = 50)
    private String tipeKendaraan;

    @Column(name = "id_supplier")
    private Long supplierId;

}
