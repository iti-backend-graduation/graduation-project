package com.bm.copilot.knowledge.evaluation;

import java.util.List;
import java.util.Objects;

public class EvaluationRunner {
    public EvaluationReport evaluate(List<EvaluationCase> cases, CaseEvaluator evaluator) {
        Objects.requireNonNull(cases, "cases");
        Objects.requireNonNull(evaluator, "evaluator");
        if (cases.isEmpty()) {
            return new EvaluationReport(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        int retrievalCases = 0;
        int retrievalHits = 0;
        int groundedAnswers = 0;
        int correctRefusals = 0;
        int expectedRefusals = 0;
        int correctClarifications = 0;
        int expectedClarifications = 0;
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
            if (observation.outcome() == ResponseOutcome.ANSWER) {
                answeredCases++;
                if (observation.citationsGrounded()) {
                    groundedAnswers++;
                }
            }
            switch (evaluationCase.expectedBehavior()) {
                case "ANSWER" -> {
                    expectedAnswers++;
                    if (observation.outcome() == ResponseOutcome.ANSWER) {
                        correctAnswers++;
                    }
                }
                case "REFUSE" -> {
                    expectedRefusals++;
                    if (observation.outcome() == ResponseOutcome.REFUSE) {
                        correctRefusals++;
                    }
                }
                case "CLARIFY" -> {
                    expectedClarifications++;
                    if (observation.outcome() == ResponseOutcome.CLARIFY) {
                        correctClarifications++;
                    }
                }
                default -> throw new IllegalArgumentException(
                        "Unsupported expected behavior: " + evaluationCase.expectedBehavior());
            }
        }
        return new EvaluationReport(cases.size(), retrievalCases, retrievalHits, groundedAnswers,
                answeredCases, correctRefusals, expectedRefusals, correctClarifications,
                expectedClarifications, correctAnswers, expectedAnswers);
    }

    @FunctionalInterface
    public interface CaseEvaluator {
        EvaluationObservation evaluate(String question);
    }

    public record EvaluationCase(String question, String expectedDocument, String expectedBehavior) {
    }

    public enum ResponseOutcome {
        ANSWER,
        REFUSE,
        CLARIFY
    }

    public record EvaluationObservation(
            List<String> documents, ResponseOutcome outcome, boolean citationsGrounded) {
        public EvaluationObservation {
            documents = documents == null ? List.of() : List.copyOf(documents);
            Objects.requireNonNull(outcome, "outcome");
        }

        public EvaluationObservation(String document, boolean answered, boolean citationsGrounded) {
            this(document == null ? List.of() : List.of(document),
                    answered ? ResponseOutcome.ANSWER : ResponseOutcome.REFUSE, citationsGrounded);
        }
    }

    public record EvaluationReport(int cases, int retrievalCases, int retrievalHits, int groundedAnswers,
                                   int answeredCases, int correctRefusals, int expectedRefusals,
                                   int correctClarifications, int expectedClarifications,
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

        public double clarificationCorrectness() {
            return expectedClarifications == 0 ? 1
                    : (double) correctClarifications / expectedClarifications;
        }

        private double percentage(int value, int total) {
            return total == 0 ? 0 : (double) value / total;
        }
    }
}
