ALTER TABLE ai_result_documents
    ADD COLUMN IF NOT EXISTS content_format VARCHAR(20) NOT NULL DEFAULT 'MARKDOWN',
    ADD COLUMN IF NOT EXISTS save_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    ADD COLUMN IF NOT EXISTS saved_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS classification_source VARCHAR(30),
    ADD COLUMN IF NOT EXISTS classification_confidence DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS prompt_version VARCHAR(80);

CREATE TABLE IF NOT EXISTS ai_llm_usage_events (
    id UUID PRIMARY KEY,
    result_document_id UUID REFERENCES ai_result_documents(id) ON DELETE CASCADE,
    purpose VARCHAR(30) NOT NULL CHECK (purpose IN ('CLASSIFICATION', 'ANSWER')),
    provider VARCHAR(30) NOT NULL,
    model VARCHAR(120),
    input_tokens INTEGER,
    output_tokens INTEGER,
    total_tokens INTEGER,
    success BOOLEAN NOT NULL,
    error_code VARCHAR(100),
    called_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ai_result_documents_saved_owner
    ON ai_result_documents(requested_by_user_id, saved_at DESC)
    WHERE save_status = 'SAVED';
CREATE INDEX IF NOT EXISTS idx_ai_llm_usage_result ON ai_llm_usage_events(result_document_id);
