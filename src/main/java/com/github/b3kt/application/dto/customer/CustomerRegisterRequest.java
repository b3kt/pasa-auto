package com.github.b3kt.application.dto.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerRegisterRequest {

    @NotBlank(message = "Email harus diisi")
    @Email(message = "Email tidak valid")
    private String email;

    private String phone;

    @NotBlank(message = "Password harus diisi")
    private String password;

    @NotBlank(message = "Nama harus diisi")
    private String fullName;
}
