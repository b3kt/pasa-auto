package com.github.b3kt.infrastructure.persistence.entity.pazaauto;

import com.github.b3kt.application.helper.PhoneNormalizer;
import com.github.b3kt.domain.model.pazaauto.Pelanggan;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

import com.github.b3kt.infrastructure.persistence.entity.BaseEntity;

/**
 * Customer master record: who the customer is (name, contacts, address, provenance). Vehicles are
 * owned through {@code tb_pelanggan_kendaraan}; see ADR-0011.
 */
@Entity
@Table(name = "tb_pelanggan")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TbPelangganEntity extends BaseEntity {

    /**
     * Deprecated single-vehicle view of the primary vehicle. Read-only: the column is maintained by the
     * {@code trg_pk_sync_legacy_vehicle} trigger from the ownership junction and dropped in V36. The
     * field is still set in memory by the compatibility read layer and carried to ownership sync.
     */
    @Column(name = "nopol", length = 10, insertable = false, updatable = false)
    private String nopol;

    @Column(name = "nama_pelanggan", length = 100, nullable = false)
    private String namaPelanggan;

    @Column(name = "alamat", length = 500)
    private String alamat;

    @Column(name = "contact_person", length = 30)
    private String contactPerson;

    /**
     * Deprecated alias of {@link #noTelepon}: mirrored from it on every write and dropped in V36. A
     * write that sets only this field (no {@code noTelepon}) is applied to {@code noTelepon}.
     */
    @Column(name = "tlpn", length = 25)
    private String telepon;

    /** Deprecated, read-only; see {@link #nopol}. */
    @Column(name = "merk", length = 50, insertable = false, updatable = false)
    private String merk;

    /** Deprecated, read-only; see {@link #nopol}. */
    @Column(name = "jenis", length = 50, insertable = false, updatable = false)
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

    /** Mobile phone, canonical national digits (see {@link PhoneNormalizer}). */
    @Column(name = "no_hp", length = 25)
    private String noHp;

    /** Landline phone, canonical national digits (see {@link PhoneNormalizer}). */
    @Column(name = "no_telepon", length = 25)
    private String noTelepon;

    @Column(name = "tanggal_join")
    private LocalDate tanggalJoin;

    /** {@link Pelanggan#SUMBER_STAFF} or {@link Pelanggan#SUMBER_PORTAL}. */
    @Column(name = "sumber", length = 10, nullable = false)
    private String sumber = Pelanggan.SUMBER_STAFF;

    /** Set when this record was merged into another; stale references resolve through it. */
    @Column(name = "merged_into_id")
    private Long mergedIntoId;

    @PrePersist
    @PreUpdate
    void normalizeContacts() {
        if (noTelepon == null && telepon != null && !telepon.isBlank()) {
            noTelepon = telepon;
        }
        noHp = PhoneNormalizer.normalize(noHp);
        noTelepon = PhoneNormalizer.normalize(noTelepon);
        telepon = noTelepon;
        if (sumber == null || sumber.isBlank()) {
            sumber = Pelanggan.SUMBER_STAFF;
        }
    }

    /** @return true when the customer registered through the self-service portal. */
    public boolean isPortalSourced() {
        return Pelanggan.SUMBER_PORTAL.equals(sumber);
    }

    public Pelanggan toDomain() {
        Pelanggan p = new Pelanggan();
        p.setId(this.id);
        p.setNopol(this.nopol);
        if (this.namaPelanggan != null && !this.namaPelanggan.trim().isEmpty()) {
            p.setNamaPelanggan(this.namaPelanggan);
        }
        p.setAlamat(this.alamat);
        p.setContactPerson(this.contactPerson);
        p.setTelepon(this.telepon);
        p.setMerk(this.merk);
        p.setJenis(this.jenis);
        p.setEmail(this.email);
        p.setJenisKelamin(this.jenisKelamin);
        p.setKeterangan(this.keterangan);
        p.setKodePos(this.kodePos);
        p.setKota(this.kota);
        p.setNoHp(this.noHp);
        p.setNoTelepon(this.noTelepon);
        p.setTanggalJoin(this.tanggalJoin);
        p.setSumber(this.sumber);
        p.setMergedIntoId(this.mergedIntoId);
        return p;
    }

    public static TbPelangganEntity fromDomain(Pelanggan p) {
        TbPelangganEntity e = new TbPelangganEntity();
        e.setId(p.getId());
        e.setNopol(p.getNopol());
        e.setNamaPelanggan(p.getNamaPelanggan());
        e.setAlamat(p.getAlamat());
        e.setContactPerson(p.getContactPerson());
        e.setTelepon(p.getTelepon());
        e.setMerk(p.getMerk());
        e.setJenis(p.getJenis());
        e.setEmail(p.getEmail());
        e.setJenisKelamin(p.getJenisKelamin());
        e.setKeterangan(p.getKeterangan());
        e.setKodePos(p.getKodePos());
        e.setKota(p.getKota());
        e.setNoHp(p.getNoHp());
        e.setNoTelepon(p.getNoTelepon());
        e.setTanggalJoin(p.getTanggalJoin());
        e.setSumber(p.getSumber());
        e.setMergedIntoId(p.getMergedIntoId());
        return e;
    }
}
