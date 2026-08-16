package com.github.b3kt.application.dto.pazaauto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.DecimalMin;
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
public class BarangDto {

    private Long id;

    @NotBlank(message = "Nama Barang is required")
    @Size(max = 100, message = "Nama Barang must not exceed 100 characters")
    private String namaBarang;

    @NotNull(message = "Harga Jual is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Harga Jual must be non-negative")
    private BigDecimal hargaJual;

    @DecimalMin(value = "0.0", inclusive = true, message = "Harga Beli must be non-negative")
    private BigDecimal hargaBeli;

    @Size(max = 500, message = "Keterangan must not exceed 500 characters")
    private String keterangan;

    @Size(max = 20, message = "Satuan must not exceed 20 characters")
    private String satuan;

    @Min(value = 0, message = "Stok must be non-negative")
    private Integer stok;

    @Min(value = 0, message = "Stok Minimal must be non-negative")
    private Integer stokMinimal;

    private Integer supplierId;

    private Boolean active;

    @Size(max = 20, message = "Kode Barang must not exceed 20 characters")
    private String kodeBarang;
}