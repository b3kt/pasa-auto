package com.github.b3kt.application.dto.pazaauto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@RegisterForReflection
public class AbsensiDto {

    private Long id;

    @NotNull(message = "Karyawan ID is required")
    private Long karyawanId;

    @NotNull(message = "Tanggal is required")
    private LocalDate tanggal;

    private LocalTime jamMasuk;

    private LocalTime jamKeluar;

    @Size(max = 20, message = "Status must not exceed 20 characters")
    private String status = "HADIR";

    @Size(max = 500, message = "Keterangan must not exceed 500 characters")
    private String keterangan;

    private Boolean terlambat = false;

    private Boolean pulangCepat = false;

    private Integer lembur = 0;

    @Size(max = 200, message = "Lokasi Masuk must not exceed 200 characters")
    private String lokasiMasuk;

    @Size(max = 200, message = "Lokasi Keluar must not exceed 200 characters")
    private String lokasiKeluar;

    @Size(max = 50, message = "IP Masuk must not exceed 50 characters")
    private String ipMasuk;

    @Size(max = 50, message = "IP Keluar must not exceed 50 characters")
    private String ipKeluar;

    @Size(max = 500, message = "Device Info must not exceed 500 characters")
    private String deviceInfo;

    private String namaKaryawan;
}