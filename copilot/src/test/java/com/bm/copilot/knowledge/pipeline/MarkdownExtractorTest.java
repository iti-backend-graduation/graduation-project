package com.bm.copilot.knowledge.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class MarkdownExtractorTest {
    @Test
    void extractsHeadingSections() throws Exception {
        var sections = new MarkdownExtractor().extract(
                "# Cards\nReplace a card.\n## Fees\nThe fee is synthetic.".getBytes(StandardCharsets.UTF_8));

        assertThat(sections).extracting("section")
                .containsExactly("Cards", "Fees");
        assertThat(sections).extracting("text")
                .allMatch(text -> text.toString().contains("."));
        assertThat(sections.get(0).text()).startsWith("Cards");
    }
}
