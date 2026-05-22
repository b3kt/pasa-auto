package com.github.b3kt.domain.model.pazaauto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class Pelanggan {
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

    public Pelanggan(String nopol, String namaPelanggan, String merk) {
        setNopol(nopol);
        setNamaPelanggan(namaPelanggan);
        setMerk(merk);
    }

    public void setNopol(String nopol) {
        if (nopol == null || nopol.trim().isEmpty()) {
            throw new IllegalArgumentException("nopol must not be blank");
        }
        this.nopol = nopol;
    }

    public void setNamaPelanggan(String namaPelanggan) {
        if (namaPelanggan == null || namaPelanggan.trim().isEmpty()) {
            throw new IllegalArgumentException("namaPelanggan must not be blank");
        }
        this.namaPelanggan = namaPelanggan;
    }

    public void setMerk(String merk) {
        if (merk == null || merk.trim().isEmpty()) {
            throw new IllegalArgumentException("merk must not be blank");
        }
        this.merk = merk;
    }
}
