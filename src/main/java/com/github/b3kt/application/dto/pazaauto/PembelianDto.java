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
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@RegisterForReflection
public class PembelianDto {

    private Long id;

    @NotBlank(message = "No Pembelian is required")
    @Size(max = 15, message = "No Pembelian must not exceed 15 characters")
    private String noPembelian;

    private Integer noUrut;

    private LocalDateTime tanggalPembelian;

    @NotBlank(message = "Jenis Pembelian is required")
    @Size(max = 20, message = "Jenis Pembelian must not exceed 20 characters")
    private String jenisPembelian;

    @Size(max = 50, message = "Jenis Operasional must not exceed 50 characters")
    private String jenisOperasional;

    @Size(max = 20, message = "Kategori Operasional must not exceed 20 characters")
    private String kategoriOperasional;

    private Integer supplierId;

    @DecimalMin(value = "0.0", inclusive = true, message = "Grand Total must be non-negative")
    private BigDecimal grandTotal;

    @Size(max = 20, message = "Jenis Pembayaran must not exceed 20 characters")
    private String jenisPembayaran;

    @Size(max = 20, message = "Status Pembayaran must not exceed 20 characters")
    private String statusPembayaran;

    @Size(max = 20, message = "Metode Pembayaran must not exceed 20 characters")
    private String metodePembayaran;

    @DecimalMin(value = "0.0", inclusive = true, message = "Diskon must be non-negative")
    private BigDecimal diskon;

    @Size(max = 500, message = "Keterangan must not exceed 500 characters")
    private String keterangan;

    private Long karyawanId;

    private String namaSupplier;

    private List<PembelianDetailDto> details;
}