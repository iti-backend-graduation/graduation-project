package com.bm.copilot.copilot;

import com.bm.copilot.copilot.agent.RequestDraft;
import com.bm.copilot.retrieval.Citation;
import java.util.List;

/**
 * What the employee sees: the answer, where it comes from, whether a service request is suggested,
 * and a prepared request draft when one was made (null otherwise).
 * When the question is ambiguous, {@code clarificationQuestion} asks for the missing detail; the answer then
 * holds the same question and there are no citations.
 */
public record AskResponse(String answer, List<Citation> citations, boolean refused, boolean suggestServiceRequest,
                          RequestDraft requestDraft, String clarificationQuestion) {

    static final String REFUSAL = "I could not find enough information in the approved documents to answer this question.";

    public static AskResponse refusal() {
        return new AskResponse(REFUSAL, List.of(), true, false, null, null);
    }

    public static AskResponse clarification(String question) {
        return new AskResponse(question, List.of(), false, false, null, question);
    }

    public boolean needsClarification() {
        return clarificationQuestion != null;
    }

    public AskResponse withRequestDraft(RequestDraft draft) {
        return new AskResponse(answer, citations, refused, suggestServiceRequest, draft, clarificationQuestion);
    }
}
