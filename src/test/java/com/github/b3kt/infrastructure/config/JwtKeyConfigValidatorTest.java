package com.github.b3kt.infrastructure.config;

import io.quarkus.runtime.StartupEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtKeyConfigValidator Tests")
class JwtKeyConfigValidatorTest {

    private final JwtKeyConfigValidator validator = new JwtKeyConfigValidator();

    /** The test profile points both properties at the classpath test keys, so startup must pass. */
    @Test
    @DisplayName("Accepts the configured keys")
    void acceptsConfiguredKeys() {
        assertDoesNotThrow(() -> validator.onStart(new StartupEvent()));
    }

    @Test
    @DisplayName("Rejects a missing key file with an actionable message")
    void rejectsMissingFile(@TempDir Path dir) throws IOException {
        Path missing = dir.resolve("absent.pem");
        String previous = System.getProperty(JwtKeyConfigValidator.SIGN_KEY_PROPERTY);
        System.setProperty(JwtKeyConfigValidator.SIGN_KEY_PROPERTY, "file:" + missing);
        try {
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> validator.onStart(new StartupEvent()));
            assertTrue(ex.getMessage().contains("JWT_PRIVATE_KEY"), ex.getMessage());
            assertTrue(ex.getMessage().contains("cannot be read"), ex.getMessage());

            // A readable file passes
            Files.writeString(missing, "-----BEGIN PRIVATE KEY-----");
            assertDoesNotThrow(() -> validator.onStart(new StartupEvent()));
        } finally {
            if (previous == null) {
                System.clearProperty(JwtKeyConfigValidator.SIGN_KEY_PROPERTY);
            } else {
                System.setProperty(JwtKeyConfigValidator.SIGN_KEY_PROPERTY, previous);
            }
        }
    }

    @Test
    @DisplayName("Rejects a blank configuration value")
    void rejectsBlank() {
        String previous = System.getProperty(JwtKeyConfigValidator.VERIFY_KEY_PROPERTY);
        System.setProperty(JwtKeyConfigValidator.VERIFY_KEY_PROPERTY, " ");
        try {
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> validator.onStart(new StartupEvent()));
            assertTrue(ex.getMessage().contains("JWT_PUBLIC_KEY"), ex.getMessage());
        } finally {
            if (previous == null) {
                System.clearProperty(JwtKeyConfigValidator.VERIFY_KEY_PROPERTY);
            } else {
                System.setProperty(JwtKeyConfigValidator.VERIFY_KEY_PROPERTY, previous);
            }
        }
    }
}
