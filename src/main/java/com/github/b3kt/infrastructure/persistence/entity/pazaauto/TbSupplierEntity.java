package com.github.b3kt.infrastructure.persistence.entity.pazaauto;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_supplier")
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

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNamaSupplier() {
        return namaSupplier;
    }

    public void setNamaSupplier(String namaSupplier) {
        this.namaSupplier = namaSupplier;
    }

    public String getAlamat() {
        return alamat;
    }

    public void setAlamat(String alamat) {
        this.alamat = alamat;
    }

    public String getTelepon() {
        return telepon;
    }

    public void setTelepon(String telepon) {
        this.telepon = telepon;
    }

    public String getDetailSupplier() {
        return detailSupplier;
    }

    public void setDetailSupplier(String detailSupplier) {
        this.detailSupplier = detailSupplier;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getKeterangan() {
        return keterangan;
    }

    public void setKeterangan(String keterangan) {
        this.keterangan = keterangan;
    }

    public String getKodePos() {
        return kodePos;
    }

    public void setKodePos(String kodePos) {
        this.kodePos = kodePos;
    }

    public String getKontakPerson() {
        return kontakPerson;
    }

    public void setKontakPerson(String kontakPerson) {
        this.kontakPerson = kontakPerson;
    }

    public String getKota() {
        return kota;
    }

    public void setKota(String kota) {
        this.kota = kota;
    }

    public String getNoHpKontak() {
        return noHpKontak;
    }

    public void setNoHpKontak(String noHpKontak) {
        this.noHpKontak = noHpKontak;
    }

    public String getNoTelepon() {
        return noTelepon;
    }

    public void setNoTelepon(String noTelepon) {
        this.noTelepon = noTelepon;
    }
}
