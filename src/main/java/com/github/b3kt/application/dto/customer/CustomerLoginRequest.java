package com.github.b3kt.application.dto.customer;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerLoginRequest {

    @NotBlank(message = "Email harus diisi")
    private String email;

    @NotBlank(message = "Password harus diisi")
    private String password;
}
