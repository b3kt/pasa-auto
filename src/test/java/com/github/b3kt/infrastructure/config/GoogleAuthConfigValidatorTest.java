package com.github.b3kt.infrastructure.config;

import com.github.b3kt.application.properties.GoogleAuthProperties;
import io.quarkus.runtime.StartupEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Misconfigured Google sign-in must stop startup, not break the flow after the user has already
 * been handed to Google.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("GoogleAuthConfigValidator Tests")
class GoogleAuthConfigValidatorTest {

    private static final String REDIRECT = "http://localhost:8080/api/auth/google/callback";

    @Mock
    GoogleAuthProperties properties;

    private final GoogleAuthConfigValidator validator = new GoogleAuthConfigValidator();

    @BeforeEach
    void setUp() {
        validator.properties = properties;
        when(properties.enabled()).thenReturn(true);
        when(properties.clientId()).thenReturn(Optional.of("client-id"));
        when(properties.clientSecret()).thenReturn(Optional.of("client-secret"));
        when(properties.redirectUri()).thenReturn(Optional.of(REDIRECT));
    }

    private IllegalStateException failure() {
        return assertThrows(IllegalStateException.class, () -> validator.onStart(new StartupEvent()));
    }

    @Test
    @DisplayName("A complete configuration starts")
    void acceptsCompleteConfiguration() {
        assertDoesNotThrow(() -> validator.onStart(new StartupEvent()));
    }

    /**
     * Nothing may be required while the feature is off, or every environment without the
     * credentials - including each test run - would fail to boot.
     */
    @Test
    @DisplayName("Nothing is checked while the feature is disabled")
    void skipsEverythingWhenDisabled() {
        when(properties.enabled()).thenReturn(false);
        when(properties.clientId()).thenReturn(Optional.empty());
        when(properties.clientSecret()).thenReturn(Optional.empty());
        when(properties.redirectUri()).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> validator.onStart(new StartupEvent()));
        verify(properties, never()).clientId();
    }

    @Test
    @DisplayName("A missing client id is reported with the variable and the property to set")
    void rejectsMissingClientId() {
        when(properties.clientId()).thenReturn(Optional.empty());

        String message = failure().getMessage();
        assertTrue(message.contains("GOOGLE_CLIENT_ID"), message);
        assertTrue(message.contains("app.auth.google.client-id"), message);
        assertTrue(message.contains("GOOGLE_AUTH_ENABLED=false"),
                "the message should offer the way out as well as the fix, got: " + message);
    }

    @Test
    @DisplayName("A missing client secret is reported")
    void rejectsMissingClientSecret() {
        when(properties.clientSecret()).thenReturn(Optional.empty());

        assertTrue(failure().getMessage().contains("GOOGLE_CLIENT_SECRET"));
    }

    @Test
    @DisplayName("A missing redirect URI is reported")
    void rejectsMissingRedirectUri() {
        when(properties.redirectUri()).thenReturn(Optional.empty());

        assertTrue(failure().getMessage().contains("GOOGLE_REDIRECT_URI"));
    }

    /** An unset environment variable expands to an empty string, not to an absent value. */
    @Test
    @DisplayName("A blank value counts as unset")
    void rejectsBlankValue() {
        when(properties.clientId()).thenReturn(Optional.of("   "));

        assertTrue(failure().getMessage().contains("GOOGLE_CLIENT_ID"));
    }

    /**
     * A relative URI would never match an authorized redirect URI on the OAuth client, so Google
     * would reject the request after the user had already been sent there.
     */
    @Test
    @DisplayName("A relative redirect URI is rejected")
    void rejectsRelativeRedirectUri() {
        when(properties.redirectUri()).thenReturn(Optional.of("/api/auth/google/callback"));

        String message = failure().getMessage();
        assertTrue(message.contains("must be an absolute URL"), message);
        assertTrue(message.contains("/api/auth/google/callback"),
                "the offending value should be echoed back, got: " + message);
    }

    @Test
    @DisplayName("An https redirect URI is accepted")
    void acceptsHttpsRedirectUri() {
        when(properties.redirectUri()).thenReturn(Optional.of("https://pasa.example.com/api/auth/google/callback"));

        assertDoesNotThrow(() -> validator.onStart(new StartupEvent()));
    }
}
