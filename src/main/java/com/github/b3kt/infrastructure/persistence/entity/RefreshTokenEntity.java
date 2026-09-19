package com.github.b3kt.infrastructure.persistence.entity;

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
 * Server-side record of an issued refresh token, keyed by the token's {@code jti} claim.
 * Tokens issued from one login share a {@code familyId}; rotating a token keeps the family,
 * so a stolen token that gets reused can take down the whole chain.
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refresh_tokens_username", columnList = "username"),
        @Index(name = "idx_refresh_tokens_family_id", columnList = "family_id")
})
@Getter
@Setter
@NoArgsConstructor
public class RefreshTokenEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "family_id", nullable = false, length = 36)
    private String familyId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** Set when the token was exchanged for a new pair. */
    @Column(name = "rotated_at")
    private LocalDateTime rotatedAt;

    /** Set on logout or when reuse of a rotated token was detected. */
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
}
