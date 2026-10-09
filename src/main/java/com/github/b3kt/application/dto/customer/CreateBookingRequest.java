package com.github.b3kt.application.dto.customer;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CreateBookingRequest {

    private String nopol;

    @NotNull(message = "Tanggal booking harus diisi")
    private LocalDateTime requestedDateTime;

    private String serviceType;

    private String description;
}
