package com.github.b3kt.application.service;

import com.github.b3kt.domain.exception.TooManyAttemptsException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.util.Locale;

/**
 * Throttles password guessing. Failed attempts are counted per account and per client IP; once either
 * reaches its limit, further attempts are refused until the lockout period has passed since the last failure.
 * <p>
 * The per-account limit stops guessing one user's password from many addresses; the per-IP limit (higher, so
 * a shared office connection isn't locked out by one person) stops trying many accounts from one address.
 * Counters live in memory, so they are per application instance and reset on restart.
 */
@Slf4j
@ApplicationScoped
public class LoginAttemptService {

    @ConfigProperty(name = "app.security.login.max-failures-per-user", defaultValue = "5")
    int maxFailuresPerUser;

    @ConfigProperty(name = "app.security.login.max-failures-per-ip", defaultValue = "20")
    int maxFailuresPerIp;

    @ConfigProperty(name = "app.security.login.lockout-minutes", defaultValue = "15")
    long lockoutMinutes;

    Ticker ticker = Ticker.systemTicker();

    private Cache<String, Attempts> failures;

    /** Failure count and the time of the latest failure, in ticker nanoseconds. */
    private record Attempts(int count, long lastFailureNanos) {
    }

    @PostConstruct
    void init() {
        failures = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(lockoutMinutes))
                // Bounded so a flood of random usernames can't exhaust memory
                .maximumSize(100_000)
                .ticker(ticker)
                .build();
    }

    /**
     * @throws TooManyAttemptsException when the account or the client IP is currently locked out
     */
    public void checkAllowed(String username, String clientIp) {
        long retryAfter = Math.max(
                retryAfterSeconds(userKey(username), maxFailuresPerUser),
                retryAfterSeconds(ipKey(clientIp), maxFailuresPerIp));
        if (retryAfter > 0) {
            throw new TooManyAttemptsException(retryAfter);
        }
    }

    public void recordFailure(String username, String clientIp) {
        long now = ticker.read();
        int userCount = increment(userKey(username), now);
        int ipCount = increment(ipKey(clientIp), now);
        if (userCount == maxFailuresPerUser || ipCount == maxFailuresPerIp) {
            log.warn("Login locked for {} minutes after repeated failures (user: {}, ip: {})",
                    lockoutMinutes, username, clientIp);
        }
    }

    /** A successful login clears the account's failures; the IP counter keeps decaying on its own. */
    public void recordSuccess(String username) {
        String key = userKey(username);
        if (key != null) {
            failures.invalidate(key);
        }
    }

    private int increment(String key, long now) {
        if (key == null) {
            return 0;
        }
        return failures.asMap()
                .merge(key, new Attempts(1, now), (old, one) -> new Attempts(old.count() + 1, now))
                .count();
    }

    private long retryAfterSeconds(String key, int limit) {
        Attempts attempts = key == null ? null : failures.getIfPresent(key);
        if (attempts == null || attempts.count() < limit) {
            return 0;
        }
        long elapsedNanos = ticker.read() - attempts.lastFailureNanos();
        long remainingNanos = Duration.ofMinutes(lockoutMinutes).toNanos() - elapsedNanos;
        return remainingNanos <= 0 ? 0 : Math.max(1, Duration.ofNanos(remainingNanos).toSeconds());
    }

    private static String userKey(String username) {
        return username == null || username.isBlank() ? null : "user:" + username.trim().toLowerCase(Locale.ROOT);
    }

    private static String ipKey(String clientIp) {
        return clientIp == null || clientIp.isBlank() ? null : "ip:" + clientIp;
    }
}
