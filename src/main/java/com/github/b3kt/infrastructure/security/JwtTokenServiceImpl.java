package com.github.b3kt.infrastructure.security;

import com.github.b3kt.application.dto.UserInfo;
import com.github.b3kt.application.properties.RbacProperties;
import com.github.b3kt.application.service.RbacService;
import com.github.b3kt.domain.model.Permission;
import com.github.b3kt.domain.model.User;
import com.github.b3kt.infrastructure.persistence.entity.RoleEntity;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of JWT token service using SmallRye JWT.
 */
@ApplicationScoped
public class JwtTokenServiceImpl implements JwtTokenService {

    @ConfigProperty(name = "jwt.issuer", defaultValue = "https://quarkus-quasar.example.com")
    String issuer;

    @ConfigProperty(name = "jwt.expiration.hours", defaultValue = "24")
    long expirationHours;
    
    @ConfigProperty(name = "jwt.refresh.expiration.days", defaultValue = "7")
    long refreshExpirationDays;

    @Inject
    RbacProperties rbacProperties;

    @Inject
    RbacService rbacService;

    @Override
    public String generateToken(User user) {
        io.smallrye.jwt.build.JwtClaimsBuilder jwtBuilder = Jwt.issuer(issuer)
                .upn(user.getUsername())
                .subject(user.getUsername())
                .groups(user.getRoles().stream().map(RoleEntity::getName).collect(Collectors.toSet()))
                .claim("email", user.getEmail())
                .expiresIn(Duration.ofHours(expirationHours));

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
                // If RBAC is enabled but user permissions can't be fetched, log and continue
                // This allows the token to be generated without permissions
                System.err.println(
                        "Warning: Could not fetch permissions for user " + user.getUsername() + ": " + e.getMessage());
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
        return Duration.ofHours(expirationHours).getSeconds();
    }
    
    @Override
    public String generateRefreshToken(User user) {
        // Generate a refresh token with longer expiration (7 days by default)
        // This token only contains the username and is used solely for refreshing access tokens
        return Jwt.issuer(issuer)
                .upn(user.getUsername())
                .subject(user.getUsername())
                .claim("type", "refresh")
                .expiresIn(Duration.ofDays(refreshExpirationDays))
                .sign();
    }
    
    @Override
    public String validateRefreshToken(String refreshToken) {
        try {
            // Decode the JWT payload to validate it
            String[] parts = refreshToken.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            
            // Parse the JSON payload manually to extract claims
            // Check if it's a refresh token and if it's not expired
            if (!payload.contains("\"type\":\"refresh\"")) {
                return null;
            }
            
            // Extract expiration time
            int expStart = payload.indexOf("\"exp\":") + 6;
            int expEnd = payload.indexOf(",", expStart);
            if (expEnd == -1) {
                expEnd = payload.indexOf("}", expStart);
            }
            long exp = Long.parseLong(payload.substring(expStart, expEnd).trim());
            
            if (Instant.now().getEpochSecond() > exp) {
                return null; // Token expired
            }
            
            // Extract subject (username)
            int subStart = payload.indexOf("\"sub\":\"") + 7;
            int subEnd = payload.indexOf("\"", subStart);
            return payload.substring(subStart, subEnd);
        } catch (Exception e) {
            return null;
        }
    }
}
