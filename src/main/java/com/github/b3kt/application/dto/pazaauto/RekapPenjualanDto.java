package com.github.b3kt.application.dto.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPenjualanEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkDetailEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkEntity;
import com.github.b3kt.infrastructure.persistence.entity.subentity.SpkMekanik;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RekapPenjualanDto {

    private String noSpk;
    private Integer noAntrian;
    private String tanggalJamSpk;
    private String nopol;
    private Long pelangganId;
    private String namaKaryawan;
    private Integer km;
    private String statusSpk;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String keterangan;
    private Integer kmSaatIni;
    private String status;
    private Long mekanikId;
    private List<SpkMekanik> mekanikList; // Stores JSON array of mechanic assignments
    private java.util.List<TbSpkDetailEntity> details;
    private boolean startProcess;
    private String namaPelanggan;
    private String alamatPelanggan;
    private String merkKendaraan;
    private String jenisKendaraan;
    private BigDecimal grandTotal;

    private String noPenjualan;
    private Date tanggalJamPenjualan;
    private BigDecimal kembalian;
    private String statusPembayaran;
    private BigDecimal uangDibayar;
    private Long karyawanId;
    private Long kendaraanId;
    private String metodePembayaran;
    private String noPolisi;

    public RekapPenjualanDto(TbSpkEntity spk, TbPenjualanEntity penjualan){
        noSpk = spk.getNoSpk();
        noAntrian = spk.getNoAntrian();
        tanggalJamSpk = spk.getTanggalJamSpk();
        nopol = spk.getNopol();
        pelangganId = spk.getPelangganId();
        namaKaryawan = spk.getNamaKaryawan();
        km = spk.getKm();
        statusSpk = spk.getStatusSpk();
        startedAt = spk.getStartedAt();
        finishedAt = spk.getFinishedAt();
        keterangan = spk.getKeterangan();
        kmSaatIni = spk.getKmSaatIni();
        status = spk.getStatus();
        mekanikId = spk.getMekanikId();

        namaPelanggan = spk.getNamaPelanggan();
        
        // Handle null penjualan case
        if (penjualan != null) {
            alamatPelanggan = penjualan.getAlamatPelanggan();
            merkKendaraan = penjualan.getMerkKendaraan();
            jenisKendaraan = penjualan.getJenisKendaraan();
            grandTotal = penjualan.getGrandTotal();
            noPenjualan = penjualan.getNoPenjualan();
            tanggalJamPenjualan = penjualan.getTanggalJamPenjualan();
            kembalian = penjualan.getKembalian();
            statusPembayaran = penjualan.getStatusPembayaran();
            uangDibayar = penjualan.getUangDibayar();
            karyawanId = penjualan.getKaryawanId();
            kendaraanId = penjualan.getKendaraanId();
            metodePembayaran = penjualan.getMetodePembayaran();
            noPolisi = penjualan.getNoPolisi();
        } else {
            // Set default values when penjualan is null
            alamatPelanggan = null;
            merkKendaraan = null;
            jenisKendaraan = null;
            grandTotal = null;
            noPenjualan = null;
            tanggalJamPenjualan = null;
            kembalian = null;
            statusPembayaran = null;
            uangDibayar = null;
            karyawanId = null;
            kendaraanId = null;
            metodePembayaran = null;
            noPolisi = null;
        }
    }
}
