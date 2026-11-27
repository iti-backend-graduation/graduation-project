package com.bm.copilot.retrieval;

import java.util.List;

/** Shared contract: finds the chunks most relevant to a question, best first. */
public interface RetrievalService {

    List<RetrievedChunk> search(String query, int topK);
}
