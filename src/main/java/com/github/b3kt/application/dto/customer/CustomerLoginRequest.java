package com.github.b3kt.application.dto.customer;

import io.quarkus.runtime.annotations.RegisterForReflection;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@RegisterForReflection
public class CustomerLoginRequest {

    /**
     * Email or phone number. Also accepted under the old "email" key, so a portal bundle cached by the
     * PWA before this change keeps working.
     */
    @JsonAlias("email")
    @NotBlank(message = "Email atau nomor HP harus diisi")
    private String identifier;

    @NotBlank(message = "Password harus diisi")
    private String password;
}
