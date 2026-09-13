package com.github.b3kt.application.dto.pazaauto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request to transfer a vehicle instance (nopol) to another customer.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@RegisterForReflection
public class KendaraanTransferRequest {

    @NotBlank(message = "nopol is required")
    @Size(max = 10, message = "nopol must not exceed 10 characters")
    private String nopol;

    @NotNull(message = "idPelangganBaru is required")
    private Long idPelangganBaru;

    private LocalDate tanggalAwal;

    @Size(max = 500, message = "keterangan must not exceed 500 characters")
    private String keterangan;
}