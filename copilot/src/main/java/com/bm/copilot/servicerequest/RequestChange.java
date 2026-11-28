package com.bm.copilot.servicerequest;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * One line of a request's history: who changed which field, from what, to what, and when.
 * Status moves are recorded here too (field "status"), so the history tells the whole story.
 * Only {@link ServiceRequest} creates these, so no change can be made without being recorded.
 */
@Entity
@Table(name = "service_request_changes")
public class RequestChange {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false, updatable = false)
    private ServiceRequest request;

    @Column(name = "changed_by", nullable = false, updatable = false)
    private UUID changedBy;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    @Column(nullable = false, updatable = false, length = 40)
    private String field;

    @Column(name = "old_value", updatable = false, length = 2000)
    private String oldValue;

    @Column(name = "new_value", updatable = false, length = 2000)
    private String newValue;

    /** Required by JPA, which creates empty objects and fills the fields itself. */
    protected RequestChange() {
    }

    RequestChange(ServiceRequest request, UUID changedBy, Instant changedAt, String field,
                  String oldValue, String newValue) {
        this.id = UUID.randomUUID();
        this.request = request;
        this.changedBy = changedBy;
        this.changedAt = changedAt;
        this.field = field;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public UUID getChangedBy() {
        return changedBy;
    }

    public Instant getChangedAt() {
        return changedAt;
    }

    public String getField() {
        return field;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }
}
