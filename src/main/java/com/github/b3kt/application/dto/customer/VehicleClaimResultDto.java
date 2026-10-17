package com.github.b3kt.application.dto.customer;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Outcome of a vehicle claim request, so the portal can tell the customer what happened. */
@Getter
@AllArgsConstructor
public class VehicleClaimResultDto {
    public static final String STATUS_LINKED = "LINKED";
    public static final String STATUS_PENDING_APPROVAL = "PENDING_APPROVAL";

    private String status;
    private String message;
}
