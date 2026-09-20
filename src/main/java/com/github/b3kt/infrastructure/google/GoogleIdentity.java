package com.github.b3kt.infrastructure.google;

/**
 * The identity Google vouches for: the account id and the verified email address.
 *
 * @param subject the Google account identifier, stable across email changes
 * @param email   the verified email address
 */
public record GoogleIdentity(String subject, String email) {
}
