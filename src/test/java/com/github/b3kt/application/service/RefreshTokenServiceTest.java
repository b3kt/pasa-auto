package com.github.b3kt.application.service;

import com.github.b3kt.domain.exception.AuthenticationException;
import com.github.b3kt.domain.model.User;
import com.github.b3kt.infrastructure.persistence.entity.RefreshTokenEntity;
import com.github.b3kt.infrastructure.persistence.repository.RefreshTokenRepository;
import com.github.b3kt.infrastructure.security.JwtTokenService;
import com.github.b3kt.infrastructure.security.RefreshTokenClaims;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RefreshTokenService Tests")
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository repository;

    @Mock
    private JwtTokenService jwtTokenService;

    @InjectMocks
    private RefreshTokenService service;

    private final RefreshTokenClaims claims = new RefreshTokenClaims("admin", "token-id");

    @BeforeEach
    void setUp() {
        service.reuseGraceSeconds = 30;
        when(jwtTokenService.getRefreshTokenLifetime()).thenReturn(Duration.ofDays(7));
    }

    private RefreshTokenEntity storedToken() {
        RefreshTokenEntity token = new RefreshTokenEntity();
        token.setId("token-id");
        token.setUsername("admin");
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
        @DisplayName("Should persist the token and sign it with its id")
        void testIssue_newFamily() {
            User user = new User("admin", "admin@test.com", "hash", Set.of());
            when(jwtTokenService.generateRefreshToken(eq(user), anyString())).thenReturn("signed");

            assertEquals("signed", service.issue(user));

            ArgumentCaptor<RefreshTokenEntity> saved = ArgumentCaptor.forClass(RefreshTokenEntity.class);
            verify(repository).persist(saved.capture());
            assertEquals("admin", saved.getValue().getUsername());
            assertNotNull(saved.getValue().getFamilyId());
            assertTrue(saved.getValue().getExpiresAt().isAfter(LocalDateTime.now().plusDays(6)));
            verify(jwtTokenService).generateRefreshToken(user, saved.getValue().getId());
            verify(repository).deleteExpiredForUser(eq("admin"), any());
        }

        @Test
        @DisplayName("Should keep the family on rotation")
        void testIssue_existingFamily() {
            User user = new User("admin", "admin@test.com", "hash", Set.of());

            service.issue(user, "family-1");

            ArgumentCaptor<RefreshTokenEntity> saved = ArgumentCaptor.forClass(RefreshTokenEntity.class);
            verify(repository).persist(saved.capture());
            assertEquals("family-1", saved.getValue().getFamilyId());
        }
    }

    @Nested
    @DisplayName("consume")
    class ConsumeTests {

        @Test
        @DisplayName("Should mark an unused token as rotated")
        void testConsume_fresh() {
            RefreshTokenEntity token = storedToken();

            assertEquals("family-1", service.consume(claims));
            assertNotNull(token.getRotatedAt());
        }

        @Test
        @DisplayName("Should reject an unknown token")
        void testConsume_unknown() {
            when(repository.findByIdForUpdate("token-id")).thenReturn(Optional.empty());

            assertThrows(AuthenticationException.class, () -> service.consume(claims));
        }

        @Test
        @DisplayName("Should reject a token belonging to another user")
        void testConsume_otherUser() {
            storedToken();

            assertThrows(AuthenticationException.class,
                    () -> service.consume(new RefreshTokenClaims("someone-else", "token-id")));
        }

        @Test
        @DisplayName("Should reject a revoked token")
        void testConsume_revoked() {
            storedToken().setRevokedAt(LocalDateTime.now());

            assertThrows(AuthenticationException.class, () -> service.consume(claims));
        }

        @Test
        @DisplayName("Should allow reuse within the grace period without revoking")
        void testConsume_reuseWithinGrace() {
            storedToken().setRotatedAt(LocalDateTime.now().minusSeconds(5));

            assertEquals("family-1", service.consume(claims));
            verify(repository, never()).revokeFamily(anyString(), any());
        }

        @Test
        @DisplayName("Should revoke the family when a rotated token is reused later")
        void testConsume_reuseAfterGrace() {
            storedToken().setRotatedAt(LocalDateTime.now().minusMinutes(5));

            assertThrows(AuthenticationException.class, () -> service.consume(claims));
            verify(repository).revokeFamily(eq("family-1"), any());
        }
    }

    @Nested
    @DisplayName("revoke")
    class RevokeTests {

        @Test
        @DisplayName("Should revoke the token's family")
        void testRevokeSession() {
            RefreshTokenEntity token = storedToken();
            when(repository.findByIdOptional("token-id")).thenReturn(Optional.of(token));

            service.revokeSession(claims);

            verify(repository).revokeFamily(eq("family-1"), any());
        }

        @Test
        @DisplayName("Should revoke every token of the user")
        void testRevokeAllForUser() {
            service.revokeAllForUser("admin");

            verify(repository).revokeAllForUser(eq("admin"), any());
        }
    }
}
