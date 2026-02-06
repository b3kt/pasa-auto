package com.github.b3kt.infrastructure.persistence.entity.pazaauto;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.github.b3kt.infrastructure.persistence.entity.BaseEntity;

@Entity
@Table(name = "tb_pembelian_detail")
@Getter
@Setter
public class TbPembelianDetailEntity extends BaseEntity {

    @Column(name = "id_pembelian", nullable = false)
    private Long pembelianId;

    @Column(name = "nama_item", length = 100, nullable = false)
    private String namaItem;

    @Column(name = "kategori_item", length = 20)
    private String kategoriItem;

    @Column(name = "harga", precision = 18, scale = 2, nullable = false)
    private BigDecimal harga;

    @Column(name = "kuantiti", nullable = false)
    private Integer kuantiti;

    @Column(name = "total", precision = 18, scale = 2)
    private BigDecimal total;

    @Column(name = "keterangan", length = 255)
    private String keterangan;

    @Column(name = "id_barang")
    private Long barangId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_barang", insertable = false, updatable = false)
    private TbBarangEntity barang;

    @Column(name = "id_sparepart", length = 20)
    private Long sparepartId;

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
        // Auto-calculate total
        if (harga != null && kuantiti != null) {
            total = harga.multiply(BigDecimal.valueOf(kuantiti));
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        // Auto-calculate total
        if (harga != null && kuantiti != null) {
            total = harga.multiply(BigDecimal.valueOf(kuantiti));
        }
    }

}
