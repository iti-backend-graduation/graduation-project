package com.bm.copilot.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import com.bm.copilot.copilot.CopilotEvaluationTest.DiskCachedEmbedder;
import com.bm.copilot.knowledge.pipeline.Embedder;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DiskCachedEmbedderTest {

    @TempDir
    Path tempDir;

    private final AtomicInteger calls = new AtomicInteger();
    private final Embedder gemini = text -> {
        calls.incrementAndGet();
        return new float[] {0.25f, -0.5f};
    };

    @Test
    void sameTextAndConfigurationIsEmbeddedOnceEvenAfterARestart() {
        Path file = tempDir.resolve("cache");
        String config = "gemini-embedding-001|768|RETRIEVAL_DOCUMENT";

        new DiskCachedEmbedder(gemini, file, config, 0).embed("card fee");
        float[] fromDisk = new DiskCachedEmbedder(gemini, file, config, 0).embed("card fee");

        assertThat(calls).hasValue(1);
        assertThat(fromDisk).containsExactly(0.25f, -0.5f);
    }

    @Test
    void differentModelConfigurationDoesNotReuseTheCachedVector() {
        Path file = tempDir.resolve("cache");

        new DiskCachedEmbedder(gemini, file, "gemini-embedding-001|768|RETRIEVAL_DOCUMENT", 0).embed("card fee");
        new DiskCachedEmbedder(gemini, file, "gemini-embedding-001|1536|RETRIEVAL_DOCUMENT", 0).embed("card fee");

        assertThat(calls).hasValue(2);
    }
}
