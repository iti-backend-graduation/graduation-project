package com.bm.copilot.identity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

/**
 * Records every refusal (403) in the audit trail, then steps aside: it returns null, which means
 * "not handled", so GlobalExceptionHandler still builds the response exactly as before.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class AccessDeniedAuditResolver implements HandlerExceptionResolver {

    private final SecurityAuditor auditor;

    AccessDeniedAuditResolver(SecurityAuditor auditor) {
        this.auditor = auditor;
    }

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response,
                                         Object handler, Exception exception) {
        if (exception instanceof AccessDeniedException) {
            auditor.accessDenied(request);
        }
        return null;
    }
}
