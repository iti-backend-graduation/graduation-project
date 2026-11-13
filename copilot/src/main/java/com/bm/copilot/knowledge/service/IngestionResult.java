package com.bm.copilot.knowledge.service;

import java.util.UUID;

public record IngestionResult(Status status, UUID documentId, int chunkCount) {
    public enum Status {INGESTED, ALREADY_INGESTED}
}
