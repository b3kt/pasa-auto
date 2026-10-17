package com.github.b3kt.application.service.customer;

import com.github.b3kt.domain.exception.TooManyAttemptsException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

/**
 * Throttles plate lookups in the customer portal (ADR-0013). A lookup reveals whether a plate is on
 * record, with its brand/type and a masked owner name, so probing many plates must be bounded.
 *
 * <p>What is counted is <em>distinct plates</em> per fixed window, per customer account and per client IP:
 * checking the same plate again (every blur of the plate field) is free, while walking through many
 * plates is cut off. The per-IP limit is higher (a shared connection may host several customers) and
 * stops one person spreading lookups across many freshly registered accounts. Counters live in memory,
 * so they are per application instance and reset on restart.
 */
@Slf4j
@ApplicationScoped
public class NopolLookupRateLimiter {

    @ConfigProperty(name = "app.security.nopol-lookup.max-plates-per-account", defaultValue = "20")
    int maxPlatesPerAccount;

    @ConfigProperty(name = "app.security.nopol-lookup.max-plates-per-ip", defaultValue = "60")
    int maxPlatesPerIp;

    @ConfigProperty(name = "app.security.nopol-lookup.window-minutes", defaultValue = "15")
    long windowMinutes;

    Ticker ticker = Ticker.systemTicker();

    private Cache<String, Window> windows;

    /** Plates looked up since {@code startNanos} (ticker time). Immutable; replaced on every change. */
    private record Window(long startNanos, Set<String> plates) {
    }

    @PostConstruct
    void init() {
        windows = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(windowMinutes))
                // Bounded so a flood of accounts or addresses can't exhaust memory
                .maximumSize(100_000)
                .ticker(ticker)
                .build();
    }

    /**
     * Admits a lookup of {@code plate} (canonical form) and records it.
     *
     * @throws TooManyAttemptsException when the account or the client IP already looked up the maximum
     *                                  number of other plates in the current window
     */
    public void checkAndRecord(Long customerAccountId, String clientIp, String plate) {
        String accountKey = customerAccountId == null ? null : "account:" + customerAccountId;
        String ipKey = clientIp == null || clientIp.isBlank() ? null : "ip:" + clientIp;
        long now = ticker.read();

        long retryAfter = Math.max(
                retryAfterSeconds(accountKey, plate, maxPlatesPerAccount, now),
                retryAfterSeconds(ipKey, plate, maxPlatesPerIp, now));
        if (retryAfter > 0) {
            log.warn("Plate lookups throttled (account: {}, ip: {})", customerAccountId, clientIp);
            throw new TooManyAttemptsException("Too many plate lookups", retryAfter);
        }
        record(accountKey, plate, now);
        record(ipKey, plate, now);
    }

    private long retryAfterSeconds(String key, String plate, int limit, long now) {
        Window window = key == null ? null : current(windows.getIfPresent(key), now);
        if (window == null || window.plates().contains(plate) || window.plates().size() < limit) {
            return 0;
        }
        long remainingNanos = Duration.ofMinutes(windowMinutes).toNanos() - (now - window.startNanos());
        return Math.max(1, Duration.ofNanos(remainingNanos).toSeconds());
    }

    private void record(String key, String plate, long now) {
        if (key == null) {
            return;
        }
        windows.asMap().compute(key, (k, existing) -> {
            Window window = current(existing, now);
            if (window == null) {
                return new Window(now, Set.of(plate));
            }
            if (window.plates().contains(plate)) {
                return window;
            }
            Set<String> plates = new HashSet<>(window.plates());
            plates.add(plate);
            return new Window(window.startNanos(), Set.copyOf(plates));
        });
    }

    /** The window if it is still running at {@code now}, else null (a new one starts). */
    private Window current(Window window, long now) {
        if (window == null || now - window.startNanos() >= Duration.ofMinutes(windowMinutes).toNanos()) {
            return null;
        }
        return window;
    }
}
