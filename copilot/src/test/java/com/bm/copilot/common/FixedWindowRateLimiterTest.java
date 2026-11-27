package com.bm.copilot.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class FixedWindowRateLimiterTest {

    private final MutableClock clock = new MutableClock();
    private final FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(3, Duration.ofMinutes(1), clock);

    @Test
    void allowsUpToTheMaximumNumberOfRequests() {
        assertThatCode(() -> {
            limiter.check("user-1");
            limiter.check("user-1");
            limiter.check("user-1");
        }).doesNotThrowAnyException();
    }

    @Test
    void refusesTheNextRequestAndSaysHowLongToWait() {
        for (int i = 0; i < 3; i++) {
            limiter.check("user-1");
        }
        clock.advance(Duration.ofSeconds(20));

        assertThatThrownBy(() -> limiter.check("user-1"))
                .isInstanceOfSatisfying(TooManyRequestsException.class,
                        e -> assertThat(e.retryAfterSeconds()).isBetween(39L, 42L));
    }

    @Test
    void aNewWindowStartsAfterTheWindowEnds() {
        for (int i = 0; i < 3; i++) {
            limiter.check("user-1");
        }
        clock.advance(Duration.ofMinutes(1));

        assertThatCode(() -> limiter.check("user-1")).doesNotThrowAnyException();
    }

    @Test
    void usersAreCountedSeparately() {
        for (int i = 0; i < 3; i++) {
            limiter.check("user-1");
        }

        assertThatCode(() -> limiter.check("user-2")).doesNotThrowAnyException();
    }

    /** A clock the test can move forward. */
    private static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
