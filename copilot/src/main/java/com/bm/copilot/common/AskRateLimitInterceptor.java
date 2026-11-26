package com.bm.copilot.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Limits how often one logged-in user can call the copilot, because every question can cost several model calls. */
@Component
class AskRateLimitInterceptor implements HandlerInterceptor {

    private final FixedWindowRateLimiter limiter;

    AskRateLimitInterceptor(@Value("${copilot.rate-limit.ask.max-requests:10}") int maxRequests,
                            @Value("${copilot.rate-limit.ask.window:1m}") Duration window) {
        this.limiter = new FixedWindowRateLimiter(maxRequests, window, Clock.systemUTC());
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication caller = SecurityContextHolder.getContext().getAuthentication();
        if (caller != null && caller.isAuthenticated() && !(caller instanceof AnonymousAuthenticationToken)) {
            limiter.check(caller.getName());
        }
        return true;
    }
}
