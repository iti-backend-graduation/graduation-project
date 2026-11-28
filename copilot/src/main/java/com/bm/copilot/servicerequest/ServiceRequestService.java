package com.bm.copilot.servicerequest;

import com.bm.copilot.audit.AuditEventCommand;
import com.bm.copilot.audit.AuditService;
import com.bm.copilot.common.NotFoundException;
import com.bm.copilot.identity.CurrentUser;
import com.bm.copilot.identity.CurrentUserProvider;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * What an employee can do with their own service requests: create, edit and submit drafts, and read them.
 * <p>
 * Ownership rule: a request that belongs to someone else is reported as 404 Not Found, never 403,
 * so nobody can even learn that it exists. Supervisors may read any request, but edit only their own.
 */
@Service
public class ServiceRequestService {

    public static final String ENTITY_TYPE = "SERVICE_REQUEST";
    static final String NOT_FOUND = "Service request not found";

    private final ServiceRequestRepository requests;
    private final CurrentUserProvider currentUser;
    private final AuditService audit;

    public ServiceRequestService(ServiceRequestRepository requests, CurrentUserProvider currentUser,
                                 AuditService audit) {
        this.requests = requests;
        this.currentUser = currentUser;
        this.audit = audit;
    }

    @Transactional
    public ServiceRequestResponse create(CreateRequestDto dto) {
        CurrentUser user = currentUser.require();
        RequestRules.checkFields(dto.category(), dto.cardLastFour());
        ServiceRequest request = new ServiceRequest(user.id(), dto.category(), clean(dto.customerReference()),
                clean(dto.cardLastFour()), dto.description().strip());
        requests.save(request);
        audit.record(new AuditEventCommand(user.id(), "SERVICE_REQUEST_CREATED", ENTITY_TYPE,
                request.getId().toString(), Map.of("status", RequestStatus.DRAFT.name(),
                        "category", request.getCategory().name())));
        return ServiceRequestResponse.from(request);
    }

    /** Replaces the draft's fields. Only the owner can edit, and only while it is a DRAFT (409 otherwise). */
    @Transactional
    public ServiceRequestResponse update(UUID id, CreateRequestDto dto) {
        CurrentUser user = currentUser.require();
        ServiceRequest request = ownedBy(id, user);
        RequestRules.checkFields(dto.category(), dto.cardLastFour());
        List<RequestChange> made = request.edit(user.id(), dto.category(), clean(dto.customerReference()),
                clean(dto.cardLastFour()), dto.description().strip());
        if (!made.isEmpty()) {
            audit.record(new AuditEventCommand(user.id(), "SERVICE_REQUEST_UPDATED", ENTITY_TYPE, id.toString(),
                    Map.of("changedFields", made.stream().map(RequestChange::getField).toList())));
        }
        return ServiceRequestResponse.from(request);
    }

    /** DRAFT -> PENDING_APPROVAL, after checking the request is complete. */
    @Transactional
    public ServiceRequestResponse submit(UUID id) {
        CurrentUser user = currentUser.require();
        ServiceRequest request = ownedBy(id, user);
        if (request.getStatus() == RequestStatus.DRAFT) {
            RequestRules.checkComplete(request.getCategory(), request.getCustomerReference(),
                    request.getCardLastFour());
        }
        request.submit(user.id());
        audit.record(new AuditEventCommand(user.id(), "SERVICE_REQUEST_SUBMITTED", ENTITY_TYPE, id.toString(),
                Map.of("from", RequestStatus.DRAFT.name(), "to", RequestStatus.PENDING_APPROVAL.name())));
        return ServiceRequestResponse.from(request);
    }

    /** The caller's own requests, newest first. */
    @Transactional(readOnly = true)
    public List<ServiceRequestResponse> mine() {
        CurrentUser user = currentUser.require();
        return requests.findByRequesterIdOrderByCreatedAtDesc(user.id()).stream()
                .map(ServiceRequestResponse::from)
                .toList();
    }

    /** One request and its history. The owner or any supervisor; everyone else gets 404. */
    @Transactional(readOnly = true)
    public ServiceRequestDetails details(UUID id) {
        CurrentUser user = currentUser.require();
        ServiceRequest request = requests.findById(id)
                .filter(r -> r.isOwnedBy(user.id()) || user.isSupervisor())
                .orElseThrow(() -> new NotFoundException(NOT_FOUND));
        return ServiceRequestDetails.from(request);
    }

    private ServiceRequest ownedBy(UUID id, CurrentUser user) {
        return requests.findById(id)
                .filter(r -> r.isOwnedBy(user.id()))
                .orElseThrow(() -> new NotFoundException(NOT_FOUND));
    }

    private static String clean(String value) {
        return RequestRules.isBlank(value) ? null : value.strip();
    }
}
