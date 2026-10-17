package com.github.b3kt.domain.exception;

/**
 * Thrown when an operation was attempted too often recently (failed logins, lookups); the caller should
 * wait before retrying.
 */
public class TooManyAttemptsException extends RuntimeException {

    private final long retryAfterSeconds;

    public TooManyAttemptsException(long retryAfterSeconds) {
        this("Too many failed attempts", retryAfterSeconds);
    }

    /** @param reason what was attempted too often; the wait time is appended */
    public TooManyAttemptsException(String reason, long retryAfterSeconds) {
        super(reason + ". Try again in " + Math.max(1, (retryAfterSeconds + 59) / 60) + " minute(s).");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
