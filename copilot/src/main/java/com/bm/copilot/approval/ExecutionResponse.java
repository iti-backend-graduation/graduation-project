package com.bm.copilot.approval;

import com.bm.copilot.servicerequest.RequestStatus;
import java.time.Instant;
import java.util.UUID;

/** The result of a simulated execution. */
public record ExecutionResponse(UUID requestId, RequestStatus status, String message, UUID executedBy,
                                Instant executedAt) {

    static ExecutionResponse of(RequestStatus status, ExecutionOutcome outcome) {
        return new ExecutionResponse(outcome.getRequestId(), status, outcome.getMessage(), outcome.getExecutedBy(),
                outcome.getExecutedAt());
    }
}
