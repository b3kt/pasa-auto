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

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@RegisterForReflection
public class PenjualanDetailDto {

    private Long id;

    @Size(max = 10, message = "Kategori must not exceed 10 characters")
    private String kategori;

    @NotNull(message = "Harga Jual is required")
    @Min(value = 0, message = "Harga Jual must be non-negative")
    private Integer hargaJual;

    @NotNull(message = "Kuantiti is required")
    @Min(value = 1, message = "Kuantiti must be at least 1")
    private Integer kuantiti;

    private Integer total;

    @DecimalMin(value = "0.0", inclusive = true, message = "Diskon must be non-negative")
    private BigDecimal diskon;

    @Size(max = 255, message = "Keterangan must not exceed 255 characters")
    private String keterangan;

    private Long barangId;
    private Long jasaId;
    private Long sparepartId;

    @Size(max = 15, message = "No Penjualan must not exceed 15 characters")
    private String noPenjualan;

    @Size(max = 100, message = "Nama Jasa Barang must not exceed 100 characters")
    private String namaJasaBarang;
}