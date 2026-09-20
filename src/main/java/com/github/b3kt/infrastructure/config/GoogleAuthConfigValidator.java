package com.github.b3kt.infrastructure.config;

import com.github.b3kt.application.properties.GoogleAuthProperties;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.Optional;

/**
 * Fails startup with an actionable message when Google sign-in is switched on without the
 * credentials it needs, instead of letting the application come up and break the flow halfway
 * through, after the user has already been sent to Google.
 *
 * <p>Nothing is checked while the feature is disabled, which is why
 * {@link GoogleAuthProperties} holds the credentials as {@code Optional}.
 */
@Slf4j
@ApplicationScoped
public class GoogleAuthConfigValidator {

    @Inject
    GoogleAuthProperties properties;

    void onStart(@Observes StartupEvent event) {
        if (!properties.enabled()) {
            log.info("Google sign-in is disabled (app.auth.google.enabled=false)");
            return;
        }

        require(properties.clientId(), "GOOGLE_CLIENT_ID", "app.auth.google.client-id");
        require(properties.clientSecret(), "GOOGLE_CLIENT_SECRET", "app.auth.google.client-secret");
        String redirectUri = require(properties.redirectUri(), "GOOGLE_REDIRECT_URI", "app.auth.google.redirect-uri");

        if (!URI.create(redirectUri).isAbsolute()) {
            throw new IllegalStateException(
                    "GOOGLE_REDIRECT_URI must be an absolute URL that matches an authorized redirect URI on the "
                            + "Google OAuth client, for example http://localhost:8080/api/auth/google/callback - got: "
                            + redirectUri);
        }

        log.info("Google sign-in is enabled, redirecting to {}", redirectUri);
    }

    private String require(Optional<String> value, String envVariable, String property) {
        return value
                .filter(candidate -> !candidate.isBlank())
                .orElseThrow(() -> new IllegalStateException(
                        "Google sign-in is enabled but " + envVariable + " (property " + property + ") is not set. "
                                + "Set it, or switch the feature off with GOOGLE_AUTH_ENABLED=false."));
    }
}
