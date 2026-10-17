package com.github.b3kt.infrastructure.persistence.entity.customer;

import com.github.b3kt.infrastructure.persistence.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A claim by a customer on a plate (nopol) currently owned by another customer. Only used for the
 * conflict path - when the plate is free, the normal vehicle registration links it directly.
 * Default flow queues the row for Admin/Owner review; with {@code IMMEDIATE} mode it is never created.
 */
@Entity
@Table(name = "tb_customer_vehicle_claim_requests")
@Getter
@Setter
public class CustomerVehicleClaimRequestEntity extends BaseEntity {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    @Column(name = "id_customer_account", nullable = false)
    private Long customerAccountId;

    @Column(name = "id_pelanggan", nullable = false)
    private Long pelangganId;

    @Column(name = "nopol", length = 10, nullable = false)
    private String nopol;

    @Column(name = "id_current_owner")
    private Long currentOwnerPelangganId;

    @Column(name = "status", length = 20, nullable = false)
    private String status = STATUS_PENDING;

    @Column(name = "reviewed_by", length = 50)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_note", length = 500)
    private String reviewNote;
}
