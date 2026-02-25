package com.github.b3kt.infrastructure.persistence.entity.pazaauto;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.github.b3kt.infrastructure.persistence.entity.BaseEntity;

@Entity
@Table(name = "tb_pembelian")
@Getter
@Setter
@NoArgsConstructor
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

    @Column(name = "keterangan", length = 500)
    private String keterangan;

    @Column(name = "id_karyawan")
    private Long karyawanId;

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

    private transient String namaSupplier;

    private transient List<TbPembelianDetailEntity> details;

    public TbPembelianEntity(TbPembelianEntity entity, String namaSupplier){
        this.id = entity.getId();
        this.noPembelian = entity.getNoPembelian();
        this.noUrut = entity.getNoUrut();
        this.tanggalPembelian = entity.getTanggalPembelian();
        this.jenisPembelian = entity.getJenisPembelian();
        this.jenisOperasional = entity.getJenisOperasional();
        this.kategoriOperasional = entity.getKategoriOperasional();
        this.supplierId = entity.getSupplierId();
        this.grandTotal = entity.getGrandTotal();
        this.jenisPembayaran = entity.getJenisPembayaran();
        this.statusPembayaran = entity.getStatusPembayaran();
        this.metodePembayaran = entity.getMetodePembayaran();
        this.diskon = entity.getDiskon();
        this.keterangan = entity.getKeterangan();
        this.karyawanId = entity.getKaryawanId();
        this.namaSupplier = namaSupplier;
    }
}
