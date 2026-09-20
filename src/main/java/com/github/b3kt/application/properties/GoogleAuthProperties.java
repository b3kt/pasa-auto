package com.github.b3kt.application.properties;

import io.smallrye.config.ConfigMapping;

import java.util.Optional;

/**
 * Configuration for signing in with Google.
 *
 * <p>The credentials are {@link Optional} on purpose: a required accessor would fail startup
 * anywhere the environment variables are unset - including every test run - even with the feature
 * switched off. They are checked instead by {@code GoogleAuthConfigValidator}, and only when
 * {@link #enabled()} is true.
 */
@ConfigMapping(prefix = "app.auth.google")
public interface GoogleAuthProperties {

    /**
     * Enable or disable Google sign-in. While false every /api/auth/google endpoint answers 404.
     * @return true if Google sign-in is enabled
     */
    boolean enabled();

    Optional<String> clientId();

    Optional<String> clientSecret();

    /**
     * Absolute URL of the callback, matching an authorized redirect URI on the Google OAuth client.
     * It is taken from configuration rather than from the incoming request, which a spoofed Host
     * header could otherwise control.
     */
    Optional<String> redirectUri();
}
