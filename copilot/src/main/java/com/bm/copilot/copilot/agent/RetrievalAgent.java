package com.bm.copilot.copilot.agent;

import com.bm.copilot.retrieval.RetrievalService;
import com.bm.copilot.retrieval.RetrievedChunk;
import java.util.List;
import org.springframework.stereotype.Component;

/** Knowledge Retrieval Agent. Its only tool is the hybrid search; it cannot change anything. */
@Component
public class RetrievalAgent implements Agent<RetrievalAgent.Query, List<RetrievedChunk>> {

    public record Query(String question, int topK) {
    }

    private final RetrievalService retrievalService;

    public RetrievalAgent(RetrievalService retrievalService) {
        this.retrievalService = retrievalService;
    }

    @Override
    public List<RetrievedChunk> run(Query query) {
        return retrievalService.search(query.question(), query.topK());
    }
}
