package com.github.b3kt.infrastructure.persistence.entity.pazaauto;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

import com.github.b3kt.infrastructure.persistence.entity.BaseEntity;

@Entity
@Table(name = "tb_penjualan_detail")
@Setter
@Getter
public class TbPenjualanDetailEntity extends BaseEntity {

    // @EmbeddedId
    // private TbPenjualanDetailId id;

    @Column(name = "kategori", length = 10)
    private String kategori;

    @Column(name = "harga_jual", nullable = false)
    private Integer hargaJual;

    @Column(name = "kuantiti", nullable = false)
    private Integer kuantiti;

    @Column(name = "total")
    private Integer total;

    @Column(name = "diskon", precision = 18, scale = 2)
    private BigDecimal diskon;

    @Column(name = "keterangan", length = 255)
    private String keterangan;

    @Column(name = "id_barang", nullable = true)
    private Long barangId;

    @Column(name = "id_jasa", nullable = true)
    private Long jasaId;

    @Column(name = "id_sparepart", nullable = true)
    private Long sparepartId;

    @Column(name = "no_penjualan", nullable = true)
    private String noPenjualan;

    @Column(name = "nama_jasa_barang", nullable = true)
    private String namaJasaBarang;

}
