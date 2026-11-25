package com.bm.copilot.copilot;

import com.bm.copilot.retrieval.Citation;
import java.util.List;

/** What the employee sees: the answer, where it comes from, and whether a service request is suggested. */
public record AskResponse(String answer, List<Citation> citations, boolean refused, boolean suggestServiceRequest) {

    static final String REFUSAL = "I could not find enough information in the approved documents to answer this question.";

    static AskResponse refusal() {
        return new AskResponse(REFUSAL, List.of(), true, false);
    }
}
