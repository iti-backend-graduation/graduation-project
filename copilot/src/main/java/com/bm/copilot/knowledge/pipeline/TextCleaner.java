package com.bm.copilot.knowledge.pipeline;

import org.springframework.stereotype.Component;

@Component
public class TextCleaner {
    public String clean(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("(?m)^\\s*Page\\s+\\d+\\s*$", "")
                .replaceAll("(?m)(\\w)-\\s*\\n\\s*(\\w)", "$1 $2")
                .replaceAll("\\s*\\n\\s*", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
