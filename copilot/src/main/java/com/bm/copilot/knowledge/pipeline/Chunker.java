package com.bm.copilot.knowledge.pipeline;

import com.bm.copilot.knowledge.model.ExtractedSection;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class Chunker {
    private static final int CHUNK_WORDS = 650;
    private static final int OVERLAP_WORDS = 100;

    public List<ChunkDraft> chunk(List<ExtractedSection> sections) {
        List<ChunkDraft> result = new ArrayList<>();
        for (ExtractedSection section : sections) {
            String[] words = section.text().trim().split("\\s+");
            if (words.length == 1 && words[0].isBlank()) continue;
            for (int start = 0; start < words.length; start += CHUNK_WORDS - OVERLAP_WORDS) {
                int end = Math.min(start + CHUNK_WORDS, words.length);
                String text = String.join(" ", Arrays.copyOfRange(words, start, end));
                result.add(new ChunkDraft(text, section.page(), section.section()));
                if (end == words.length) break;
            }
        }
        return result;
    }

    public record ChunkDraft(String text, Integer page, String section) {
        public int tokenCount() {
            return text.split("\\s+").length;
        }
    }
}
