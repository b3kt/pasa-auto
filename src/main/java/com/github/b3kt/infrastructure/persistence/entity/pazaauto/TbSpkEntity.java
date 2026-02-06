package com.github.b3kt.infrastructure.persistence.entity.pazaauto;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.github.b3kt.infrastructure.persistence.entity.BaseEntity;
import com.github.b3kt.infrastructure.persistence.entity.subentity.SpkMekanik;

@Entity
@Table(name = "tb_spk")
@Getter
@Setter
public class TbSpkEntity extends BaseEntity {

    @Column(name = "no_spk", length = 30, nullable = false)
    private String noSpk;

    @Column(name = "no_antrian")
    private Integer noAntrian;

    @Column(name = "tgl_jam_spk", length = 25)
    private String tanggalJamSpk;

    @Column(name = "nopol", length = 10)
    private String nopol;

    @Column(name = "id_pelanggan")
    private Long pelangganId;

    @Column(name = "nama_karyawan", length = 300)
    private String namaKaryawan;

    @Column(name = "km")
    private Integer km;

    @Column(name = "status_spk", length = 20)
    private String statusSpk;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "diskon", precision = 18, scale = 2)
    private BigDecimal diskon;

    @Column(name = "keluhan", length = 1000)
    private String keluhan;

    @Column(name = "keterangan", length = 1000)
    private String keterangan;

    @Column(name = "km_saat_ini")
    private Integer kmSaatIni;

    @Column(name = "ppn", precision = 18, scale = 2)
    private BigDecimal ppn;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "id_cs")
    private Long csId;

    @Column(name = "id_mekanik")
    private Long mekanikId;

    @Column(name = "mekanik_list", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<SpkMekanik> mekanikList; // Stores JSON array of mechanic assignments

    @Transient
    private java.util.List<TbSpkDetailEntity> details;

    @Transient
    private boolean startProcess;

    @Transient
    private String namaPelanggan;

}
