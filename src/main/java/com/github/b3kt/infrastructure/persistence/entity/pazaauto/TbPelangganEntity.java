package com.github.b3kt.infrastructure.persistence.entity.pazaauto;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

import com.github.b3kt.infrastructure.persistence.entity.BaseEntity;

@Entity
@Table(name = "tb_pelanggan")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TbPelangganEntity extends BaseEntity {

    @Column(name = "nopol", length = 10, nullable = false)
    private String nopol;

    @Column(name = "nama_pelanggan", length = 100, nullable = false)
    private String namaPelanggan;

    @Column(name = "alamat", length = 500)
    private String alamat;

    @Column(name = "contact_person", length = 30)
    private String contactPerson;

    @Column(name = "tlpn", length = 15)
    private String telepon;

    @Column(name = "merk", length = 50, nullable = false)
    private String merk;

    @Column(name = "jenis", length = 50)
    private String jenis;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "jenis_kelamin", length = 1)
    private String jenisKelamin;

    @Column(name = "keterangan", length = 500)
    private String keterangan;

    @Column(name = "kode_pos", length = 10)
    private String kodePos;

    @Column(name = "kota", length = 100)
    private String kota;

    @Column(name = "no_hp", length = 20)
    private String noHp;

    @Column(name = "no_telepon", length = 20)
    private String noTelepon;

    @Column(name = "tanggal_join")
    private LocalDate tanggalJoin;
}
