package com.github.b3kt.application.dto.customer;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@RegisterForReflection
public class CustomerBookingDto {
    private Long id;
    private String nopol;
    private Integer km;
    private LocalDateTime requestedDateTime;
    private String serviceType;
    private String description;
    private String status;
    private String staffNote;
    private Long resultingSpkId;
    private LocalDateTime createdAt;
}
