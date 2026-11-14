package com.bm.copilot.common;

import java.time.Instant;
import java.util.List;

/**
 * The single error format returned by every endpoint.
 * <p>
 * {@code code} is a stable machine-readable value (for example VALIDATION_FAILED)
 * that the frontend can switch on; {@code message} is for humans.
 * {@code correlationId} is filled in Week 2, when the tracing filter exists.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        String correlationId,
        List<FieldViolation> violations) {

    /** One invalid input field, used for validation errors. */
    public record FieldViolation(String field, String message) {}

    public static ApiError of(int status, String code, String message, String path) {
        return new ApiError(Instant.now(), status, code, message, path, null, List.of());
    }

    public static ApiError of(int status, String code, String message, String path,
                              List<FieldViolation> violations) {
        return new ApiError(Instant.now(), status, code, message, path, null, violations);
    }
}