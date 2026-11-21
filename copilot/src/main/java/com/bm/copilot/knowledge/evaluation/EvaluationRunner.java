package com.bm.copilot.knowledge.evaluation;

import java.util.List;
import java.util.Objects;

public class EvaluationRunner {
    public EvaluationReport evaluate(List<EvaluationCase> cases, CaseEvaluator evaluator) {
        Objects.requireNonNull(cases, "cases");
        Objects.requireNonNull(evaluator, "evaluator");
        if (cases.isEmpty()) {
            return new EvaluationReport(0, 0, 0, 0, 0, 0);
        }

        int retrievalHits = 0;
        int groundedAnswers = 0;
        int correctRefusals = 0;
        int expectedRefusals = 0;
        int answeredCases = 0;
        for (EvaluationCase evaluationCase : cases) {
            EvaluationObservation observation = evaluator.evaluate(evaluationCase.question());
            if (evaluationCase.expectedDocument() != null
                    && evaluationCase.expectedDocument().equals(observation.document())) {
                retrievalHits++;
            }
            if (observation.answered()) {
                answeredCases++;
                if (observation.citationsGrounded()) {
                    groundedAnswers++;
                }
            }
            if ("REFUSE".equals(evaluationCase.expectedBehavior())) {
                expectedRefusals++;
                if (!observation.answered()) {
                    correctRefusals++;
                }
            }
        }
        return new EvaluationReport(cases.size(), retrievalHits, groundedAnswers,
                correctRefusals, answeredCases, expectedRefusals);
    }

    @FunctionalInterface
    public interface CaseEvaluator {
        EvaluationObservation evaluate(String question);
    }

    public record EvaluationCase(String question, String expectedDocument, String expectedBehavior) {
    }

    public record EvaluationObservation(String document, boolean answered, boolean citationsGrounded) {
    }

    public record EvaluationReport(int cases, int retrievalHits, int groundedAnswers,
                                   int correctRefusals, int answeredCases, int expectedRefusals) {
        public double retrievalHitRate() {
            return percentage(retrievalHits, cases);
        }

        public double groundedness() {
            return answeredCases == 0 ? 0 : (double) groundedAnswers / answeredCases;
        }

        public double refusalCorrectness() {
            return expectedRefusals == 0 ? 1 : (double) correctRefusals / expectedRefusals;
        }

        private double percentage(int value, int total) {
            return total == 0 ? 0 : (double) value / total;
        }
    }
}
