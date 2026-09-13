package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.subentity.SpkMekanik;

import java.util.List;

public interface SpkEnrichable {
    String getNopol();
    void setNopol(String nopol);
    Long getPelangganId();
    void setPelangganId(Long pelangganId);
    String getNamaPelanggan();
    void setNamaPelanggan(String namaPelanggan);
    String getAlamatPelanggan();
    void setAlamatPelanggan(String alamatPelanggan);
    String getMerkKendaraan();
    void setMerkKendaraan(String merkKendaraan);
    String getJenisKendaraan();
    void setJenisKendaraan(String jenisKendaraan);
    Long getMekanikId();
    void setMekanikId(Long mekanikId);
    List<SpkMekanik> getMekanikList();
    void setMekanikList(List<SpkMekanik> mekanikList);
    String getNamaKaryawan();
    void setNamaKaryawan(String namaKaryawan);
    Integer getKm();
    void setKm(Integer km);
    Integer getKmSaatIni();
    void setKmSaatIni(Integer kmSaatIni);
}
