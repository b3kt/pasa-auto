package com.github.b3kt.infrastructure.persistence.entity.customer;

import com.github.b3kt.infrastructure.persistence.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A customer's login identity for the self-service portal. Independent of {@code tb_pelanggan}
 * (the business record): a customer registers here first, then links to their existing
 * pelanggan/vehicle history as a separate step. See {@code idPelanggan} for the strict 1:1 link.
 */
@Entity
@Table(name = "customer_accounts")
@Getter
@Setter
public class CustomerAccountEntity extends BaseEntity {

    @Column(name = "email", length = 150, nullable = false)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    @Column(name = "full_name", length = 150, nullable = false)
    private String fullName;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    /** Nullable until the customer links to an existing pelanggan record; strict 1:1 enforced by a DB partial unique index. */
    @Column(name = "id_pelanggan")
    private Long pelangganId;

    @Column(name = "linked_at")
    private LocalDateTime linkedAt;

    public boolean isLinked() {
        return pelangganId != null;
    }
}
