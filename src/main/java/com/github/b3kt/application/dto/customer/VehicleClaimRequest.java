package com.github.b3kt.application.dto.customer;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** A customer's request to claim a plate already registered to another customer. */
@Getter
@Setter
@RegisterForReflection
public class VehicleClaimRequest {
    @NotBlank(message = "nopol is required")
    @Size(max = 10)
    private String nopol;
}
