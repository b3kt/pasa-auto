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
public class SparepartDto {

    private Long id;

    @Size(max = 9, message = "Kode Barang must not exceed 9 characters")
    private String kodeBarang;

    @Size(max = 40, message = "Nama Barang must not exceed 40 characters")
    private String namaBarang;

    @DecimalMin(value = "0.0", inclusive = true, message = "Harga Beli must be non-negative")
    private BigDecimal hargaBeli;

    @NotNull(message = "Harga Jual is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Harga Jual must be non-negative")
    private BigDecimal hargaJual;

    @Size(max = 500, message = "Keterangan must not exceed 500 characters")
    private String keterangan;

    @NotBlank(message = "Kode Sparepart is required")
    @Size(max = 20, message = "Kode Sparepart must not exceed 20 characters")
    private String kodeSparepart;

    @Size(max = 50, message = "Merek must not exceed 50 characters")
    private String merek;

    @NotBlank(message = "Nama Sparepart is required")
    @Size(max = 100, message = "Nama Sparepart must not exceed 100 characters")
    private String namaSparepart;

    @Size(max = 20, message = "Satuan must not exceed 20 characters")
    private String satuan;

    @Min(value = 0, message = "Stok must be non-negative")
    private Integer stok;

    @Min(value = 0, message = "Stok Minimal must be non-negative")
    private Integer stokMinimal;

    @Size(max = 50, message = "Tipe Kendaraan must not exceed 50 characters")
    private String tipeKendaraan;

    private Long supplierId;

    private Boolean active;
}