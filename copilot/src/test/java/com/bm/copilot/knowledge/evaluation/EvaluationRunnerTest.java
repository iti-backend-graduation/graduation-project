package com.bm.copilot.knowledge.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class EvaluationRunnerTest {
    @Test
    void calculatesRetrievalGroundednessAndBehaviorMetrics() {
        List<EvaluationRunner.EvaluationCase> cases = List.of(
                new EvaluationRunner.EvaluationCase("limit", "cards.md", "ANSWER"),
                new EvaluationRunner.EvaluationCase("missed", "accounts.md", "ANSWER"),
                new EvaluationRunner.EvaluationCase("unknown", null, "REFUSE"),
                new EvaluationRunner.EvaluationCase("ambiguous", null, "CLARIFY"));

        EvaluationRunner.EvaluationReport report = new EvaluationRunner().evaluate(cases, question ->
                switch (question) {
                    case "limit" -> new EvaluationRunner.EvaluationObservation(
                            List.of("other.md", "cards.md"), EvaluationRunner.ResponseOutcome.ANSWER, true);
                    case "missed" -> new EvaluationRunner.EvaluationObservation(
                            List.of("cards.md"), EvaluationRunner.ResponseOutcome.REFUSE, false);
                    case "unknown" -> new EvaluationRunner.EvaluationObservation(
                            List.of("accounts.md"), EvaluationRunner.ResponseOutcome.CLARIFY, false);
                    default -> new EvaluationRunner.EvaluationObservation(
                            List.of(), EvaluationRunner.ResponseOutcome.CLARIFY, false);
                });

        assertThat(report.retrievalHitRate()).isEqualTo(0.5);
        assertThat(report.groundedness()).isEqualTo(1.0);
        assertThat(report.refusalCorrectness()).isEqualTo(0.0);
        assertThat(report.clarificationCorrectness()).isEqualTo(1.0);
        assertThat(report.expectedRefusals()).isEqualTo(1);
        assertThat(report.correctRefusals()).isZero();
        assertThat(report.expectedClarifications()).isEqualTo(1);
        assertThat(report.answerBehaviorCorrectness()).isEqualTo(0.5);
        assertThat(report.retrievalCases()).isEqualTo(2);
    }

    @Test
    void clarificationDoesNotCountAsACorrectRefusal() {
        List<EvaluationRunner.EvaluationCase> cases = List.of(
                new EvaluationRunner.EvaluationCase("out-of-scope", null, "REFUSE"),
                new EvaluationRunner.EvaluationCase("ambiguous", null, "CLARIFY"));
        EvaluationRunner.EvaluationReport report = new EvaluationRunner().evaluate(cases, question ->
                new EvaluationRunner.EvaluationObservation(List.of(),
                        "ambiguous".equals(question)
                                ? EvaluationRunner.ResponseOutcome.CLARIFY
                                : EvaluationRunner.ResponseOutcome.ANSWER,
                        false));

        assertThat(report.refusalCorrectness()).isEqualTo(0.0);
        assertThat(report.correctRefusals()).isZero();
        assertThat(report.clarificationCorrectness()).isEqualTo(1.0);
        assertThat(report.expectedClarifications()).isEqualTo(1);
    }

    @Test
    void emptyDatasetProducesZeroMetrics() {
        EvaluationRunner.EvaluationReport report = new EvaluationRunner().evaluate(List.of(), question -> {
            throw new AssertionError("The evaluator must not run for an empty dataset");
        });

        assertThat(report.cases()).isZero();
        assertThat(report.retrievalHitRate()).isZero();
        assertThat(report.groundedness()).isZero();
        assertThat(report.refusalCorrectness()).isEqualTo(1.0);
        assertThat(report.clarificationCorrectness()).isEqualTo(1.0);
        assertThat(report.answerBehaviorCorrectness()).isEqualTo(1.0);
    }

    @Test
    void evaluationDatasetCoversTheRequiredCategoriesAndHasAtLeast15Cases() throws Exception {
        String json = Files.readString(Path.of("eval/cases.json"), StandardCharsets.UTF_8);
        List<DatasetCase> cases = new ObjectMapper().readValue(
                json, new TypeReference<>() {});

        assertThat(cases).hasSizeGreaterThanOrEqualTo(15);
        assertThat(cases.stream().map(DatasetCase::category).distinct())
                .contains("retrieval", "out-of-scope", "ambiguous", "prompt-injection",
                        "incomplete-request", "unauthorized-action", "duplicate-execution",
                        "workflow", "agent-failure");
        assertThat(cases.stream().map(DatasetCase::expectedBehavior).distinct())
                .contains("ANSWER", "REFUSE", "CLARIFY", "FALLBACK_ANSWER");
        assertThat(cases.stream().filter(testCase -> testCase.expectedBehavior().equals("ANSWER")))
                .hasSize(12);
        assertThat(cases.stream().filter(testCase -> testCase.expectedBehavior().equals("REFUSE")))
                .hasSize(10);
        assertThat(cases.stream().filter(testCase -> testCase.expectedBehavior().equals("CLARIFY")))
                .hasSize(1);
        assertThat(cases.stream().filter(testCase -> testCase.expectedBehavior().equals("FALLBACK_ANSWER")))
                .hasSize(2);
        assertThat(cases).filteredOn(testCase -> testCase.id().equals("ambiguous-01"))
                .extracting(DatasetCase::expectedBehavior).containsExactly("CLARIFY");
        assertThat(cases).filteredOn(testCase -> testCase.id().equals("account-01"))
                .extracting(DatasetCase::expectedDocument)
                .containsExactly("11-required-documents-checklist.md");
    }

    private record DatasetCase(
            String id,
            String category,
            String question,
            String expectedDocument,
            Integer expectedPage,
            String expectedBehavior) {
    }
}
