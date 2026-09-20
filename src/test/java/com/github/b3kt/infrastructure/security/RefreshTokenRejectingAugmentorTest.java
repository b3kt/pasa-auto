package com.github.b3kt.infrastructure.security;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.Principal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Refresh tokens are signed with the same key as access tokens, so without this augmentor a
 * refresh token presented as a bearer token would authenticate. That is the whole point of the
 * class, and it is what these tests pin.
 */
@DisplayName("RefreshTokenRejectingAugmentor Tests")
class RefreshTokenRejectingAugmentorTest {

    private final RefreshTokenRejectingAugmentor augmentor = new RefreshTokenRejectingAugmentor();

    private SecurityIdentity identityWithType(Object typeClaim) {
        JsonWebToken jwt = mock(JsonWebToken.class);
        when(jwt.getClaim("type")).thenReturn(typeClaim);
        return QuarkusSecurityIdentity.builder()
                .setPrincipal(jwt)
                .addRoles(Set.of(Roles.OWNER))
                .build();
    }

    private SecurityIdentity augment(SecurityIdentity identity) {
        return augmentor.augment(identity, null).await().indefinitely();
    }

    @Test
    @DisplayName("A refresh token presented as a bearer token is refused")
    void refusesRefreshToken() {
        SecurityIdentity identity = identityWithType("refresh");

        AuthenticationFailedException ex = assertThrows(AuthenticationFailedException.class,
                () -> augment(identity));
        assertTrue(ex.getMessage().contains("cannot be used as access tokens"), ex.getMessage());
    }

    @Test
    @DisplayName("An access token passes through untouched")
    void allowsAccessToken() {
        SecurityIdentity access = identityWithType("access");
        assertSame(access, augment(access));

        SecurityIdentity noType = identityWithType(null);
        assertSame(noType, augment(noType));
        assertEquals(Set.of(Roles.OWNER), augment(noType).getRoles());
    }

    /** Only the exact claim value is a refresh token; anything else is left alone. */
    @Test
    @DisplayName("An unrelated type claim is not treated as a refresh token")
    void ignoresOtherTypeClaims() {
        assertDoesNotThrow(() -> augment(identityWithType("REFRESH")));
        assertDoesNotThrow(() -> augment(identityWithType(42)));
    }

    @Test
    @DisplayName("A non-JWT principal is passed through")
    void ignoresNonJwtPrincipal() {
        Principal principal = () -> "basic-user";
        SecurityIdentity identity = QuarkusSecurityIdentity.builder().setPrincipal(principal).build();

        assertSame(identity, augment(identity));
    }
}
