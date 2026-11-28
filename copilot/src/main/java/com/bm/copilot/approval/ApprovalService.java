package com.bm.copilot.approval;

import com.bm.copilot.audit.AuditEventCommand;
import com.bm.copilot.audit.AuditService;
import com.bm.copilot.common.ApiError.FieldViolation;
import com.bm.copilot.common.NotFoundException;
import com.bm.copilot.common.ValidationFailedException;
import com.bm.copilot.identity.CurrentUser;
import com.bm.copilot.identity.CurrentUserProvider;
import com.bm.copilot.servicerequest.RequestCategory;
import com.bm.copilot.servicerequest.RequestChange;
import com.bm.copilot.servicerequest.RequestRules;
import com.bm.copilot.servicerequest.RequestStatus;
import com.bm.copilot.servicerequest.ServiceRequest;
import com.bm.copilot.servicerequest.ServiceRequestRepository;
import com.bm.copilot.servicerequest.ServiceRequestResponse;
import com.bm.copilot.servicerequest.ServiceRequestService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Supervisor review: the queue, approve, reject with a reason, and edit-and-approve.
 * <ul>
 *   <li>Only a PENDING_APPROVAL request can be decided (the entity's state machine refuses anything else: 409).</li>
 *   <li>Four-eyes rule: a supervisor cannot decide on a request they created themselves (403).</li>
 *   <li>Every decision is stored as an {@link ApprovalDecision} and written to the audit trail.</li>
 * </ul>
 * Role checks are on the controller and the URL rule; this class adds the rules that depend on the data.
 */
@Service
public class ApprovalService {

    private final ServiceRequestRepository requests;
    private final ApprovalDecisionRepository decisions;
    private final CurrentUserProvider currentUser;
    private final AuditService audit;

    public ApprovalService(ServiceRequestRepository requests, ApprovalDecisionRepository decisions,
                           CurrentUserProvider currentUser, AuditService audit) {
        this.requests = requests;
        this.decisions = decisions;
        this.currentUser = currentUser;
        this.audit = audit;
    }

    /** Every request waiting for a decision, oldest submission first. */
    @Transactional(readOnly = true)
    public List<ServiceRequestResponse> queue() {
        return requests.findByStatusOrderBySubmittedAtAsc(RequestStatus.PENDING_APPROVAL).stream()
                .map(ServiceRequestResponse::from)
                .toList();
    }

    @Transactional
    public DecisionResponse approve(UUID id) {
        CurrentUser supervisor = currentUser.require();
        ServiceRequest request = decidable(id, supervisor);
        request.approve(supervisor.id());
        return decided(request, supervisor, Decision.APPROVED, null, Map.of(), "SERVICE_REQUEST_APPROVED",
                Map.of());
    }

    @Transactional
    public DecisionResponse reject(UUID id, String reason) {
        CurrentUser supervisor = currentUser.require();
        ServiceRequest request = decidable(id, supervisor);
        String cleanReason = reason.strip();
        request.reject(supervisor.id(), cleanReason);
        return decided(request, supervisor, Decision.REJECTED, cleanReason, Map.of(), "SERVICE_REQUEST_REJECTED",
                Map.of("reason", cleanReason));
    }

    /** Applies the supervisor's changes, checks the result is still a complete request, then approves it. */
    @Transactional
    public DecisionResponse editAndApprove(UUID id, EditAndApproveDto dto) {
        CurrentUser supervisor = currentUser.require();
        ServiceRequest request = decidable(id, supervisor);

        RequestCategory category = dto.category() == null ? request.getCategory() : dto.category();
        String customerReference = dto.customerReference() == null
                ? request.getCustomerReference() : dto.customerReference().strip();
        String cardLastFour = dto.cardLastFour() == null ? request.getCardLastFour() : dto.cardLastFour();
        String description = dto.description() == null ? request.getDescription() : dto.description().strip();

        if (request.getStatus() == RequestStatus.PENDING_APPROVAL) {
            boolean changed = category != request.getCategory()
                    || !Objects.equals(customerReference, request.getCustomerReference())
                    || !Objects.equals(cardLastFour, request.getCardLastFour())
                    || !description.equals(request.getDescription());
            if (!changed) {
                throw new ValidationFailedException(List.of(new FieldViolation("changes",
                        "no field was changed; use approve instead")));
            }
            RequestRules.checkComplete(category, customerReference, cardLastFour);
        }

        List<RequestChange> made = request.editAndApprove(supervisor.id(), category, customerReference,
                cardLastFour, description);
        Map<String, Object> changes = new LinkedHashMap<>();
        for (RequestChange change : made) {
            Map<String, Object> fromTo = new LinkedHashMap<>();
            fromTo.put("from", change.getOldValue());
            fromTo.put("to", change.getNewValue());
            changes.put(change.getField(), fromTo);
        }
        return decided(request, supervisor, Decision.EDITED_AND_APPROVED, null, changes,
                "SERVICE_REQUEST_EDITED_AND_APPROVED", Map.of("changedFields", List.copyOf(changes.keySet())));
    }

    /** Loads a request a supervisor may decide on: it exists, and it is not their own. */
    private ServiceRequest decidable(UUID id, CurrentUser supervisor) {
        ServiceRequest request = requests.findById(id)
                .orElseThrow(() -> new NotFoundException("Service request not found"));
        if (request.isOwnedBy(supervisor.id())) {
            throw new AccessDeniedException("A supervisor cannot decide on their own request");
        }
        return request;
    }

    private DecisionResponse decided(ServiceRequest request, CurrentUser supervisor, Decision decision,
                                     String reason, Map<String, Object> changes, String auditAction,
                                     Map<String, Object> auditExtra) {
        ApprovalDecision saved = decisions.save(
                new ApprovalDecision(request.getId(), supervisor.id(), decision, reason, changes));
        Map<String, Object> details = new LinkedHashMap<>(auditExtra);
        details.put("from", RequestStatus.PENDING_APPROVAL.name());
        details.put("to", request.getStatus().name());
        details.put("decision", decision.name());
        audit.record(new AuditEventCommand(supervisor.id(), auditAction, ServiceRequestService.ENTITY_TYPE,
                request.getId().toString(), details));
        return DecisionResponse.of(ServiceRequestResponse.from(request), saved);
    }
}
