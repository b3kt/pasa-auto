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
public class AbsensiConfigDto {

    private Long id;

    @NotBlank(message = "Config Key is required")
    @Size(max = 100, message = "Config Key must not exceed 100 characters")
    private String configKey;

    @Size(max = 500, message = "Config Value must not exceed 500 characters")
    private String configValue;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Size(max = 50, message = "Config Type must not exceed 50 characters")
    private String configType; // TIME, INTEGER, BOOLEAN, STRING, IP_LIST
}