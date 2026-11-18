package com.bm.copilot.knowledge.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "document")
public class Document {

    @Id
    private UUID id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "format", nullable = false)
    private String format;

    @Column(name = "content_hash", nullable = false, unique = true, columnDefinition = "char(64)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String contentHash;

    @Column(nullable = false)
    private int version;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "ingested_at", nullable = false)
    private OffsetDateTime ingestedAt;

    protected Document() {
    }

    public Document(UUID id, String fileName, String format, String contentHash, int version,
                    Integer pageCount, OffsetDateTime ingestedAt) {
        this.id = id;
        this.fileName = fileName;
        this.format = format;
        this.contentHash = contentHash;
        this.version = version;
        this.pageCount = pageCount;
        this.ingestedAt = ingestedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFormat() {
        return format;
    }

    public String getContentHash() {
        return contentHash;
    }

    public int getVersion() {
        return version;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public OffsetDateTime getIngestedAt() {
        return ingestedAt;
    }
}
