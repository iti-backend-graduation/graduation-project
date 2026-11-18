package com.bm.copilot.knowledge.repository;

import com.bm.copilot.knowledge.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    Optional<Document> findByContentHash(String contentHash);

    int countByFileName(String fileName);

    List<Document> findAllByOrderByIngestedAtDesc();
}
