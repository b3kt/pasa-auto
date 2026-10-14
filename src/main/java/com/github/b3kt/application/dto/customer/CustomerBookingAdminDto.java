package com.github.b3kt.application.dto.customer;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Admin-facing view of a {@code CustomerBookingEntity}, enriched with the customer's name for the review queue. */
@Getter
@Setter
public class CustomerBookingAdminDto {
    private Long id;
    private Long customerAccountId;
    private String customerEmail;
    private Long pelangganId;
    private String namaPelanggan;
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
