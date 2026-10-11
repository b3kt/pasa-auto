package com.github.b3kt.infrastructure.security;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CustomerPrincipal Tests")
class CustomerPrincipalTest {

    @Mock
    JsonWebToken jwt;

    @Mock
    CustomerJwtTokenService customerJwtTokenService;

    @InjectMocks
    CustomerPrincipal principal;

    @Test
    @DisplayName("resolves the account id from the token")
    void accountId() {
        when(customerJwtTokenService.extractCustomerAccountId(jwt)).thenReturn(5L);

        assertEquals(5L, principal.customerAccountId());
    }

    @Test
    @DisplayName("resolves the pelanggan id, or null when unlinked")
    void pelangganIdOrNull() {
        when(customerJwtTokenService.extractPelangganId(jwt)).thenReturn(7L);
        assertEquals(7L, principal.pelangganIdOrNull());

        when(customerJwtTokenService.extractPelangganId(jwt)).thenReturn(null);
        assertNull(principal.pelangganIdOrNull());
    }

    @Test
    @DisplayName("returns the pelanggan id when linked")
    void pelangganIdOrThrow_linked() {
        when(customerJwtTokenService.extractPelangganId(jwt)).thenReturn(7L);

        assertEquals(7L, principal.pelangganIdOrThrow());
    }

    @Test
    @DisplayName("fails closed instead of falling back to 'show everything' when unlinked")
    void pelangganIdOrThrow_unlinked() {
        when(customerJwtTokenService.extractPelangganId(jwt)).thenReturn(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> principal.pelangganIdOrThrow());
        assertEquals("Link your account to a customer record first", ex.getMessage());
    }
}
