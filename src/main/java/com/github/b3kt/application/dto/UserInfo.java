package com.github.b3kt.application.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * DTO for user information.
 */
@Schema(description = "User information")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInfo {
    @Schema(description = "Username", example = "admin")
    private String username;

    @Schema(description = "User email address", example = "admin@example.com")
    private String email;

    @Schema(description = "User roles", example = "[\"user\", \"admin\"]")
    private Set<String> roles;

    @Schema(description = "Karyawan ID", example = "1")
    private Long karyawanId;

    @Schema(description = "Karyawan name", example = "John Doe")
    private String karyawanNama;

}
