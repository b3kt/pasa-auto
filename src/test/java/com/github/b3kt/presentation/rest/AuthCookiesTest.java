package com.github.b3kt.presentation.rest;

import io.vertx.core.http.HttpServerRequest;
import jakarta.ws.rs.core.NewCookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * How the auth cookies are scoped. These are the flags that decide whether a stolen cookie is
 * usable, so each one is pinned rather than left to the builder's defaults.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthCookies Tests")
class AuthCookiesTest {

    private final AuthCookies authCookies = new AuthCookies();

    @Mock
    HttpServerRequest request;

    private HttpServerRequest over(String scheme) {
        when(request.scheme()).thenReturn(scheme);
        return request;
    }

    @Test
    @DisplayName("The refresh cookie is httpOnly, Strict, and confined to /api/auth")
    void refreshCookieIsScopedToAuth() {
        NewCookie cookie = authCookies.refresh(over("https"), "refresh-value", 604800L);

        assertEquals(AuthCookies.REFRESH_COOKIE, cookie.getName());
        assertEquals("refresh-value", cookie.getValue());
        assertEquals("/api/auth", cookie.getPath(),
                "a wider path would send the refresh token to every API call");
        assertTrue(cookie.isHttpOnly(), "page scripts must not be able to read it");
        assertEquals(NewCookie.SameSite.STRICT, cookie.getSameSite());
        assertEquals(604800, cookie.getMaxAge());
    }

    @Test
    @DisplayName("The state cookie is Lax and confined to /api/auth/google")
    void oauthStateCookieIsLaxAndNarrow() {
        NewCookie cookie = authCookies.oauthState(over("https"), "state:verifier", 300L);

        assertEquals("google_oauth_state", cookie.getName());
        assertEquals("state:verifier", cookie.getValue());
        assertEquals("/api/auth/google", cookie.getPath(),
                "it must not ride along on /api/auth/refresh");
        assertTrue(cookie.isHttpOnly());
        assertEquals(NewCookie.SameSite.LAX, cookie.getSameSite(),
                "Strict would not be sent on the cross-site return from accounts.google.com");
        assertEquals(300, cookie.getMaxAge());
    }

    @Test
    @DisplayName("A null value clears the cookie rather than throwing")
    void nullValueBecomesEmpty() {
        assertEquals("", authCookies.oauthState(over("https"), null, 0).getValue());
        assertEquals(0, authCookies.oauthState(over("https"), null, 0).getMaxAge());
        assertEquals("", authCookies.refresh(over("https"), null, 0).getValue());
    }

    @Test
    @DisplayName("Secure follows the request scheme")
    void secureFollowsScheme() {
        assertTrue(authCookies.refresh(over("https"), "v", 60).isSecure());
        assertTrue(authCookies.refresh(over("HTTPS"), "v", 60).isSecure(), "the scheme is not case sensitive");
        assertFalse(authCookies.refresh(over("http"), "v", 60).isSecure());
        assertTrue(authCookies.oauthState(over("https"), "v", 60).isSecure());
        assertFalse(authCookies.oauthState(over("http"), "v", 60).isSecure());
    }

    @Test
    @DisplayName("A missing request is treated as insecure instead of failing")
    void nullRequestIsNotSecure() {
        assertFalse(authCookies.refresh(null, "v", 60).isSecure());
        assertFalse(authCookies.oauthState(null, "v", 60).isSecure());
    }

    /**
     * Pins the documented decision not to trust X-Forwarded-Proto here: on a plain-http deployment
     * anyone could set the header, have the cookie marked Secure and have the browser discard it.
     * Behind a real proxy the flag comes from quarkus.http.proxy.proxy-address-forwarding, which
     * rewrites the scheme this reads.
     */
    @Test
    @DisplayName("X-Forwarded-Proto alone does not make the cookie Secure")
    void forwardedProtoHeaderIsNotTrusted() {
        when(request.scheme()).thenReturn("http");
        when(request.getHeader("X-Forwarded-Proto")).thenReturn("https");

        assertFalse(authCookies.refresh(request, "v", 60).isSecure());
        verify(request, never()).getHeader("X-Forwarded-Proto");
    }
}
