package com.github.b3kt.application.service.customer;

import com.github.b3kt.domain.exception.AuthenticationException;
import com.github.b3kt.infrastructure.persistence.entity.customer.CustomerAccountEntity;
import com.github.b3kt.infrastructure.persistence.entity.customer.CustomerRefreshTokenEntity;
import com.github.b3kt.infrastructure.persistence.repository.customer.CustomerRefreshTokenRepository;
import com.github.b3kt.infrastructure.security.CustomerJwtTokenService;
import com.github.b3kt.infrastructure.security.CustomerRefreshTokenClaims;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tracks issued customer refresh tokens so they can be rotated on use and revoked on logout.
 * Mirrors {@code RefreshTokenService}, keyed by customer account id instead of staff username.
 */
@Slf4j
@ApplicationScoped
public class CustomerRefreshTokenService {

    static final String INVALID_TOKEN_MESSAGE = "Invalid or expired refresh token";

    @Inject
    CustomerRefreshTokenRepository repository;

    @Inject
    CustomerJwtTokenService jwtTokenService;

    @ConfigProperty(name = "jwt.refresh.reuse-grace-seconds", defaultValue = "30")
    long reuseGraceSeconds;

    @Transactional
    public String issue(CustomerAccountEntity account) {
        return issue(account, UUID.randomUUID().toString());
    }

    @Transactional
    public String issue(CustomerAccountEntity account, String familyId) {
        LocalDateTime now = LocalDateTime.now();
        repository.deleteExpiredForAccount(account.getId(), now);

        CustomerRefreshTokenEntity entity = new CustomerRefreshTokenEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setCustomerAccountId(account.getId());
        entity.setFamilyId(familyId);
        entity.setCreatedAt(now);
        entity.setExpiresAt(now.plus(jwtTokenService.getRefreshTokenLifetime()));
        repository.persist(entity);

        return jwtTokenService.generateRefreshToken(account, entity.getId());
    }

    @Transactional(dontRollbackOn = AuthenticationException.class)
    public String consume(CustomerRefreshTokenClaims claims) {
        LocalDateTime now = LocalDateTime.now();
        CustomerRefreshTokenEntity token = repository.findByIdForUpdate(claims.tokenId())
                .filter(t -> t.getCustomerAccountId().equals(claims.customerAccountId()))
                .orElseThrow(() -> new AuthenticationException(INVALID_TOKEN_MESSAGE));

        if (token.getRevokedAt() != null || token.getExpiresAt().isBefore(now)) {
            throw new AuthenticationException(INVALID_TOKEN_MESSAGE);
        }

        if (token.getRotatedAt() != null) {
            if (token.getRotatedAt().plusSeconds(reuseGraceSeconds).isBefore(now)) {
                repository.revokeFamily(token.getFamilyId(), now);
                log.warn("Reuse of rotated refresh token detected for customer account {}; revoked its session",
                        token.getCustomerAccountId());
                throw new AuthenticationException(INVALID_TOKEN_MESSAGE);
            }
            return token.getFamilyId();
        }

        token.setRotatedAt(now);
        return token.getFamilyId();
    }

    @Transactional
    public void revokeSession(CustomerRefreshTokenClaims claims) {
        repository.findByIdOptional(claims.tokenId())
                .filter(t -> t.getCustomerAccountId().equals(claims.customerAccountId()))
                .ifPresent(t -> repository.revokeFamily(t.getFamilyId(), LocalDateTime.now()));
    }

    @Transactional
    public void revokeAllForAccount(Long customerAccountId) {
        repository.revokeAllForAccount(customerAccountId, LocalDateTime.now());
    }
}
