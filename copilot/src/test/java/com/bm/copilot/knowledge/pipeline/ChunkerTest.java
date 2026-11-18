package com.bm.copilot.knowledge.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import com.bm.copilot.knowledge.model.ExtractedSection;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChunkerTest {
    @Test
    void createsOverlappingChunksAndPreservesMetadata() {
        String text = java.util.stream.IntStream.range(0, 1_000)
                .mapToObj(Integer::toString).collect(java.util.stream.Collectors.joining(" "));
        List<Chunker.ChunkDraft> chunks = new Chunker().chunk(
                List.of(new ExtractedSection(text, 4, "Card replacement")));

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).page()).isEqualTo(4);
        assertThat(chunks.get(0).section()).isEqualTo("Card replacement");
        assertThat(chunks.get(0).text()).contains("0").contains("649");
        assertThat(chunks.get(1).text()).contains("550").contains("999");
    }
}
