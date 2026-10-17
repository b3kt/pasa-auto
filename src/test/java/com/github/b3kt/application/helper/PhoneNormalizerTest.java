package com.github.b3kt.application.helper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class PhoneNormalizerTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "'0812-3456-7890',    081234567890",
            "'+62 812 3456 7890', 081234567890",
            "'62812345',          0812345",
            "'+62 0812 345',      0812345",
            "'(021) 555-1234',    0215551234",
            "'  0811000111 ',     0811000111"
    })
    @DisplayName("canonicalizes to national digits")
    void canonicalizes(String raw, String expected) {
        assertEquals(expected, PhoneNormalizer.normalize(raw));
    }

    @Test
    @DisplayName("returns null when there are no digits")
    void noDigits() {
        assertNull(PhoneNormalizer.normalize(null));
        assertNull(PhoneNormalizer.normalize(""));
        assertNull(PhoneNormalizer.normalize("  "));
        assertNull(PhoneNormalizer.normalize("-"));
    }

    @Test
    @DisplayName("is idempotent")
    void idempotent() {
        String once = PhoneNormalizer.normalize("+62 812-3456-7890");
        assertEquals(once, PhoneNormalizer.normalize(once));
    }

    @Test
    @DisplayName("isValid accepts plausible national numbers only")
    void isValid() {
        assertTrue(PhoneNormalizer.isValid("+62 812-3456-7890"));
        assertTrue(PhoneNormalizer.isValid("0215551234"));
        assertFalse(PhoneNormalizer.isValid("0812"));
        assertFalse(PhoneNormalizer.isValid("12345678"));
        assertFalse(PhoneNormalizer.isValid("abc"));
        assertFalse(PhoneNormalizer.isValid(null));
    }
}
