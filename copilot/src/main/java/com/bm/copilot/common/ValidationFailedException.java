package com.bm.copilot.common;

import java.util.List;

/**
 * Thrown when input breaks a business rule that annotations cannot express, such as "a card request needs the
 * last 4 card digits". Maps to HTTP 400 VALIDATION_FAILED with the same shape as {@code @Valid} failures.
 */
public class ValidationFailedException extends RuntimeException {

    private final List<ApiError.FieldViolation> violations;

    public ValidationFailedException(List<ApiError.FieldViolation> violations) {
        super("Request validation failed");
        this.violations = List.copyOf(violations);
    }

    public List<ApiError.FieldViolation> violations() {
        return violations;
    }
}
