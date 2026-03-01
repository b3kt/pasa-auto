package com.github.b3kt.infrastructure.persistence.entity.pazaauto;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_supplier")
@Getter
@Setter
@NoArgsConstructor
public class TbSupplierEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "nama_supplier", length = 100, nullable = false)
    private String namaSupplier;

    @Column(name = "alamat", length = 500)
    private String alamat;

    @Column(name = "tlpn", length = 13)
    private String telepon;

    @Column(name = "detail_supplier", length = 100)
    private String detailSupplier;

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

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "keterangan", length = 500)
    private String keterangan;

    @Column(name = "kode_pos", length = 10)
    private String kodePos;

    @Column(name = "kontak_person", length = 100)
    private String kontakPerson;

    @Column(name = "kota", length = 100)
    private String kota;

    @Column(name = "no_hp_kontak", length = 20)
    private String noHpKontak;

    @Column(name = "no_telepon", length = 20)
    private String noTelepon;
}
