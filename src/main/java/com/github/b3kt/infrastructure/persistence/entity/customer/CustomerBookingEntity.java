package com.github.b3kt.infrastructure.persistence.entity.customer;

import com.github.b3kt.infrastructure.persistence.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A customer's request to book a service appointment. Deliberately not a {@code tb_spk} work
 * order - staff reviews it and converts it into one once the vehicle actually arrives.
 */
@Entity
@Table(name = "customer_bookings")
@Getter
@Setter
public class CustomerBookingEntity extends BaseEntity {

    public static final String STATUS_REQUESTED = "REQUESTED";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_CONVERTED = "CONVERTED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    @Column(name = "id_customer_account", nullable = false)
    private Long customerAccountId;

    /** Denormalized at booking time so a later unlink/relink doesn't change a historical booking's ownership. */
    @Column(name = "id_pelanggan", nullable = false)
    private Long pelangganId;

    @Column(name = "nopol", length = 10)
    private String nopol;

    @Column(name = "requested_date_time", nullable = false)
    private LocalDateTime requestedDateTime;

    @Column(name = "service_type", length = 100)
    private String serviceType;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "status", length = 20, nullable = false)
    private String status = STATUS_REQUESTED;

    @Column(name = "staff_note", length = 500)
    private String staffNote;

    @Column(name = "id_resulting_spk")
    private Long resultingSpkId;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "converted_at")
    private LocalDateTime convertedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
}
