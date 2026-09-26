package com.github.b3kt.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * DTO for updating the current user's own profile.
 */
@Schema(description = "Request to update the current user's profile")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateProfileRequest {

    @NotBlank(message = "Email harus diisi")
    @Email(message = "Email tidak valid")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    @Schema(description = "The new email address", required = true)
    private String email;
}
