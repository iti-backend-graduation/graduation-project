package com.bm.copilot.knowledge.service;

import com.bm.copilot.knowledge.model.Document;
import com.bm.copilot.knowledge.model.ExtractedSection;
import com.bm.copilot.knowledge.pipeline.*;
import com.bm.copilot.knowledge.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class IngestionService {
    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final DocumentRepository documentRepository;
    private final List<DocumentExtractor> extractors;
    private final TextCleaner cleaner;
    private final Chunker chunker;
    private final Embedder embedder;
    private final Indexer indexer;

    public IngestionService(DocumentRepository documentRepository, List<DocumentExtractor> extractors,
                            TextCleaner cleaner, Chunker chunker, Embedder embedder, Indexer indexer) {
        this.documentRepository = documentRepository;
        this.extractors = extractors;
        this.cleaner = cleaner;
        this.chunker = chunker;
        this.embedder = embedder;
        this.indexer = indexer;
    }

    @Transactional
    public IngestionResult ingest(String fileName, byte[] content) throws java.io.IOException {
        log.info("Knowledge ingestion started: file={}, bytes={}", fileName, content.length);
        String hash = sha256(content);
        var existing = documentRepository.findByContentHash(hash);
        if (existing.isPresent()) {
            log.info("Knowledge ingestion duplicate skipped: file={}, documentId={}",
                    fileName, existing.get().getId());
            return new IngestionResult(IngestionResult.Status.ALREADY_INGESTED, existing.get().getId(), 0);
        }

        DocumentExtractor extractor = extractors.stream()
                .filter(candidate -> candidate.supports(fileName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Only PDF and Markdown files are supported"));
        log.info("Knowledge extraction stage: file={}, extractor={}", fileName,
                extractor.getClass().getSimpleName());
        List<ExtractedSection> extracted = extractor.extract(content);
        List<ExtractedSection> cleaned = extracted.stream()
                .map(section -> new ExtractedSection(cleaner.clean(section.text()), section.page(), section.section()))
                .filter(section -> !section.text().isBlank())
                .toList();
        log.info("Knowledge clean stage: file={}, extractedSections={}, cleanedSections={}",
                fileName, extracted.size(), cleaned.size());
        List<Chunker.ChunkDraft> chunks = chunker.chunk(cleaned);
        log.info("Knowledge chunk stage: file={}, chunkCount={}", fileName, chunks.size());
        String format = fileName.toLowerCase().endsWith(".pdf") ? "PDF" : "MARKDOWN";
        int version = documentRepository.countByFileName(fileName) + 1;
        Integer pageCount = cleaned.stream().map(ExtractedSection::page).filter(java.util.Objects::nonNull)
                .max(Integer::compareTo).orElse(null);
        Document document = new Document(UUID.randomUUID(), fileName, format, hash, version,
                pageCount, OffsetDateTime.now());
        documentRepository.save(document);
        int indexed = indexer.index(document, chunks, embedder);
        log.info("Knowledge index stage complete: file={}, documentId={}, indexedChunks={}",
                fileName, document.getId(), indexed);
        return new IngestionResult(IngestionResult.Status.INGESTED, document.getId(), indexed);
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the runtime", exception);
        }
    }
}
