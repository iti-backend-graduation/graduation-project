package com.bm.copilot.approval;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A supervisor's decision on one request: who, what, why, which fields they changed, and when. Never updated. */
@Entity
@Table(name = "approval_decisions")
class ApprovalDecision {

    @Id
    private UUID id;

    @Column(name = "request_id", nullable = false, updatable = false)
    private UUID requestId;

    @Column(name = "supervisor_id", nullable = false, updatable = false)
    private UUID supervisorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 30)
    private Decision decision;

    @Column(updatable = false, length = 500)
    private String reason;

    /** Edit-and-approve only: field name -> {"from": old, "to": new}. Empty for the other decisions. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, updatable = false, columnDefinition = "jsonb")
    private Map<String, Object> changes;

    @Column(name = "decided_at", nullable = false, updatable = false)
    private Instant decidedAt;

    /** Required by JPA, which creates empty objects and fills the fields itself. */
    protected ApprovalDecision() {
    }

    ApprovalDecision(UUID requestId, UUID supervisorId, Decision decision, String reason,
                     Map<String, Object> changes) {
        this.id = UUID.randomUUID();
        this.requestId = requestId;
        this.supervisorId = supervisorId;
        this.decision = decision;
        this.reason = reason;
        this.changes = changes;
        this.decidedAt = Instant.now();
    }

    UUID getRequestId() {
        return requestId;
    }

    UUID getSupervisorId() {
        return supervisorId;
    }

    Decision getDecision() {
        return decision;
    }

    String getReason() {
        return reason;
    }

    Map<String, Object> getChanges() {
        return changes;
    }

    Instant getDecidedAt() {
        return decidedAt;
    }
}
