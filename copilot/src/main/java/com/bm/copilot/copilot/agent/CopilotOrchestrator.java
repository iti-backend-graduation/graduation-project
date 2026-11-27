package com.bm.copilot.copilot.agent;

import com.bm.copilot.copilot.AskResponse;
import com.bm.copilot.copilot.CopilotService;
import com.bm.copilot.knowledge.policy.ValidationResult;
import com.bm.copilot.retrieval.RetrievedChunk;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Runs the copilot agents: retrieve, answer, check the answer against policy, and prepare a request draft
 * when the answer suggests one.
 * <ul>
 *   <li>At most {@value #MAX_ATTEMPTS} attempts; the second and third search wider. The run ends when an answer
 *       passes the policy check, the model refuses, or the model asks a clarifying question (there is no claim
 *       to check yet); after the last failed check it refuses.</li>
 *   <li>Every step has a timeout. Any failure or timeout falls back to the plain answer flow, which is still
 *       policy-checked.</li>
 *   <li>Agents only read and draft: nothing here can submit, approve or execute a service request.</li>
 * </ul>
 */
@Service
public class CopilotOrchestrator {

    static final int MAX_ATTEMPTS = 3;
    private static final int FIRST_TOP_K = 5;
    private static final int RETRY_TOP_K = 10;

    private static final Logger log = LoggerFactory.getLogger(CopilotOrchestrator.class);

    private final RetrievalAgent retrievalAgent;
    private final PolicyCheckAgent policyCheckAgent;
    private final RequestPreparationAgent requestPreparationAgent;
    private final CopilotService copilotService;
    private final Duration stepTimeout;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public CopilotOrchestrator(RetrievalAgent retrievalAgent, PolicyCheckAgent policyCheckAgent,
                               RequestPreparationAgent requestPreparationAgent, CopilotService copilotService,
                               @Value("${copilot.orchestrator.step-timeout:30s}") Duration stepTimeout) {
        this.retrievalAgent = retrievalAgent;
        this.policyCheckAgent = policyCheckAgent;
        this.requestPreparationAgent = requestPreparationAgent;
        this.copilotService = copilotService;
        this.stepTimeout = stepTimeout;
    }

    public AskResponse ask(String question) {
        try {
            return runAgents(question);
        } catch (RuntimeException e) {
            log.warn("Orchestrator failed, falling back to the plain answer flow: {}", e.toString());
            return fallback(question);
        }
    }

    private AskResponse runAgents(String question) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            int topK = attempt == 1 ? FIRST_TOP_K : RETRY_TOP_K;
            List<RetrievedChunk> evidence = step("retrieval", () -> retrievalAgent.run(new RetrievalAgent.Query(question, topK)));
            AskResponse answer = step("answer", () -> copilotService.answer(question, evidence));
            if (answer.refused() || answer.needsClarification()) {
                log.info("Orchestrator attempt {}: model refused or asked a clarifying question", attempt);
                return answer;
            }
            ValidationResult check = step("policy-check",
                    () -> policyCheckAgent.run(new PolicyCheckAgent.Input(answer.answer(), citedEvidence(answer, evidence))));
            if (check.supported()) {
                log.info("Orchestrator attempt {}: answer passed the policy check", attempt);
                return withDraftIfSuggested(question, answer, evidence);
            }
            log.info("Orchestrator attempt {}: policy check failed {} {}", attempt, check.missingInfo(), check.unsupportedClaims());
        }
        return AskResponse.refusal();
    }

    private AskResponse withDraftIfSuggested(String question, AskResponse answer, List<RetrievedChunk> evidence) {
        if (!answer.suggestServiceRequest()) {
            return answer;
        }
        RequestDraft draft = step("request-preparation",
                () -> requestPreparationAgent.run(new RequestPreparationAgent.Input(question, evidence)));
        return answer.withRequestDraft(draft);
    }

    /** Plain retrieve-and-answer, still checked against policy. No retries and no request draft. */
    private AskResponse fallback(String question) {
        List<RetrievedChunk> evidence = copilotService.retrieve(question);
        AskResponse answer = copilotService.answer(question, evidence);
        if (answer.refused() || answer.needsClarification()) {
            return answer;
        }
        ValidationResult check = policyCheckAgent.run(new PolicyCheckAgent.Input(answer.answer(), citedEvidence(answer, evidence)));
        return check.supported() ? answer : AskResponse.refusal();
    }

    private static List<RetrievedChunk> citedEvidence(AskResponse answer, List<RetrievedChunk> evidence) {
        return evidence.stream().filter(chunk -> answer.citations().contains(chunk.citation())).toList();
    }

    /** Runs one step on its own thread with a timeout, carrying the logging context (correlation ID) along. */
    private <T> T step(String name, Supplier<T> work) {
        Map<String, String> loggingContext = MDC.getCopyOfContextMap();
        long start = System.nanoTime();
        T result = CompletableFuture.supplyAsync(() -> {
                    if (loggingContext != null) {
                        MDC.setContextMap(loggingContext);
                    }
                    try {
                        return work.get();
                    } finally {
                        MDC.clear();
                    }
                }, executor)
                .orTimeout(stepTimeout.toMillis(), TimeUnit.MILLISECONDS)
                .join();
        log.info("Orchestrator step {} finished in {} ms", name, (System.nanoTime() - start) / 1_000_000);
        return result;
    }
}
