package com.github.b3kt.presentation.rest;

import com.github.b3kt.application.dto.LoginResponse;
import com.github.b3kt.application.properties.GoogleAuthProperties;
import com.github.b3kt.application.service.GoogleAuthService;
import com.github.b3kt.infrastructure.google.GoogleIdTokenReader;
import com.github.b3kt.infrastructure.google.GoogleIdentity;
import com.github.b3kt.infrastructure.google.GoogleTokenClient;
import com.github.b3kt.infrastructure.google.GoogleTokenResponse;
import com.github.b3kt.infrastructure.security.JwtTokenService;
import io.vertx.core.http.HttpServerRequest;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Sign in with Google, as an OAuth authorization-code flow handled entirely on the server.
 *
 * <p>The browser is sent to Google by {@code /start} and comes back to {@code /callback}, which
 * exchanges the code and - on success - issues this application's own session: the same access
 * token and refresh cookie a password login produces. The access token never travels in a URL;
 * the callback sets the refresh cookie and hands the browser to the SPA, which mints an access
 * token from it through the existing refresh endpoint.
 *
 * <p>Every endpoint answers 404 while {@code app.auth.google.enabled} is false, so a disabled
 * feature has no reachable surface.
 */
@Slf4j
@Path("/api/auth/google")
@PermitAll
@RequestScoped
public class GoogleAuthResource {

    private static final String GOOGLE_AUTHORIZE_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final int STATE_COOKIE_MAX_AGE_SECONDS = 300;
    private static final String LOGIN_PATH = "/#/login";

    @Inject
    GoogleAuthProperties properties;

    @Inject
    GoogleAuthService googleAuthService;

    @Inject
    GoogleIdTokenReader idTokenReader;

    @Inject
    AuthCookies authCookies;

    @Inject
    JwtTokenService jwtTokenService;

    @RestClient
    GoogleTokenClient tokenClient;

    @Context
    HttpServerRequest request;

    /**
     * Send the browser to Google's consent screen.
     *
     * <p>The {@code state} and the PKCE verifier are kept together in one short-lived cookie, so a
     * state value cannot be replayed against a verifier from a different attempt.
     */
    @GET
    @Path("/start")
    public Response start() {
        if (!properties.enabled()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        String state = randomToken();
        String codeVerifier = randomToken();

        String authorizeUrl = GOOGLE_AUTHORIZE_ENDPOINT
                + "?client_id=" + encode(properties.clientId().orElseThrow())
                + "&redirect_uri=" + encode(properties.redirectUri().orElseThrow())
                + "&response_type=code"
                + "&scope=" + encode("openid email profile")
                + "&state=" + encode(state)
                + "&code_challenge=" + encode(codeChallenge(codeVerifier))
                + "&code_challenge_method=S256"
                + "&prompt=select_account";

        return Response.seeOther(URI.create(authorizeUrl))
                .cookie(authCookies.oauthState(request, state + ":" + codeVerifier, STATE_COOKIE_MAX_AGE_SECONDS))
                .build();
    }

    /**
     * Handle the return from Google.
     *
     * <p>Always answers with a redirect back to the login page, never with a JSON error: an
     * exception escaping to {@code GlobalExceptionHandler} would render an API envelope as a raw
     * page in the user's browser.
     */
    @GET
    @Path("/callback")
    public Response callback(
            @QueryParam("code") String code,
            @QueryParam("state") String state,
            @QueryParam("error") String error,
            @CookieParam("google_oauth_state") String stateCookie) {

        if (!properties.enabled()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        try {
            if (error != null && !error.isBlank()) {
                // The user declined at Google's consent screen
                return redirect("denied", null);
            }
            if (code == null || code.isBlank() || state == null || state.isBlank()) {
                return redirect("error", null);
            }

            String codeVerifier = verifierForState(state, stateCookie);
            if (codeVerifier == null) {
                log.warn("Google callback rejected: state did not match the cookie");
                return redirect("error", null);
            }

            GoogleTokenResponse tokens = tokenClient.exchangeCode(
                    code,
                    properties.clientId().orElseThrow(),
                    properties.clientSecret().orElseThrow(),
                    properties.redirectUri().orElseThrow(),
                    codeVerifier,
                    "authorization_code");

            GoogleIdentity identity = idTokenReader.read(tokens.getIdToken(), properties.clientId().orElseThrow());
            GoogleAuthService.Result result = googleAuthService.signIn(identity);

            return switch (result.outcome()) {
                case SUCCESS -> redirect("ok", result.session());
                case PENDING -> redirect("pending", null);
                case REJECTED -> redirect("rejected", null);
                case DISABLED -> redirect("disabled", null);
                case LINK_REQUIRED -> redirect("link_required", null);
                case AMBIGUOUS -> redirect("error", null);
            };
        } catch (Exception e) {
            log.warn("Google sign-in failed", e);
            return redirect("error", null);
        }
    }

    /**
     * Redirect back to the SPA, clearing the state cookie and - on success - setting the refresh
     * cookie the SPA immediately exchanges for an access token.
     */
    private Response redirect(String outcome, LoginResponse session) {
        Response.ResponseBuilder builder = Response.seeOther(URI.create(LOGIN_PATH + "?google=" + outcome))
                .cookie(authCookies.oauthState(request, null, 0));

        if (session != null && session.getRefreshToken() != null) {
            builder.cookie(authCookies.refresh(request, session.getRefreshToken(),
                    jwtTokenService.getRefreshTokenLifetime().toSeconds()));
        }
        return builder.build();
    }

    /** The PKCE verifier stored with this state, or null when the pair does not match. */
    private String verifierForState(String state, String stateCookie) {
        if (stateCookie == null) {
            return null;
        }
        int separator = stateCookie.indexOf(':');
        if (separator < 0) {
            return null;
        }
        String expectedState = stateCookie.substring(0, separator);
        String codeVerifier = stateCookie.substring(separator + 1);

        boolean matches = MessageDigest.isEqual(
                expectedState.getBytes(StandardCharsets.UTF_8),
                state.getBytes(StandardCharsets.UTF_8));
        return matches ? codeVerifier : null;
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String codeChallenge(String codeVerifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required for PKCE", e);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
