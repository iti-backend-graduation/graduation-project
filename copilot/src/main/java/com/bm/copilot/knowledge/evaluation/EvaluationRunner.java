package com.bm.copilot.knowledge.evaluation;

import java.util.List;
import java.util.Objects;

public class EvaluationRunner {
    public EvaluationReport evaluate(List<EvaluationCase> cases, CaseEvaluator evaluator) {
        Objects.requireNonNull(cases, "cases");
        Objects.requireNonNull(evaluator, "evaluator");
        if (cases.isEmpty()) {
            return new EvaluationReport(0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        int retrievalCases = 0;
        int retrievalHits = 0;
        int groundedAnswers = 0;
        int correctRefusals = 0;
        int expectedRefusals = 0;
        int correctAnswers = 0;
        int expectedAnswers = 0;
        int answeredCases = 0;
        for (EvaluationCase evaluationCase : cases) {
            EvaluationObservation observation = evaluator.evaluate(evaluationCase.question());
            if (evaluationCase.expectedDocument() != null) {
                retrievalCases++;
                if (observation.documents().contains(evaluationCase.expectedDocument())) {
                    retrievalHits++;
                }
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
            } else if ("ANSWER".equals(evaluationCase.expectedBehavior())) {
                expectedAnswers++;
                if (observation.answered()) {
                    correctAnswers++;
                }
            }
        }
        return new EvaluationReport(cases.size(), retrievalCases, retrievalHits, groundedAnswers,
                answeredCases, correctRefusals, expectedRefusals, correctAnswers, expectedAnswers);
    }

    @FunctionalInterface
    public interface CaseEvaluator {
        EvaluationObservation evaluate(String question);
    }

    public record EvaluationCase(String question, String expectedDocument, String expectedBehavior) {
    }

    public record EvaluationObservation(List<String> documents, boolean answered, boolean citationsGrounded) {
        public EvaluationObservation {
            documents = documents == null ? List.of() : List.copyOf(documents);
        }

        public EvaluationObservation(String document, boolean answered, boolean citationsGrounded) {
            this(document == null ? List.of() : List.of(document), answered, citationsGrounded);
        }
    }

    public record EvaluationReport(int cases, int retrievalCases, int retrievalHits, int groundedAnswers,
                                   int answeredCases, int correctRefusals, int expectedRefusals,
                                   int correctAnswers, int expectedAnswers) {
        public double retrievalHitRate() {
            return percentage(retrievalHits, retrievalCases);
        }

        public double groundedness() {
            return answeredCases == 0 ? 0 : (double) groundedAnswers / answeredCases;
        }

        public double refusalCorrectness() {
            return expectedRefusals == 0 ? 1 : (double) correctRefusals / expectedRefusals;
        }

        public double answerBehaviorCorrectness() {
            return expectedAnswers == 0 ? 1 : (double) correctAnswers / expectedAnswers;
        }

        private double percentage(int value, int total) {
            return total == 0 ? 0 : (double) value / total;
        }
    }
}
