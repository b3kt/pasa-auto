package com.github.b3kt.application.dto.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.subentity.SpkMekanik;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@RegisterForReflection
public class SpkDto {

    private Long id;

    @NotBlank(message = "No SPK is required")
    @Size(max = 30, message = "No SPK must not exceed 30 characters")
    private String noSpk;

    private Integer noAntrian;

    @Size(max = 25, message = "Tanggal Jam SPK must not exceed 25 characters")
    private String tanggalJamSpk;

    @Size(max = 10, message = "No Polisi must not exceed 10 characters")
    private String nopol;

    private Long pelangganId;

    @Size(max = 300, message = "Nama Karyawan must not exceed 300 characters")
    private String namaKaryawan;

    private Integer km;

    @Size(max = 20, message = "Status SPK must not exceed 20 characters")
    private String statusSpk;

    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    @Size(max = 1000, message = "Keterangan must not exceed 1000 characters")
    private String keterangan;

    private Integer kmSaatIni;

    @Size(max = 20, message = "Status must not exceed 20 characters")
    private String status;

    private Long mekanikId;
    private List<SpkMekanik> mekanikList;
    private List<SpkDetailDto> details;
    private boolean startProcess;
    private String namaPelanggan;
    private String alamatPelanggan;
    private String merkKendaraan;
    private String jenisKendaraan;
    private BigDecimal grandTotal;
}