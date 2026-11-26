package com.bm.copilot.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bm.copilot.common.TooManyRequestsException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class LoginAttemptLimiterTest {

    private final MutableClock clock = new MutableClock();
    private final LoginAttemptLimiter limiter = new LoginAttemptLimiter(3, Duration.ofMinutes(15), clock);
    private final String key = LoginAttemptLimiter.keyFor("10.0.0.1", "employee1");

    @Test
    void allowsAttemptsBelowTheLimit() {
        limiter.recordFailure(key);
        limiter.recordFailure(key);

        assertThatCode(() -> limiter.checkAllowed(key)).doesNotThrowAnyException();
    }

    @Test
    void blocksAfterTheMaximumNumberOfFailuresAndSaysHowLongToWait() {
        for (int i = 0; i < 3; i++) {
            limiter.recordFailure(key);
        }
        clock.advance(Duration.ofMinutes(5));

        assertThatThrownBy(() -> limiter.checkAllowed(key))
                .isInstanceOfSatisfying(TooManyRequestsException.class,
                        e -> assertThat(e.retryAfterSeconds()).isBetween(595L, 605L));
    }

    @Test
    void theBlockEndsWhenTheWindowEnds() {
        for (int i = 0; i < 3; i++) {
            limiter.recordFailure(key);
        }
        clock.advance(Duration.ofMinutes(15));

        assertThatCode(() -> limiter.checkAllowed(key)).doesNotThrowAnyException();
    }

    @Test
    void aSuccessfulLoginResetsTheCounter() {
        limiter.recordFailure(key);
        limiter.recordFailure(key);
        limiter.reset(key);
        limiter.recordFailure(key);
        limiter.recordFailure(key);

        assertThatCode(() -> limiter.checkAllowed(key)).doesNotThrowAnyException();
    }

    @Test
    void otherUsersAndOtherAddressesAreNotAffected() {
        for (int i = 0; i < 3; i++) {
            limiter.recordFailure(key);
        }

        assertThatCode(() -> limiter.checkAllowed(LoginAttemptLimiter.keyFor("10.0.0.1", "supervisor1")))
                .doesNotThrowAnyException();
        assertThatCode(() -> limiter.checkAllowed(LoginAttemptLimiter.keyFor("10.0.0.2", "employee1")))
                .doesNotThrowAnyException();
    }

    @Test
    void theUsernameIsMatchedIgnoringCaseAndSpaces() {
        for (int i = 0; i < 3; i++) {
            limiter.recordFailure(LoginAttemptLimiter.keyFor("10.0.0.1", "  EMPLOYEE1 "));
        }

        assertThatThrownBy(() -> limiter.checkAllowed(key)).isInstanceOf(TooManyRequestsException.class);
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
