package com.github.b3kt.application.service.customer;

import com.github.b3kt.domain.exception.TooManyAttemptsException;
import com.github.benmanes.caffeine.cache.Ticker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NopolLookupRateLimiter Tests")
class NopolLookupRateLimiterTest {

    private final AtomicLong nanos = new AtomicLong();
    private NopolLookupRateLimiter limiter;

    @BeforeEach
    void setUp() {
        limiter = new NopolLookupRateLimiter();
        limiter.maxPlatesPerAccount = 3;
        limiter.maxPlatesPerIp = 5;
        limiter.windowMinutes = 15;
        limiter.ticker = (Ticker) nanos::get;
        limiter.init();
    }

    private void advance(Duration duration) {
        nanos.addAndGet(duration.toNanos());
    }

    private void lookUp(long accountId, String ip, String... plates) {
        for (String plate : plates) {
            limiter.checkAndRecord(accountId, ip, plate);
        }
    }

    @Test
    @DisplayName("Blocks an account after the distinct-plate limit, with the time left in the window")
    void blocksAccount() {
        lookUp(1L, "10.0.0.1", "B1", "B2", "B3");
        advance(Duration.ofMinutes(5));

        TooManyAttemptsException ex = assertThrows(TooManyAttemptsException.class,
                () -> limiter.checkAndRecord(1L, "10.0.0.1", "B4"));
        assertEquals(Duration.ofMinutes(10).toSeconds(), ex.getRetryAfterSeconds());
        assertTrue(ex.getMessage().startsWith("Too many plate lookups"));
    }

    @Test
    @DisplayName("Re-checking a plate already looked up is free, even at the limit")
    void repeatPlateIsFree() {
        lookUp(1L, "10.0.0.1", "B1", "B2", "B3");

        assertDoesNotThrow(() -> lookUp(1L, "10.0.0.1", "B1", "B2", "B3", "B1"));
    }

    @Test
    @DisplayName("A new window starts once the old one has passed")
    void windowResets() {
        lookUp(1L, "10.0.0.1", "B1", "B2", "B3");
        advance(Duration.ofMinutes(15));

        assertDoesNotThrow(() -> lookUp(1L, "10.0.0.1", "B4", "B5", "B6"));
    }

    @Test
    @DisplayName("Spreading lookups over many accounts from one address hits the per-IP limit")
    void blocksAddressAcrossAccounts() {
        lookUp(1L, "10.0.0.1", "B1", "B2");
        lookUp(2L, "10.0.0.1", "B3", "B4");
        lookUp(3L, "10.0.0.1", "B5");

        assertThrows(TooManyAttemptsException.class, () -> limiter.checkAndRecord(4L, "10.0.0.1", "B6"));
        // The same account from another address is not affected by that address's limit
        assertDoesNotThrow(() -> limiter.checkAndRecord(4L, "10.0.0.2", "B6"));
    }

    @Test
    @DisplayName("A refused lookup is not recorded, so it doesn't use up the address's quota")
    void refusedLookupNotRecorded() {
        lookUp(1L, "10.0.0.1", "B1", "B2", "B3");
        for (int i = 0; i < 10; i++) {
            String plate = "X" + i;
            assertThrows(TooManyAttemptsException.class, () -> limiter.checkAndRecord(1L, "10.0.0.1", plate));
        }

        assertDoesNotThrow(() -> lookUp(2L, "10.0.0.1", "C1", "C2"));
    }

    @Test
    @DisplayName("Works without a client address (account limit only)")
    void noClientAddress() {
        lookUp(1L, null, "B1", "B2", "B3");

        assertThrows(TooManyAttemptsException.class, () -> limiter.checkAndRecord(1L, null, "B4"));
    }
}
