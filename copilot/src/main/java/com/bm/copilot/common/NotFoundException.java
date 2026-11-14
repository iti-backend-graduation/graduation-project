package com.bm.copilot.common;

/** Thrown when a requested record does not exist (or the caller must not know it exists). Maps to HTTP 404. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}