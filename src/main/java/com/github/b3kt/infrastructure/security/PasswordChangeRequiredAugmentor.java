package com.github.b3kt.infrastructure.security;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Tokens of users who still have a temporary password carry the {@value #CLAIM} claim. Their roles are
 * dropped, so every {@code @RolesAllowed} endpoint answers 403 while {@code @Authenticated} ones
 * (change password, logout, current user) keep working until the password is changed.
 */
@ApplicationScoped
public class PasswordChangeRequiredAugmentor implements SecurityIdentityAugmentor {

    public static final String CLAIM = "pwd_change";

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
        if (identity.getPrincipal() instanceof JsonWebToken jwt && isSet(jwt.getClaim(CLAIM))) {
            return Uni.createFrom().item(QuarkusSecurityIdentity.builder()
                    .setPrincipal(identity.getPrincipal())
                    .addCredentials(identity.getCredentials())
                    .addAttributes(identity.getAttributes())
                    .setAnonymous(false)
                    .build());
        }
        return Uni.createFrom().item(identity);
    }

    /** The claim may surface as a Boolean or as a JSON value depending on how the token was parsed. */
    private static boolean isSet(Object claim) {
        return claim != null && Boolean.parseBoolean(claim.toString());
    }
}
