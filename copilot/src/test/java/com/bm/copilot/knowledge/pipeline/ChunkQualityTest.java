package com.bm.copilot.knowledge.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import com.bm.copilot.knowledge.model.ExtractedSection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class ChunkQualityTest {

    @Test
    void tenSampleMarkdownChunksAreCleanAndKeepSectionMetadata() throws Exception {
        List<Chunker.ChunkDraft> allChunks = new ArrayList<>();
        MarkdownExtractor extractor = new MarkdownExtractor();
        TextCleaner cleaner = new TextCleaner();
        Chunker chunker = new Chunker();

        for (Path file : Files.walk(Path.of("docs-src"))
                .filter(path -> path.toString().endsWith(".md"))
                .toList()) {
            List<ExtractedSection> extracted = extractor.extract(
                    Files.readString(file, StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8));
            List<ExtractedSection> cleaned = extracted.stream()
                    .map(section -> new ExtractedSection(
                            cleaner.clean(section.text()), section.page(), section.section()))
                    .filter(section -> !section.text().isBlank())
                    .toList();
            allChunks.addAll(chunker.chunk(cleaned));
        }

        assertThat(allChunks).hasSizeGreaterThanOrEqualTo(10);
        List<Chunker.ChunkDraft> samples = new ArrayList<>();
        Random random = new Random(2026);
        while (samples.size() < 10) {
            Chunker.ChunkDraft candidate = allChunks.get(random.nextInt(allChunks.size()));
            if (!samples.contains(candidate)) {
                samples.add(candidate);
            }
        }

        for (Chunker.ChunkDraft chunk : samples) {
            assertThat(chunk.text()).isNotBlank();
            assertThat(chunk.text()).doesNotContain("  ");
            assertThat(chunk.tokenCount()).isGreaterThan(0);
            assertThat(chunk.tokenCount()).isLessThanOrEqualTo(650);
            assertThat(chunk.section()).isNotBlank();
        }
    }
}
