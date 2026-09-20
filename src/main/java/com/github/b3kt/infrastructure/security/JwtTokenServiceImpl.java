package com.github.b3kt.infrastructure.security;

import com.github.b3kt.application.dto.UserInfo;
import com.github.b3kt.application.properties.RbacProperties;
import com.github.b3kt.application.service.RbacService;
import com.github.b3kt.domain.model.Permission;
import com.github.b3kt.domain.model.User;
import com.github.b3kt.infrastructure.persistence.entity.RoleEntity;

import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of JWT token service using SmallRye JWT.
 */
@Slf4j
@ApplicationScoped
public class JwtTokenServiceImpl implements JwtTokenService {

    @ConfigProperty(name = "jwt.issuer", defaultValue = "https://quarkus-quasar.example.com")
    String issuer;

    @ConfigProperty(name = "jwt.expiration.minutes", defaultValue = "30")
    long expirationMinutes;
    
    @ConfigProperty(name = "jwt.refresh.expiration.days", defaultValue = "7")
    long refreshExpirationDays;

    @Inject
    RbacProperties rbacProperties;

    @Inject
    RbacService rbacService;

    @Inject
    JWTParser jwtParser;

    @Override
    public String generateToken(User user) {
        io.smallrye.jwt.build.JwtClaimsBuilder jwtBuilder = Jwt.issuer(issuer)
                .upn(user.getUsername())
                .subject(user.getUsername())
                // An approved account can legitimately have no roles yet - a Google sign-in assigns
                // none - so this must tolerate an empty or absent set rather than throwing
                .groups(user.getRoles() == null ? java.util.Set.<String>of()
                        : user.getRoles().stream().map(RoleEntity::getName).collect(Collectors.toSet()))
                .claim("email", user.getEmail())
                .expiresIn(Duration.ofMinutes(expirationMinutes));

        if (user.isMustChangePassword()) {
            jwtBuilder.claim(PasswordChangeRequiredAugmentor.CLAIM, true);
        }
        if (user.getKaryawanId() != null) {
            jwtBuilder.claim("karyawanId", user.getKaryawanId());
        }
        if (user.getKaryawanNama() != null) {
            jwtBuilder.claim("karyawanNama", user.getKaryawanNama());
        }

        // If RBAC is enabled, include permissions in the token
        if (rbacProperties.enabled()) {
            try {
                Set<Permission> permissions = rbacService.getUserPermissions(user.getUsername());
                Set<String> permissionNames = permissions.stream()
                        .map(Permission::getName)
                        .collect(Collectors.toSet());
                jwtBuilder.claim("permissions", permissionNames);
            } catch (Exception e) {
                // The token is still issued, but without permissions: log it properly so the degraded
                // access is visible in the logs rather than only on stderr
                log.warn("Could not fetch permissions for user {}; issuing token without them", user.getUsername(), e);
            }
        }

        return jwtBuilder.sign();
    }

    @Override
    public UserInfo extractUserInfo(JsonWebToken jwt) {
        String username = jwt.getSubject();
        String email = jwt.getClaim("email");
        java.util.Set<String> roles = jwt.getGroups();

        Long karyawanId = null;
        Object karyawanIdClaim = jwt.getClaim("karyawanId");
        if (karyawanIdClaim != null) {
            karyawanId = Long.parseLong(karyawanIdClaim.toString());
        }

        String karyawanNama = null;
        Object karyawanNamaClaim = jwt.getClaim("karyawanNama");
        if (karyawanNamaClaim != null) {
            karyawanNama = karyawanNamaClaim.toString();
        }

        UserInfo userInfo = new UserInfo(username, email, roles,
                karyawanId,
                karyawanNama);

        // If RBAC is enabled, permissions are already in the token but not in UserInfo
        // UserInfo currently only contains roles, not permissions
        // If you need permissions in UserInfo, you would need to extend UserInfo DTO

        return userInfo;
    }

    @Override
    public long getTokenExpirationSeconds() {
        return Duration.ofMinutes(expirationMinutes).getSeconds();
    }
    
    @Override
    public String generateRefreshToken(User user, String tokenId) {
        // Only carries the username and the server-side token id; it is used solely for refreshing access tokens
        return Jwt.issuer(issuer)
                .upn(user.getUsername())
                .subject(user.getUsername())
                .claim(Claims.jti.name(), tokenId)
                .claim("type", "refresh")
                .expiresIn(getRefreshTokenLifetime())
                .sign();
    }

    @Override
    public Duration getRefreshTokenLifetime() {
        return Duration.ofDays(refreshExpirationDays);
    }
    
    @Override
    public RefreshTokenClaims validateRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return null;
        }
        try {
            // Verifies signature, issuer and expiry against the mp.jwt.verify.* config
            JsonWebToken token = jwtParser.parse(refreshToken);
            if (!"refresh".equals(token.getClaim("type")) || token.getTokenID() == null) {
                return null;
            }
            return new RefreshTokenClaims(token.getSubject(), token.getTokenID());
        } catch (ParseException e) {
            return null;
        }
    }
}
