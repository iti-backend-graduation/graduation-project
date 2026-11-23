package com.bm.copilot.retrieval;

import static org.assertj.core.api.Assertions.assertThat;

import com.bm.copilot.TestcontainersConfiguration;
import com.bm.copilot.knowledge.model.Chunk;
import com.bm.copilot.knowledge.model.Document;
import com.bm.copilot.knowledge.repository.ChunkRepository;
import com.bm.copilot.knowledge.repository.DocumentRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class ChunkSearchRepositoryTest {

    @Autowired
    ChunkSearchRepository searchRepository;

    @Autowired
    DocumentRepository documentRepository;

    @Autowired
    ChunkRepository chunkRepository;

    @BeforeEach
    void seedChunks() {
        Document cards = documentRepository.save(new Document(UUID.randomUUID(), "cards.md", "MARKDOWN",
                "a".repeat(64), 1, null, OffsetDateTime.now()));
        Document fees = documentRepository.save(new Document(UUID.randomUUID(), "fees.pdf", "PDF",
                "b".repeat(64), 1, 3, OffsetDateTime.now()));
        chunkRepository.save(new Chunk(UUID.randomUUID(), cards, 0,
                "A lost card must be blocked immediately through the hotline.", null, "Lost cards", 10,
                vector(0)));
        chunkRepository.save(new Chunk(UUID.randomUUID(), fees, 0,
                "The card replacement fee is 100 EGP.", 2, "Card fees", 8, vector(1)));
        chunkRepository.flush();
    }

    @Test
    void semanticSearchReturnsTheClosestChunkFirstWithItsCitation() {
        float[] query = vector(1);
        query[0] = 0.2f;

        List<RetrievedChunk> results = searchRepository.semanticSearch(query, 5);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).citation()).isEqualTo(new Citation("fees.pdf", 2, "Card fees"));
        assertThat(results.get(0).score()).isGreaterThan(results.get(1).score());
    }

    @Test
    void keywordSearchFindsTheExactChunkFromANaturalQuestion() {
        List<RetrievedChunk> results = searchRepository.keywordSearch("How much is the replacement fee?", 5);

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).text()).contains("replacement fee");
        assertThat(results.get(0).citation().page()).isEqualTo(2);
    }

    @Test
    void keywordSearchTreatsSqlAsPlainText() {
        List<RetrievedChunk> results = searchRepository.keywordSearch("'; DROP TABLE chunk; --", 5);

        assertThat(results).isEmpty();
        assertThat(chunkRepository.count()).isEqualTo(2);
    }

    @Test
    void keywordSearchWithNoUsableWordsReturnsNothing() {
        assertThat(searchRepository.keywordSearch("?!", 5)).isEmpty();
    }

    private static float[] vector(int hotIndex) {
        float[] vector = new float[768];
        vector[hotIndex] = 1;
        return vector;
    }
}
