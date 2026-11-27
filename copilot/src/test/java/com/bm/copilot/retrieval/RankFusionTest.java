package com.bm.copilot.retrieval;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RankFusionTest {

    private final RetrievedChunk a = chunk("a");
    private final RetrievedChunk b = chunk("b");
    private final RetrievedChunk c = chunk("c");
    private final RetrievedChunk d = chunk("d");

    @Test
    void chunkRankedFirstInBothListsComesFirst() {
        List<RetrievedChunk> fused = RankFusion.fuse(List.of(a, b, c), List.of(a, c), 5);

        assertThat(fused.get(0).chunkId()).isEqualTo(a.chunkId());
        assertThat(fused.get(0).score()).isEqualTo(2.0 / 61);
    }

    @Test
    void chunkFoundByOnlyOneSearchStillAppears() {
        List<RetrievedChunk> fused = RankFusion.fuse(List.of(a), List.of(d), 5);

        assertThat(fused).extracting(RetrievedChunk::chunkId).containsExactlyInAnyOrder(a.chunkId(), d.chunkId());
    }

    @Test
    void chunkInBothListsBeatsAChunkRankedFirstInOnlyOne() {
        List<RetrievedChunk> fused = RankFusion.fuse(List.of(a, b), List.of(c, b), 5);

        assertThat(fused.get(0).chunkId()).isEqualTo(b.chunkId());
    }

    @Test
    void keepsOnlyTopK() {
        assertThat(RankFusion.fuse(List.of(a, b, c), List.of(d), 2)).hasSize(2);
        assertThat(RankFusion.fuse(List.of(), List.of(), 5)).isEmpty();
    }

    private static RetrievedChunk chunk(String text) {
        return new RetrievedChunk(UUID.randomUUID(), text, new Citation("doc.md", null, "Section"), 0.9);
    }
}
