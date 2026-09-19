package com.github.b3kt.application.service;

import com.github.b3kt.domain.exception.AuthenticationException;
import com.github.b3kt.domain.model.User;
import com.github.b3kt.infrastructure.persistence.entity.RefreshTokenEntity;
import com.github.b3kt.infrastructure.persistence.repository.RefreshTokenRepository;
import com.github.b3kt.infrastructure.security.JwtTokenService;
import com.github.b3kt.infrastructure.security.RefreshTokenClaims;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tracks issued refresh tokens so they can be rotated on use and revoked on logout.
 */
@Slf4j
@ApplicationScoped
public class RefreshTokenService {

    static final String INVALID_TOKEN_MESSAGE = "Invalid or expired refresh token";

    @Inject
    RefreshTokenRepository repository;

    @Inject
    JwtTokenService jwtTokenService;

    /**
     * How long an already-rotated token is still accepted. Covers two tabs or a retried request
     * refreshing with the same token at nearly the same time; later reuse is treated as theft.
     */
    @ConfigProperty(name = "jwt.refresh.reuse-grace-seconds", defaultValue = "30")
    long reuseGraceSeconds;

    /**
     * Issue a refresh token that starts a new family (a new login).
     */
    @Transactional
    public String issue(User user) {
        return issue(user, UUID.randomUUID().toString());
    }

    /**
     * Issue a refresh token in an existing family (a rotation).
     */
    @Transactional
    public String issue(User user, String familyId) {
        LocalDateTime now = LocalDateTime.now();
        repository.deleteExpiredForUser(user.getUsername(), now);

        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setUsername(user.getUsername());
        entity.setFamilyId(familyId);
        entity.setCreatedAt(now);
        entity.setExpiresAt(now.plus(jwtTokenService.getRefreshTokenLifetime()));
        repository.persist(entity);

        return jwtTokenService.generateRefreshToken(user, entity.getId());
    }

    /**
     * Mark a refresh token as used and return its family id, so the replacement joins the same family.
     * A rotated token presented again after the grace period means it leaked, so the whole family is revoked.
     */
    @Transactional(dontRollbackOn = AuthenticationException.class)
    public String consume(RefreshTokenClaims claims) {
        LocalDateTime now = LocalDateTime.now();
        RefreshTokenEntity token = repository.findByIdForUpdate(claims.tokenId())
                .filter(t -> t.getUsername().equals(claims.username()))
                .orElseThrow(() -> new AuthenticationException(INVALID_TOKEN_MESSAGE));

        if (token.getRevokedAt() != null || token.getExpiresAt().isBefore(now)) {
            throw new AuthenticationException(INVALID_TOKEN_MESSAGE);
        }

        if (token.getRotatedAt() != null) {
            if (token.getRotatedAt().plusSeconds(reuseGraceSeconds).isBefore(now)) {
                repository.revokeFamily(token.getFamilyId(), now);
                log.warn("Reuse of rotated refresh token detected for user {}; revoked its session", token.getUsername());
                throw new AuthenticationException(INVALID_TOKEN_MESSAGE);
            }
            return token.getFamilyId();
        }

        token.setRotatedAt(now);
        return token.getFamilyId();
    }

    /**
     * Revoke the session (token family) the given refresh token belongs to.
     */
    @Transactional
    public void revokeSession(RefreshTokenClaims claims) {
        repository.findByIdOptional(claims.tokenId())
                .filter(t -> t.getUsername().equals(claims.username()))
                .ifPresent(t -> repository.revokeFamily(t.getFamilyId(), LocalDateTime.now()));
    }

    /**
     * Revoke every refresh token of a user, ending all of their sessions.
     */
    @Transactional
    public void revokeAllForUser(String username) {
        repository.revokeAllForUser(username, LocalDateTime.now());
    }
}
