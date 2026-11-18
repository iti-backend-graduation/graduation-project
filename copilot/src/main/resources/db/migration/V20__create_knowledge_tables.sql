-- Knowledge module (Dev B): ingested documents and their searchable chunks.
-- Rule: never edit this file once merged - add a new migration instead.

CREATE TABLE document (
    id           UUID         PRIMARY KEY,
    file_name    VARCHAR(255) NOT NULL,
    format       VARCHAR(20)  NOT NULL,
    content_hash CHAR(64)     NOT NULL,
    version      INTEGER      NOT NULL,
    page_count   INTEGER,
    ingested_at  TIMESTAMPTZ  NOT NULL,
    -- the same bytes can only be ingested once (duplicate prevention, race-safe)
    CONSTRAINT uq_document_content_hash UNIQUE (content_hash),
    -- re-uploading changed content under the same file name creates the next version
    CONSTRAINT uq_document_file_name_version UNIQUE (file_name, version),
    CONSTRAINT ck_document_format CHECK (format IN ('PDF', 'MARKDOWN'))
);

CREATE TABLE chunk (
    id          UUID          PRIMARY KEY,
    document_id UUID          NOT NULL REFERENCES document (id) ON DELETE CASCADE,
    chunk_index INTEGER       NOT NULL,
    text        TEXT          NOT NULL,
    page        INTEGER,                -- NULL for formats without pages (Markdown)
    section     VARCHAR(500),           -- nearest heading, e.g. '3.2 Card replacement fees'
    token_count INTEGER       NOT NULL,
    -- 1536 = OpenAI text-embedding-3-small. Changing the embedding model means a new migration.
    embedding   vector(1536)  NOT NULL,
    -- keyword search column: heading text weighted B, body text weighted A
    tsv         tsvector GENERATED ALWAYS AS (
                    setweight(to_tsvector('english', coalesce(section, '')), 'B') ||
                    setweight(to_tsvector('english', text), 'A')
                ) STORED,
    CONSTRAINT uq_chunk_document_index UNIQUE (document_id, chunk_index)
);

CREATE INDEX idx_chunk_document_id ON chunk (document_id);
CREATE INDEX idx_chunk_tsv ON chunk USING gin (tsv);
CREATE INDEX idx_chunk_embedding ON chunk USING hnsw (embedding vector_cosine_ops);
