package com.github.b3kt.presentation.rest;

import io.vertx.core.http.HttpServerRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.NewCookie;

/**
 * Builds the auth cookies, shared by the password and Google sign-in paths so there is one
 * definition of how they are scoped rather than a copy per resource.
 */
@ApplicationScoped
public class AuthCookies {

    public static final String REFRESH_COOKIE = "refresh_token";

    /**
     * The refresh cookie: only ever sent to the auth endpoints, unreadable by page scripts, and
     * {@code SameSite=STRICT}.
     *
     * <p>Strict works for the Google flow only because the callback hands the browser back to the
     * SPA, which then calls {@code /api/auth/refresh} as a same-site request. A "simplification"
     * that redirected straight from Google into a protected page would break it.
     */
    public NewCookie refresh(HttpServerRequest request, String value, long maxAgeSeconds) {
        return new NewCookie.Builder(REFRESH_COOKIE)
                .value(value == null ? "" : value)
                .path("/api/auth")
                .httpOnly(true)
                .secure(isSecure(request))
                .sameSite(NewCookie.SameSite.STRICT)
                .maxAge((int) maxAgeSeconds)
                .build();
    }

    /**
     * The short-lived cookie holding the OAuth {@code state} and PKCE verifier.
     *
     * <p>{@code SameSite=LAX} is required here, not a relaxation: the browser returns from
     * accounts.google.com as a cross-site top-level navigation, and a Strict cookie would not be
     * sent on it - which is the one moment it is needed. The narrow path keeps it out of every
     * other request, including {@code /api/auth/refresh}.
     */
    public NewCookie oauthState(HttpServerRequest request, String value, long maxAgeSeconds) {
        return new NewCookie.Builder("google_oauth_state")
                .value(value == null ? "" : value)
                .path("/api/auth/google")
                .httpOnly(true)
                .secure(isSecure(request))
                .sameSite(NewCookie.SameSite.LAX)
                .maxAge((int) maxAgeSeconds)
                .build();
    }

    /**
     * Whether to mark cookies Secure. Unchanged from how the refresh cookie has always been issued.
     *
     * <p>Known caveat, deliberately left alone here: behind a TLS-terminating proxy this reports
     * http unless {@code quarkus.http.proxy.proxy-address-forwarding} is enabled, so the flag is
     * dropped in that deployment. Reading {@code X-Forwarded-Proto} directly would fix it but
     * bypasses the trusted-proxy allowlist - on a plain-http deployment anyone could then set the
     * header, have the cookie marked Secure, and have the browser silently discard it. Fix it by
     * turning on Quarkus's proxy support, as its own change.
     */
    private boolean isSecure(HttpServerRequest request) {
        return request != null && "https".equalsIgnoreCase(request.scheme());
    }
}
