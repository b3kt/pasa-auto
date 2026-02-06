package com.github.b3kt.infrastructure.persistence.entity.pazaauto;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

import com.github.b3kt.infrastructure.persistence.entity.BaseEntity;

/**
 * Entity mapping for tb_barang table.
 */
@Entity
@Table(name = "tb_barang")
@Getter
@Setter
public class TbBarangEntity extends BaseEntity {

    @Column(name = "nama_barang", length = 100, nullable = false)
    private String namaBarang;

    @Column(name = "harga_jual", precision = 18, scale = 2)
    private BigDecimal hargaJual;

    @Column(name = "harga_beli", precision = 18, scale = 2)
    private BigDecimal hargaBeli;

    @Column(name = "keterangan", length = 500)
    private String keterangan;

    @Column(name = "satuan", length = 20)
    private String satuan;

    @Column(name = "stok")
    private Integer stok;

    @Column(name = "stok_minimal")
    private Integer stokMinimal;

    @Column(name = "id_supplier")
    private Integer supplierId;

    @Column(name = "is_active")
    private Boolean active;

    @Column(name = "kode_barang", length = 20)
    private String kodeBarang;

}
