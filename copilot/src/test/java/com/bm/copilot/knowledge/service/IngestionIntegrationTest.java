package com.bm.copilot.knowledge.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bm.copilot.TestcontainersConfiguration;
import com.bm.copilot.knowledge.pipeline.Embedder;
import com.bm.copilot.knowledge.repository.DocumentRepository;
import com.bm.copilot.knowledge.repository.ChunkRepository;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import({TestcontainersConfiguration.class, IngestionIntegrationTest.FixedEmbedderConfiguration.class})
@Transactional
class IngestionIntegrationTest {

    @Autowired
    IngestionService ingestionService;

    @Autowired
    DocumentRepository documentRepository;

    @Autowired
    ChunkRepository chunkRepository;

    @Test
    void persistsTwoMarkdownDocumentsAndPreventsDuplicateIngestion() throws Exception {
        byte[] first = "# Synthetic Policy\nA card is valid for five years.".getBytes(StandardCharsets.UTF_8);
        byte[] second = "# Digital Banking\nCustomers can use the synthetic support channel."
                .getBytes(StandardCharsets.UTF_8);

        IngestionResult firstResult = ingestionService.ingest("sample-card-policy.md", first);
        IngestionResult duplicate = ingestionService.ingest("sample-card-policy.md", first);
        IngestionResult secondResult = ingestionService.ingest("sample-digital-policy.md", second);

        assertThat(firstResult.status()).isEqualTo(IngestionResult.Status.INGESTED);
        assertThat(firstResult.chunkCount()).isGreaterThan(0);
        assertThat(duplicate.status()).isEqualTo(IngestionResult.Status.ALREADY_INGESTED);
        assertThat(duplicate.chunkCount()).isZero();
        assertThat(secondResult.status()).isEqualTo(IngestionResult.Status.INGESTED);
        assertThat(secondResult.chunkCount()).isGreaterThan(0);
        assertThat(documentRepository.count()).isEqualTo(2);
        assertThat(chunkRepository.count()).isGreaterThanOrEqualTo(2);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedEmbedderConfiguration {
        @Bean
        @Primary
        Embedder fixedEmbedder() {
            return text -> {
                float[] vector = new float[768];
                vector[0] = text == null ? 0 : text.hashCode();
                return vector;
            };
        }
    }
}
