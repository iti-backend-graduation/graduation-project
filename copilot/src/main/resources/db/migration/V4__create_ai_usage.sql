-- Audit module: one row per AI model call (tokens and estimated cost).
-- Append-only: rows are inserted and read, never updated or deleted.
CREATE TABLE ai_usage (
    id                uuid           PRIMARY KEY DEFAULT gen_random_uuid(),
    step              varchar(60)    NOT NULL,
    model             varchar(100)   NOT NULL,
    prompt_tokens     integer        NOT NULL CHECK (prompt_tokens >= 0),
    completion_tokens integer        NOT NULL CHECK (completion_tokens >= 0),
    estimated_cost    numeric(12, 6) NOT NULL CHECK (estimated_cost >= 0),
    correlation_id    varchar(64),
    created_at        timestamptz    NOT NULL DEFAULT now()
);

CREATE INDEX idx_ai_usage_correlation_id ON ai_usage (correlation_id);
