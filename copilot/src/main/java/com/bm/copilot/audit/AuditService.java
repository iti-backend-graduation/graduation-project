package com.bm.copilot.audit;

/** Public entry point of the audit module. Other modules depend on this, never on audit internals. */
public interface AuditService {

    /** Records the event inside the caller's transaction: saved or rolled back together with the business change. */
    void record(AuditEventCommand event);

    /** Records the event in its own transaction, so it survives a rollback (failed logins, denied access). */
    void recordInNewTransaction(AuditEventCommand event);
}