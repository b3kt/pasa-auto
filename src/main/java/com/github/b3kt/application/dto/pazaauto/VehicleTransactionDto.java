package com.github.b3kt.application.dto.pazaauto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A single historical transaction (SPK or Penjualan) for a vehicle/customer,
 * with the owner that was active at the time of the transaction.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@RegisterForReflection
public class VehicleTransactionDto {

    private String jenis; // SPK or PENJUALAN

    private String noSpk;
    private String noPenjualan;

    private LocalDateTime tanggal;
    private LocalDate tanggalOnly;

    private String status;
    private BigDecimal grandTotal;

    private Long ownerPelangganId;
    private String ownerNamaPelanggan;
}