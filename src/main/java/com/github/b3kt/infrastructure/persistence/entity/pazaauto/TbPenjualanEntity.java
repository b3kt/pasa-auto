package com.github.b3kt.infrastructure.persistence.entity.pazaauto;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

import com.github.b3kt.infrastructure.persistence.entity.BaseEntity;

@Entity
@Getter
@Setter
@Table(name = "tb_penjualan")
public class TbPenjualanEntity extends BaseEntity {

    @Column(name = "no_penjualan", length = 15)
    private String noPenjualan;

    @Column(name = "tgl_jam_penjualan", length = 25)
    private Date tanggalJamPenjualan;

    @Column(name = "no_spk", length = 11)
    private String noSpk;

    @Column(name = "grand_total")
    private Integer grandTotal;

    @Column(name = "diskon", precision = 18, scale = 2)
    private BigDecimal diskon;

    @Column(name = "kembalian", precision = 18, scale = 2)
    private BigDecimal kembalian;

    @Column(name = "keterangan", length = 500)
    private String keterangan;

    @Column(name = "ppn", precision = 18, scale = 2)
    private BigDecimal ppn;

    @Column(name = "status_pembayaran", length = 20)
    private String statusPembayaran;

    @Column(name = "uang_dibayar", precision = 18, scale = 2)
    private BigDecimal uangDibayar;

    @Column(name = "id_karyawan")
    private Long karyawanId;

    @Column(name = "id_kendaraan")
    private Long kendaraanId;

    @Column(name = "id_pelanggan")
    private Long pelangganId;

    @Column(name = "metode_pembayaran", length = 20)
    private String metodePembayaran;

}
