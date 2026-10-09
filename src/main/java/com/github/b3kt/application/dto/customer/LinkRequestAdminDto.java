package com.github.b3kt.application.dto.customer;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Admin-facing view of a {@code CustomerPelangganLinkRequestEntity}, enriched with names for the approval queue. */
@Getter
@Setter
public class LinkRequestAdminDto {
    private Long id;
    private Long customerAccountId;
    private String customerEmail;
    private String customerFullName;
    private Long pelangganId;
    private String namaPelanggan;
    private String matchType;
    private String matchValue;
    private String status;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewNote;
    private LocalDateTime createdAt;
}
