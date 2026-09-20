package com.github.b3kt.infrastructure.security;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.security.Principal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit-level counterpart to {@code PasswordChangeRequiredTest}: which claim shapes strip the roles.
 * The claim arrives as a Boolean or as a JSON value depending on how the token was parsed, so both
 * have to be recognised - a shape that slipped through would silently hand a temporary-password
 * token its full roles.
 */
@DisplayName("PasswordChangeRequiredAugmentor Tests")
class PasswordChangeRequiredAugmentorTest {

    private final PasswordChangeRequiredAugmentor augmentor = new PasswordChangeRequiredAugmentor();

    private SecurityIdentity identityWithClaim(Object claimValue) {
        JsonWebToken jwt = mock(JsonWebToken.class);
        when(jwt.getName()).thenReturn("budi");
        when(jwt.getClaim(PasswordChangeRequiredAugmentor.CLAIM)).thenReturn(claimValue);
        return QuarkusSecurityIdentity.builder()
                .setPrincipal(jwt)
                .addRoles(Set.of(Roles.OWNER))
                .addAttribute("tenant", "pasa")
                .build();
    }

    private SecurityIdentity augment(SecurityIdentity identity) {
        return augmentor.augment(identity, null).await().indefinitely();
    }

    @ParameterizedTest(name = "claim \"{0}\" strips the roles")
    @ValueSource(strings = {"true", "TRUE", "True"})
    @DisplayName("A claim parsed as a string still strips the roles")
    void stringClaimStripsRoles(String claim) {
        assertTrue(augment(identityWithClaim(claim)).getRoles().isEmpty());
    }

    @Test
    @DisplayName("A boolean claim strips the roles but keeps the identity authenticated")
    void booleanClaimStripsRoles() {
        SecurityIdentity augmented = augment(identityWithClaim(Boolean.TRUE));

        assertTrue(augmented.getRoles().isEmpty(),
                "@RolesAllowed endpoints must answer 403 until the password is changed");
        assertFalse(augmented.isAnonymous(),
                "@Authenticated endpoints - change password, logout, current user - must keep working");
        assertEquals("budi", augmented.getPrincipal().getName());
        assertEquals("pasa", augmented.getAttribute("tenant"), "other attributes are carried over");
    }

    @Test
    @DisplayName("A false or absent claim leaves the identity untouched")
    void falseOrAbsentClaimKeepsRoles() {
        SecurityIdentity fromFalse = identityWithClaim(Boolean.FALSE);
        assertSame(fromFalse, augment(fromFalse));
        assertEquals(Set.of(Roles.OWNER), augment(fromFalse).getRoles());

        SecurityIdentity fromAbsent = identityWithClaim(null);
        assertSame(fromAbsent, augment(fromAbsent));
        assertEquals(Set.of(Roles.OWNER), augment(fromAbsent).getRoles());

        SecurityIdentity fromFalseString = identityWithClaim("false");
        assertSame(fromFalseString, augment(fromFalseString));
    }

    /** Basic auth or a test identity has no JsonWebToken principal; the augmentor must not touch it. */
    @Test
    @DisplayName("A non-JWT principal is passed through")
    void nonJwtPrincipalIsUntouched() {
        Principal principal = () -> "basic-user";
        SecurityIdentity identity = QuarkusSecurityIdentity.builder()
                .setPrincipal(principal)
                .addRoles(Set.of(Roles.OWNER))
                .build();

        assertSame(identity, augment(identity));
        assertEquals(Set.of(Roles.OWNER), augment(identity).getRoles());
    }
}
