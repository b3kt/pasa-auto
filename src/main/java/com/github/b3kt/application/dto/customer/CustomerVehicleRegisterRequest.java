package com.github.b3kt.application.dto.customer;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A customer registering a plate from the portal. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RegisterForReflection
public class CustomerVehicleRegisterRequest {

    @NotBlank(message = "nopol is required")
    @Size(max = 10, message = "nopol must not exceed 10 characters")
    private String nopol;

    private String merk;

    private String jenis;

    /**
     * Answer to "this plate is already in our records - is it yours?" (check status
     * {@link NopolCheckDto#ON_RECORD}). {@code true}: the existing customer record is the caller's and is
     * linked to their account. {@code false} or no answer: only the vehicle is transferred to the
     * caller's own record. Ignored for plates that are not on record.
     */
    private Boolean ownRecord;
}
