package com.bm.copilot.servicerequest;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A simulated service request and its state machine.
 * <p>
 * Every status move is a method that throws {@link InvalidTransitionException} when the current status does not
 * allow it, so no service can skip a step by accident (for example execute a request nobody approved).
 * Every change, including each status move, is added to {@link #getChanges() the history}.
 * {@code @Version} makes two simultaneous updates of the same request impossible: the second one fails.
 */
@Entity
@Table(name = "service_requests")
public class ServiceRequest {

    static final String FIELD_STATUS = "status";
    static final String FIELD_CATEGORY = "category";
    static final String FIELD_CUSTOMER_REFERENCE = "customerReference";
    static final String FIELD_CARD_LAST_FOUR = "cardLastFour";
    static final String FIELD_DESCRIPTION = "description";

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RequestCategory category;

    @Column(name = "requester_id", nullable = false, updatable = false)
    private UUID requesterId;

    @Column(name = "customer_reference", length = 40)
    private String customerReference;

    @Column(name = "card_last_four", length = 4)
    private String cardLastFour;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Version
    private Long version;

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("changedAt ASC, field ASC")
    private List<RequestChange> changes = new ArrayList<>();

    /** Required by JPA, which creates empty objects and fills the fields itself. */
    protected ServiceRequest() {
    }

    /** A new request always starts as a DRAFT owned by the employee who created it. */
    ServiceRequest(UUID requesterId, RequestCategory category, String customerReference, String cardLastFour,
                   String description) {
        Instant now = Instant.now();
        this.id = UUID.randomUUID();
        this.requesterId = Objects.requireNonNull(requesterId, "requesterId");
        this.category = Objects.requireNonNull(category, "category");
        this.customerReference = customerReference;
        this.cardLastFour = cardLastFour;
        this.description = Objects.requireNonNull(description, "description");
        this.status = RequestStatus.DRAFT;
        this.createdAt = now;
        this.updatedAt = now;
        record(requesterId, now, FIELD_STATUS, null, RequestStatus.DRAFT.name());
    }

    // ---- state machine -------------------------------------------------------------------------------------

    /** The owner edits the request. Only a DRAFT can be edited. Returns the changes made (empty if nothing changed). */
    List<RequestChange> edit(UUID actorId, RequestCategory newCategory, String newCustomerReference,
                             String newCardLastFour, String newDescription) {
        requireStatus(RequestStatus.DRAFT, "edit");
        return applyFields(actorId, Instant.now(), newCategory, newCustomerReference, newCardLastFour, newDescription);
    }

    /** DRAFT -> PENDING_APPROVAL. */
    void submit(UUID actorId) {
        requireStatus(RequestStatus.DRAFT, "submit");
        Instant now = Instant.now();
        this.submittedAt = now;
        moveTo(RequestStatus.PENDING_APPROVAL, actorId, now);
    }

    /** PENDING_APPROVAL -> APPROVED. */
    public void approve(UUID supervisorId) {
        requireStatus(RequestStatus.PENDING_APPROVAL, "approve");
        moveTo(RequestStatus.APPROVED, supervisorId, Instant.now());
    }

    /**
     * PENDING_APPROVAL -> APPROVED, after the supervisor changed some fields. A null argument keeps the current value.
     * Returns the field changes made (not counting the status move).
     */
    public List<RequestChange> editAndApprove(UUID supervisorId, RequestCategory newCategory,
                                              String newCustomerReference, String newCardLastFour,
                                              String newDescription) {
        requireStatus(RequestStatus.PENDING_APPROVAL, "edit and approve");
        Instant now = Instant.now();
        List<RequestChange> made = applyFields(supervisorId, now,
                newCategory == null ? category : newCategory,
                newCustomerReference == null ? customerReference : newCustomerReference,
                newCardLastFour == null ? cardLastFour : newCardLastFour,
                newDescription == null ? description : newDescription);
        moveTo(RequestStatus.APPROVED, supervisorId, now);
        return made;
    }

    /** PENDING_APPROVAL -> REJECTED. A reason is required, so the employee knows what to fix. */
    public void reject(UUID supervisorId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A rejection reason is required");
        }
        requireStatus(RequestStatus.PENDING_APPROVAL, "reject");
        this.rejectionReason = reason.strip();
        moveTo(RequestStatus.REJECTED, supervisorId, Instant.now());
    }

    /** APPROVED -> EXECUTED. Only an approved request can run, and only once: EXECUTED cannot execute again. */
    public void markExecuted(UUID actorId) {
        requireStatus(RequestStatus.APPROVED, "execute");
        moveTo(RequestStatus.EXECUTED, actorId, Instant.now());
    }

    // ---- helpers -------------------------------------------------------------------------------------------

    private void requireStatus(RequestStatus expected, String action) {
        if (status != expected) {
            throw new InvalidTransitionException(action, status);
        }
    }

    private void moveTo(RequestStatus next, UUID actorId, Instant at) {
        record(actorId, at, FIELD_STATUS, status.name(), next.name());
        this.status = next;
        this.updatedAt = at;
    }

    private List<RequestChange> applyFields(UUID actorId, Instant at, RequestCategory newCategory,
                                            String newCustomerReference, String newCardLastFour,
                                            String newDescription) {
        Objects.requireNonNull(newCategory, "category");
        Objects.requireNonNull(newDescription, "description");
        List<RequestChange> made = new ArrayList<>();
        if (newCategory != category) {
            made.add(record(actorId, at, FIELD_CATEGORY, category.name(), newCategory.name()));
            this.category = newCategory;
        }
        if (!Objects.equals(newCustomerReference, customerReference)) {
            made.add(record(actorId, at, FIELD_CUSTOMER_REFERENCE, customerReference, newCustomerReference));
            this.customerReference = newCustomerReference;
        }
        if (!Objects.equals(newCardLastFour, cardLastFour)) {
            made.add(record(actorId, at, FIELD_CARD_LAST_FOUR, cardLastFour, newCardLastFour));
            this.cardLastFour = newCardLastFour;
        }
        if (!newDescription.equals(description)) {
            made.add(record(actorId, at, FIELD_DESCRIPTION, description, newDescription));
            this.description = newDescription;
        }
        if (!made.isEmpty()) {
            this.updatedAt = at;
        }
        return made;
    }

    private RequestChange record(UUID actorId, Instant at, String field, String oldValue, String newValue) {
        RequestChange change = new RequestChange(this, actorId, at, field, oldValue, newValue);
        changes.add(change);
        return change;
    }

    // ---- getters -------------------------------------------------------------------------------------------

    public UUID getId() {
        return id;
    }

    public RequestCategory getCategory() {
        return category;
    }

    public UUID getRequesterId() {
        return requesterId;
    }

    public boolean isOwnedBy(UUID userId) {
        return requesterId.equals(userId);
    }

    public String getCustomerReference() {
        return customerReference;
    }

    public String getCardLastFour() {
        return cardLastFour;
    }

    public String getDescription() {
        return description;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    /** The history, oldest first. Read-only: changes are only added through the state machine methods. */
    public List<RequestChange> getChanges() {
        return Collections.unmodifiableList(changes);
    }
}
