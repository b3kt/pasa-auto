package com.github.b3kt.application.dto.customer;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@RegisterForReflection
public class LinkAccountRequest {

    @NotNull(message = "pelangganId harus diisi")
    private Long pelangganId;

    /** NOPOL or CONTACT - which search step produced this candidate; kept for the admin approval queue's context. */
    private String matchType;

    private String matchValue;
}
