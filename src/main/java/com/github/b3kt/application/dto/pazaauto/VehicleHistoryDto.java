package com.github.b3kt.application.dto.pazaauto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Full history for one vehicle instance (nopol): its owners over time
 * and all transactions bound to it.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@RegisterForReflection
public class VehicleHistoryDto {

    private String nopol;
    private Long idKendaraan;
    private String merk;
    private String jenis;
    private String model;

    private List<KendaraanOwnershipDto> owners;
    private List<VehicleTransactionDto> transactions;
}