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
public class KaryawanPosisiDto {

    private Long id;

    @NotBlank(message = "Posisi is required")
    @Size(max = 20, message = "Posisi must not exceed 20 characters")
    private String posisi;

    @Size(max = 255, message = "Keterangan must not exceed 255 characters")
    private String keterangan;
}