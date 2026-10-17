package com.github.b3kt.application.service.customer;

import com.github.b3kt.application.dto.customer.CustomerLoginResponse;
import com.github.b3kt.application.dto.customer.CustomerRegisterRequest;
import com.github.b3kt.domain.exception.AuthenticationException;
import com.github.b3kt.infrastructure.persistence.entity.customer.CustomerAccountEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganEntity;
import com.github.b3kt.infrastructure.persistence.repository.customer.CustomerAccountRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPelangganRepository;
import com.github.b3kt.infrastructure.security.CustomerJwtTokenService;
import com.github.b3kt.infrastructure.security.CustomerRefreshTokenClaims;
import com.github.b3kt.infrastructure.security.PasswordEncoder;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CustomerAuthService Tests")
class CustomerAuthServiceTest {

    @Mock
    CustomerAccountRepository accountRepository;

    @Mock
    CustomerJwtTokenService jwtTokenService;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    CustomerRefreshTokenService refreshTokenService;

    @Mock
    TbPelangganRepository pelangganRepository;

    @InjectMocks
    CustomerAuthService service;

    @BeforeEach
    void setUp() {
        when(jwtTokenService.generateToken(any(CustomerAccountEntity.class))).thenReturn("access");
        when(jwtTokenService.getTokenExpirationSeconds()).thenReturn(1800L);
        when(refreshTokenService.issue(any(CustomerAccountEntity.class))).thenReturn("refresh");
        when(refreshTokenService.issue(any(CustomerAccountEntity.class), anyString())).thenReturn("refresh-2");
    }

    private CustomerAccountEntity account(boolean active, Long pelangganId) {
        CustomerAccountEntity account = new CustomerAccountEntity();
        account.setId(5L);
        account.setEmail("budi@test.com");
        account.setFullName("Budi");
        account.setPasswordHash("hash");
        account.setActive(active);
        account.setPelangganId(pelangganId);
        return account;
    }

    private CustomerRegisterRequest registerRequest() {
        CustomerRegisterRequest request = new CustomerRegisterRequest();
        request.setEmail("budi@test.com");
        request.setFullName("Budi");
        request.setPassword("ValidPass123");
        return request;
    }

    @Nested
    @DisplayName("display name")
    class DisplayNameTests {

        @Test
        @DisplayName("uses the linked customer record's name once the account is linked")
        void linkedUsesPelangganName() {
            CustomerAccountEntity account = account(true, 7L);
            TbPelangganEntity pelanggan = new TbPelangganEntity();
            pelanggan.setNamaPelanggan("Budi Santoso");
            when(accountRepository.findByEmail("budi@test.com")).thenReturn(Optional.of(account));
            when(passwordEncoder.matches("pw", "hash")).thenReturn(true);
            when(pelangganRepository.findByIdCached(7L)).thenReturn(Optional.of(pelanggan));

            assertEquals("Budi Santoso", service.login("budi@test.com", "pw").getFullName());
        }

        @Test
        @DisplayName("falls back to the registration name when unlinked or the record is gone")
        void fallsBackToAccountName() {
            when(accountRepository.findByEmail("budi@test.com")).thenReturn(Optional.of(account(true, null)));
            when(passwordEncoder.matches("pw", "hash")).thenReturn(true);
            assertEquals("Budi", service.login("budi@test.com", "pw").getFullName());

            when(accountRepository.findByEmail("budi@test.com")).thenReturn(Optional.of(account(true, 7L)));
            when(pelangganRepository.findByIdCached(7L)).thenReturn(Optional.empty());
            assertEquals("Budi", service.login("budi@test.com", "pw").getFullName());
        }
    }

    @Nested
    @DisplayName("register")
    class RegisterTests {

        @Test
        @DisplayName("creates the account and issues tokens when the email and phone are free")
        void success() {
            when(accountRepository.findByEmail("budi@test.com")).thenReturn(Optional.empty());
            when(accountRepository.findByPhone("08123")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("ValidPass123")).thenReturn("hash");
            CustomerRegisterRequest request = registerRequest();
            request.setPhone("08123");

            CustomerLoginResponse response = service.register(request);

            assertEquals("access", response.getToken());
            assertEquals("refresh", response.getRefreshToken());
            assertEquals("budi@test.com", response.getEmail());
            assertEquals(1800L, response.getExpiresIn());
            verify(accountRepository).persist(any(CustomerAccountEntity.class));
        }

        @Test
        @DisplayName("treats a blank phone as absent")
        void blankPhone() {
            when(accountRepository.findByEmail("budi@test.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("hash");
            CustomerRegisterRequest request = registerRequest();
            request.setPhone("   ");

            assertDoesNotThrow(() -> service.register(request));
            verify(accountRepository, never()).findByPhone(anyString());
        }

        @Test
        @DisplayName("stores the phone in canonical form and checks duplicates against it")
        void canonicalPhone() {
            when(accountRepository.findByEmail("budi@test.com")).thenReturn(Optional.empty());
            when(accountRepository.findByPhone("081234567")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("hash");
            CustomerRegisterRequest request = registerRequest();
            request.setPhone("+62 812-345-67");

            service.register(request);

            ArgumentCaptor<CustomerAccountEntity> captor = ArgumentCaptor.forClass(CustomerAccountEntity.class);
            verify(accountRepository).persist(captor.capture());
            assertEquals("081234567", captor.getValue().getPhone());
            verify(accountRepository).findByPhone("081234567");
        }

        @Test
        @DisplayName("rejects an email that is already registered")
        void duplicateEmail() {
            when(accountRepository.findByEmail("budi@test.com")).thenReturn(Optional.of(account(true, null)));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.register(registerRequest()));
            assertEquals("Email is already registered", ex.getMessage());
        }

        @Test
        @DisplayName("rejects a phone that is already registered")
        void duplicatePhone() {
            when(accountRepository.findByEmail("budi@test.com")).thenReturn(Optional.empty());
            when(accountRepository.findByPhone("08123")).thenReturn(Optional.of(account(true, null)));
            CustomerRegisterRequest request = registerRequest();
            request.setPhone("08123");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.register(request));
            assertEquals("Phone number is already registered", ex.getMessage());
        }

        @Test
        @DisplayName("refuses a weak password")
        void weakPassword() {
            when(accountRepository.findByEmail("budi@test.com")).thenReturn(Optional.empty());
            CustomerRegisterRequest request = registerRequest();
            request.setPassword("short");

            assertThrows(IllegalArgumentException.class, () -> service.register(request));
        }
    }

    @Nested
    @DisplayName("login")
    class LoginTests {

        @Test
        @DisplayName("issues tokens on valid credentials")
        void success() {
            CustomerAccountEntity account = account(true, 7L);
            when(accountRepository.findByEmail("budi@test.com")).thenReturn(Optional.of(account));
            when(passwordEncoder.matches("secret", "hash")).thenReturn(true);

            CustomerLoginResponse response = service.login("budi@test.com", "secret");

            assertEquals("access", response.getToken());
            assertTrue(response.isLinked());
        }

        @Test
        @DisplayName("rejects an unknown account without revealing that it is unknown")
        void unknownAccount() {
            when(accountRepository.findByEmail("budi@test.com")).thenReturn(Optional.empty());

            AuthenticationException ex = assertThrows(AuthenticationException.class,
                    () -> service.login("budi@test.com", "secret"));
            assertEquals("Invalid email or password", ex.getMessage());
            // A dummy hash is still computed so the timing does not differ from a real account
            verify(passwordEncoder).matches(eq("secret"), isNull());
        }

        @Test
        @DisplayName("rejects a wrong password")
        void wrongPassword() {
            when(accountRepository.findByEmail("budi@test.com")).thenReturn(Optional.of(account(true, null)));
            when(passwordEncoder.matches("secret", "hash")).thenReturn(false);

            assertThrows(AuthenticationException.class, () -> service.login("budi@test.com", "secret"));
        }

        @Test
        @DisplayName("rejects an inactive account")
        void inactive() {
            when(accountRepository.findByEmail("budi@test.com")).thenReturn(Optional.of(account(false, null)));
            when(passwordEncoder.matches("secret", "hash")).thenReturn(true);

            AuthenticationException ex = assertThrows(AuthenticationException.class,
                    () -> service.login("budi@test.com", "secret"));
            assertEquals("Account is not active", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("refreshToken")
    class RefreshTests {

        @Test
        @DisplayName("rotates the token for an active account")
        void success() {
            CustomerRefreshTokenClaims claims = new CustomerRefreshTokenClaims(5L, "token-id");
            when(jwtTokenService.validateRefreshToken("refresh")).thenReturn(claims);
            when(refreshTokenService.consume(claims)).thenReturn("family-1");
            when(accountRepository.findByIdOptional(5L)).thenReturn(Optional.of(account(true, null)));

            CustomerLoginResponse response = service.refreshToken("refresh");

            assertEquals("access", response.getToken());
            assertEquals("refresh-2", response.getRefreshToken());
            verify(refreshTokenService).issue(any(CustomerAccountEntity.class), eq("family-1"));
        }

        @Test
        @DisplayName("rejects an unparseable token")
        void invalidToken() {
            when(jwtTokenService.validateRefreshToken("bad")).thenReturn(null);

            assertThrows(AuthenticationException.class, () -> service.refreshToken("bad"));
        }

        @Test
        @DisplayName("rejects a token whose account no longer exists")
        void accountMissing() {
            CustomerRefreshTokenClaims claims = new CustomerRefreshTokenClaims(5L, "token-id");
            when(jwtTokenService.validateRefreshToken("refresh")).thenReturn(claims);
            when(refreshTokenService.consume(claims)).thenReturn("family-1");
            when(accountRepository.findByIdOptional(5L)).thenReturn(Optional.empty());

            AuthenticationException ex = assertThrows(AuthenticationException.class,
                    () -> service.refreshToken("refresh"));
            assertEquals("Account not found", ex.getMessage());
        }

        @Test
        @DisplayName("revokes every token and rejects when the account became inactive")
        void inactive() {
            CustomerRefreshTokenClaims claims = new CustomerRefreshTokenClaims(5L, "token-id");
            when(jwtTokenService.validateRefreshToken("refresh")).thenReturn(claims);
            when(refreshTokenService.consume(claims)).thenReturn("family-1");
            when(accountRepository.findByIdOptional(5L)).thenReturn(Optional.of(account(false, null)));

            assertThrows(AuthenticationException.class, () -> service.refreshToken("refresh"));
            verify(refreshTokenService).revokeAllForAccount(5L);
        }
    }

    @Nested
    @DisplayName("logout")
    class LogoutTests {

        @Test
        @DisplayName("revokes all sessions when no token is supplied")
        void noToken() {
            service.logout(5L, null);

            verify(refreshTokenService).revokeAllForAccount(5L);
        }

        @Test
        @DisplayName("revokes all sessions when the token is blank")
        void blankToken() {
            service.logout(5L, "   ");

            verify(refreshTokenService).revokeAllForAccount(5L);
        }

        @Test
        @DisplayName("revokes the session when the token matches the account")
        void matchingToken() {
            CustomerRefreshTokenClaims claims = new CustomerRefreshTokenClaims(5L, "token-id");
            when(jwtTokenService.validateRefreshToken("refresh")).thenReturn(claims);

            service.logout(5L, "refresh");

            verify(refreshTokenService).revokeSession(claims);
        }

        @Test
        @DisplayName("does nothing when the token belongs to another account")
        void mismatchedAccount() {
            CustomerRefreshTokenClaims claims = new CustomerRefreshTokenClaims(9L, "token-id");
            when(jwtTokenService.validateRefreshToken("refresh")).thenReturn(claims);

            service.logout(5L, "refresh");

            verify(refreshTokenService, never()).revokeSession(any());
        }

        @Test
        @DisplayName("does nothing when the token cannot be validated")
        void invalidToken() {
            when(jwtTokenService.validateRefreshToken("refresh")).thenReturn(null);

            service.logout(5L, "refresh");

            verify(refreshTokenService, never()).revokeSession(any());
        }
    }

    @Nested
    @DisplayName("changePassword")
    class ChangePasswordTests {

        @Test
        @DisplayName("rehashes, revokes sessions and issues fresh tokens")
        void success() {
            CustomerAccountEntity account = account(true, null);
            when(accountRepository.findByIdOptional(5L)).thenReturn(Optional.of(account));
            when(passwordEncoder.matches("OldPass123", "hash")).thenReturn(true);
            when(passwordEncoder.encode("NewPass123")).thenReturn("new-hash");

            CustomerLoginResponse response = service.changePassword(5L, "OldPass123", "NewPass123");

            assertEquals("new-hash", account.getPasswordHash());
            assertEquals("access", response.getToken());
            verify(refreshTokenService).revokeAllForAccount(5L);
        }

        @Test
        @DisplayName("rejects an unknown or inactive account")
        void inactive() {
            when(accountRepository.findByIdOptional(5L)).thenReturn(Optional.empty());

            AuthenticationException ex = assertThrows(AuthenticationException.class,
                    () -> service.changePassword(5L, "OldPass123", "NewPass123"));
            assertEquals("Account is not active", ex.getMessage());
        }

        @Test
        @DisplayName("rejects a wrong current password")
        void wrongCurrent() {
            when(accountRepository.findByIdOptional(5L)).thenReturn(Optional.of(account(true, null)));
            when(passwordEncoder.matches("nope", "hash")).thenReturn(false);

            AuthenticationException ex = assertThrows(AuthenticationException.class,
                    () -> service.changePassword(5L, "nope", "NewPass123"));
            assertEquals("Current password is incorrect", ex.getMessage());
        }

        @Test
        @DisplayName("rejects a new password equal to the current one")
        void samePassword() {
            when(accountRepository.findByIdOptional(5L)).thenReturn(Optional.of(account(true, null)));
            when(passwordEncoder.matches("SamePass123", "hash")).thenReturn(true);

            assertThrows(IllegalArgumentException.class,
                    () -> service.changePassword(5L, "SamePass123", "SamePass123"));
        }

        @Test
        @DisplayName("refuses a weak new password")
        void weakNewPassword() {
            when(accountRepository.findByIdOptional(5L)).thenReturn(Optional.of(account(true, null)));
            when(passwordEncoder.matches("OldPass123", "hash")).thenReturn(true);

            assertThrows(IllegalArgumentException.class,
                    () -> service.changePassword(5L, "OldPass123", "short"));
        }
    }
}
