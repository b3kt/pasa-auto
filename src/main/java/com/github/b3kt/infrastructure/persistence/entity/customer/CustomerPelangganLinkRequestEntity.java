package com.github.b3kt.infrastructure.persistence.entity.customer;

import com.github.b3kt.infrastructure.persistence.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A pending claim on a pelanggan record that is already linked to a different customer account.
 * Only created for the "conflict" path - auto-link never creates a row here. Reviewed by staff.
 */
@Entity
@Table(name = "tb_customer_pelanggan_link_requests")
@Getter
@Setter
public class CustomerPelangganLinkRequestEntity extends BaseEntity {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    public static final String MATCH_NOPOL = "NOPOL";
    public static final String MATCH_CONTACT = "CONTACT";

    @Column(name = "id_customer_account", nullable = false)
    private Long customerAccountId;

    @Column(name = "id_pelanggan", nullable = false)
    private Long pelangganId;

    @Column(name = "match_type", length = 20, nullable = false)
    private String matchType;

    @Column(name = "match_value", length = 100)
    private String matchValue;

    @Column(name = "status", length = 20, nullable = false)
    private String status = STATUS_PENDING;

    @Column(name = "reviewed_by", length = 50)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_note", length = 500)
    private String reviewNote;
}
