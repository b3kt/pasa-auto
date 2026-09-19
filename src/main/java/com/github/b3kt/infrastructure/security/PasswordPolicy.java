package com.github.b3kt.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * Rules for user-chosen passwords, and generation of temporary ones.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    /** bcrypt only uses the first 72 bytes; longer input would be silently truncated. */
    public static final int MAX_BYTES = 72;

    private static final int TEMPORARY_LENGTH = 12;
    /** No look-alike characters (0/O, 1/l/I), so a temporary password can be read out or copied by hand. */
    private static final String TEMPORARY_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordPolicy() {
    }

    /**
     * @throws IllegalArgumentException with a user-facing message when the password is not acceptable
     */
    public static void validate(String password, String username) {
        if (password == null || password.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_LENGTH + " characters");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new IllegalArgumentException("Password must be at most " + MAX_BYTES + " bytes");
        }
        if (username != null && password.equalsIgnoreCase(username)) {
            throw new IllegalArgumentException("Password must not be the same as the username");
        }
    }

    public static String generateTemporary() {
        StringBuilder password = new StringBuilder(TEMPORARY_LENGTH);
        for (int i = 0; i < TEMPORARY_LENGTH; i++) {
            password.append(TEMPORARY_ALPHABET.charAt(RANDOM.nextInt(TEMPORARY_ALPHABET.length())));
        }
        return password.toString();
    }
}
