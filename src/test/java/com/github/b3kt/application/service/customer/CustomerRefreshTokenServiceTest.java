package com.github.b3kt.application.service.customer;

import com.github.b3kt.domain.exception.AuthenticationException;
import com.github.b3kt.infrastructure.persistence.entity.customer.CustomerAccountEntity;
import com.github.b3kt.infrastructure.persistence.entity.customer.CustomerRefreshTokenEntity;
import com.github.b3kt.infrastructure.persistence.repository.customer.CustomerRefreshTokenRepository;
import com.github.b3kt.infrastructure.security.CustomerJwtTokenService;
import com.github.b3kt.infrastructure.security.CustomerRefreshTokenClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CustomerRefreshTokenService Tests")
class CustomerRefreshTokenServiceTest {

    @Mock
    CustomerRefreshTokenRepository repository;

    @Mock
    CustomerJwtTokenService jwtTokenService;

    @InjectMocks
    CustomerRefreshTokenService service;

    private final CustomerRefreshTokenClaims claims = new CustomerRefreshTokenClaims(5L, "token-id");

    private CustomerAccountEntity account() {
        CustomerAccountEntity account = new CustomerAccountEntity();
        account.setId(5L);
        account.setEmail("budi@test.com");
        return account;
    }

    @BeforeEach
    void setUp() {
        service.reuseGraceSeconds = 30;
        when(jwtTokenService.getRefreshTokenLifetime()).thenReturn(Duration.ofDays(7));
    }

    private CustomerRefreshTokenEntity stored() {
        CustomerRefreshTokenEntity token = new CustomerRefreshTokenEntity();
        token.setId("token-id");
        token.setCustomerAccountId(5L);
        token.setFamilyId("family-1");
        token.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        token.setExpiresAt(LocalDateTime.now().plusDays(7));
        when(repository.findByIdForUpdate("token-id")).thenReturn(Optional.of(token));
        return token;
    }

    @Nested
    @DisplayName("issue")
    class IssueTests {

        @Test
        @DisplayName("starts a new family, prunes expired tokens and signs the new id")
        void newFamily() {
            CustomerAccountEntity account = account();
            when(jwtTokenService.generateRefreshToken(eq(account), anyString())).thenReturn("signed");

            assertEquals("signed", service.issue(account));

            ArgumentCaptor<CustomerRefreshTokenEntity> saved =
                    ArgumentCaptor.forClass(CustomerRefreshTokenEntity.class);
            verify(repository).persist(saved.capture());
            assertEquals(5L, saved.getValue().getCustomerAccountId());
            assertNotNull(saved.getValue().getFamilyId());
            assertTrue(saved.getValue().getExpiresAt().isAfter(LocalDateTime.now().plusDays(6)));
            verify(repository).deleteExpiredForAccount(eq(5L), any());
            verify(jwtTokenService).generateRefreshToken(account, saved.getValue().getId());
        }

        @Test
        @DisplayName("keeps the family id on rotation")
        void existingFamily() {
            service.issue(account(), "family-1");

            ArgumentCaptor<CustomerRefreshTokenEntity> saved =
                    ArgumentCaptor.forClass(CustomerRefreshTokenEntity.class);
            verify(repository).persist(saved.capture());
            assertEquals("family-1", saved.getValue().getFamilyId());
        }
    }

    @Nested
    @DisplayName("consume")
    class ConsumeTests {

        @Test
        @DisplayName("marks an unused token as rotated and returns its family")
        void fresh() {
            CustomerRefreshTokenEntity token = stored();

            assertEquals("family-1", service.consume(claims));
            assertNotNull(token.getRotatedAt());
        }

        @Test
        @DisplayName("rejects an unknown token")
        void unknown() {
            when(repository.findByIdForUpdate("token-id")).thenReturn(Optional.empty());

            assertThrows(AuthenticationException.class, () -> service.consume(claims));
        }

        @Test
        @DisplayName("rejects a token that belongs to another account")
        void otherAccount() {
            stored();

            assertThrows(AuthenticationException.class,
                    () -> service.consume(new CustomerRefreshTokenClaims(99L, "token-id")));
        }

        @Test
        @DisplayName("rejects a revoked token")
        void revoked() {
            stored().setRevokedAt(LocalDateTime.now());

            assertThrows(AuthenticationException.class, () -> service.consume(claims));
        }

        @Test
        @DisplayName("rejects an expired token")
        void expired() {
            stored().setExpiresAt(LocalDateTime.now().minusSeconds(1));

            assertThrows(AuthenticationException.class, () -> service.consume(claims));
        }

        @Test
        @DisplayName("allows reuse within the grace period without revoking the family")
        void reuseWithinGrace() {
            stored().setRotatedAt(LocalDateTime.now().minusSeconds(5));

            assertEquals("family-1", service.consume(claims));
            verify(repository, never()).revokeFamily(anyString(), any());
        }

        @Test
        @DisplayName("revokes the family when a rotated token is replayed after the grace period")
        void reuseAfterGrace() {
            stored().setRotatedAt(LocalDateTime.now().minusMinutes(5));

            assertThrows(AuthenticationException.class, () -> service.consume(claims));
            verify(repository).revokeFamily(eq("family-1"), any());
        }
    }

    @Nested
    @DisplayName("revoke")
    class RevokeTests {

        @Test
        @DisplayName("revokes the token's family when it is found")
        void sessionFound() {
            CustomerRefreshTokenEntity token = new CustomerRefreshTokenEntity();
            token.setCustomerAccountId(5L);
            token.setFamilyId("family-1");
            when(repository.findByIdOptional("token-id")).thenReturn(Optional.of(token));

            service.revokeSession(claims);

            verify(repository).revokeFamily(eq("family-1"), any());
        }

        @Test
        @DisplayName("does nothing when the token is not found")
        void sessionMissing() {
            when(repository.findByIdOptional("token-id")).thenReturn(Optional.empty());

            service.revokeSession(claims);

            verify(repository, never()).revokeFamily(anyString(), any());
        }

        @Test
        @DisplayName("revokes every token of the account")
        void allForAccount() {
            service.revokeAllForAccount(5L);

            verify(repository).revokeAllForAccount(eq(5L), any());
        }
    }
}
