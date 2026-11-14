package com.bm.copilot.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** TEMPORARY stub: only logs. Deleted when the real database-backed AuditService is added (Week 2). */
@Service
class LoggingAuditService implements AuditService {

    private static final Logger log = LoggerFactory.getLogger(LoggingAuditService.class);

    @Override
    public void record(AuditEventCommand event) {
        logEvent(event);
    }

    @Override
    public void recordInNewTransaction(AuditEventCommand event) {
        logEvent(event);
    }

    private void logEvent(AuditEventCommand event) {
        // details are deliberately not logged: they may contain sensitive data
        log.info("AUDIT (stub) actor={} action={} entity={}:{}",
                event.actorId(), event.action(), event.entityType(), event.entityId());
    }
}