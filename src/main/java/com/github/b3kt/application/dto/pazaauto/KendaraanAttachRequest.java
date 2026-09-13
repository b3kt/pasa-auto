package com.github.b3kt.application.dto.pazaauto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request to attach an additional vehicle (new nopol) to an existing customer.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@RegisterForReflection
public class KendaraanAttachRequest {

    @NotBlank(message = "nopol is required")
    @Size(max = 10, message = "nopol must not exceed 10 characters")
    private String nopol;

    private String merk;

    private String jenis;

    private LocalDate tanggalMulai;

    @Size(max = 500, message = "keterangan must not exceed 500 characters")
    private String keterangan;
}