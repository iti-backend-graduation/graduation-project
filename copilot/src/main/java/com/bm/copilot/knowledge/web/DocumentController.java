package com.bm.copilot.knowledge.web;

import com.bm.copilot.knowledge.repository.ChunkRepository;
import com.bm.copilot.knowledge.repository.DocumentRepository;
import com.bm.copilot.knowledge.service.IngestionResult;
import com.bm.copilot.knowledge.service.IngestionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/documents")
public class DocumentController {
    private final IngestionService ingestionService;
    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;

    public DocumentController(IngestionService ingestionService, DocumentRepository documentRepository,
                              ChunkRepository chunkRepository) {
        this.ingestionService = ingestionService;
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ResponseEntity<IngestionResult> upload(@RequestPart("file") MultipartFile file) throws java.io.IOException {
        if (file.isEmpty() || file.getOriginalFilename() == null) {
            return ResponseEntity.badRequest().build();
        }
        IngestionResult result = ingestionService.ingest(file.getOriginalFilename(), file.getBytes());
        return ResponseEntity.status(result.status() == IngestionResult.Status.INGESTED
                ? HttpStatus.CREATED : HttpStatus.OK).body(result);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Void> invalidDocument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().build();
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPERVISOR')")
    public List<DocumentSummary> list() {
        return documentRepository.findAllByOrderByIngestedAtDesc().stream()
                .map(document -> new DocumentSummary(document.getId(), document.getFileName(), document.getFormat(),
                        document.getVersion(), document.getPageCount(), document.getIngestedAt(),
                        chunkRepository.countByDocumentId(document.getId())))
                .toList();
    }

    public record DocumentSummary(java.util.UUID id, String fileName, String format, int version,
                                  Integer pageCount, java.time.OffsetDateTime ingestedAt, long chunkCount) {
    }
}
