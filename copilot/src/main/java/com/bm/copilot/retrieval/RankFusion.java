package com.bm.copilot.retrieval;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Reciprocal Rank Fusion: score = sum of 1 / (K + rank) over every list the chunk appears in.
 * It only uses positions, so cosine similarity and ts_rank never have to be put on the same scale.
 */
final class RankFusion {

    static final int K = 60;

    private RankFusion() {
    }

    static List<RetrievedChunk> fuse(List<RetrievedChunk> semantic, List<RetrievedChunk> keyword, int topK) {
        Map<UUID, RetrievedChunk> chunks = new LinkedHashMap<>();
        Map<UUID, Double> scores = new LinkedHashMap<>();
        addRanks(semantic, chunks, scores);
        addRanks(keyword, chunks, scores);

        List<RetrievedChunk> fused = new ArrayList<>();
        for (Map.Entry<UUID, Double> entry : scores.entrySet()) {
            RetrievedChunk chunk = chunks.get(entry.getKey());
            fused.add(new RetrievedChunk(chunk.chunkId(), chunk.text(), chunk.citation(), entry.getValue()));
        }
        fused.sort(Comparator.comparingDouble(RetrievedChunk::score).reversed());
        return fused.subList(0, Math.min(topK, fused.size()));
    }

    private static void addRanks(List<RetrievedChunk> ranked, Map<UUID, RetrievedChunk> chunks,
                                 Map<UUID, Double> scores) {
        for (int i = 0; i < ranked.size(); i++) {
            RetrievedChunk chunk = ranked.get(i);
            int rank = i + 1;
            chunks.putIfAbsent(chunk.chunkId(), chunk);
            scores.merge(chunk.chunkId(), 1.0 / (K + rank), Double::sum);
        }
    }
}
