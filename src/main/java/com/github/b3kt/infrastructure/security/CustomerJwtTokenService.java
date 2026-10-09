package com.github.b3kt.infrastructure.security;

import com.github.b3kt.infrastructure.persistence.entity.customer.CustomerAccountEntity;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.Duration;

/**
 * Mints and validates JWTs for the customer portal. Same signing infrastructure as the staff
 * {@link JwtTokenService}, but a distinct claim set (see {@link Roles#CUSTOMER}) and identity
 * space - never a {@code User}/{@code karyawanId}.
 */
public interface CustomerJwtTokenService {

    /**
     * @param account the account; {@code pelangganId} is included only when linked, and is
     *                omitted entirely (never a null claim) when it is not, so downstream code
     *                fails closed rather than trusting an absent link as "no restriction".
     */
    String generateToken(CustomerAccountEntity account);

    long getTokenExpirationSeconds();

    String generateRefreshToken(CustomerAccountEntity account, String tokenId);

    Duration getRefreshTokenLifetime();

    CustomerRefreshTokenClaims validateRefreshToken(String refreshToken);

    /** Reads the {@code pelangganId} claim off an already-verified customer access token, or null when unlinked. */
    Long extractPelangganId(JsonWebToken jwt);

    Long extractCustomerAccountId(JsonWebToken jwt);
}
