package com.github.b3kt.application.dto.customer;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Admin-facing view of a {@code CustomerVehicleClaimRequestEntity}, enriched with names for the review queue. */
@Getter
@Setter
public class VehicleClaimAdminDto {
    private Long id;
    private Long customerAccountId;
    private String customerEmail;
    private String customerFullName;
    private Long pelangganId;
    private String namaPelanggan;
    private String nopol;
    private Long currentOwnerPelangganId;
    private String currentOwnerName;
    private String status;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewNote;
    private LocalDateTime createdAt;
}
