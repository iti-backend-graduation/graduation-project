package com.bm.copilot.audit;

import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stores audit events in the audit_events table.
 * The two public methods must not call each other: a call inside the same class skips Spring's transaction handling.
 */
@Service
class DatabaseAuditService implements AuditService {

    private final AuditEventRepository repository;

    DatabaseAuditService(AuditEventRepository repository) {
        this.repository = repository;
    }

    /** Joins the caller's transaction: saved or rolled back together with the business change. */
    @Override
    @Transactional
    public void record(AuditEventCommand event) {
        save(event);
    }

    /** Own transaction: kept even if the caller rolls back (failed logins, denied access). */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordInNewTransaction(AuditEventCommand event) {
        save(event);
    }

    private void save(AuditEventCommand event) {
        if (isBlank(event.action()) || isBlank(event.entityType()) || isBlank(event.entityId())) {
            throw new IllegalArgumentException("action, entityType and entityId are required");
        }
        repository.save(new AuditEvent(
                event.actorId(),
                event.action(),
                event.entityType(),
                event.entityId(),
                event.details(),
                MDC.get(DatabaseAiUsageService.CORRELATION_ID_KEY)));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
