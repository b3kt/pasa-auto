package com.github.b3kt.application.service;

import com.github.b3kt.application.dto.LoginResponse;
import com.github.b3kt.domain.model.ApprovalStatus;
import com.github.b3kt.infrastructure.google.GoogleIdentity;
import com.github.b3kt.infrastructure.persistence.entity.UserEntity;
import com.github.b3kt.infrastructure.persistence.repository.UserEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * The decision table in {@link GoogleAuthService}: who a verified Google identity is allowed to be.
 */
@ExtendWith(MockitoExtension.class)
class GoogleAuthServiceTest {

    private static final GoogleIdentity IDENTITY = new GoogleIdentity("google-sub-1", "budi@example.com");

    @Mock
    UserEntityRepository userRepository;

    @Mock
    AuthService authService;

    @Mock
    AuditTrailService auditTrailService;

    @Mock
    LoginAttemptService loginAttemptService;

    @InjectMocks
    GoogleAuthService googleAuthService;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setUsername("budi");
        user.setEmail("budi@example.com");
        user.setActive(true);
        user.setApprovalStatus(ApprovalStatus.APPROVED);
    }

    @Test
    @DisplayName("a linked, approved, active account gets a session")
    void linkedAccountSignsIn() {
        user.setGoogleSub(IDENTITY.subject());
        when(userRepository.findByGoogleSub(IDENTITY.subject())).thenReturn(java.util.Optional.of(user));
        when(authService.issueSessionFor("budi")).thenReturn(new LoginResponse());

        GoogleAuthService.Result result = googleAuthService.signIn(IDENTITY);

        assertEquals(GoogleAuthService.Outcome.SUCCESS, result.outcome());
        assertNotNull(result.session());
        // A Google sign-in clears a password lockout, like a password login does
        verify(loginAttemptService).recordSuccess("budi");
    }

    @Test
    @DisplayName("a pending account is refused and no session is issued")
    void pendingAccountIsRefused() {
        user.setGoogleSub(IDENTITY.subject());
        user.setApprovalStatus(ApprovalStatus.PENDING);
        user.setActive(false);
        when(userRepository.findByGoogleSub(IDENTITY.subject())).thenReturn(java.util.Optional.of(user));

        GoogleAuthService.Result result = googleAuthService.signIn(IDENTITY);

        assertEquals(GoogleAuthService.Outcome.PENDING, result.outcome());
        assertNull(result.session());
        verify(authService, never()).issueSessionFor(any());
    }

    @Test
    @DisplayName("a rejected account is refused")
    void rejectedAccountIsRefused() {
        user.setGoogleSub(IDENTITY.subject());
        user.setApprovalStatus(ApprovalStatus.REJECTED);
        when(userRepository.findByGoogleSub(IDENTITY.subject())).thenReturn(java.util.Optional.of(user));

        assertEquals(GoogleAuthService.Outcome.REJECTED, googleAuthService.signIn(IDENTITY).outcome());
        verify(authService, never()).issueSessionFor(any());
    }

    @Test
    @DisplayName("an approved but deactivated account is refused")
    void deactivatedAccountIsRefused() {
        user.setGoogleSub(IDENTITY.subject());
        user.setActive(false);
        when(userRepository.findByGoogleSub(IDENTITY.subject())).thenReturn(java.util.Optional.of(user));

        assertEquals(GoogleAuthService.Outcome.DISABLED, googleAuthService.signIn(IDENTITY).outcome());
        verify(authService, never()).issueSessionFor(any());
    }

    @Test
    @DisplayName("a matching email that an admin has not enabled for Google is refused")
    void matchingEmailWithoutLinkIsRefused() {
        when(userRepository.findByGoogleSub(IDENTITY.subject())).thenReturn(java.util.Optional.empty());
        when(userRepository.findByEmailIgnoreCase(IDENTITY.email())).thenReturn(List.of(user));

        GoogleAuthService.Result result = googleAuthService.signIn(IDENTITY);

        assertEquals(GoogleAuthService.Outcome.LINK_REQUIRED, result.outcome());
        assertNull(user.getGoogleSub(), "an unlinked account must not be bound to the Google subject");
        verify(authService, never()).issueSessionFor(any());
    }

    @Test
    @DisplayName("a matching email an admin has enabled is bound to the Google account and signs in")
    void matchingEmailWithLinkBindsAndSignsIn() {
        user.setGoogleLoginEnabled(true);
        when(userRepository.findByGoogleSub(IDENTITY.subject())).thenReturn(java.util.Optional.empty());
        when(userRepository.findByEmailIgnoreCase(IDENTITY.email())).thenReturn(List.of(user));
        when(authService.issueSessionFor("budi")).thenReturn(new LoginResponse());

        GoogleAuthService.Result result = googleAuthService.signIn(IDENTITY);

        assertEquals(GoogleAuthService.Outcome.SUCCESS, result.outcome());
        assertEquals(IDENTITY.subject(), user.getGoogleSub(), "the account should now be bound to the subject");
    }

    @Test
    @DisplayName("an email shared by several accounts is ambiguous and refused")
    void duplicateEmailIsRefused() {
        UserEntity other = new UserEntity();
        other.setUsername("budi2");
        when(userRepository.findByGoogleSub(IDENTITY.subject())).thenReturn(java.util.Optional.empty());
        when(userRepository.findByEmailIgnoreCase(IDENTITY.email())).thenReturn(List.of(user, other));

        assertEquals(GoogleAuthService.Outcome.AMBIGUOUS, googleAuthService.signIn(IDENTITY).outcome());
        verify(authService, never()).issueSessionFor(any());
    }

    @Test
    @DisplayName("an unknown email creates a pending account that cannot authenticate")
    void unknownEmailCreatesPendingAccount() {
        when(userRepository.findByGoogleSub(IDENTITY.subject())).thenReturn(java.util.Optional.empty());
        when(userRepository.findByEmailIgnoreCase(IDENTITY.email())).thenReturn(List.of());
        when(userRepository.existsByUsername("budi")).thenReturn(false);

        GoogleAuthService.Result result = googleAuthService.signIn(IDENTITY);

        assertEquals(GoogleAuthService.Outcome.PENDING, result.outcome());

        ArgumentCaptor<UserEntity> created = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).persist(created.capture());
        UserEntity pending = created.getValue();

        assertEquals("budi", pending.getUsername());
        assertEquals(IDENTITY.email(), pending.getEmail());
        assertEquals(IDENTITY.subject(), pending.getGoogleSub());
        assertEquals(ApprovalStatus.PENDING, pending.getApprovalStatus());
        assertFalse(pending.isActive(), "a pending account must also be inactive");
        assertTrue(pending.getRoles().isEmpty(), "approval must not grant any role implicitly");
        assertEquals("!", pending.getPasswordHash(), "no password may ever match");
        assertFalse(pending.isMustChangePassword(),
                "must_change_password would strip the roles and could never be cleared without a password");
        assertTrue(pending.toDomain().canAuthenticate() == false, "a pending account cannot authenticate");
    }

    @Test
    @DisplayName("a taken username gets a numeric suffix")
    void usernameCollisionGetsSuffix() {
        when(userRepository.existsByUsername("budi")).thenReturn(true);
        when(userRepository.existsByUsername("budi2")).thenReturn(false);

        assertEquals("budi2", googleAuthService.deriveUsername("budi@example.com"));
    }

    @Test
    @DisplayName("a username is sanitised and fits the column")
    void usernameIsSanitisedAndTruncated() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);

        assertEquals("budi.santoso", googleAuthService.deriveUsername("Budi.Santoso+tag@example.com"));
        assertTrue(googleAuthService.deriveUsername("x".repeat(80) + "@example.com").length() <= 50);
    }

    /**
     * The "+tag" is dropped before sanitising rather than after, so the tag does not run into the
     * name. Only the username is shaped this way: the email is stored and matched as Google gave
     * it, since not every provider treats "+" as an alias.
     */
    @Test
    @DisplayName("a subaddress tag is dropped rather than folded into the username")
    void subaddressTagIsDropped() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);

        assertEquals("budi", googleAuthService.deriveUsername("budi+shopping@example.com"));
        assertEquals("budi", googleAuthService.deriveUsername("budi+a+b@example.com"));
    }

    /**
     * A local part that sanitises away to nothing - all tag, or nothing but characters the column
     * does not take - still has to yield a usable username. String.split would hand back an empty
     * array for "+@..." and throw.
     */
    @Test
    @DisplayName("a local part that sanitises away falls back to a usable username")
    void emptyLocalPartFallsBack() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);

        assertEquals("google", googleAuthService.deriveUsername("+@example.com"));
        assertEquals("google", googleAuthService.deriveUsername("+tag@example.com"));
        assertEquals("google", googleAuthService.deriveUsername("@example.com"));
        assertEquals("google", googleAuthService.deriveUsername("\u5f20\u4f1f@example.com"));
    }

    @Test
    @DisplayName("collisions keep counting past the first suffix")
    void usernameCollisionKeepsCounting() {
        when(userRepository.existsByUsername("budi")).thenReturn(true);
        when(userRepository.existsByUsername("budi2")).thenReturn(true);
        when(userRepository.existsByUsername("budi3")).thenReturn(false);

        assertEquals("budi3", googleAuthService.deriveUsername("budi@example.com"));
    }

    /** The suffix has to fit inside the column too, not be appended past its end. */
    @Test
    @DisplayName("a suffixed username still fits the column")
    void suffixedUsernameFitsTheColumn() {
        when(userRepository.existsByUsername(anyString()))
                .thenAnswer(invocation -> "x".repeat(50).equals(invocation.getArgument(0)));

        String username = googleAuthService.deriveUsername("x".repeat(80) + "@example.com");
        assertTrue(username.length() <= 50, username);
        assertTrue(username.endsWith("2"), username);
    }

    /**
     * The suffix search is bounded. A thousand accounts sharing a local part is not a case to
     * paper over silently, so it is reported rather than looping or returning a duplicate.
     */
    @Test
    @DisplayName("an exhausted suffix range is reported rather than looping")
    void usernameSuffixesCanRunOut() {
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> googleAuthService.deriveUsername("budi@example.com"));
        assertTrue(ex.getMessage().contains("budi@example.com"), ex.getMessage());
    }
}
