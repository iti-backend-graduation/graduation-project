package com.bm.copilot.retrieval;

import java.util.UUID;

/** One chunk returned by a search, with its citation and the score used to rank it. */
public record RetrievedChunk(UUID chunkId, String text, Citation citation, double score) {
}
