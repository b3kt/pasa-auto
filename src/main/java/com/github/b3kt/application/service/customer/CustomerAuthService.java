package com.github.b3kt.application.service.customer;

import com.github.b3kt.application.dto.customer.CustomerLoginResponse;
import com.github.b3kt.application.dto.customer.CustomerRegisterRequest;
import com.github.b3kt.application.helper.PhoneNormalizer;
import com.github.b3kt.domain.exception.AuthenticationException;
import com.github.b3kt.infrastructure.persistence.entity.customer.CustomerAccountEntity;
import com.github.b3kt.infrastructure.persistence.repository.customer.CustomerAccountRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPelangganRepository;
import com.github.b3kt.infrastructure.security.CustomerJwtTokenService;
import com.github.b3kt.infrastructure.security.CustomerRefreshTokenClaims;
import com.github.b3kt.infrastructure.security.PasswordEncoder;
import com.github.b3kt.infrastructure.security.PasswordPolicy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.Locale;
import java.util.Optional;

/**
 * Registration/login/refresh/logout for the customer portal. Independent of the staff
 * {@code AuthService} - a customer is never a {@code User}, so nothing here shares state with it
 * beyond the password hashing utility.
 */
@ApplicationScoped
public class CustomerAuthService {

    @Inject
    CustomerAccountRepository accountRepository;

    @Inject
    CustomerJwtTokenService jwtTokenService;

    @Inject
    PasswordEncoder passwordEncoder;

    @Inject
    CustomerRefreshTokenService refreshTokenService;

    @Inject
    TbPelangganRepository pelangganRepository;

    private volatile String dummyHash;

    /**
     * Registers with an email, a phone number, or both (at least one); either can be used to log in.
     */
    @Transactional
    public CustomerLoginResponse register(CustomerRegisterRequest request) {
        String email = request.getEmail() == null || request.getEmail().isBlank() ? null : request.getEmail().trim();
        // Canonical form, so "+62 812-3" and "0812 3" are recognised as the same login phone.
        String phone = PhoneNormalizer.normalize(request.getPhone());
        if (request.getPhone() != null && !request.getPhone().isBlank() && !PhoneNormalizer.isValid(phone)) {
            throw new IllegalArgumentException("Phone number is not valid");
        }
        if (email == null && phone == null) {
            throw new IllegalArgumentException("Enter an email or a phone number");
        }
        if (email != null && accountRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email is already registered");
        }
        if (phone != null && accountRepository.findByPhone(phone).isPresent()) {
            throw new IllegalArgumentException("Phone number is already registered");
        }
        PasswordPolicy.validate(request.getPassword(), email);
        PasswordPolicy.validate(request.getPassword(), phone);

        CustomerAccountEntity account = new CustomerAccountEntity();
        account.setEmail(email);
        account.setPhone(phone);
        account.setFullName(request.getFullName().trim());
        account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        account.setActive(true);
        accountRepository.persist(account);

        return issueTokens(account);
    }

    /**
     * Logs in with either login identifier: an email (anything containing {@code @}) or a phone number
     * in any format. Unknown identifiers take the same time as a wrong password.
     */
    @Transactional
    public CustomerLoginResponse login(String identifier, String password) {
        CustomerAccountEntity account = findByLoginIdentifier(identifier).orElse(null);
        if (account == null) {
            passwordEncoder.matches(password, dummyHash());
            throw new AuthenticationException("Invalid email/phone or password");
        }
        if (!passwordEncoder.matches(password, account.getPasswordHash())) {
            throw new AuthenticationException("Invalid email/phone or password");
        }
        if (!account.isActive()) {
            throw new AuthenticationException("Account is not active");
        }
        return issueTokens(account);
    }

    @Transactional
    public CustomerLoginResponse refreshToken(String refreshToken) {
        CustomerRefreshTokenClaims claims = jwtTokenService.validateRefreshToken(refreshToken);
        if (claims == null) {
            throw new AuthenticationException("Invalid or expired refresh token");
        }
        String familyId = refreshTokenService.consume(claims);

        CustomerAccountEntity account = accountRepository.findByIdOptional(claims.customerAccountId())
                .orElseThrow(() -> new AuthenticationException("Account not found"));
        if (!account.isActive()) {
            refreshTokenService.revokeAllForAccount(account.getId());
            throw new AuthenticationException("Account is not active");
        }
        return issueTokens(account, refreshTokenService.issue(account, familyId));
    }

    @Transactional
    public void logout(Long customerAccountId, String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            refreshTokenService.revokeAllForAccount(customerAccountId);
            return;
        }
        CustomerRefreshTokenClaims claims = jwtTokenService.validateRefreshToken(refreshToken);
        if (claims != null && claims.customerAccountId().equals(customerAccountId)) {
            refreshTokenService.revokeSession(claims);
        }
    }

    @Transactional
    public CustomerLoginResponse changePassword(Long customerAccountId, String currentPassword, String newPassword) {
        CustomerAccountEntity account = accountRepository.findByIdOptional(customerAccountId)
                .filter(CustomerAccountEntity::isActive)
                .orElseThrow(() -> new AuthenticationException("Account is not active"));

        if (!passwordEncoder.matches(currentPassword, account.getPasswordHash())) {
            throw new AuthenticationException("Current password is incorrect");
        }
        PasswordPolicy.validate(newPassword, account.getEmail());
        PasswordPolicy.validate(newPassword, account.getPhone());
        if (newPassword.equals(currentPassword)) {
            throw new IllegalArgumentException("New password must be different from the current password");
        }

        account.setPasswordHash(passwordEncoder.encode(newPassword));
        refreshTokenService.revokeAllForAccount(account.getId());
        return issueTokens(account);
    }

    /**
     * The registration name until the account is linked; afterwards the customer record's name, which
     * is authoritative (ADR-0011) and may have been corrected by staff.
     */
    private String displayName(CustomerAccountEntity account) {
        if (account.getPelangganId() == null) {
            return account.getFullName();
        }
        return pelangganRepository.findByIdCached(account.getPelangganId())
                .map(p -> p.getNamaPelanggan())
                .filter(name -> !name.isBlank())
                .orElse(account.getFullName());
    }

    /**
     * Canonical form of a login identifier (lower-case email, or canonical phone), so that rate limiting
     * counts "+62 812-3" and "08123" as the same target. Never returns null.
     */
    public static String loginKey(String identifier) {
        if (identifier == null) {
            return "";
        }
        String trimmed = identifier.trim();
        if (trimmed.contains("@")) {
            return trimmed.toLowerCase(Locale.ROOT);
        }
        String phone = PhoneNormalizer.normalize(trimmed);
        return phone != null ? phone : trimmed;
    }

    private Optional<CustomerAccountEntity> findByLoginIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }
        if (identifier.contains("@")) {
            return accountRepository.findByEmail(identifier);
        }
        return PhoneNormalizer.normalize(identifier) == null
                ? Optional.empty()
                : accountRepository.findByPhone(identifier);
    }

    private String dummyHash() {
        if (dummyHash == null) {
            dummyHash = passwordEncoder.encode(java.util.UUID.randomUUID().toString());
        }
        return dummyHash;
    }

    private CustomerLoginResponse issueTokens(CustomerAccountEntity account) {
        return issueTokens(account, refreshTokenService.issue(account));
    }

    private CustomerLoginResponse issueTokens(CustomerAccountEntity account, String refreshToken) {
        return new CustomerLoginResponse(
                jwtTokenService.generateToken(account),
                refreshToken,
                account.getEmail(),
                displayName(account),
                jwtTokenService.getTokenExpirationSeconds(),
                account.isLinked());
    }
}
