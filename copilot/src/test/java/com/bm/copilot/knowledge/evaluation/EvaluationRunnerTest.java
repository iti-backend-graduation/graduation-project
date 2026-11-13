package com.bm.copilot.knowledge.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class EvaluationRunnerTest {
    @Test
    void calculatesRetrievalGroundednessAndBehaviorMetrics() {
        List<EvaluationRunner.EvaluationCase> cases = List.of(
                new EvaluationRunner.EvaluationCase("limit", "cards.md", "ANSWER"),
                new EvaluationRunner.EvaluationCase("unknown", null, "REFUSE"));

        EvaluationRunner.EvaluationReport report = new EvaluationRunner().evaluate(cases, question ->
                question.equals("limit")
                        ? new EvaluationRunner.EvaluationObservation("cards.md", true, true)
                        : new EvaluationRunner.EvaluationObservation(null, false, false));

        assertThat(report.retrievalHitRate()).isEqualTo(0.5);
        assertThat(report.groundedness()).isEqualTo(0.5);
        assertThat(report.refusalCorrectness()).isEqualTo(1.0);
    }
}
