package com.bm.copilot.servicerequest;

import java.time.Instant;
import java.util.UUID;

/** A service request as the API returns it. */
public record ServiceRequestResponse(
        UUID id,
        RequestCategory category,
        RequestStatus status,
        UUID requesterId,
        String customerReference,
        String cardLastFour,
        String description,
        String rejectionReason,
        Instant createdAt,
        Instant updatedAt,
        Instant submittedAt) {

    public static ServiceRequestResponse from(ServiceRequest request) {
        return new ServiceRequestResponse(request.getId(), request.getCategory(), request.getStatus(),
                request.getRequesterId(), request.getCustomerReference(), request.getCardLastFour(),
                request.getDescription(), request.getRejectionReason(), request.getCreatedAt(),
                request.getUpdatedAt(), request.getSubmittedAt());
    }
}
