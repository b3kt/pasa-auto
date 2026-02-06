package com.github.b3kt.infrastructure.persistence.entity.pazaauto;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

import com.github.b3kt.infrastructure.persistence.entity.BaseEntity;

@Entity
@Table(name = "tb_karyawan")
@Getter
@Setter
public class TbKaryawanEntity extends BaseEntity {

    @Column(name = "nama_karyawan", length = 30, nullable = false)
    private String namaKaryawan;

    @Column(name = "alamat", length = 500)
    private String alamat;

    @Column(name = "no_tlpn", length = 15)
    private String noTlpn;

    @Column(name = "bergabung", length = 10)
    private String bergabung;

    @Column(name = "posisi_id")
    private Integer posisiId;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "jenis_kelamin", length = 1)
    private String jenisKelamin;

    @Column(name = "no_telepon", length = 20)
    private String noTelepon;

    @Column(name = "tanggal_bergabung")
    private LocalDate tanggalBergabung;

    @Column(name = "id_posisi")
    private Long idPosisi;

    @Transient
    private String namePosisi;

    @Transient
    private java.util.List<String> roles;
}
