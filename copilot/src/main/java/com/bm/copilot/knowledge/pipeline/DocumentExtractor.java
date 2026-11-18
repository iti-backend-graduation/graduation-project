package com.bm.copilot.knowledge.pipeline;

import com.bm.copilot.knowledge.model.ExtractedSection;

import java.io.IOException;
import java.util.List;

public interface DocumentExtractor {
    boolean supports(String fileName);

    List<ExtractedSection> extract(byte[] content) throws IOException;
}
