package com.github.b3kt.application.dto.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.subentity.SpkMekanik;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
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
public class SpkDetailDto {

    @Size(max = 30, message = "No SPK must not exceed 30 characters")
    private String noSpk;

    @Min(value = 1, message = "Urut must be at least 1")
    private Integer urut;

    @NotNull(message = "Harga is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Harga must be greater than 0")
    private BigDecimal harga;

    @DecimalMin(value = "0.0", inclusive = true, message = "Harga Master must be non-negative")
    private BigDecimal hargaMaster;

    @NotNull(message = "Jumlah is required")
    @Min(value = 1, message = "Jumlah must be at least 1")
    private Integer jumlah;

    @Size(max = 500, message = "Keterangan must not exceed 500 characters")
    private String keterangan;

    private Long jasaId;
    private Long sparepartId;
    private String namaJasa;
    private String namaSparepart;
}