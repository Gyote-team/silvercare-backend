CREATE TABLE ai_result_documents (
    id UUID PRIMARY KEY,
    request_id UUID NOT NULL UNIQUE,
    patient_id UUID NOT NULL,
    requested_by_user_id UUID NOT NULL,
    question TEXT NOT NULL,
    question_type VARCHAR(40) NOT NULL,
    job_status VARCHAR(20) NOT NULL,
    result_status VARCHAR(20),
    title VARCHAR(200),
    content TEXT,
    citation_json TEXT,
    error_message VARCHAR(500),
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ(6),
    CONSTRAINT ai_result_documents_job_status_check
        CHECK (job_status IN ('QUEUED', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELED')),
    CONSTRAINT ai_result_documents_result_status_check
        CHECK (result_status IS NULL OR result_status IN ('COMPLETE', 'PARTIAL', 'INSUFFICIENT')),
    CONSTRAINT ai_result_documents_patient_fk FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT ai_result_documents_requester_fk FOREIGN KEY (requested_by_user_id) REFERENCES users (id)
);

CREATE INDEX ai_result_documents_patient_created_idx
    ON ai_result_documents (patient_id, created_at DESC);
