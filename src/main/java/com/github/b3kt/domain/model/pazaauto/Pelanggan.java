package com.github.b3kt.domain.model.pazaauto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * A workshop customer. Vehicles are owned through {@code tb_pelanggan_kendaraan} (ADR-0010/0011), so a
 * customer may have zero, one or many vehicles; {@code nopol}/{@code merk}/{@code jenis} here are the
 * deprecated single-vehicle view of the primary vehicle and are optional.
 */
@Getter
@Setter
@NoArgsConstructor
public class Pelanggan {

    /** Record entered by workshop staff. */
    public static final String SUMBER_STAFF = "STAFF";
    /** Record created by the customer through the self-service portal. */
    public static final String SUMBER_PORTAL = "PORTAL";

    private Long id;
    private String nopol;
    private String namaPelanggan;
    private String alamat;
    private String contactPerson;
    private String telepon;
    private String merk;
    private String jenis;
    private String email;
    private String jenisKelamin;
    private String keterangan;
    private String kodePos;
    private String kota;
    private String noHp;
    private String noTelepon;
    private LocalDate tanggalJoin;
    private String sumber;
    private Long mergedIntoId;

    public Pelanggan(String nopol, String namaPelanggan, String merk) {
        setNopol(nopol);
        setNamaPelanggan(namaPelanggan);
        setMerk(merk);
    }

    /** Optional: a blank plate means the customer has no primary vehicle. */
    public void setNopol(String nopol) {
        this.nopol = blankToNull(nopol);
    }

    public void setNamaPelanggan(String namaPelanggan) {
        if (namaPelanggan == null || namaPelanggan.trim().isEmpty()) {
            throw new IllegalArgumentException("namaPelanggan must not be blank");
        }
        this.namaPelanggan = namaPelanggan;
    }

    /** Optional: only meaningful together with a plate. */
    public void setMerk(String merk) {
        this.merk = blankToNull(merk);
    }

    private static String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value;
    }
}
