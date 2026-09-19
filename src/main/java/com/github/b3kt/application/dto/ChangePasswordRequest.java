package com.github.b3kt.application.dto;

import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * DTO for changing the current user's password.
 */
@Schema(description = "Request to change the current user's password")
public class ChangePasswordRequest {

    @NotBlank(message = "Password lama harus diisi")
    @Schema(description = "The current password", required = true)
    private String currentPassword;

    @NotBlank(message = "Password baru harus diisi")
    @Schema(description = "The new password (at least 8 characters)", required = true)
    private String newPassword;

    public ChangePasswordRequest() {
    }

    public ChangePasswordRequest(String currentPassword, String newPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
