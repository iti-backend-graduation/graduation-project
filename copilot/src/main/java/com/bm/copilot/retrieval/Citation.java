package com.bm.copilot.retrieval;

/** Where a piece of evidence comes from. Markdown files have no page, so they are cited by section. */
public record Citation(String documentName, Integer page, String section) {
}
