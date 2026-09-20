package com.github.b3kt.application.service;

import com.github.b3kt.application.dto.LoginResponse;
import com.github.b3kt.domain.model.ApprovalStatus;
import com.github.b3kt.infrastructure.google.GoogleIdentity;
import com.github.b3kt.infrastructure.persistence.entity.AuditTrailEntity;
import com.github.b3kt.infrastructure.persistence.entity.UserEntity;
import com.github.b3kt.infrastructure.persistence.repository.UserEntityRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Locale;

/**
 * Turns a Google identity into a session, or into a reason why there isn't one.
 *
 * <p>Google establishes who someone is; this decides whether that person may use the application.
 * An email nobody has claimed creates an account an Owner must approve, and an email that matches
 * an existing account is refused until an Owner has marked that account as Google-enabled -
 * otherwise anyone able to create a Google account with a staff member's address could walk into
 * their account.
 */
@Slf4j
@ApplicationScoped
public class GoogleAuthService {

    /** Unusable password sentinel, as established by V18: PasswordEncoderImpl only matches "$2" hashes. */
    private static final String UNUSABLE_PASSWORD = "!";

    private static final int USERNAME_MAX_LENGTH = 50;

    @Inject
    UserEntityRepository userRepository;

    @Inject
    AuthService authService;

    @Inject
    AuditTrailService auditTrailService;

    @Inject
    LoginAttemptService loginAttemptService;

    /** Why a Google sign-in did not produce a session, or that it did. */
    public enum Outcome {
        SUCCESS,
        /** The account exists but is waiting for an Owner to approve it. */
        PENDING,
        /** An Owner rejected the account. */
        REJECTED,
        /** An Owner switched the account off. */
        DISABLED,
        /** The email matches an account that has not been marked as Google-enabled. */
        LINK_REQUIRED,
        /** More than one account carries this email, so the identity is ambiguous. */
        AMBIGUOUS
    }

    /**
     * @param outcome  what happened
     * @param session  the session, present only when the outcome is {@link Outcome#SUCCESS}
     */
    public record Result(Outcome outcome, LoginResponse session) {

        static Result of(Outcome outcome) {
            return new Result(outcome, null);
        }
    }

    @Transactional
    public Result signIn(GoogleIdentity identity) {
        UserEntity byGoogleSub = userRepository.findByGoogleSub(identity.subject()).orElse(null);
        if (byGoogleSub != null) {
            return signInExisting(byGoogleSub);
        }

        List<UserEntity> byEmail = userRepository.findByEmailIgnoreCase(identity.email());
        if (byEmail.size() > 1) {
            log.warn("Google sign-in refused: {} accounts share the email address", byEmail.size());
            return Result.of(Outcome.AMBIGUOUS);
        }

        if (byEmail.size() == 1) {
            UserEntity user = byEmail.get(0);
            if (!user.isGoogleLoginEnabled()) {
                log.info("Google sign-in refused for '{}': not enabled for Google by an admin", user.getUsername());
                return Result.of(Outcome.LINK_REQUIRED);
            }
            // Trust-on-first-use: bind the account to this Google subject, which is authoritative from now on
            user.setGoogleSub(identity.subject());
            log.info("Linked user '{}' to their Google account", user.getUsername());
            return signInExisting(user);
        }

        return createPending(identity);
    }

    private Result signInExisting(UserEntity user) {
        if (user.getApprovalStatus() == ApprovalStatus.PENDING) {
            return Result.of(Outcome.PENDING);
        }
        if (user.getApprovalStatus() == ApprovalStatus.REJECTED) {
            return Result.of(Outcome.REJECTED);
        }
        if (!user.isActive()) {
            return Result.of(Outcome.DISABLED);
        }

        LoginResponse session = authService.issueSessionFor(user.getUsername());
        // Keep the two login routes consistent: a successful sign-in clears any password lockout
        loginAttemptService.recordSuccess(user.getUsername());
        return new Result(Outcome.SUCCESS, session);
    }

    private Result createPending(GoogleIdentity identity) {
        UserEntity user = new UserEntity();
        user.setUsername(deriveUsername(identity.email()));
        user.setEmail(identity.email());
        user.setGoogleSub(identity.subject());
        user.setPasswordHash(UNUSABLE_PASSWORD);
        // Explicit, not defaulted: a Google account with this flag set would have its roles stripped
        // by PasswordChangeRequiredAugmentor and could never clear it, since no password matches '!'
        user.setMustChangePassword(false);
        user.setApprovalStatus(ApprovalStatus.PENDING);
        // Belt and braces: even if the status were lost in a mapping, canAuthenticate() still refuses
        user.setActive(false);
        userRepository.persist(user);

        recordAudit(user, "GOOGLE_SIGNUP");
        log.info("Created pending account '{}' from a Google sign-in", user.getUsername());
        return Result.of(Outcome.PENDING);
    }

    /**
     * Username for a self-created account, derived from the email local part.
     *
     * <p>It has to fit {@code VARCHAR(50)} and be unique: it is also the JWT subject, the
     * refresh-token key, the login-attempt cache key and the audit author.
     */
    String deriveUsername(String email) {
        // Drop any "+tag" subaddress before sanitising, so budi.santoso+tag@ becomes budi.santoso
        // rather than budi.santosotag. This only shapes the username: the email itself is stored
        // and matched exactly as Google gave it, since not every provider treats "+" as an alias.
        // indexOf rather than split: String.split drops trailing empty strings, so "+@x" would
        // hand back an empty array and throw.
        String localPart = beforeFirst(beforeFirst(email, '@'), '+')
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]", "");
        String base = localPart.isBlank() ? "google" : localPart;
        base = base.substring(0, Math.min(base.length(), USERNAME_MAX_LENGTH));

        if (!userRepository.existsByUsername(base)) {
            return base;
        }
        for (int suffix = 2; suffix < 1000; suffix++) {
            String candidate = truncateForSuffix(base, suffix) + suffix;
            if (!userRepository.existsByUsername(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not derive a free username from " + email);
    }

    private String beforeFirst(String value, char separator) {
        int index = value.indexOf(separator);
        return index < 0 ? value : value.substring(0, index);
    }

    private String truncateForSuffix(String base, int suffix) {
        int room = USERNAME_MAX_LENGTH - String.valueOf(suffix).length();
        return base.substring(0, Math.min(base.length(), room));
    }

    private void recordAudit(UserEntity user, String action) {
        // The users table has no audit trigger (V12 covers only tb_* tables), so this is recorded here
        AuditTrailEntity audit = new AuditTrailEntity();
        audit.setTableName("users");
        audit.setAction(action);
        audit.setRecordId(user.getId());
        audit.setUsername(user.getUsername());
        auditTrailService.record(audit);
    }
}
