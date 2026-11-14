package com.bm.copilot.audit;

/**
 * One model call made while handling a request.
 *
 * @param step             which part of the workflow made the call, e.g. RETRIEVAL_AGENT, ANSWER_GENERATION
 * @param model            model name as reported by the provider
 * @param promptTokens     tokens sent to the model
 * @param completionTokens tokens the model returned
 */
public record AiUsageCommand(
        String step,
        String model,
        int promptTokens,
        int completionTokens) {
}