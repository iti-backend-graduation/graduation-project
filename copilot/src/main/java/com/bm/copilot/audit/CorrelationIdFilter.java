package com.bm.copilot.audit;

import com.bm.copilot.common.CorrelationId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gives every request a correlation ID: the caller's X-Correlation-Id if it has a safe format, otherwise a new UUID.
 * Runs before Spring Security, so even 401 and 403 responses carry the ID.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class CorrelationIdFilter extends OncePerRequestFilter {

    /** Only letters, digits, - and _ up to 64 characters: a client-chosen value must never be able to fake log lines. */
    private static final Pattern SAFE_FORMAT = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String correlationId = resolve(request.getHeader(CorrelationId.HEADER));
        MDC.put(CorrelationId.KEY, correlationId);
        response.setHeader(CorrelationId.HEADER, correlationId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(CorrelationId.KEY); // server threads are reused: never leave the ID behind
        }
    }

    static String resolve(String candidate) {
        if (candidate != null && SAFE_FORMAT.matcher(candidate).matches()) {
            return candidate;
        }
        return UUID.randomUUID().toString();
    }
}
