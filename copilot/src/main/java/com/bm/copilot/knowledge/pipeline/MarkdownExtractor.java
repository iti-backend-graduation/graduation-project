package com.bm.copilot.knowledge.pipeline;

import com.bm.copilot.knowledge.model.ExtractedSection;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class MarkdownExtractor implements DocumentExtractor {
    @Override
    public boolean supports(String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(".md");
    }

    @Override
    public List<ExtractedSection> extract(byte[] content) throws IOException {
        String markdown = new String(content, StandardCharsets.UTF_8);
        List<ExtractedSection> sections = new ArrayList<>();
        String heading = null;
        StringBuilder body = new StringBuilder();
        for (String line : markdown.replace("\r\n", "\n").split("\n")) {
            if (line.matches("^#{1,6}\\s+.+")) {
                addSection(sections, body, heading);
                heading = line.replaceFirst("^#{1,6}\\s+", "").trim();
                body.append(heading).append('\n');
            } else {
                body.append(line).append('\n');
            }
        }
        addSection(sections, body, heading);
        return sections;
    }

    private void addSection(List<ExtractedSection> sections, StringBuilder body, String heading) {
        if (!body.toString().isBlank()) {
            sections.add(new ExtractedSection(body.toString(), null, heading));
            body.setLength(0);
        }
    }
}
