package com.github.b3kt.application.service;

import com.github.b3kt.domain.exception.TooManyAttemptsException;
import com.github.benmanes.caffeine.cache.Ticker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LoginAttemptService Tests")
class LoginAttemptServiceTest {

    private final AtomicLong nanos = new AtomicLong();
    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        service = new LoginAttemptService();
        service.maxFailuresPerUser = 5;
        service.maxFailuresPerIp = 20;
        service.lockoutMinutes = 15;
        service.ticker = (Ticker) nanos::get;
        service.init();
    }

    private void advance(Duration duration) {
        nanos.addAndGet(duration.toNanos());
    }

    private void fail(String username, String ip, int times) {
        for (int i = 0; i < times; i++) {
            service.recordFailure(username, ip);
        }
    }

    @Test
    @DisplayName("Locks an account after the failure limit, whatever the source address")
    void locksAccount() {
        fail("budi", "10.0.0.1", 4);
        assertDoesNotThrow(() -> service.checkAllowed("budi", "10.0.0.1"));

        service.recordFailure("budi", "10.0.0.2");

        TooManyAttemptsException ex = assertThrows(TooManyAttemptsException.class,
                () -> service.checkAllowed("budi", "10.0.0.9"));
        assertEquals(Duration.ofMinutes(15).toSeconds(), ex.getRetryAfterSeconds());
        // Other accounts are unaffected
        assertDoesNotThrow(() -> service.checkAllowed("siti", "10.0.0.9"));
    }

    @Test
    @DisplayName("Usernames are matched case-insensitively")
    void caseInsensitive() {
        fail("Budi", "10.0.0.1", 5);

        assertThrows(TooManyAttemptsException.class, () -> service.checkAllowed(" budi ", "10.0.0.2"));
    }

    @Test
    @DisplayName("Locks a client IP that tries many accounts")
    void locksIp() {
        for (int i = 0; i < 20; i++) {
            service.recordFailure("user" + i, "10.0.0.1");
        }

        assertThrows(TooManyAttemptsException.class, () -> service.checkAllowed("someone-new", "10.0.0.1"));
        assertDoesNotThrow(() -> service.checkAllowed("someone-new", "10.0.0.2"));
    }

    @Test
    @DisplayName("The lockout ends once the period has passed since the last failure")
    void lockoutExpires() {
        fail("budi", "10.0.0.1", 5);
        advance(Duration.ofMinutes(10));

        TooManyAttemptsException ex = assertThrows(TooManyAttemptsException.class,
                () -> service.checkAllowed("budi", "10.0.0.1"));
        assertEquals(Duration.ofMinutes(5).toSeconds(), ex.getRetryAfterSeconds());

        advance(Duration.ofMinutes(5).plusSeconds(1));
        assertDoesNotThrow(() -> service.checkAllowed("budi", "10.0.0.1"));
    }

    @Test
    @DisplayName("A successful login clears the account's failures")
    void successResets() {
        fail("budi", "10.0.0.1", 4);
        service.recordSuccess("budi");
        fail("budi", "10.0.0.1", 4);

        assertDoesNotThrow(() -> service.checkAllowed("budi", "10.0.0.1"));
    }

    @Test
    @DisplayName("Missing username or IP is tolerated")
    void nullSafe() {
        assertDoesNotThrow(() -> {
            service.recordFailure(null, null);
            service.recordSuccess(null);
            service.checkAllowed(null, null);
        });
    }

    @Test
    @DisplayName("The message tells the user when to retry")
    void message() {
        assertEquals("Too many failed attempts. Try again in 15 minute(s).",
                new TooManyAttemptsException(900).getMessage());
        assertEquals("Too many failed attempts. Try again in 1 minute(s).",
                new TooManyAttemptsException(10).getMessage());
    }
}
