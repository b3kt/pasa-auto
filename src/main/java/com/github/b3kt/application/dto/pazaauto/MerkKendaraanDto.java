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
public class MerkKendaraanDto {

    private Long id;

    @NotBlank(message = "Nama merk is required")
    @Size(max = 50, message = "Nama merk must not exceed 50 characters")
    private String nama;

    @Size(max = 500, message = "Keterangan must not exceed 500 characters")
    private String keterangan;
}
