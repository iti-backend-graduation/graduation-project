-- Switch the vector dimensions from OpenAI text-embedding-3-small (1536)
-- to Gemini text-embedding-004 (768). Existing OpenAI vectors cannot be reused.

DROP INDEX IF EXISTS idx_chunk_embedding;
DELETE FROM document;

ALTER TABLE chunk
    ALTER COLUMN embedding TYPE vector(768);

CREATE INDEX idx_chunk_embedding ON chunk USING hnsw (embedding vector_cosine_ops);
