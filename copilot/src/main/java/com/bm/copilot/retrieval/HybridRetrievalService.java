package com.bm.copilot.retrieval;

import com.bm.copilot.knowledge.pipeline.Embedder;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Runs semantic and keyword search, drops semantic hits that are not similar enough,
 * and merges both lists with Reciprocal Rank Fusion.
 */
@Service
public class HybridRetrievalService implements RetrievalService {

    private static final Logger log = LoggerFactory.getLogger(HybridRetrievalService.class);
    private static final int CANDIDATES_PER_SEARCH = 20;

    private final ChunkSearchRepository searchRepository;
    private final Embedder embedder;
    private final double minSimilarity;

    public HybridRetrievalService(ChunkSearchRepository searchRepository, Embedder embedder,
                                  @Value("${copilot.retrieval.min-similarity:0.5}") double minSimilarity) {
        this.searchRepository = searchRepository;
        this.embedder = embedder;
        this.minSimilarity = minSimilarity;
    }

    @Override
    public List<RetrievedChunk> search(String query, int topK) {
        if (query == null || query.isBlank() || topK <= 0) {
            return List.of();
        }
        List<RetrievedChunk> semantic = searchRepository
                .semanticSearch(embedder.embed(query), CANDIDATES_PER_SEARCH).stream()
                .filter(chunk -> chunk.score() >= minSimilarity)
                .toList();
        List<RetrievedChunk> keyword = searchRepository.keywordSearch(query, CANDIDATES_PER_SEARCH);
        List<RetrievedChunk> results = RankFusion.fuse(semantic, keyword, topK);
        log.info("Hybrid retrieval: semanticHits={}, keywordHits={}, returned={}",
                semantic.size(), keyword.size(), results.size());
        return results;
    }
}
