package com.bm.copilot.copilot.agent;

import com.bm.copilot.retrieval.RetrievedChunk;
import java.util.List;

/**
 * Request Preparation Agent (owned by Developer D). It may only return a draft:
 * it must never submit, approve or execute a request.
 */
public interface RequestPreparationAgent extends Agent<RequestPreparationAgent.Input, RequestDraft> {

    record Input(String question, List<RetrievedChunk> evidence) {
    }
}
