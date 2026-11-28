package com.bm.copilot.servicerequest;

import com.bm.copilot.common.ConflictException;

/** Thrown when a request is asked to make a move its current status does not allow, such as approving a draft. HTTP 409. */
public class InvalidTransitionException extends ConflictException {

    public InvalidTransitionException(String action, RequestStatus current) {
        super("Cannot " + action + " a request that is " + current);
    }
}
