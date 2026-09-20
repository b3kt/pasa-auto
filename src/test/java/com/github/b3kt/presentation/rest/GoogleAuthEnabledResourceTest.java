package com.github.b3kt.presentation.rest;

import com.github.b3kt.application.dto.LoginResponse;
import com.github.b3kt.application.service.GoogleAuthService;
import com.github.b3kt.infrastructure.google.GoogleIdTokenReader;
import com.github.b3kt.infrastructure.google.GoogleIdentity;
import com.github.b3kt.infrastructure.google.GoogleTokenClient;
import com.github.b3kt.infrastructure.google.GoogleTokenResponse;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.response.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * The Google sign-in flow with the feature switched on - the half {@link GoogleAuthResourceTest}
 * cannot reach, since it only asserts that a disabled feature has no surface.
 *
 * <p>Google itself is replaced by mocks, so nothing here touches the network.
 */
@QuarkusTest
@Tag("quarkus")
@TestProfile(GoogleAuthEnabledResourceTest.GoogleEnabledProfile.class)
@DisplayName("Google sign-in (enabled)")
class GoogleAuthEnabledResourceTest {

    static final String CLIENT_ID = "test-client-id.apps.googleusercontent.com";
    static final String CLIENT_SECRET = "test-client-secret";
    static final String REDIRECT_URI = "http://localhost:8081/api/auth/google/callback";

    public static class GoogleEnabledProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "app.auth.google.enabled", "true",
                    "app.auth.google.client-id", CLIENT_ID,
                    "app.auth.google.client-secret", CLIENT_SECRET,
                    "app.auth.google.redirect-uri", REDIRECT_URI,
                    // The generated REST client is @Dependent by default, which @InjectMock cannot
                    // replace; a normal scope is needed to stand a mock in for Google.
                    "quarkus.rest-client.google-oauth.scope", "jakarta.enterprise.context.ApplicationScoped");
        }
    }

    @InjectMock
    @RestClient
    GoogleTokenClient tokenClient;

    @InjectMock
    GoogleIdTokenReader idTokenReader;

    @InjectMock
    GoogleAuthService googleAuthService;

    private static final GoogleIdentity IDENTITY = new GoogleIdentity("google-sub-1", "budi@example.com");

    @BeforeEach
    void setUp() {
        GoogleTokenResponse tokens = new GoogleTokenResponse();
        tokens.setIdToken("id-token");
        when(tokenClient.exchangeCode(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(tokens);
        when(idTokenReader.read("id-token", CLIENT_ID)).thenReturn(IDENTITY);
    }

    // --- /start ------------------------------------------------------------------------------

    @Test
    @DisplayName("GET /start - redirects to Google with the configured client and redirect URI")
    void startRedirectsToGoogle() {
        Response response = start();

        assertEquals(303, response.statusCode());
        URI location = URI.create(response.header("Location"));
        assertEquals("accounts.google.com", location.getHost());
        assertEquals("/o/oauth2/v2/auth", location.getPath());

        Map<String, String> query = queryOf(location);
        assertEquals(CLIENT_ID, query.get("client_id"));
        assertEquals(REDIRECT_URI, query.get("redirect_uri"),
                "the redirect URI comes from configuration, never from the request Host header");
        assertEquals("code", query.get("response_type"));
        assertEquals("openid email profile", query.get("scope"));
        assertEquals("select_account", query.get("prompt"));
        assertFalse(query.getOrDefault("state", "").isBlank());
    }

    @Test
    @DisplayName("GET /start - the client secret never reaches the browser")
    void startDoesNotLeakTheSecret() {
        Response response = start();

        assertFalse(response.header("Location").contains(CLIENT_SECRET));
        assertFalse(stateCookie(response).contains(CLIENT_SECRET));
    }

    @Test
    @DisplayName("GET /start - the challenge is the S256 hash of the verifier in the cookie")
    void startSendsAPkceChallengeMatchingTheStoredVerifier() {
        Response response = start();

        Map<String, String> query = queryOf(URI.create(response.header("Location")));
        assertEquals("S256", query.get("code_challenge_method"));

        String[] cookie = stateCookie(response).split(":", 2);
        assertEquals(query.get("state"), cookie[0], "the state is stored alongside its own verifier");
        assertEquals(s256(cookie[1]), query.get("code_challenge"),
                "a challenge that did not match the verifier would make PKCE decorative");
    }

    @Test
    @DisplayName("GET /start - each attempt gets a fresh state and verifier")
    void startIsNotReplayable() {
        assertNotEquals(stateCookie(start()), stateCookie(start()));
    }

    // --- /callback ---------------------------------------------------------------------------

    @Test
    @DisplayName("GET /callback - a valid code sets the refresh cookie and clears the state cookie")
    void callbackIssuesTheSession() {
        when(googleAuthService.signIn(IDENTITY))
                .thenReturn(new GoogleAuthService.Result(GoogleAuthService.Outcome.SUCCESS,
                        new LoginResponse("access", "refresh-value", "budi", "budi@example.com", 1800L)));

        Response response = callback("the-code", "st", "st:verifier");

        assertRedirectsTo("ok", response);
        assertEquals("refresh-value", response.getCookie(AuthCookies.REFRESH_COOKIE));
        assertEquals("", stateCookie(response), "the state cookie is spent");

        assertFalse(response.header("Location").contains("access"),
                "the access token must never travel in a URL");

        verify(tokenClient).exchangeCode("the-code", CLIENT_ID, CLIENT_SECRET, REDIRECT_URI,
                "verifier", "authorization_code");
    }

    @ParameterizedTest(name = "{0} redirects to google={1}")
    @CsvSource({
            "PENDING, pending",
            "REJECTED, rejected",
            "DISABLED, disabled",
            "LINK_REQUIRED, link_required",
            "AMBIGUOUS, error"
    })
    @DisplayName("GET /callback - a refused sign-in reports why and issues no cookie")
    void callbackReportsRefusals(GoogleAuthService.Outcome outcome, String expected) {
        when(googleAuthService.signIn(IDENTITY))
                .thenReturn(new GoogleAuthService.Result(outcome, null));

        Response response = callback("the-code", "st", "st:verifier");

        assertRedirectsTo(expected, response);
        assertNull(response.getCookie(AuthCookies.REFRESH_COOKIE));
    }

    @Test
    @DisplayName("GET /callback - a declined consent screen is reported as denied, not as an error")
    void callbackHandlesUserDenial() {
        Response response = given().redirects().follow(false)
                .queryParam("error", "access_denied")
                .when().get("/api/auth/google/callback").andReturn();

        assertRedirectsTo("denied", response);
        verifyNoInteractions(tokenClient);
    }

    @ParameterizedTest(name = "state \"{1}\" against cookie \"{2}\"")
    @CsvSource(value = {
            "a wrong state,       attacker-state, real-state:verifier",
            "a missing cookie,    st,             NULL",
            "a malformed cookie,  st,             no-separator",
            "a missing code,      NULL,           st:verifier"
    }, nullValues = "NULL")
    @DisplayName("GET /callback - a state that does not match its cookie never reaches Google")
    void callbackRejectsMismatchedState(String scenario, String state, String cookie) {
        String code = "a missing code".equals(scenario) ? null : "the-code";

        Response response = callback(code, state, cookie);

        assertRedirectsTo("error", response, scenario);
        assertNull(response.getCookie(AuthCookies.REFRESH_COOKIE), scenario);
        verifyNoInteractions(tokenClient);
    }

    @Test
    @DisplayName("GET /callback - a blank code or state is refused like a missing one")
    void callbackRejectsBlankParameters() {
        assertRedirectsTo("error", callback("  ", "st", "st:verifier"));
        assertRedirectsTo("error", callback("the-code", "  ", "st:verifier"));
        verifyNoInteractions(tokenClient);
    }

    /**
     * An exception escaping to GlobalExceptionHandler would render a JSON API envelope as a raw
     * page in the user's browser, so every failure has to come back as a redirect.
     */
    @Test
    @DisplayName("GET /callback - a failure at Google redirects instead of rendering a JSON error")
    void callbackRedirectsOnFailure() {
        reset(tokenClient);
        when(tokenClient.exchangeCode(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("token endpoint is down"));

        Response response = callback("the-code", "st", "st:verifier");

        assertEquals(303, response.statusCode());
        assertRedirectsTo("error", response);
        assertNull(response.getCookie(AuthCookies.REFRESH_COOKIE));
    }

    @Test
    @DisplayName("GET /callback - an unreadable id token redirects instead of failing")
    void callbackRedirectsOnUnreadableIdToken() {
        when(idTokenReader.read(anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("signature does not verify"));

        assertRedirectsTo("error", callback("the-code", "st", "st:verifier"));
        verifyNoInteractions(googleAuthService);
    }

    // --- /api/auth/config --------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/auth/config - reports the feature as on so the SPA shows the button")
    void configReportsEnabled() {
        given().when().get("/api/auth/config")
                .then().statusCode(200)
                .body("data.googleLoginEnabled", org.hamcrest.Matchers.equalTo(true));
    }

    // --- helpers -----------------------------------------------------------------------------

    private static void assertRedirectsTo(String outcome, Response response) {
        assertRedirectsTo(outcome, response, "redirect");
    }

    /** JAX-RS resolves the relative redirect against the request, so only the tail is asserted. */
    private static void assertRedirectsTo(String outcome, Response response, String message) {
        String location = response.header("Location");
        assertTrue(location.endsWith("/#/login?google=" + outcome), message + ", got: " + location);
    }

    /** The state cookie carries a ':' separator, so the container quotes its value. */
    private static String stateCookie(Response response) {
        String value = response.getCookie("google_oauth_state");
        return value == null ? null : value.replaceAll("^\"|\"$", "");
    }

    private Response start() {
        return given().redirects().follow(false).when().get("/api/auth/google/start").andReturn();
    }

    private Response callback(String code, String state, String stateCookie) {
        var request = given().redirects().follow(false);
        if (code != null) {
            request.queryParam("code", code);
        }
        if (state != null) {
            request.queryParam("state", state);
        }
        if (stateCookie != null) {
            request.cookie("google_oauth_state", stateCookie);
        }
        return request.when().get("/api/auth/google/callback").andReturn();
    }

    private static Map<String, String> queryOf(URI uri) {
        return java.util.Arrays.stream(uri.getRawQuery().split("&"))
                .map(pair -> pair.split("=", 2))
                .collect(java.util.stream.Collectors.toMap(
                        pair -> pair[0],
                        pair -> java.net.URLDecoder.decode(pair[1], StandardCharsets.UTF_8)));
    }

    private static String s256(String verifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
