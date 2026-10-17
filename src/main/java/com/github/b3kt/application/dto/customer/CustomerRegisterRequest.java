package com.github.b3kt.application.dto.customer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** Registration with an email, a phone number, or both; each one becomes a login identifier. */
@Getter
@Setter
public class CustomerRegisterRequest {

    @Email(message = "Email tidak valid")
    private String email;

    private String phone;

    @NotBlank(message = "Password harus diisi")
    private String password;

    @NotBlank(message = "Nama harus diisi")
    private String fullName;

    @JsonIgnore
    @AssertTrue(message = "Isi email atau nomor HP")
    public boolean isEmailOrPhoneProvided() {
        return (email != null && !email.isBlank()) || (phone != null && !phone.isBlank());
    }
}
