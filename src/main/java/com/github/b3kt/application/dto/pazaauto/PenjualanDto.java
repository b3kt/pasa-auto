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
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@RegisterForReflection
public class PenjualanDto {

    private Long id;

    @Size(max = 15, message = "No Penjualan must not exceed 15 characters")
    private String noPenjualan;

    private Date tanggalJamPenjualan;

    @Size(max = 11, message = "No SPK must not exceed 11 characters")
    private String noSpk;

    @DecimalMin(value = "0.0", inclusive = true, message = "Grand Total must be non-negative")
    private BigDecimal grandTotal;

    @DecimalMin(value = "0.0", inclusive = true, message = "Kembalian must be non-negative")
    private BigDecimal kembalian;

    @Size(max = 500, message = "Keterangan must not exceed 500 characters")
    private String keterangan;

    @Size(max = 20, message = "Status Pembayaran must not exceed 20 characters")
    private String statusPembayaran;

    @DecimalMin(value = "0.0", inclusive = true, message = "Uang Dibayar must be non-negative")
    private BigDecimal uangDibayar;

    private Long karyawanId;

    private Long kendaraanId;

    private Long pelangganId;

    @Size(max = 20, message = "Metode Pembayaran must not exceed 20 characters")
    private String metodePembayaran;

    @DecimalMin(value = "0.0", inclusive = true, message = "Discount must be non-negative")
    private BigDecimal discount;

    private String namaPelanggan;
    private String alamatPelanggan;
    private String merkKendaraan;
    private String jenisKendaraan;
    private String noPolisi;

    private List<PenjualanDetailDto> details;
}