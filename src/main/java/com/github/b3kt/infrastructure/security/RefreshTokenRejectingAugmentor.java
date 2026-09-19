package com.github.b3kt.infrastructure.security;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Refresh tokens are signed with the same key as access tokens, so without this check a refresh token
 * would be accepted as a bearer token. They may only be exchanged at {@code /api/auth/refresh}
 * (which reads them from the request body, not the Authorization header).
 */
@ApplicationScoped
public class RefreshTokenRejectingAugmentor implements SecurityIdentityAugmentor {

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
        if (identity.getPrincipal() instanceof JsonWebToken jwt && "refresh".equals(jwt.getClaim("type"))) {
            return Uni.createFrom().failure(
                    new AuthenticationFailedException("Refresh tokens cannot be used as access tokens"));
        }
        return Uni.createFrom().item(identity);
    }
}
