CREATE TABLE ai_detailed_documents (
    id UUID PRIMARY KEY,
    result_document_id UUID NOT NULL UNIQUE REFERENCES ai_result_documents(id) ON DELETE CASCADE,
    requested_by_user_id UUID NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL CHECK (status IN ('GENERATING','READY','FAILED')),
    title VARCHAR(200), content TEXT, citation_json TEXT,
    pdf_storage_key VARCHAR(500), pdf_file_name VARCHAR(300), error_message VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), completed_at TIMESTAMPTZ
);
CREATE INDEX idx_ai_detailed_documents_owner ON ai_detailed_documents(requested_by_user_id, created_at DESC);
ALTER TABLE ai_llm_usage_events DROP CONSTRAINT IF EXISTS ai_llm_usage_events_purpose_check;
ALTER TABLE ai_llm_usage_events ADD CONSTRAINT ai_llm_usage_events_purpose_check CHECK (purpose IN ('CLASSIFICATION','ANSWER','DOCUMENT_DETAIL'));
