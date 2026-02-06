package com.github.b3kt.infrastructure.persistence.entity.pazaauto;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.github.b3kt.infrastructure.persistence.entity.BaseEntity;

@Entity
@Table(name = "tb_pembelian")
@Getter
@Setter
public class TbPembelianEntity extends BaseEntity {

    @Column(name = "no_pembelian", length = 15, nullable = false, unique = true)
    private String noPembelian;

    @Column(name = "no_urut")
    private Integer noUrut;

    @Column(name = "tgl_pembelian")
    private LocalDateTime tanggalPembelian;

    @Column(name = "jenis_pembelian", length = 20, nullable = false)
    private String jenisPembelian;

    @Column(name = "jenis_operasional", length = 50)
    private String jenisOperasional;

    @Column(name = "kategori_operasional", length = 20)
    private String kategoriOperasional;

    @Column(name = "id_supplier")
    private Integer supplierId;

    @Column(name = "grand_total", precision = 18, scale = 2)
    private BigDecimal grandTotal;

    @Column(name = "jenis_pembayaran", length = 20)
    private String jenisPembayaran;

    @Column(name = "status_pembayaran", length = 20)
    private String statusPembayaran;

    @Column(name = "metode_pembayaran", length = 20)
    private String metodePembayaran;

    @Column(name = "diskon", precision = 18, scale = 2)
    private BigDecimal diskon;

    @Column(name = "ppn", precision = 18, scale = 2)
    private BigDecimal ppn;

    @Column(name = "keterangan", length = 500)
    private String keterangan;

    @Column(name = "id_karyawan")
    private Long karyawanId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "version")
    private Integer version;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
