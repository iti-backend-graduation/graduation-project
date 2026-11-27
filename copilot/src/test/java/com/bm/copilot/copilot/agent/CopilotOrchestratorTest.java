package com.bm.copilot.copilot.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bm.copilot.copilot.AskResponse;
import com.bm.copilot.copilot.CopilotService;
import com.bm.copilot.knowledge.policy.ValidationResult;
import com.bm.copilot.retrieval.Citation;
import com.bm.copilot.retrieval.RetrievedChunk;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;

class CopilotOrchestratorTest {

    private static final Citation FEES = new Citation("16-synthetic-fee-schedule.pdf", 2, "Card fees");
    private static final RetrievedChunk FEE_CHUNK =
            new RetrievedChunk(UUID.randomUUID(), "The card replacement fee is 100 EGP.", FEES, 0.03);
    private static final AskResponse FEE_ANSWER =
            new AskResponse("The fee is 100 EGP.", List.of(FEES), false, false, null, null);

    private final RetrievalAgent retrievalAgent = mock(RetrievalAgent.class);
    private final PolicyCheckAgent policyCheckAgent = mock(PolicyCheckAgent.class);
    private final RequestPreparationAgent requestPreparationAgent = mock(RequestPreparationAgent.class);
    private final CopilotService copilotService = mock(CopilotService.class);
    private final CopilotOrchestrator orchestrator = new CopilotOrchestrator(retrievalAgent, policyCheckAgent,
            requestPreparationAgent, copilotService, Duration.ofMillis(300));

    @AfterEach
    void clearLoggingContext() {
        MDC.clear();
    }

    @Test
    void returnsASupportedAnswerAfterOneAttempt() {
        when(retrievalAgent.run(any())).thenReturn(List.of(FEE_CHUNK));
        when(copilotService.answer(anyString(), anyList())).thenReturn(FEE_ANSWER);
        when(policyCheckAgent.run(any())).thenReturn(supported());

        AskResponse response = orchestrator.ask("How much is a card replacement?");

        assertThat(response).isEqualTo(FEE_ANSWER);
        verify(copilotService, times(1)).answer(anyString(), anyList());
        verify(requestPreparationAgent, never()).run(any());
    }

    @Test
    void stopsAfterThreeFailedPolicyChecksAndRefuses() {
        when(retrievalAgent.run(any())).thenReturn(List.of(FEE_CHUNK));
        when(copilotService.answer(anyString(), anyList())).thenReturn(FEE_ANSWER);
        when(policyCheckAgent.run(any())).thenReturn(unsupported());

        AskResponse response = orchestrator.ask("How much is a card replacement?");

        assertThat(response.refused()).isTrue();
        verify(copilotService, times(3)).answer(anyString(), anyList());
        ArgumentCaptor<RetrievalAgent.Query> queries = ArgumentCaptor.forClass(RetrievalAgent.Query.class);
        verify(retrievalAgent, times(3)).run(queries.capture());
        assertThat(queries.getAllValues()).extracting(RetrievalAgent.Query::topK).containsExactly(5, 10, 10);
    }

    @Test
    void modelRefusalEndsTheRunWithoutAPolicyCheck() {
        when(retrievalAgent.run(any())).thenReturn(List.of(FEE_CHUNK));
        when(copilotService.answer(anyString(), anyList())).thenReturn(AskResponse.refusal());

        AskResponse response = orchestrator.ask("What is the mortgage rate?");

        assertThat(response.refused()).isTrue();
        verify(policyCheckAgent, never()).run(any());
        verify(copilotService, times(1)).answer(anyString(), anyList());
    }

    @Test
    void clarifyingQuestionEndsTheRunWithoutAPolicyCheckOrDraft() {
        AskResponse clarification = AskResponse.clarification("Which card is it, debit or credit?");
        when(retrievalAgent.run(any())).thenReturn(List.of(FEE_CHUNK));
        when(copilotService.answer(anyString(), anyList())).thenReturn(clarification);

        AskResponse response = orchestrator.ask("What is the card limit?");

        assertThat(response).isEqualTo(clarification);
        verify(copilotService, times(1)).answer(anyString(), anyList());
        verify(policyCheckAgent, never()).run(any());
        verify(requestPreparationAgent, never()).run(any());
    }

    @Test
    void failingAgentFallsBackToThePlainFlow() {
        when(retrievalAgent.run(any())).thenThrow(new IllegalStateException("database down"));
        when(copilotService.retrieve(anyString())).thenReturn(List.of(FEE_CHUNK));
        when(copilotService.answer(anyString(), anyList())).thenReturn(FEE_ANSWER);
        when(policyCheckAgent.run(any())).thenReturn(supported());

        AskResponse response = orchestrator.ask("How much is a card replacement?");

        assertThat(response).isEqualTo(FEE_ANSWER);
        verify(copilotService).retrieve("How much is a card replacement?");
    }

    @Test
    void stepRunningPastItsTimeoutFallsBackInTime() {
        when(retrievalAgent.run(any())).thenReturn(List.of(FEE_CHUNK));
        AtomicInteger answerCalls = new AtomicInteger();
        when(copilotService.answer(anyString(), anyList())).thenAnswer(invocation -> {
            if (answerCalls.incrementAndGet() == 1) {
                Thread.sleep(5_000);
            }
            return FEE_ANSWER;
        });
        when(copilotService.retrieve(anyString())).thenReturn(List.of(FEE_CHUNK));
        when(policyCheckAgent.run(any())).thenReturn(supported());

        long start = System.nanoTime();
        AskResponse response = orchestrator.ask("How much is a card replacement?");
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(response).isEqualTo(FEE_ANSWER);
        assertThat(elapsedMs).isLessThan(2_000);
    }

    @Test
    void fallbackAnswerIsStillPolicyChecked() {
        when(retrievalAgent.run(any())).thenThrow(new IllegalStateException("database down"));
        when(copilotService.retrieve(anyString())).thenReturn(List.of(FEE_CHUNK));
        when(copilotService.answer(anyString(), anyList())).thenReturn(FEE_ANSWER);
        when(policyCheckAgent.run(any())).thenReturn(unsupported());

        AskResponse response = orchestrator.ask("How much is a card replacement?");

        assertThat(response.refused()).isTrue();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("agentFailureCases")
    void evaluationDatasetAgentFailureCasesUseTheDocumentedFallback(AgentFailureCase evaluationCase) {
        assertThat(evaluationCase.expectedBehavior()).isEqualTo("FALLBACK_ANSWER");
        when(copilotService.retrieve(anyString())).thenReturn(List.of(FEE_CHUNK));
        when(copilotService.answer(anyString(), anyList())).thenReturn(FEE_ANSWER);

        if (evaluationCase.id().equals("agent-failure-01")) {
            when(retrievalAgent.run(any())).thenThrow(new IllegalStateException("retrieval unavailable"));
            when(policyCheckAgent.run(any())).thenReturn(supported());
        } else {
            when(retrievalAgent.run(any())).thenReturn(List.of(FEE_CHUNK));
            AtomicInteger policyCalls = new AtomicInteger();
            when(policyCheckAgent.run(any())).thenAnswer(invocation -> {
                if (policyCalls.incrementAndGet() == 1) {
                    Thread.sleep(1_000);
                    return unsupported();
                }
                return supported();
            });
        }

        AskResponse response = orchestrator.ask(evaluationCase.question());

        assertThat(response).isEqualTo(FEE_ANSWER);
        verify(copilotService).retrieve(evaluationCase.question());
    }

    static Stream<AgentFailureCase> agentFailureCases() throws Exception {
        List<AgentFailureCase> cases = new ObjectMapper().readValue(
                Files.readString(Path.of("eval/cases.json"), StandardCharsets.UTF_8),
                new TypeReference<>() {});
        return cases.stream().filter(evaluationCase -> "agent-failure".equals(evaluationCase.category()));
    }

    @Test
    void attachesARequestDraftOnlyWhenTheAnswerSuggestsARequest() {
        AskResponse suggestsRequest = new AskResponse("Raise a card replacement request.", List.of(FEES), false, true, null, null);
        RequestDraft draft = new RequestDraft("CARD_SERVICE", "Card replacement", List.of("customerReference"));
        when(retrievalAgent.run(any())).thenReturn(List.of(FEE_CHUNK));
        when(copilotService.answer(anyString(), anyList())).thenReturn(suggestsRequest);
        when(policyCheckAgent.run(any())).thenReturn(supported());
        when(requestPreparationAgent.run(any())).thenReturn(draft);

        AskResponse response = orchestrator.ask("The customer lost their card");

        assertThat(response.requestDraft()).isEqualTo(draft);
        assertThat(response.answer()).isEqualTo(suggestsRequest.answer());
    }

    @Test
    void correlationIdIsVisibleInsideSteps() {
        MDC.put("correlationId", "abc-123");
        AtomicReference<String> seenInRetrieval = new AtomicReference<>();
        when(retrievalAgent.run(any())).thenAnswer(invocation -> {
            seenInRetrieval.set(MDC.get("correlationId"));
            return List.of(FEE_CHUNK);
        });
        when(copilotService.answer(anyString(), anyList())).thenReturn(FEE_ANSWER);
        when(policyCheckAgent.run(any())).thenReturn(supported());

        orchestrator.ask("How much is a card replacement?");

        assertThat(seenInRetrieval).hasValue("abc-123");
    }

    private static ValidationResult supported() {
        return new ValidationResult(true, List.of(), List.of());
    }

    private static ValidationResult unsupported() {
        return new ValidationResult(false, List.of(), List.of("The draft contains claims not supported by the retrieved evidence"));
    }

    private record AgentFailureCase(
            String id,
            String category,
            String question,
            String expectedDocument,
            Integer expectedPage,
            String expectedBehavior) {
        @Override
        public String toString() {
            return id;
        }
    }
}
