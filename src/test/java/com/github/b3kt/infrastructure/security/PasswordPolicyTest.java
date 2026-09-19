package com.github.b3kt.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PasswordPolicy Tests")
class PasswordPolicyTest {

    @Test
    @DisplayName("Accepts a password that meets the rules")
    void acceptsValid() {
        assertDoesNotThrow(() -> PasswordPolicy.validate("Bengkel-2026", "budi"));
    }

    @Test
    @DisplayName("Rejects missing, short, too long and username-equal passwords")
    void rejectsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> PasswordPolicy.validate(null, "budi"));
        assertThrows(IllegalArgumentException.class, () -> PasswordPolicy.validate("short", "budi"));
        assertThrows(IllegalArgumentException.class, () -> PasswordPolicy.validate("x".repeat(73), "budi"));
        assertThrows(IllegalArgumentException.class, () -> PasswordPolicy.validate("BudiSantoso", "budisantoso"));
    }

    @Test
    @DisplayName("Temporary passwords satisfy the policy, avoid look-alike characters and are unique")
    void generatesTemporary() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            String password = PasswordPolicy.generateTemporary();
            assertDoesNotThrow(() -> PasswordPolicy.validate(password, "user"));
            assertFalse(password.matches(".*[0O1lI].*"), password);
            seen.add(password);
        }
        assertEquals(200, seen.size());
    }
}
