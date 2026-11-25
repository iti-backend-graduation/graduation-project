package com.bm.copilot.common;

import org.slf4j.MDC;

/**
 * The correlation ID ties one request to its log lines, audit rows and AI-usage rows.
 * The audit module's filter sets it; everyone else only reads it.
 */
public final class CorrelationId {

    /** Request and response header that carries the ID. */
    public static final String HEADER = "X-Correlation-Id";

    /** Key in the logging context (MDC), so every log line can include the ID. */
    public static final String KEY = "correlationId";

    private CorrelationId() {
    }

    /** The ID of the request being handled, or null outside a request. */
    public static String current() {
        return MDC.get(KEY);
    }
}
