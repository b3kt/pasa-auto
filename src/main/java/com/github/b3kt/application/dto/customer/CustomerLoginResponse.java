package com.github.b3kt.application.dto.customer;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;

/** Mirrors {@code LoginResponse}'s shape for the staff auth flow. */
@Getter
@Setter
@RegisterForReflection
public class CustomerLoginResponse {

    private String token;
    private String email;
    private String fullName;
    private Long expiresIn;
    private boolean linked;

    /** Carried out-of-band as an HttpOnly cookie by the resource layer; never serialized in the body. */
    private transient String refreshToken;

    public CustomerLoginResponse(String token, String refreshToken, String email, String fullName,
            Long expiresIn, boolean linked) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.email = email;
        this.fullName = fullName;
        this.expiresIn = expiresIn;
        this.linked = linked;
    }
}
