package com.github.b3kt.infrastructure.google;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.b3kt.domain.exception.AuthenticationException;
import jakarta.enterprise.context.ApplicationScoped;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;

/**
 * Reads the claims out of a Google id token and checks them.
 *
 * <p>The signature is deliberately not verified. OpenID Connect Core 3.1.3.7 allows skipping it
 * when the token was fetched directly from the issuer's token endpoint over a TLS-verified
 * channel, which is exactly how {@link GoogleTokenClient} obtains it - there is no path by which a
 * caller can inject a token of their own here. Were this ever to accept a token from the browser
 * instead, signature verification against Google's JWKS would become mandatory.
 */
@ApplicationScoped
public class GoogleIdTokenReader {

    private static final Set<String> VALID_ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * @param idToken  the id token from the token endpoint
     * @param clientId our OAuth client id, which the token's audience must match
     * @return the verified identity
     * @throws AuthenticationException if the token is malformed, expired, for another audience, or
     *                                 carries an unverified email
     */
    public GoogleIdentity read(String idToken, String clientId) {
        JsonNode claims = decodePayload(idToken);

        String issuer = text(claims, "iss");
        // Set.of(...).contains(null) throws NPE by contract, and a token may simply have no iss
        // claim - that has to be refused like any other wrong issuer, not crash.
        if (issuer == null || !VALID_ISSUERS.contains(issuer)) {
            throw new AuthenticationException("Google id token has an unexpected issuer: " + issuer);
        }

        String audience = text(claims, "aud");
        if (!clientId.equals(audience)) {
            throw new AuthenticationException("Google id token was issued for another client");
        }

        long expiry = claims.path("exp").asLong(0);
        if (expiry <= 0 || Instant.ofEpochSecond(expiry).isBefore(Instant.now())) {
            throw new AuthenticationException("Google id token has expired");
        }

        // An unverified email must never be trusted: it is what accounts are matched on
        if (!claims.path("email_verified").asBoolean(false)) {
            throw new AuthenticationException("Google has not verified this email address");
        }

        String subject = text(claims, "sub");
        String email = text(claims, "email");
        if (subject == null || subject.isBlank() || email == null || email.isBlank()) {
            throw new AuthenticationException("Google id token is missing the subject or email claim");
        }

        return new GoogleIdentity(subject, email.toLowerCase());
    }

    private JsonNode decodePayload(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new AuthenticationException("Google did not return an id token");
        }
        String[] parts = idToken.split("\\.");
        if (parts.length != 3) {
            throw new AuthenticationException("Google id token is malformed");
        }
        try {
            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            return objectMapper.readTree(new String(payload, StandardCharsets.UTF_8));
        } catch (IllegalArgumentException | com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new AuthenticationException("Google id token could not be read");
        }
    }

    private String text(JsonNode claims, String field) {
        JsonNode value = claims.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
