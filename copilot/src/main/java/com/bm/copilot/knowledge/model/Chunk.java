package com.bm.copilot.knowledge.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "chunk")
public class Chunk {

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(name = "text", nullable = false, columnDefinition = "TEXT")
    private String text;

    private Integer page;

    @Column(length = 500)
    private String section;

    @Column(name = "token_count", nullable = false)
    private int tokenCount;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Column(nullable = false, columnDefinition = "vector(1536)")
    private float[] embedding;

    protected Chunk() {
    }

    public Chunk(UUID id, Document document, int chunkIndex, String text, Integer page,
                 String section, int tokenCount, float[] embedding) {
        this.id = id;
        this.document = document;
        this.chunkIndex = chunkIndex;
        this.text = text;
        this.page = page;
        this.section = section;
        this.tokenCount = tokenCount;
        this.embedding = embedding;
    }

    public UUID getId() {
        return id;
    }

    public Document getDocument() {
        return document;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public String getText() {
        return text;
    }

    public Integer getPage() {
        return page;
    }

    public String getSection() {
        return section;
    }

    public int getTokenCount() {
        return tokenCount;
    }

    public float[] getEmbedding() {
        return embedding;
    }
}
