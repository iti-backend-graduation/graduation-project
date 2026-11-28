package com.bm.copilot.approval;

import com.bm.copilot.audit.AuditEventCommand;
import com.bm.copilot.audit.AuditService;
import com.bm.copilot.common.NotFoundException;
import com.bm.copilot.identity.CurrentUser;
import com.bm.copilot.identity.CurrentUserProvider;
import com.bm.copilot.servicerequest.RequestStatus;
import com.bm.copilot.servicerequest.ServiceRequest;
import com.bm.copilot.servicerequest.ServiceRequestRepository;
import com.bm.copilot.servicerequest.ServiceRequestService;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Simulated execution. Nothing real happens: an {@link ExecutionOutcome} row is written and the request becomes
 * EXECUTED. Duplicate execution is stopped three times over:
 * <ol>
 *   <li>the state machine: only APPROVED can execute, and EXECUTED cannot execute again (409);</li>
 *   <li>{@code @Version}: of two simultaneous calls, the second one's update fails (409);</li>
 *   <li>the database: execution_outcomes.request_id is unique.</li>
 * </ol>
 */
@Service
public class ExecutionService {

    private final ServiceRequestRepository requests;
    private final ExecutionOutcomeRepository outcomes;
    private final CurrentUserProvider currentUser;
    private final AuditService audit;

    public ExecutionService(ServiceRequestRepository requests, ExecutionOutcomeRepository outcomes,
                            CurrentUserProvider currentUser, AuditService audit) {
        this.requests = requests;
        this.outcomes = outcomes;
        this.currentUser = currentUser;
        this.audit = audit;
    }

    @Transactional
    public ExecutionResponse execute(UUID id) {
        CurrentUser actor = currentUser.require();
        ServiceRequest request = requests.findById(id)
                .orElseThrow(() -> new NotFoundException("Service request not found"));

        request.markExecuted(actor.id());
        // Write the status change now, before the outcome: a concurrent execution blocks here and then fails the
        // @Version check, instead of reaching the insert.
        requests.saveAndFlush(request);

        ExecutionOutcome outcome = outcomes.save(new ExecutionOutcome(request.getId(), actor.id(),
                simulatedResult(request)));
        audit.record(new AuditEventCommand(actor.id(), "SERVICE_REQUEST_EXECUTED", ServiceRequestService.ENTITY_TYPE,
                id.toString(), Map.of("from", RequestStatus.APPROVED.name(), "to", RequestStatus.EXECUTED.name(),
                        "outcome", outcome.getMessage())));
        return ExecutionResponse.of(request.getStatus(), outcome);
    }

    /** What the simulated back office "did". No real system is called. */
    static String simulatedResult(ServiceRequest request) {
        String customer = " for customer " + request.getCustomerReference();
        return switch (request.getCategory()) {
            case CARD_SERVICE -> "Simulated: card service request logged for the card ending "
                    + request.getCardLastFour() + customer;
            case DIGITAL_BANKING -> "Simulated: digital banking request logged" + customer;
            case ACCOUNT_SERVICE -> "Simulated: account service request logged" + customer;
            case ADDITIONAL_DOCUMENTS -> "Simulated: additional documents recorded" + customer;
        };
    }
}
