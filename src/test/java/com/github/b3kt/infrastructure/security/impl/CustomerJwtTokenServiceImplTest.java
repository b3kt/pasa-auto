package com.github.b3kt.infrastructure.security.impl;

import com.github.b3kt.infrastructure.persistence.entity.customer.CustomerAccountEntity;
import com.github.b3kt.infrastructure.security.CustomerRefreshTokenClaims;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CustomerJwtTokenServiceImpl Tests")
class CustomerJwtTokenServiceImplTest {

    @Mock
    JWTParser jwtParser;

    @Mock
    JsonWebToken parsedToken;

    @InjectMocks
    CustomerJwtTokenServiceImpl service;

    @BeforeEach
    void setUp() {
        service.issuer = "test-issuer";
        service.expirationMinutes = 30;
        service.refreshExpirationDays = 7;
    }

    private CustomerAccountEntity account(Long pelangganId) {
        CustomerAccountEntity account = new CustomerAccountEntity();
        account.setId(5L);
        account.setEmail("budi@test.com");
        account.setFullName("Budi");
        account.setPelangganId(pelangganId);
        return account;
    }

    private String payload(String token) {
        return new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);
    }

    @Nested
    @DisplayName("generateToken")
    class GenerateTokenTests {

        @Test
        @DisplayName("signs a token carrying the account identity")
        void unlinked() {
            String token = service.generateToken(account(null));

            assertEquals(3, token.split("\\.").length);
            String payload = payload(token);
            assertTrue(payload.contains("\"email\":\"budi@test.com\""), payload);
            assertTrue(payload.contains("customer"), payload);
            // An unlinked account must not carry a pelangganId claim at all, so downstream fails closed.
            assertFalse(payload.contains("pelangganId"), payload);
        }

        @Test
        @DisplayName("includes the pelangganId claim once linked")
        void linked() {
            String token = service.generateToken(account(7L));

            assertTrue(payload(token).contains("pelangganId"), payload(token));
        }
    }

    @Nested
    @DisplayName("getTokenExpirationSeconds")
    class ExpirationTests {

        @Test
        @DisplayName("converts the configured minutes to seconds")
        void seconds() {
            assertEquals(1800L, service.getTokenExpirationSeconds());
        }
    }

    @Nested
    @DisplayName("generateRefreshToken / getRefreshTokenLifetime")
    class RefreshTokenTests {

        @Test
        @DisplayName("signs a refresh token")
        void signs() {
            String token = service.generateRefreshToken(account(null), "token-id");

            assertEquals(3, token.split("\\.").length);
            assertTrue(payload(token).contains("customer-refresh"));
        }

        @Test
        @DisplayName("reports the configured refresh lifetime")
        void lifetime() {
            assertEquals(Duration.ofDays(7), service.getRefreshTokenLifetime());
        }
    }

    @Nested
    @DisplayName("validateRefreshToken")
    class ValidateRefreshTokenTests {

        @Test
        @DisplayName("returns null for a null or blank token without parsing")
        void blank() throws Exception {
            assertNull(service.validateRefreshToken(null));
            assertNull(service.validateRefreshToken("  "));
            verify(jwtParser, never()).parse(anyString());
        }

        @Test
        @DisplayName("returns null when signature, issuer or expiry verification fails")
        void rejectedByParser() throws Exception {
            when(jwtParser.parse("forged")).thenThrow(new ParseException("bad signature"));

            assertNull(service.validateRefreshToken("forged"));
        }

        @Test
        @DisplayName("returns null for a verified token that is not a customer refresh token")
        void wrongType() throws Exception {
            when(parsedToken.getClaim("type")).thenReturn("refresh");
            when(parsedToken.getTokenID()).thenReturn("token-id");
            when(jwtParser.parse("access")).thenReturn(parsedToken);

            assertNull(service.validateRefreshToken("access"));
        }

        @Test
        @DisplayName("returns null for a refresh token without a token id")
        void missingTokenId() throws Exception {
            when(parsedToken.getClaim("type")).thenReturn("customer-refresh");
            when(parsedToken.getTokenID()).thenReturn(null);
            when(jwtParser.parse("refresh")).thenReturn(parsedToken);

            assertNull(service.validateRefreshToken("refresh"));
        }

        @Test
        @DisplayName("returns null when the subject is not a number")
        void nonNumericSubject() throws Exception {
            when(parsedToken.getClaim("type")).thenReturn("customer-refresh");
            when(parsedToken.getTokenID()).thenReturn("token-id");
            when(parsedToken.getSubject()).thenReturn("not-a-number");
            when(jwtParser.parse("refresh")).thenReturn(parsedToken);

            assertNull(service.validateRefreshToken("refresh"));
        }

        @Test
        @DisplayName("returns the account id and token id of a verified refresh token")
        void valid() throws Exception {
            when(parsedToken.getClaim("type")).thenReturn("customer-refresh");
            when(parsedToken.getTokenID()).thenReturn("token-id");
            when(parsedToken.getSubject()).thenReturn("5");
            when(jwtParser.parse("refresh")).thenReturn(parsedToken);

            assertEquals(new CustomerRefreshTokenClaims(5L, "token-id"),
                    service.validateRefreshToken("refresh"));
        }
    }

    @Nested
    @DisplayName("claim extraction")
    class ClaimExtractionTests {

        @Test
        @DisplayName("returns null when the pelangganId claim is absent")
        void pelangganIdAbsent() {
            when(parsedToken.getClaim("pelangganId")).thenReturn(null);

            assertNull(service.extractPelangganId(parsedToken));
        }

        @Test
        @DisplayName("parses the pelangganId claim whether it is a number or a string")
        void pelangganIdPresent() {
            when(parsedToken.getClaim("pelangganId")).thenReturn(7L);
            assertEquals(7L, service.extractPelangganId(parsedToken));

            when(parsedToken.getClaim("pelangganId")).thenReturn("7");
            assertEquals(7L, service.extractPelangganId(parsedToken));
        }

        @Test
        @DisplayName("reads the account id from the subject")
        void customerAccountId() {
            when(parsedToken.getSubject()).thenReturn("5");

            assertEquals(5L, service.extractCustomerAccountId(parsedToken));
        }
    }
}
