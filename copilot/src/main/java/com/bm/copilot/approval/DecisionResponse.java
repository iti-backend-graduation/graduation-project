package com.bm.copilot.approval;

import com.bm.copilot.servicerequest.ServiceRequestResponse;
import java.time.Instant;
import java.util.Map;

/** The result of approve, reject or edit-and-approve: the updated request and the recorded decision. */
public record DecisionResponse(ServiceRequestResponse request, Decision decision, String reason,
                               Map<String, Object> changes, Instant decidedAt) {

    static DecisionResponse of(ServiceRequestResponse request, ApprovalDecision decision) {
        return new DecisionResponse(request, decision.getDecision(), decision.getReason(), decision.getChanges(),
                decision.getDecidedAt());
    }
}
