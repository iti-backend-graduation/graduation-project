package com.bm.copilot.knowledge.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextCleanerTest {
    private final TextCleaner cleaner = new TextCleaner();

    @Test
    void removesPageMarkersAndNormalizesWhitespace() {
        assertThat(cleaner.clean("Card-\n replacement\n\nPage 2\n fee"))
                .isEqualTo("Card replacement fee");
    }
}
