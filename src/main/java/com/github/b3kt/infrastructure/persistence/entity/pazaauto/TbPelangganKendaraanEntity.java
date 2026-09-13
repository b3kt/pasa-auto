package com.github.b3kt.infrastructure.persistence.entity.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "tb_pelanggan_kendaraan")
@Getter
@Setter
public class TbPelangganKendaraanEntity extends BaseEntity {

    @Column(name = "id_pelanggan", nullable = false)
    private Long pelangganId;

    @Column(name = "id_kendaraan", nullable = false)
    private Long kendaraanId;

    @Column(name = "nopol", length = 10, nullable = false)
    private String nopol;

    @Column(name = "tanggal_mulai", nullable = false)
    private LocalDate tanggalMulai;

    @Column(name = "tanggal_akhir")
    private LocalDate tanggalAkhir;

    @Column(name = "is_current", nullable = false)
    private boolean current;

    @Column(name = "keterangan", length = 500)
    private String keterangan;

    public boolean isCurrent() {
        return current;
    }
}