package com.github.b3kt.infrastructure.persistence.entity.customer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Server-side record of an issued customer refresh token, mirroring {@code RefreshTokenEntity}
 * but keyed by {@code customerAccountId} rather than a staff username - a separate identity space.
 */
@Entity
@Table(name = "tb_customer_refresh_tokens", indexes = {
        @Index(name = "idx_tb_customer_refresh_tokens_account", columnList = "id_customer_account"),
        @Index(name = "idx_tb_customer_refresh_tokens_family_id", columnList = "family_id")
})
@Getter
@Setter
@NoArgsConstructor
public class CustomerRefreshTokenEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "id_customer_account", nullable = false)
    private Long customerAccountId;

    @Column(name = "family_id", nullable = false, length = 36)
    private String familyId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "rotated_at")
    private LocalDateTime rotatedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
}
