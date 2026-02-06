package com.github.b3kt.infrastructure.persistence.entity.pazaauto;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

import com.github.b3kt.infrastructure.persistence.entity.BaseEntity;

@Entity
@Table(name = "tb_absensi", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "karyawan_id", "tanggal" })
})
@Getter
@Setter
public class TbAbsensiEntity extends BaseEntity {

    @Column(name = "karyawan_id", nullable = false)
    private Long karyawanId;

    @Column(name = "tanggal", nullable = false)
    private LocalDate tanggal;

    @Column(name = "jam_masuk")
    private LocalTime jamMasuk;

    @Column(name = "jam_keluar")
    private LocalTime jamKeluar;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "HADIR";

    @Column(name = "keterangan", length = 500)
    private String keterangan;

    @Column(name = "terlambat")
    private Boolean terlambat = false;

    @Column(name = "pulang_cepat")
    private Boolean pulangCepat = false;

    @Column(name = "lembur")
    private Integer lembur = 0;

    @Column(name = "lokasi_masuk", length = 200)
    private String lokasiMasuk;

    @Column(name = "lokasi_keluar", length = 200)
    private String lokasiKeluar;

    @Column(name = "ip_masuk", length = 50)
    private String ipMasuk;

    @Column(name = "ip_keluar", length = 50)
    private String ipKeluar;

    @Column(name = "device_info", length = 500)
    private String deviceInfo;

    @Transient
    private String namaKaryawan;
}
