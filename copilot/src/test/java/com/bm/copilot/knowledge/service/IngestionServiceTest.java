package com.bm.copilot.knowledge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bm.copilot.knowledge.model.Document;
import com.bm.copilot.knowledge.pipeline.Chunker;
import com.bm.copilot.knowledge.pipeline.DocumentExtractor;
import com.bm.copilot.knowledge.pipeline.Embedder;
import com.bm.copilot.knowledge.pipeline.Indexer;
import com.bm.copilot.knowledge.pipeline.TextCleaner;
import com.bm.copilot.knowledge.repository.DocumentRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IngestionServiceTest {
    private final DocumentRepository documents = org.mockito.Mockito.mock(DocumentRepository.class);
    private final DocumentExtractor extractor = org.mockito.Mockito.mock(DocumentExtractor.class);
    private final Indexer indexer = org.mockito.Mockito.mock(Indexer.class);
    private final Embedder embedder = org.mockito.Mockito.mock(Embedder.class);
    private final IngestionService service = new IngestionService(
            documents, List.of(extractor), new TextCleaner(), new Chunker(), embedder, indexer);

    @Test
    void runsThePipelineAndIndexesAFile() throws Exception {
        when(extractor.supports("policy.md")).thenReturn(true);
        when(extractor.extract(any())).thenReturn(
                List.of(new com.bm.copilot.knowledge.model.ExtractedSection("Card fees", null, "Fees")));
        when(documents.countByFileName("policy.md")).thenReturn(0);
        when(indexer.index(any(), any(), any())).thenReturn(1);

        IngestionResult result = service.ingest("policy.md", "source".getBytes());

        assertThat(result.status()).isEqualTo(IngestionResult.Status.INGESTED);
        assertThat(result.chunkCount()).isEqualTo(1);
        verify(documents).save(any(Document.class));
        verify(indexer).index(any(Document.class), any(), any());
    }

    @Test
    void skipsAFileWhoseHashWasAlreadyIngested() throws Exception {
        Document existing = new Document(UUID.randomUUID(), "policy.md", "MARKDOWN", "hash", 1,
                null, OffsetDateTime.now());
        when(documents.findByContentHash(any())).thenReturn(Optional.of(existing));

        IngestionResult result = service.ingest("policy.md", "source".getBytes());

        assertThat(result.status()).isEqualTo(IngestionResult.Status.ALREADY_INGESTED);
        assertThat(result.documentId()).isEqualTo(existing.getId());
        assertThat(result.chunkCount()).isZero();
    }
}
