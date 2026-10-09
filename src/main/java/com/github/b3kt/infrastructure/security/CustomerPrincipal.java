package com.github.b3kt.infrastructure.security;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Resolves the calling customer's identity from the verified JWT, once per request. Every
 * customer-facing resource must scope its queries through {@link #pelangganIdOrThrow()} rather
 * than a request parameter, so a customer can never address another customer's data.
 */
@RequestScoped
public class CustomerPrincipal {

    @Inject
    JsonWebToken jwt;

    @Inject
    CustomerJwtTokenService customerJwtTokenService;

    public Long customerAccountId() {
        return customerJwtTokenService.extractCustomerAccountId(jwt);
    }

    /** Null when the calling account has not linked a pelanggan record yet. */
    public Long pelangganIdOrNull() {
        return customerJwtTokenService.extractPelangganId(jwt);
    }

    /** @throws IllegalArgumentException when the account isn't linked - callers must not fall back to "show everything". */
    public Long pelangganIdOrThrow() {
        Long pelangganId = pelangganIdOrNull();
        if (pelangganId == null) {
            throw new IllegalArgumentException("Link your account to a customer record first");
        }
        return pelangganId;
    }
}
