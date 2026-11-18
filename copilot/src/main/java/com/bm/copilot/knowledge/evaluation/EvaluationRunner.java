package com.bm.copilot.knowledge.evaluation;

import java.util.List;
import java.util.Objects;

public class EvaluationRunner {
    public EvaluationReport evaluate(List<EvaluationCase> cases, CaseEvaluator evaluator) {
        Objects.requireNonNull(cases, "cases");
        Objects.requireNonNull(evaluator, "evaluator");
        if (cases.isEmpty()) {
            return new EvaluationReport(0, 0, 0, 0);
        }

        int retrievalHits = 0;
        int groundedAnswers = 0;
        int correctRefusals = 0;
        for (EvaluationCase evaluationCase : cases) {
            EvaluationObservation observation = evaluator.evaluate(evaluationCase.question());
            if (evaluationCase.expectedDocument() != null
                    && evaluationCase.expectedDocument().equals(observation.document())) {
                retrievalHits++;
            }
            if (observation.answered() && observation.citationsGrounded()) {
                groundedAnswers++;
            }
            if ("REFUSE".equals(evaluationCase.expectedBehavior()) && !observation.answered()) {
                correctRefusals++;
            }
            if ("ANSWER".equals(evaluationCase.expectedBehavior())
                    && observation.answered()
                    && observation.citationsGrounded()) {
                correctRefusals++;
            }
        }
        return new EvaluationReport(cases.size(), retrievalHits, groundedAnswers, correctRefusals);
    }

    @FunctionalInterface
    public interface CaseEvaluator {
        EvaluationObservation evaluate(String question);
    }

    public record EvaluationCase(String question, String expectedDocument, String expectedBehavior) {
    }

    public record EvaluationObservation(String document, boolean answered, boolean citationsGrounded) {
    }

    public record EvaluationReport(int cases, int retrievalHits, int groundedAnswers, int correctBehaviors) {
        public double retrievalHitRate() {
            return percentage(retrievalHits);
        }

        public double groundedness() {
            return percentage(groundedAnswers);
        }

        public double refusalCorrectness() {
            return percentage(correctBehaviors);
        }

        private double percentage(int value) {
            return cases == 0 ? 0 : (double) value / cases;
        }
    }
}
