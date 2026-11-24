package com.bm.copilot.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bm.copilot.knowledge.pipeline.Embedder;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class HybridRetrievalServiceTest {

    private final ChunkSearchRepository repository = mock(ChunkSearchRepository.class);
    private final Embedder embedder = text -> new float[768];
    private final HybridRetrievalService service = new HybridRetrievalService(repository, embedder, 0.6);

    @Test
    void dropsSemanticHitsBelowTheSimilarityThreshold() {
        RetrievedChunk close = chunk(0.8);
        RetrievedChunk far = chunk(0.3);
        when(repository.semanticSearch(any(), anyInt())).thenReturn(List.of(close, far));
        when(repository.keywordSearch(anyString(), anyInt())).thenReturn(List.of());

        List<RetrievedChunk> results = service.search("card fee", 5);

        assertThat(results).extracting(RetrievedChunk::chunkId).containsExactly(close.chunkId());
    }

    @Test
    void returnsNothingWhenNeitherSearchFindsAnything() {
        when(repository.semanticSearch(any(), anyInt())).thenReturn(List.of(chunk(0.1)));
        when(repository.keywordSearch(anyString(), anyInt())).thenReturn(List.of());

        assertThat(service.search("weather in Paris", 5)).isEmpty();
    }

    @Test
    void blankQueryDoesNotSearch() {
        assertThat(service.search("  ", 5)).isEmpty();
        verifyNoInteractions(repository);
    }

    private static RetrievedChunk chunk(double similarity) {
        return new RetrievedChunk(UUID.randomUUID(), "text", new Citation("doc.md", null, "S"), similarity);
    }
}
