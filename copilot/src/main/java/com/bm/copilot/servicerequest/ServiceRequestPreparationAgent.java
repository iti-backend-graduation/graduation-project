package com.bm.copilot.servicerequest;

import com.bm.copilot.copilot.agent.RequestDraft;
import com.bm.copilot.copilot.agent.RequestPreparationAgent;
import com.bm.copilot.retrieval.Citation;
import com.bm.copilot.retrieval.RetrievedChunk;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Request Preparation Agent: turns the employee's question and the retrieved evidence into a typed
 * {@link RequestDraft} (category, description, missing fields) for the employee to review.
 * <p>
 * Its boundaries:
 * <ul>
 *   <li>It has no dependencies at all: it cannot read or write the database, call the model, or reach any service.
 *       So it cannot submit, approve or execute anything.</li>
 *   <li>The draft is only shown. The employee creates the real DRAFT through POST /api/requests, the same validated
 *       endpoint as a hand-written request, so the agent's output never skips validation or ownership.</li>
 *   <li>It is deterministic (keyword scoring, no model call), so it is free, fast and repeatable in tests.</li>
 * </ul>
 */
@Component
public class ServiceRequestPreparationAgent implements RequestPreparationAgent {

    static final int MAX_DESCRIPTION = 2000;
    private static final int MAX_SOURCES = 3;
    private static final int QUESTION_WEIGHT = 3;
    private static final int EVIDENCE_WEIGHT = 1;

    private static final Map<RequestCategory, Set<String>> KEYWORDS = new EnumMap<>(Map.of(
            RequestCategory.CARD_SERVICE, Set.of("card", "cards", "debit", "credit", "atm", "pin", "lost", "stolen",
                    "chargeback", "dispute", "replacement", "pos", "card's"),
            RequestCategory.DIGITAL_BANKING, Set.of("online", "internet", "mobile", "app", "digital", "login",
                    "password", "otp", "token", "ewallet", "wallet"),
            RequestCategory.ACCOUNT_SERVICE, Set.of("account", "accounts", "open", "opening", "close", "closure",
                    "closing", "statement", "dormant", "savings", "maintenance", "transfer", "remittance"),
            RequestCategory.ADDITIONAL_DOCUMENTS, Set.of("document", "documents", "proof", "passport", "certificate",
                    "kyc", "paperwork", "copy", "checklist")));

    @Override
    public RequestDraft run(Input input) {
        String question = input.question() == null ? "" : input.question().strip();
        List<RetrievedChunk> evidence = input.evidence() == null ? List.of() : input.evidence();

        RequestCategory category = categorise(question, evidence);
        List<String> missing = new ArrayList<>();
        if (category == null) {
            missing.add(ServiceRequest.FIELD_CATEGORY);
        }
        missing.add(ServiceRequest.FIELD_CUSTOMER_REFERENCE);
        if (category == RequestCategory.CARD_SERVICE) {
            missing.add(ServiceRequest.FIELD_CARD_LAST_FOUR);
        }
        return new RequestDraft(category == null ? null : category.name(), describe(question, evidence),
                List.copyOf(missing));
    }

    /** The best-scoring category, or null when nothing matches or two categories tie (the employee decides). */
    static RequestCategory categorise(String question, List<RetrievedChunk> evidence) {
        Map<RequestCategory, Integer> scores = new EnumMap<>(RequestCategory.class);
        score(scores, question, QUESTION_WEIGHT);
        for (RetrievedChunk chunk : evidence) {
            Citation citation = chunk.citation();
            if (citation != null) {
                score(scores, citation.documentName(), EVIDENCE_WEIGHT);
                score(scores, citation.section(), EVIDENCE_WEIGHT);
            }
        }
        int best = scores.values().stream().max(Integer::compare).orElse(0);
        if (best == 0) {
            return null;
        }
        List<RequestCategory> leaders = scores.entrySet().stream()
                .filter(e -> e.getValue() == best)
                .map(Map.Entry::getKey)
                .toList();
        return leaders.size() == 1 ? leaders.get(0) : null;
    }

    private static void score(Map<RequestCategory, Integer> scores, String text, int weight) {
        if (text == null || text.isBlank()) {
            return;
        }
        // hyphens split too, so a file name such as "02-lost-stolen-card-procedure.md" counts as words
        List<String> words = Arrays.asList(text.toLowerCase(Locale.ROOT).split("[^a-z0-9']+"));
        KEYWORDS.forEach((category, keywords) -> {
            long hits = words.stream().filter(keywords::contains).count();
            if (hits > 0) {
                scores.merge(category, (int) hits * weight, Integer::sum);
            }
        });
    }

    /** The question, plus the procedures it is based on, so the supervisor can check the request against them. */
    static String describe(String question, List<RetrievedChunk> evidence) {
        String sources = evidence.stream()
                .map(RetrievedChunk::citation)
                .filter(Objects::nonNull)
                .map(ServiceRequestPreparationAgent::label)
                .distinct()
                .limit(MAX_SOURCES)
                .collect(Collectors.joining("; "));
        String description = sources.isEmpty() ? question : question + "\nBased on: " + sources;
        return description.length() <= MAX_DESCRIPTION ? description : description.substring(0, MAX_DESCRIPTION);
    }

    private static String label(Citation citation) {
        StringBuilder label = new StringBuilder(citation.documentName());
        if (citation.section() != null && !citation.section().isBlank()) {
            label.append(" - ").append(citation.section());
        }
        if (citation.page() != null) {
            label.append(" (page ").append(citation.page()).append(')');
        }
        return label.toString();
    }
}
