package com.bm.copilot.audit;

/** Public entry point for recording AI token usage and estimated cost. */
public interface AiUsageService {

    /**
     * Stores one model call. Runs in its own transaction: the model call already happened and
     * was paid for, so the usage must be kept even if the surrounding request later fails.
     */
    void record(AiUsageCommand usage);
}