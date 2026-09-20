package com.github.b3kt.infrastructure.google;

import com.github.b3kt.domain.exception.AuthenticationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * What the reader accepts out of a Google id token, and what it refuses.
 */
class GoogleIdTokenReaderTest {

    private static final String CLIENT_ID = "our-client-id.apps.googleusercontent.com";

    private final GoogleIdTokenReader reader = new GoogleIdTokenReader();

    @Test
    @DisplayName("a well-formed token yields the subject and lower-cased email")
    void readsIdentity() {
        String token = token("""
                {"iss":"https://accounts.google.com","aud":"%s","exp":%d,
                 "email_verified":true,"sub":"sub-123","email":"Budi@Example.com"}
                """.formatted(CLIENT_ID, future()));

        GoogleIdentity identity = reader.read(token, CLIENT_ID);

        assertEquals("sub-123", identity.subject());
        assertEquals("budi@example.com", identity.email());
    }

    @Test
    @DisplayName("a token for another client is refused")
    void refusesForeignAudience() {
        String token = token("""
                {"iss":"https://accounts.google.com","aud":"someone-else","exp":%d,
                 "email_verified":true,"sub":"sub-123","email":"budi@example.com"}
                """.formatted(future()));

        assertThrows(AuthenticationException.class, () -> reader.read(token, CLIENT_ID));
    }

    @Test
    @DisplayName("an expired token is refused")
    void refusesExpiredToken() {
        String token = token("""
                {"iss":"https://accounts.google.com","aud":"%s","exp":%d,
                 "email_verified":true,"sub":"sub-123","email":"budi@example.com"}
                """.formatted(CLIENT_ID, Instant.now().minusSeconds(60).getEpochSecond()));

        assertThrows(AuthenticationException.class, () -> reader.read(token, CLIENT_ID));
    }

    @Test
    @DisplayName("an unverified email is refused, since accounts are matched on it")
    void refusesUnverifiedEmail() {
        String token = token("""
                {"iss":"https://accounts.google.com","aud":"%s","exp":%d,
                 "email_verified":false,"sub":"sub-123","email":"budi@example.com"}
                """.formatted(CLIENT_ID, future()));

        assertThrows(AuthenticationException.class, () -> reader.read(token, CLIENT_ID));
    }

    @Test
    @DisplayName("an unexpected issuer is refused")
    void refusesForeignIssuer() {
        String token = token("""
                {"iss":"https://evil.example.com","aud":"%s","exp":%d,
                 "email_verified":true,"sub":"sub-123","email":"budi@example.com"}
                """.formatted(CLIENT_ID, future()));

        assertThrows(AuthenticationException.class, () -> reader.read(token, CLIENT_ID));
    }

    @Test
    @DisplayName("a malformed or missing token is refused")
    void refusesMalformedToken() {
        assertThrows(AuthenticationException.class, () -> reader.read(null, CLIENT_ID));
        assertThrows(AuthenticationException.class, () -> reader.read("", CLIENT_ID));
        assertThrows(AuthenticationException.class, () -> reader.read("not.a.jwt", CLIENT_ID));
    }


    @Test
    @DisplayName("the second issuer Google uses is accepted too")
    void acceptsBareIssuer() {
        String token = token("""
                {"iss":"accounts.google.com","aud":"%s","exp":%d,
                 "email_verified":true,"sub":"sub-123","email":"budi@example.com"}
                """.formatted(CLIENT_ID, future()));

        assertEquals("sub-123", reader.read(token, CLIENT_ID).subject());
    }

    /** A token with too few or too many segments is not a JWT. */
    @Test
    @DisplayName("a token without three segments is refused")
    void refusesWrongSegmentCount() {
        assertThrows(AuthenticationException.class, () -> reader.read("only.two", CLIENT_ID));
        assertThrows(AuthenticationException.class, () -> reader.read("a.b.c.d", CLIENT_ID));
        assertThrows(AuthenticationException.class, () -> reader.read("   ", CLIENT_ID));
    }

    /** A payload that is not base64, or not JSON, must be reported rather than thrown raw. */
    @Test
    @DisplayName("an unreadable payload is refused")
    void refusesUnreadablePayload() {
        AuthenticationException notBase64 = assertThrows(AuthenticationException.class,
                () -> reader.read("header.!!!not-base64!!!.sig", CLIENT_ID));
        assertEquals("Google id token could not be read", notBase64.getMessage());

        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String notJson = "header." + encoder.encodeToString("{not json".getBytes(StandardCharsets.UTF_8)) + ".sig";
        assertThrows(AuthenticationException.class, () -> reader.read(notJson, CLIENT_ID));
    }

    /** exp missing entirely reads as 0, which must count as expired rather than as "no expiry". */
    @Test
    @DisplayName("a token with no expiry is refused")
    void refusesMissingExpiry() {
        String token = token("""
                {"iss":"https://accounts.google.com","aud":"%s",
                 "email_verified":true,"sub":"sub-123","email":"budi@example.com"}
                """.formatted(CLIENT_ID));

        AuthenticationException ex = assertThrows(AuthenticationException.class, () -> reader.read(token, CLIENT_ID));
        assertEquals("Google id token has expired", ex.getMessage());
    }

    @Test
    @DisplayName("a token with no issuer claim at all is refused")
    void refusesMissingIssuer() {
        String token = token("""
                {"aud":"%s","exp":%d,"email_verified":true,"sub":"sub-123","email":"budi@example.com"}
                """.formatted(CLIENT_ID, future()));

        assertThrows(AuthenticationException.class, () -> reader.read(token, CLIENT_ID));
    }

    /** A null claim is not the same as an absent one, and both must read as missing. */
    @Test
    @DisplayName("a missing or null subject or email is refused")
    void refusesMissingSubjectOrEmail() {
        String noSub = token("""
                {"iss":"https://accounts.google.com","aud":"%s","exp":%d,
                 "email_verified":true,"email":"budi@example.com"}
                """.formatted(CLIENT_ID, future()));
        String nullSub = token("""
                {"iss":"https://accounts.google.com","aud":"%s","exp":%d,
                 "email_verified":true,"sub":null,"email":"budi@example.com"}
                """.formatted(CLIENT_ID, future()));
        String blankEmail = token("""
                {"iss":"https://accounts.google.com","aud":"%s","exp":%d,
                 "email_verified":true,"sub":"sub-123","email":"  "}
                """.formatted(CLIENT_ID, future()));

        for (String token : new String[]{noSub, nullSub, blankEmail}) {
            AuthenticationException ex = assertThrows(AuthenticationException.class,
                    () -> reader.read(token, CLIENT_ID));
            assertEquals("Google id token is missing the subject or email claim", ex.getMessage());
        }
    }

    /** email_verified absent must default to false, not to trusted. */
    @Test
    @DisplayName("a token with no email_verified claim is refused")
    void refusesAbsentEmailVerified() {
        String token = token("""
                {"iss":"https://accounts.google.com","aud":"%s","exp":%d,
                 "sub":"sub-123","email":"budi@example.com"}
                """.formatted(CLIENT_ID, future()));

        assertThrows(AuthenticationException.class, () -> reader.read(token, CLIENT_ID));
    }

    private long future() {
        return Instant.now().plusSeconds(300).getEpochSecond();
    }

    /** A JWT-shaped string: only the payload matters, the signature is never checked. */
    private String token(String payloadJson) {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return encoder.encodeToString("{\"alg\":\"RS256\"}".getBytes(StandardCharsets.UTF_8))
                + "." + encoder.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8))
                + ".signature-not-checked";
    }
}
