package com.bm.copilot.knowledge.repository;

import com.bm.copilot.knowledge.model.Chunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChunkRepository extends JpaRepository<Chunk, UUID> {
    long countByDocumentId(UUID documentId);
}
