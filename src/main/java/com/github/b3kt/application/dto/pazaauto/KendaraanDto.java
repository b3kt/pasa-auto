package com.github.b3kt.application.dto.pazaauto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;
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
public class KendaraanDto {

    private Long id;

    @NotBlank(message = "Jenis is required")
    @Size(max = 50, message = "Jenis must not exceed 50 characters")
    private String jenis;

    @Size(max = 50, message = "Merk must not exceed 50 characters")
    private String merk;

    @Size(max = 500, message = "Keterangan must not exceed 500 characters")
    private String keterangan;

    @Size(max = 50, message = "Model must not exceed 50 characters")
    private String model;
}