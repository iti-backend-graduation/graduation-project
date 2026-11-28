package com.bm.copilot.servicerequest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** GET /api/requests/{id}: the request plus its full history, oldest first. */
public record ServiceRequestDetails(ServiceRequestResponse request, List<HistoryEntry> history) {

    /** One change: who changed which field (or the status), from what, to what, and when. */
    public record HistoryEntry(Instant changedAt, UUID changedBy, String field, String oldValue, String newValue) {
    }

    static ServiceRequestDetails from(ServiceRequest request) {
        List<HistoryEntry> history = request.getChanges().stream()
                .map(c -> new HistoryEntry(c.getChangedAt(), c.getChangedBy(), c.getField(),
                        c.getOldValue(), c.getNewValue()))
                .toList();
        return new ServiceRequestDetails(ServiceRequestResponse.from(request), history);
    }
}
