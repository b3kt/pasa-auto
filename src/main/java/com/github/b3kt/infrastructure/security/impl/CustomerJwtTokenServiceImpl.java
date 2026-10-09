package com.github.b3kt.infrastructure.security.impl;

import com.github.b3kt.infrastructure.persistence.entity.customer.CustomerAccountEntity;
import com.github.b3kt.infrastructure.security.CustomerJwtTokenService;
import com.github.b3kt.infrastructure.security.CustomerRefreshTokenClaims;
import com.github.b3kt.infrastructure.security.Roles;

import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.Duration;
import java.util.Set;

@ApplicationScoped
public class CustomerJwtTokenServiceImpl implements CustomerJwtTokenService {

    /** A marker distinct from the staff token shape, in case Roles.CUSTOMER were ever reused elsewhere. */
    private static final String ACCT_TYPE_CLAIM = "acctType";
    private static final String ACCT_TYPE_CUSTOMER = "customer";
    private static final String PELANGGAN_ID_CLAIM = "pelangganId";

    @ConfigProperty(name = "jwt.issuer", defaultValue = "https://quarkus-quasar.example.com")
    String issuer;

    @ConfigProperty(name = "jwt.expiration.minutes", defaultValue = "30")
    long expirationMinutes;

    @ConfigProperty(name = "jwt.refresh.expiration.days", defaultValue = "7")
    long refreshExpirationDays;

    @Inject
    JWTParser jwtParser;

    @Override
    public String generateToken(CustomerAccountEntity account) {
        io.smallrye.jwt.build.JwtClaimsBuilder builder = Jwt.issuer(issuer)
                .subject(String.valueOf(account.getId()))
                .groups(Set.of(Roles.CUSTOMER))
                .claim(ACCT_TYPE_CLAIM, ACCT_TYPE_CUSTOMER)
                .claim("email", account.getEmail())
                .claim("fullName", account.getFullName())
                .expiresIn(Duration.ofMinutes(expirationMinutes));

        // Omitted entirely when unlinked, never a null claim, so downstream code fails closed.
        if (account.getPelangganId() != null) {
            builder.claim(PELANGGAN_ID_CLAIM, account.getPelangganId());
        }

        return builder.sign();
    }

    @Override
    public long getTokenExpirationSeconds() {
        return Duration.ofMinutes(expirationMinutes).getSeconds();
    }

    @Override
    public String generateRefreshToken(CustomerAccountEntity account, String tokenId) {
        return Jwt.issuer(issuer)
                .subject(String.valueOf(account.getId()))
                .claim(Claims.jti.name(), tokenId)
                .claim("type", "customer-refresh")
                .expiresIn(getRefreshTokenLifetime())
                .sign();
    }

    @Override
    public Duration getRefreshTokenLifetime() {
        return Duration.ofDays(refreshExpirationDays);
    }

    @Override
    public CustomerRefreshTokenClaims validateRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return null;
        }
        try {
            JsonWebToken token = jwtParser.parse(refreshToken);
            if (!"customer-refresh".equals(token.getClaim("type")) || token.getTokenID() == null) {
                return null;
            }
            return new CustomerRefreshTokenClaims(Long.valueOf(token.getSubject()), token.getTokenID());
        } catch (ParseException | NumberFormatException e) {
            return null;
        }
    }

    @Override
    public Long extractPelangganId(JsonWebToken jwt) {
        Object claim = jwt.getClaim(PELANGGAN_ID_CLAIM);
        return claim == null ? null : Long.parseLong(claim.toString());
    }

    @Override
    public Long extractCustomerAccountId(JsonWebToken jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
