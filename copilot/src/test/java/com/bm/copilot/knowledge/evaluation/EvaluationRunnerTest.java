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
                new EvaluationRunner.EvaluationCase("unknown", null, "REFUSE"));

        EvaluationRunner.EvaluationReport report = new EvaluationRunner().evaluate(cases, question ->
                switch (question) {
                    case "limit" -> new EvaluationRunner.EvaluationObservation(
                            List.of("other.md", "cards.md"), true, true);
                    case "missed" -> new EvaluationRunner.EvaluationObservation(
                            List.of("cards.md"), false, false);
                    default -> new EvaluationRunner.EvaluationObservation(List.of("accounts.md"), true, false);
                });

        assertThat(report.retrievalHitRate()).isEqualTo(0.5);
        assertThat(report.groundedness()).isEqualTo(0.5);
        assertThat(report.refusalCorrectness()).isEqualTo(0.0);
        assertThat(report.answerBehaviorCorrectness()).isEqualTo(0.5);
        assertThat(report.retrievalCases()).isEqualTo(2);
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
        assertThat(report.answerBehaviorCorrectness()).isEqualTo(1.0);
    }

    @Test
    void evaluationDatasetCoversTheRequiredCategoriesAndHasAtLeast15Cases() throws Exception {
        String json = Files.readString(Path.of("eval/cases.json"), StandardCharsets.UTF_8);
        List<DatasetCase> cases = new ObjectMapper().readValue(
                json, new TypeReference<List<DatasetCase>>() {});

        assertThat(cases).hasSizeGreaterThanOrEqualTo(15);
        assertThat(cases.stream().map(DatasetCase::category).distinct())
                .contains("retrieval", "out-of-scope", "ambiguous", "prompt-injection",
                        "incomplete-request", "unauthorized-action", "duplicate-execution",
                        "workflow", "agent-failure");
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
