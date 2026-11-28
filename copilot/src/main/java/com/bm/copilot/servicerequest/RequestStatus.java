package com.bm.copilot.servicerequest;

/**
 * Where a service request is in its life. The allowed moves live in {@link ServiceRequest}:
 * <pre>
 * DRAFT -> PENDING_APPROVAL -> APPROVED -> EXECUTED
 *                           \-> REJECTED
 * </pre>
 */
public enum RequestStatus {
    DRAFT,
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    EXECUTED
}
