package com.bm.copilot.common;

/** Thrown when an action conflicts with the current state, such as approving an already-approved request. Maps to HTTP 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}