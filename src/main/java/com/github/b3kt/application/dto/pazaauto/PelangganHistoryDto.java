package com.github.b3kt.application.dto.pazaauto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * History for one customer: all owned vehicle instances, current and past,
 * and all transactions across those vehicles.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@RegisterForReflection
public class PelangganHistoryDto {

    private Long idPelanggan;
    private String namaPelanggan;
    private String alamat;
    private String noHp;

    private List<KendaraanOwnershipDto> vehicles;
    private List<VehicleTransactionDto> transactions;
}