package com.bm.copilot.copilot;

import com.bm.copilot.copilot.agent.RequestDraft;
import com.bm.copilot.retrieval.Citation;
import java.util.List;

/**
 * What the employee sees: the answer, where it comes from, whether a service request is suggested,
 * and a prepared request draft when one was made (null otherwise).
 */
public record AskResponse(String answer, List<Citation> citations, boolean refused, boolean suggestServiceRequest,
                          RequestDraft requestDraft) {

    static final String REFUSAL = "I could not find enough information in the approved documents to answer this question.";

    public static AskResponse refusal() {
        return new AskResponse(REFUSAL, List.of(), true, false, null);
    }

    public AskResponse withRequestDraft(RequestDraft draft) {
        return new AskResponse(answer, citations, refused, suggestServiceRequest, draft);
    }
}
