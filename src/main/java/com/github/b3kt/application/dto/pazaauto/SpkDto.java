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
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@RegisterForReflection
public class SpkDto {

    @Schema(example = "1", description = "Unique identifier")
    private Long id;

    @NotBlank(message = "No SPK is required")
    @Size(max = 30, message = "No SPK must not exceed 30 characters")
    @Schema(example = "SPK2024081601", description = "SPK number (format: SPKYYYYMMDDNN)")
    private String noSpk;

    @Schema(example = "1", description = "Queue number")
    private Integer noAntrian;

    @Size(max = 25, message = "Tanggal Jam SPK must not exceed 25 characters")
    @Schema(example = "2024-08-16 08:30:00", description = "SPK date and time")
    private String tanggalJamSpk;

    @Size(max = 10, message = "No Polisi must not exceed 10 characters")
    @Schema(example = "B1234XYZ", description = "Vehicle license plate")
    private String nopol;

    @Schema(example = "10", description = "Customer ID")
    private Long pelangganId;

    @Size(max = 300, message = "Nama Karyawan must not exceed 300 characters")
    @Schema(example = "John Doe, Jane Smith", description = "Mechanic names (comma separated)")
    private String namaKaryawan;

    @Schema(example = "50000", description = "Current odometer reading (km)")
    private Integer km;

    @Size(max = 20, message = "Status SPK must not exceed 20 characters")
    @Schema(example = "MENUNGGU", description = "SPK status: MENUNGGU, PROSES, SELESAI, BATAL")
    private String statusSpk;

    @Schema(example = "2024-08-16T08:30:00", description = "Work start timestamp")
    private LocalDateTime startedAt;

    @Schema(example = "2024-08-16T17:00:00", description = "Work finish timestamp")
    private LocalDateTime finishedAt;

    @Size(max = 1000, message = "Keterangan must not exceed 1000 characters")
    @Schema(example = "Routine maintenance", description = "Additional notes")
    private String keterangan;

    @Schema(example = "50000", description = "Current km at time of SPK")
    private Integer kmSaatIni;

    @Size(max = 20, message = "Status must not exceed 20 characters")
    @Schema(example = "AKTIF", description = "Record status")
    private String status;

    @Schema(example = "5", description = "Primary mechanic ID")
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