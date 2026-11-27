package com.bm.copilot.common;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Allows at most {@code maxRequests} per key in each time window; the next request gets 429.
 * Kept in memory: enough for one instance (counters reset when the application restarts).
 */
class FixedWindowRateLimiter {

    private static final int CLEANUP_THRESHOLD = 10_000;

    private record Window(int count, Instant start) {}

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final int maxRequests;
    private final Duration window;
    private final Clock clock;

    FixedWindowRateLimiter(int maxRequests, Duration window, Clock clock) {
        this.maxRequests = maxRequests;
        this.window = window;
        this.clock = clock;
    }

    /** Counts one request for this key and throws when the key is over its limit. */
    void check(String key) {
        Instant now = clock.instant();
        if (windows.size() > CLEANUP_THRESHOLD) {
            windows.values().removeIf(w -> !now.isBefore(w.start().plus(window)));
        }
        Window current = windows.compute(key, (k, existing) ->
                existing == null || !now.isBefore(existing.start().plus(window))
                        ? new Window(1, now)
                        : new Window(existing.count() + 1, existing.start()));
        if (current.count() > maxRequests) {
            long retryAfter = Math.max(1, Duration.between(now, current.start().plus(window)).toSeconds() + 1);
            throw new TooManyRequestsException("Too many requests. Try again later.", retryAfter);
        }
    }
}
