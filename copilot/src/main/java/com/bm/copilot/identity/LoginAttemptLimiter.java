package com.bm.copilot.identity;

import com.bm.copilot.common.TooManyRequestsException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Blocks a (client address, username) pair after too many failed logins until the window ends.
 * Kept in memory: simple, and enough for one instance (it resets when the application restarts).
 */
@Component
class LoginAttemptLimiter {

    private static final int CLEANUP_THRESHOLD = 10_000;

    private record Attempts(int failures, Instant windowStart) {}

    private final ConcurrentHashMap<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final int maxFailures;
    private final Duration lockout;
    private final Clock clock;

    @Autowired
    LoginAttemptLimiter(@Value("${copilot.security.login.max-failures:5}") int maxFailures,
                        @Value("${copilot.security.login.lockout:15m}") Duration lockout) {
        this(maxFailures, lockout, Clock.systemUTC());
    }

    LoginAttemptLimiter(int maxFailures, Duration lockout, Clock clock) {
        this.maxFailures = maxFailures;
        this.lockout = lockout;
        this.clock = clock;
    }

    static String keyFor(String clientAddress, String username) {
        return clientAddress + "|" + username.trim().toLowerCase(Locale.ROOT);
    }

    /** Throws when this key is blocked. Does not count as an attempt. */
    void checkAllowed(String key) {
        Attempts current = attempts.get(key);
        if (current == null) {
            return;
        }
        Instant now = clock.instant();
        Instant blockedUntil = current.windowStart().plus(lockout);
        if (!now.isBefore(blockedUntil)) {
            attempts.remove(key, current);
            return;
        }
        if (current.failures() >= maxFailures) {
            long retryAfter = Math.max(1, Duration.between(now, blockedUntil).toSeconds() + 1);
            throw new TooManyRequestsException("Too many failed login attempts. Try again later.", retryAfter);
        }
    }

    void recordFailure(String key) {
        Instant now = clock.instant();
        if (attempts.size() > CLEANUP_THRESHOLD) {
            attempts.values().removeIf(a -> !now.isBefore(a.windowStart().plus(lockout)));
        }
        attempts.compute(key, (k, current) ->
                current == null || !now.isBefore(current.windowStart().plus(lockout))
                        ? new Attempts(1, now)
                        : new Attempts(current.failures() + 1, current.windowStart()));
    }

    void reset(String key) {
        attempts.remove(key);
    }
}
