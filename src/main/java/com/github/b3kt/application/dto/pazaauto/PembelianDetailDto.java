package com.github.b3kt.application.dto.pazaauto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@RegisterForReflection
public class PembelianDetailDto {

    private Long id;

    private Long pembelianId;

    @NotBlank(message = "Nama Item is required")
    @Size(max = 100, message = "Nama Item must not exceed 100 characters")
    private String namaItem;

    @Size(max = 20, message = "Kategori Item must not exceed 20 characters")
    private String kategoriItem;

    @NotNull(message = "Harga is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Harga must be greater than 0")
    private BigDecimal harga;

    @NotNull(message = "Kuantiti is required")
    @Min(value = 1, message = "Kuantiti must be at least 1")
    private Integer kuantiti;

    @DecimalMin(value = "0.0", inclusive = true, message = "Total must be non-negative")
    private BigDecimal total;

    @Size(max = 500, message = "Keterangan must not exceed 500 characters")
    private String keterangan;

    private Long barangId;

    private Long sparepartId;
}