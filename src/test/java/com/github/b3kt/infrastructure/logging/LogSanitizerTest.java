package com.github.b3kt.infrastructure.logging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LogSanitizer Tests")
class LogSanitizerTest {

    @Test
    @DisplayName("Masks sensitive JSON string values and keeps the rest")
    void masksJsonStrings() {
        String masked = LogSanitizer.mask(
                "{\"username\":\"budi\",\"password\":\"s3cr3t\",\"refreshToken\": \"eyJ.abc.def\"}");

        assertEquals("{\"username\":\"budi\",\"password\":\"***\",\"refreshToken\": \"***\"}", masked);
    }

    @Test
    @DisplayName("Matches keys case-insensitively and by substring")
    void matchesKeyVariants() {
        String masked = LogSanitizer.mask(
                "{\"newPassword\":\"a\",\"ACCESS_TOKEN\":\"b\",\"clientSecret\":\"c\",\"Authorization\":\"Bearer d\"}");

        assertFalse(masked.contains("\"a\"") || masked.contains("\"b\"") || masked.contains("\"c\"")
                || masked.contains("Bearer"), masked);
    }

    @Test
    @DisplayName("Masks escaped quotes inside values and non-string values")
    void masksEscapedAndNonStringValues() {
        assertEquals("{\"password\":\"***\",\"x\":1}", LogSanitizer.mask("{\"password\":\"a\\\"b,c\",\"x\":1}"));
        assertEquals("{\"pin\":1,\"token\":\"***\"}", LogSanitizer.mask("{\"pin\":1,\"token\":12345}"));
    }

    @Test
    @DisplayName("Masks key=value toString output")
    void masksToString() {
        assertEquals("LoginRequest(username=budi, password=***)",
                LogSanitizer.mask("LoginRequest(username=budi, password=s3cr3t)"));
    }

    @Test
    @DisplayName("Leaves ordinary bodies and null/empty untouched")
    void leavesOrdinaryBodies() {
        String json = "{\"namaBarang\":\"Oli\",\"harga\":50000}";
        assertEquals(json, LogSanitizer.mask(json));
        assertNull(LogSanitizer.mask(null));
        assertEquals("", LogSanitizer.mask(""));
    }
}
