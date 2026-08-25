package com.github.b3kt.infrastructure.security.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PasswordEncoderImpl Tests")
class PasswordEncoderImplTest {

    private final PasswordEncoderImpl encoder = new PasswordEncoderImpl();

    @Test
    @DisplayName("encode returns non-empty bcrypt hash")
    void testEncode() {
        String hash = encoder.encode("mypassword");
        assertNotNull(hash);
        assertTrue(hash.startsWith("$2"));
        assertTrue(hash.length() > 20);
    }

    @Test
    @DisplayName("matches returns false when rawPassword is null")
    void testMatches_nullRawPassword() {
        assertFalse(encoder.matches(null, "$2a$10$somehash"));
    }

    @Test
    @DisplayName("matches returns false when encodedPassword is null")
    void testMatches_nullEncodedPassword() {
        assertFalse(encoder.matches("password", null));
    }

    @Test
    @DisplayName("matches returns false when both are null")
    void testMatches_bothNull() {
        assertFalse(encoder.matches(null, null));
    }

    @Test
    @DisplayName("matches returns true for bcrypt hash")
    void testMatches_bcryptHash() {
        String hash = encoder.encode("test123");
        assertTrue(encoder.matches("test123", hash));
        assertFalse(encoder.matches("wrong", hash));
    }

    @Test
    @DisplayName("matches returns true for matching plaintext")
    void testMatches_plaintextMatch() {
        assertTrue(encoder.matches("hello", "hello"));
    }

    @Test
    @DisplayName("matches returns false for non-matching plaintext")
    void testMatches_plaintextNoMatch() {
        assertFalse(encoder.matches("hello", "world"));
    }

    @Test
    @DisplayName("isBcryptHash returns true for bcrypt prefix")
    void testIsBcryptHash_true() {
        assertTrue(encoder.isBcryptHash("$2a$10$abcdef"));
        assertTrue(encoder.isBcryptHash("$2b$10$abcdef"));
    }

    @Test
    @DisplayName("isBcryptHash returns false for non-bcrypt string")
    void testIsBcryptHash_false() {
        assertFalse(encoder.isBcryptHash("plaintext"));
        assertFalse(encoder.isBcryptHash("notabhash"));
    }

    @Test
    @DisplayName("isBcryptHash returns false for null")
    void testIsBcryptHash_null() {
        assertFalse(encoder.isBcryptHash(null));
    }
}
