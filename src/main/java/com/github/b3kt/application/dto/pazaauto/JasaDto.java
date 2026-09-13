package com.github.b3kt.application.dto.pazaauto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@RegisterForReflection
public class JasaDto {

    private Long id;

    @NotBlank(message = "Nama Jasa is required")
    @Size(max = 100, message = "Nama Jasa must not exceed 100 characters")
    private String namaJasa;

    @NotNull(message = "Harga Jasa is required")
    @Min(value = 0, message = "Harga Jasa must be non-negative")
    private Integer hargaJasa;

    @Size(max = 500, message = "Deskripsi must not exceed 500 characters")
    private String deskripsi;

    @Min(value = 0, message = "Estimasi Waktu must be non-negative")
    private Integer estimasiWaktu;

    private Integer hargaMaster;
}