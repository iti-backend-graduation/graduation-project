package com.bm.copilot.approval;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** The result of a simulated execution. At most one per request (unique in the database). Never updated. */
@Entity
@Table(name = "execution_outcomes")
class ExecutionOutcome {

    @Id
    private UUID id;

    @Column(name = "request_id", nullable = false, updatable = false)
    private UUID requestId;

    @Column(name = "executed_by", nullable = false, updatable = false)
    private UUID executedBy;

    @Column(nullable = false, updatable = false, length = 500)
    private String message;

    @Column(name = "executed_at", nullable = false, updatable = false)
    private Instant executedAt;

    /** Required by JPA, which creates empty objects and fills the fields itself. */
    protected ExecutionOutcome() {
    }

    ExecutionOutcome(UUID requestId, UUID executedBy, String message) {
        this.id = UUID.randomUUID();
        this.requestId = requestId;
        this.executedBy = executedBy;
        this.message = message;
        this.executedAt = Instant.now();
    }

    UUID getRequestId() {
        return requestId;
    }

    UUID getExecutedBy() {
        return executedBy;
    }

    String getMessage() {
        return message;
    }

    Instant getExecutedAt() {
        return executedAt;
    }
}
