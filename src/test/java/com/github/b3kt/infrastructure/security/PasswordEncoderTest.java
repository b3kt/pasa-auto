package com.github.b3kt.infrastructure.security;

import com.github.b3kt.infrastructure.security.impl.PasswordEncoderImpl;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.security.InvalidKeyException;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class PasswordEncoderTest {

    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new PasswordEncoderImpl();
    }

    @Test
    @DisplayName("Should encode password successfully")
    void testEncode() {
        // Given
        String rawPassword = "password123";

        // When
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // Then
        assertNotNull(encodedPassword);
        assertNotEquals(rawPassword, encodedPassword);
        assertTrue(encodedPassword.length() > 0);
    }

    @Test
    @DisplayName("Should generate different hashes for same password")
    void testEncodeGeneratesDifferentHashes() {
        // Given
        String rawPassword = "password123";

        // When
        String hash1 = passwordEncoder.encode(rawPassword);
        String hash2 = passwordEncoder.encode(rawPassword);

        // Then
        assertNotEquals(hash1, hash2, "Hashes should be different due to salt");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "", "a", "ab", "abc", "password", "P@ssw0rd123!", 
        "verylongpasswordwithspecialchars!@#$%^&*()",
        "🔥🔥🔥", "password\nwith\nnewlines", "password\twith\ttabs"
    })
    @DisplayName("Should encode various password formats")
    void testEncodeVariousPasswords(String password) {
        // When
        String encoded = passwordEncoder.encode(password);

        // Then
        assertNotNull(encoded);
        assertNotEquals(password, encoded);
    }

    @Test
    @DisplayName("Should verify correct password successfully")
    void testMatchesCorrectPassword() {
        // Given
        String rawPassword = "password123";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // When
        boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);

        // Then
        assertTrue(matches);
    }

    @Test
    @DisplayName("Should reject incorrect password")
    void testMatchesIncorrectPassword() {
        // Given
        String rawPassword = "password123";
        String wrongPassword = "wrongpassword";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // When
        boolean matches = passwordEncoder.matches(wrongPassword, encodedPassword);

        // Then
        assertFalse(matches);
    }

    @Test
    @DisplayName("Should handle null inputs gracefully")
    void testNullInputs() {
        // When & Then - encode(null) throws NPE in BcryptUtil
        assertThrows(NullPointerException.class,
            () -> passwordEncoder.encode(null));
        
        // matches(null, encoded) returns false
        assertFalse(passwordEncoder.matches(null, "encoded"));
        
        // matches(password, null) returns false (not throws)
        assertFalse(passwordEncoder.matches("password", null));
    }

    @Test
    @DisplayName("Should handle empty password")
    void testEmptyPassword() {
        // Given
        String emptyPassword = "";
        String encodedPassword = passwordEncoder.encode(emptyPassword);

        // When & Then
        assertNotNull(encodedPassword);
        assertTrue(passwordEncoder.matches(emptyPassword, encodedPassword));
    }

    @Test
    @DisplayName("Should reject empty encoded password")
    void testEmptyEncodedPassword() {
        // When & Then - empty string returns false, not throws
        assertFalse(passwordEncoder.matches("password", ""));
    }

    @Test
    @DisplayName("Should handle very long passwords")
    void testVeryLongPassword() {
        // Given
        StringBuilder longPassword = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longPassword.append("a");
        }
        String password = longPassword.toString();

        // When
        String encoded = passwordEncoder.encode(password);

        // Then
        assertNotNull(encoded);
        assertTrue(passwordEncoder.matches(password, encoded));
    }

    @Test
    @DisplayName("Should handle unicode characters")
    void testUnicodeCharacters() {
        // Given
        String unicodePassword = "pásswörd123🔒";

        // When
        String encoded = passwordEncoder.encode(unicodePassword);

        // Then
        assertNotNull(encoded);
        assertTrue(passwordEncoder.matches(unicodePassword, encoded));
    }

    @Test
    @DisplayName("Should be case sensitive")
    void testCaseSensitivity() {
        // Given
        String password = "Password123";
        String lowercasePassword = "password123";
        String encoded = passwordEncoder.encode(password);

        // When & Then
        assertTrue(passwordEncoder.matches(password, encoded));
        assertFalse(passwordEncoder.matches(lowercasePassword, encoded));
    }

    @Test
    @DisplayName("Should handle encoded password with special characters")
    void testEncodedPasswordWithSpecialChars() {
        // Given
        String password = "P@ssw0rd!#$";
        String encoded = passwordEncoder.encode(password);

        // When & Then
        assertNotNull(encoded);
        assertTrue(passwordEncoder.matches(password, encoded));
    }

    @Test
    @DisplayName("Should reject malformed encoded password")
    void testMalformedEncodedPassword() {
        // Given
        String password = "password123";
        String malformedEncoded = "not_a_valid_hash";

        // When & Then - malformed non-bcrypt returns false via raw comparison
        assertFalse(passwordEncoder.matches(password, malformedEncoded));
    }

    @Test
    @DisplayName("Should handle whitespace in passwords")
    void testWhitespaceInPasswords() {
        // Given
        String passwordWithSpaces = " password with spaces ";
        String encoded = passwordEncoder.encode(passwordWithSpaces);

        // When & Then
        assertNotNull(encoded);
        assertTrue(passwordEncoder.matches(passwordWithSpaces, encoded));
        assertFalse(passwordEncoder.matches("passwordwithspaces", encoded));
    }
}
