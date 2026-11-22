package com.bm.copilot.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * One audit record: who did what to which entity. A persistence detail of the audit module:
 * other modules use {@link AuditService}, never this class. Rows are only ever inserted.
 * The details must never contain passwords, tokens or other secrets.
 */
@Entity
@Table(name = "audit_events")
class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 60)
    private String entityType;

    @Column(name = "entity_id", nullable = false, length = 100)
    private String entityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> details;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Required by JPA, which creates empty objects and fills the fields itself. */
    protected AuditEvent() {
    }

    AuditEvent(UUID actorId, String action, String entityType, String entityId,
               Map<String, Object> details, String correlationId) {
        this.actorId = actorId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.details = details;
        this.correlationId = correlationId;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    UUID getId() {
        return id;
    }

    UUID getActorId() {
        return actorId;
    }

    String getAction() {
        return action;
    }

    String getEntityType() {
        return entityType;
    }

    String getEntityId() {
        return entityId;
    }

    Map<String, Object> getDetails() {
        return details;
    }

    String getCorrelationId() {
        return correlationId;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
