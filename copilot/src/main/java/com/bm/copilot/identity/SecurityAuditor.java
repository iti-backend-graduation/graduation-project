package com.bm.copilot.identity;

import com.bm.copilot.audit.AuditEventCommand;
import com.bm.copilot.audit.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Writes the security events of the identity module to the audit trail: logins and denied access.
 * Never stores passwords, tokens or the username typed for an unknown account, and never lets a
 * failing audit write change the outcome of a login or a refusal.
 */
@Component
class SecurityAuditor {

    private static final Logger log = LoggerFactory.getLogger(SecurityAuditor.class);
    private static final String UNKNOWN_ENTITY = "UNKNOWN";
    private static final int MAX_ENTITY_ID_LENGTH = 100;

    private final AuditService auditService;
    private final CurrentUserProvider currentUsers;

    SecurityAuditor(AuditService auditService, CurrentUserProvider currentUsers) {
        this.auditService = auditService;
        this.currentUsers = currentUsers;
    }

    void loginSucceeded(UUID userId) {
        record(userId, "LOGIN_SUCCEEDED", "USER", userId.toString(), Map.of());
    }

    /** {@code userId} is null when the username does not exist. The typed username is deliberately not stored. */
    void loginFailed(UUID userId, String reason) {
        record(userId, "LOGIN_FAILED", "USER", userId == null ? UNKNOWN_ENTITY : userId.toString(),
                Map.of("reason", reason));
    }

    /** A logged-in caller was refused. Only the path is stored, never the query string (it may hold secrets). */
    void accessDenied(HttpServletRequest request) {
        UUID actorId = null;
        String role = "UNKNOWN";
        try {
            CurrentUser caller = currentUsers.require();
            actorId = caller.id();
            role = caller.role().name();
        } catch (RuntimeException noAuthenticatedCaller) {
            // keep the event with an unknown actor
        }
        String path = request.getRequestURI();
        String target = path.length() > MAX_ENTITY_ID_LENGTH ? path.substring(0, MAX_ENTITY_ID_LENGTH) : path;
        record(actorId, "ACCESS_DENIED", "ENDPOINT", target, Map.of("method", request.getMethod(), "role", role));
    }

    private void record(UUID actorId, String action, String entityType, String entityId, Map<String, Object> details) {
        try {
            auditService.recordInNewTransaction(new AuditEventCommand(actorId, action, entityType, entityId, details));
        } catch (RuntimeException e) {
            log.error("Could not write audit event {}", action, e);
        }
    }
}
