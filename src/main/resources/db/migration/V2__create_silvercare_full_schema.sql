-- SilverCare 전체 도메인 스키마
-- PostgreSQL + pgvector 기준

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255),
    password_hash VARCHAR(255),
    name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ(6),
    kakao_id VARCHAR(255),
    CONSTRAINT users_role_check CHECK (role IN ('PENDING', 'PATIENT', 'CAREGIVER', 'ADMIN')),
    CONSTRAINT users_status_check CHECK (status IN ('ACTIVE', 'LOCKED', 'WITHDRAWN')),
    CONSTRAINT users_kakao_id_key UNIQUE (kakao_id)
);

CREATE TABLE patients (
    id UUID PRIMARY KEY,
    user_id UUID UNIQUE,
    birth_date DATE,
    gender VARCHAR(20),
    accessibility_profile JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT patients_status_check CHECK (status IN ('ACTIVE', 'INACTIVE', 'DECEASED')),
    CONSTRAINT patients_user_fk FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE caregiver_links (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL,
    caregiver_user_id UUID NOT NULL,
    relation VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'INVITED',
    invited_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accepted_at TIMESTAMPTZ(6),
    revoked_at TIMESTAMPTZ(6),
    CONSTRAINT caregiver_links_status_check CHECK (status IN ('INVITED', 'ACTIVE', 'REJECTED', 'REVOKED')),
    CONSTRAINT caregiver_links_patient_fk FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT caregiver_links_caregiver_fk FOREIGN KEY (caregiver_user_id) REFERENCES users (id)
);

CREATE TABLE visits (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL,
    hospital_name VARCHAR(200),
    department VARCHAR(100),
    visited_on DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    memo TEXT,
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ(6),
    CONSTRAINT visits_status_check CHECK (status IN ('PLANNED', 'OPEN', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT visits_patient_fk FOREIGN KEY (patient_id) REFERENCES patients (id)
);

CREATE TABLE documents (
    id UUID PRIMARY KEY,
    visit_id UUID NOT NULL,
    uploader_user_id UUID NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    document_type VARCHAR(30) NOT NULL DEFAULT 'UNKNOWN',
    status VARCHAR(20) NOT NULL DEFAULT 'UPLOADED',
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ(6),
    CONSTRAINT documents_type_check CHECK (document_type IN ('LAB_RESULT', 'PRESCRIPTION', 'DIAGNOSIS', 'DISCHARGE_GUIDE', 'UNKNOWN')),
    CONSTRAINT documents_status_check CHECK (status IN ('UPLOADED', 'PROCESSING', 'READY', 'NEEDS_REVIEW', 'FAILED', 'DELETED')),
    CONSTRAINT documents_visit_fk FOREIGN KEY (visit_id) REFERENCES visits (id),
    CONSTRAINT documents_uploader_fk FOREIGN KEY (uploader_user_id) REFERENCES users (id)
);

CREATE TABLE document_analyses (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    parser_version VARCHAR(50) NOT NULL,
    model_version VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    confidence DECIMAL(5, 4),
    started_at TIMESTAMPTZ(6),
    completed_at TIMESTAMPTZ(6),
    error_message TEXT,
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT document_analyses_status_check CHECK (status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED')),
    CONSTRAINT document_analyses_confidence_check CHECK (confidence IS NULL OR confidence BETWEEN 0 AND 1),
    CONSTRAINT document_analyses_document_fk FOREIGN KEY (document_id) REFERENCES documents (id)
);

CREATE TABLE document_pages (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    page_no INTEGER NOT NULL,
    ocr_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ocr_text TEXT,
    width_px INTEGER,
    height_px INTEGER,
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT document_pages_status_check CHECK (ocr_status IN ('PENDING', 'PROCESSING', 'SUCCEEDED', 'FAILED', 'SKIPPED')),
    CONSTRAINT document_pages_document_page_key UNIQUE (document_id, page_no),
    CONSTRAINT document_pages_document_fk FOREIGN KEY (document_id) REFERENCES documents (id)
);

CREATE TABLE document_chunks (
    id UUID PRIMARY KEY,
    analysis_id UUID NOT NULL,
    page_id UUID,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    bounding_box JSONB,
    token_count INTEGER,
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT document_chunks_analysis_index_key UNIQUE (analysis_id, chunk_index),
    CONSTRAINT document_chunks_analysis_fk FOREIGN KEY (analysis_id) REFERENCES document_analyses (id),
    CONSTRAINT document_chunks_page_fk FOREIGN KEY (page_id) REFERENCES document_pages (id)
);

CREATE TABLE extracted_items (
    id UUID PRIMARY KEY,
    analysis_id UUID NOT NULL,
    page_id UUID,
    item_type VARCHAR(30) NOT NULL,
    normalized_name VARCHAR(200),
    raw_value VARCHAR(500),
    normalized_value VARCHAR(500),
    unit VARCHAR(50),
    reference_range VARCHAR(100),
    confidence DECIMAL(5, 4),
    source_text TEXT NOT NULL,
    source_box JSONB,
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT extracted_items_type_check CHECK (item_type IN ('LAB_VALUE', 'MEDICATION', 'SCHEDULE', 'DIAGNOSIS_TEXT', 'CAUTION', 'OTHER')),
    CONSTRAINT extracted_items_confidence_check CHECK (confidence IS NULL OR confidence BETWEEN 0 AND 1),
    CONSTRAINT extracted_items_analysis_fk FOREIGN KEY (analysis_id) REFERENCES document_analyses (id),
    CONSTRAINT extracted_items_page_fk FOREIGN KEY (page_id) REFERENCES document_pages (id)
);

CREATE TABLE health_records (
    id UUID PRIMARY KEY,
    visit_id UUID,
    patient_id UUID NOT NULL,
    author_user_id UUID NOT NULL,
    input_type VARCHAR(10) NOT NULL,
    content TEXT NOT NULL,
    recorded_at TIMESTAMPTZ(6) NOT NULL,
    proxy_written BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ(6),
    CONSTRAINT health_records_input_type_check CHECK (input_type IN ('TEXT', 'STT')),
    CONSTRAINT health_records_visit_fk FOREIGN KEY (visit_id) REFERENCES visits (id),
    CONSTRAINT health_records_patient_fk FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT health_records_author_fk FOREIGN KEY (author_user_id) REFERENCES users (id)
);

CREATE TABLE ai_runs (
    id UUID PRIMARY KEY,
    message_id UUID,
    analysis_id UUID,
    visit_id UUID,
    run_type VARCHAR(30) NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    prompt_version VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    current_step VARCHAR(50),
    progress INTEGER,
    retryable BOOLEAN NOT NULL DEFAULT FALSE,
    retry_count INTEGER NOT NULL DEFAULT 0,
    error_code VARCHAR(100),
    failed_step VARCHAR(100),
    latency_ms INTEGER,
    input_tokens INTEGER,
    output_tokens INTEGER,
    response_text TEXT,
    error_message TEXT,
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ(6),
    CONSTRAINT ai_runs_type_check CHECK (run_type IN ('EXPLANATION', 'CHAT', 'COMPARISON', 'VALIDATION', 'PRE_VISIT_SUMMARY')),
    CONSTRAINT ai_runs_status_check CHECK (status IN ('QUEUED', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELED')),
    CONSTRAINT ai_runs_progress_check CHECK (progress IS NULL OR progress BETWEEN 0 AND 100),
    CONSTRAINT ai_runs_retry_count_check CHECK (retry_count >= 0),
    CONSTRAINT ai_runs_analysis_fk FOREIGN KEY (analysis_id) REFERENCES document_analyses (id),
    CONSTRAINT ai_runs_visit_fk FOREIGN KEY (visit_id) REFERENCES visits (id)
);

CREATE TABLE ai_explanations (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    ai_run_id UUID,
    version INTEGER NOT NULL DEFAULT 1,
    title VARCHAR(200),
    content TEXT,
    result_status VARCHAR(20),
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ(6),
    CONSTRAINT ai_explanations_document_version_key UNIQUE (document_id, version),
    CONSTRAINT ai_explanations_version_check CHECK (version > 0),
    CONSTRAINT ai_explanations_result_check CHECK (result_status IS NULL OR result_status IN ('COMPLETE', 'PARTIAL', 'INSUFFICIENT')),
    CONSTRAINT ai_explanations_document_fk FOREIGN KEY (document_id) REFERENCES documents (id),
    CONSTRAINT ai_explanations_run_fk FOREIGN KEY (ai_run_id) REFERENCES ai_runs (id)
);

CREATE TABLE explanation_sections (
    id UUID PRIMARY KEY,
    explanation_id UUID NOT NULL,
    section_type VARCHAR(30) NOT NULL,
    title VARCHAR(200) NOT NULL,
    section_order INTEGER NOT NULL,
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT explanation_sections_type_check CHECK (section_type IN ('LAB_RESULT', 'MEDICATION', 'FOLLOW_UP', 'TEST_SCHEDULE', 'CAUTION')),
    CONSTRAINT explanation_sections_order_key UNIQUE (explanation_id, section_order),
    CONSTRAINT explanation_sections_explanation_fk FOREIGN KEY (explanation_id) REFERENCES ai_explanations (id)
);

CREATE TABLE explanation_items (
    id UUID PRIMARY KEY,
    section_id UUID NOT NULL,
    extracted_item_id UUID,
    label VARCHAR(200) NOT NULL,
    display_value VARCHAR(500),
    unit VARCHAR(50),
    has_source BOOLEAN NOT NULL DEFAULT FALSE,
    item_order INTEGER NOT NULL,
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT explanation_items_order_key UNIQUE (section_id, item_order),
    CONSTRAINT explanation_items_section_fk FOREIGN KEY (section_id) REFERENCES explanation_sections (id),
    CONSTRAINT explanation_items_extracted_item_fk FOREIGN KEY (extracted_item_id) REFERENCES extracted_items (id)
);

CREATE TABLE ai_claims (
    id UUID PRIMARY KEY,
    ai_run_id UUID NOT NULL,
    claim_index INTEGER NOT NULL,
    claim_text TEXT NOT NULL,
    claim_type VARCHAR(30) NOT NULL,
    validation_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    confidence DECIMAL(5, 4),
    resolution VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ai_claims_index_key UNIQUE (ai_run_id, claim_index),
    CONSTRAINT ai_claims_validation_check CHECK (validation_status IN ('PENDING', 'SUPPORTED', 'UNSUPPORTED', 'CONFLICTED', 'BLOCKED')),
    CONSTRAINT ai_claims_resolution_check CHECK (resolution IN ('PENDING', 'EXPOSE', 'REMOVE', 'REGENERATE')),
    CONSTRAINT ai_claims_confidence_check CHECK (confidence IS NULL OR confidence BETWEEN 0 AND 1),
    CONSTRAINT ai_claims_run_fk FOREIGN KEY (ai_run_id) REFERENCES ai_runs (id)
);

CREATE TABLE claim_evidence (
    id UUID PRIMARY KEY,
    claim_id UUID NOT NULL,
    chunk_id UUID,
    extracted_item_id UUID,
    relation_type VARCHAR(20) NOT NULL,
    support_score DECIMAL(5, 4),
    quoted_text TEXT,
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT claim_evidence_relation_check CHECK (relation_type IN ('SUPPORTS', 'CONTRADICTS')),
    CONSTRAINT claim_evidence_source_check CHECK (chunk_id IS NOT NULL OR extracted_item_id IS NOT NULL),
    CONSTRAINT claim_evidence_score_check CHECK (support_score IS NULL OR support_score BETWEEN 0 AND 1),
    CONSTRAINT claim_evidence_claim_fk FOREIGN KEY (claim_id) REFERENCES ai_claims (id),
    CONSTRAINT claim_evidence_chunk_fk FOREIGN KEY (chunk_id) REFERENCES document_chunks (id),
    CONSTRAINT claim_evidence_item_fk FOREIGN KEY (extracted_item_id) REFERENCES extracted_items (id)
);

CREATE TABLE explanation_citations (
    id UUID PRIMARY KEY,
    explanation_id UUID NOT NULL,
    claim_id UUID,
    explanation_item_id UUID,
    page_id UUID,
    chunk_id UUID,
    quoted_text TEXT,
    anchor_id VARCHAR(200),
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT explanation_citations_source_check CHECK (page_id IS NOT NULL OR chunk_id IS NOT NULL),
    CONSTRAINT explanation_citations_explanation_fk FOREIGN KEY (explanation_id) REFERENCES ai_explanations (id),
    CONSTRAINT explanation_citations_claim_fk FOREIGN KEY (claim_id) REFERENCES ai_claims (id),
    CONSTRAINT explanation_citations_item_fk FOREIGN KEY (explanation_item_id) REFERENCES explanation_items (id),
    CONSTRAINT explanation_citations_page_fk FOREIGN KEY (page_id) REFERENCES document_pages (id),
    CONSTRAINT explanation_citations_chunk_fk FOREIGN KEY (chunk_id) REFERENCES document_chunks (id)
);

CREATE TABLE action_items (
    id UUID PRIMARY KEY,
    visit_id UUID NOT NULL,
    document_id UUID NOT NULL,
    type VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    due_at TIMESTAMPTZ(6),
    approval_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approved_by_user_id UUID,
    approved_at TIMESTAMPTZ(6),
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancel_reason VARCHAR(100),
    CONSTRAINT action_items_type_check CHECK (type IN ('MEDICATION', 'REVISIT', 'TEST', 'CAUTION', 'OTHER')),
    CONSTRAINT action_items_status_check CHECK (approval_status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELED')),
    CONSTRAINT action_items_visit_fk FOREIGN KEY (visit_id) REFERENCES visits (id),
    CONSTRAINT action_items_document_fk FOREIGN KEY (document_id) REFERENCES documents (id),
    CONSTRAINT action_items_approver_fk FOREIGN KEY (approved_by_user_id) REFERENCES users (id)
);

CREATE TABLE schedules (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL,
    action_item_id UUID NOT NULL,
    schedule_type VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    starts_at TIMESTAMPTZ(6) NOT NULL,
    ends_at TIMESTAMPTZ(6),
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT schedules_type_check CHECK (schedule_type IN ('MEDICATION', 'REVISIT', 'TEST', 'PERSONAL', 'OTHER')),
    CONSTRAINT schedules_status_check CHECK (status IN ('SCHEDULED', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT schedules_patient_fk FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT schedules_action_item_fk FOREIGN KEY (action_item_id) REFERENCES action_items (id)
);

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    schedule_id UUID NOT NULL,
    recipient_user_id UUID NOT NULL,
    channel VARCHAR(20) NOT NULL,
    scheduled_at TIMESTAMPTZ(6) NOT NULL,
    sent_at TIMESTAMPTZ(6),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    failure_reason TEXT,
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT notifications_channel_check CHECK (channel IN ('PUSH', 'SMS', 'EMAIL', 'IN_APP')),
    CONSTRAINT notifications_status_check CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'CANCELLED')),
    CONSTRAINT notifications_schedule_fk FOREIGN KEY (schedule_id) REFERENCES schedules (id),
    CONSTRAINT notifications_recipient_fk FOREIGN KEY (recipient_user_id) REFERENCES users (id)
);

CREATE TABLE visit_participants (
    visit_id UUID NOT NULL,
    user_id UUID NOT NULL,
    participant_role VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (visit_id, user_id),
    CONSTRAINT visit_participants_role_check CHECK (participant_role IN ('PATIENT', 'CAREGIVER', 'COMPANION')),
    CONSTRAINT visit_participants_visit_fk FOREIGN KEY (visit_id) REFERENCES visits (id),
    CONSTRAINT visit_participants_user_fk FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE consents (
    id UUID PRIMARY KEY,
    caregiver_link_id UUID NOT NULL,
    scope VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    granted_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ(6),
    revoked_at TIMESTAMPTZ(6),
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT consents_scope_check CHECK (scope IN ('RECORD', 'DOCUMENT', 'SUMMARY', 'CHAT')),
    CONSTRAINT consents_status_check CHECK (status IN ('ACTIVE', 'EXPIRED', 'REVOKED')),
    CONSTRAINT consents_caregiver_link_fk FOREIGN KEY (caregiver_link_id) REFERENCES caregiver_links (id)
);

CREATE TABLE chat_sessions (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL,
    started_by_user_id UUID NOT NULL,
    active_visit_id UUID,
    title VARCHAR(200),
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMPTZ(6),
    CONSTRAINT chat_sessions_patient_fk FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT chat_sessions_user_fk FOREIGN KEY (started_by_user_id) REFERENCES users (id),
    CONSTRAINT chat_sessions_visit_fk FOREIGN KEY (active_visit_id) REFERENCES visits (id)
);

CREATE TABLE chat_messages (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    intent VARCHAR(30),
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chat_messages_role_check CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM')),
    CONSTRAINT chat_messages_intent_check CHECK (intent IS NULL OR intent IN ('EXPLANATION', 'COMPARISON', 'SCHEDULE', 'RECORD_LOOKUP', 'MEDICAL_JUDGMENT', 'OTHER')),
    CONSTRAINT chat_messages_session_fk FOREIGN KEY (session_id) REFERENCES chat_sessions (id)
);

CREATE TABLE retrieval_results (
    id UUID PRIMARY KEY,
    ai_run_id UUID NOT NULL,
    chunk_id UUID NOT NULL,
    rank_no INTEGER NOT NULL,
    similarity_score DECIMAL(7, 6) NOT NULL,
    permission_passed BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT retrieval_results_run_fk FOREIGN KEY (ai_run_id) REFERENCES ai_runs (id),
    CONSTRAINT retrieval_results_chunk_fk FOREIGN KEY (chunk_id) REFERENCES document_chunks (id)
);

CREATE TABLE embeddings (
    id UUID PRIMARY KEY,
    source_type VARCHAR(30) NOT NULL,
    source_id UUID NOT NULL,
    patient_id UUID NOT NULL,
    visit_id UUID,
    embedding vector(768) NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    dimensions INTEGER NOT NULL DEFAULT 768,
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT embeddings_source_type_check CHECK (source_type IN ('DOCUMENT_CHUNK', 'HEALTH_RECORD')),
    CONSTRAINT embeddings_dimensions_check CHECK (dimensions = 768),
    CONSTRAINT embeddings_patient_fk FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT embeddings_visit_fk FOREIGN KEY (visit_id) REFERENCES visits (id)
);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    actor_user_id UUID,
    patient_id UUID,
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id UUID,
    result VARCHAR(20) NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT audit_logs_result_check CHECK (result IN ('SUCCESS', 'DENIED', 'FAILED')),
    CONSTRAINT audit_logs_actor_fk FOREIGN KEY (actor_user_id) REFERENCES users (id),
    CONSTRAINT audit_logs_patient_fk FOREIGN KEY (patient_id) REFERENCES patients (id)
);

CREATE INDEX patients_user_idx ON patients (user_id);
CREATE INDEX visits_patient_visited_idx ON visits (patient_id, visited_on DESC);
CREATE INDEX documents_visit_created_idx ON documents (visit_id, created_at DESC);
CREATE INDEX documents_status_created_idx ON documents (status, created_at DESC);
CREATE INDEX analyses_document_status_idx ON document_analyses (document_id, status);
CREATE INDEX pages_document_status_idx ON document_pages (document_id, ocr_status);
CREATE INDEX chunks_analysis_idx ON document_chunks (analysis_id, chunk_index);
CREATE INDEX extracted_items_analysis_type_idx ON extracted_items (analysis_id, item_type);
CREATE INDEX ai_runs_status_created_idx ON ai_runs (status, created_at DESC);
CREATE INDEX explanations_document_idx ON ai_explanations (document_id);
CREATE INDEX explanation_citations_explanation_idx ON explanation_citations (explanation_id);
CREATE INDEX action_items_visit_status_idx ON action_items (visit_id, approval_status);
CREATE INDEX schedules_patient_start_idx ON schedules (patient_id, starts_at);
CREATE INDEX chat_messages_session_created_idx ON chat_messages (session_id, created_at);
CREATE INDEX retrieval_results_run_rank_idx ON retrieval_results (ai_run_id, rank_no);
CREATE INDEX embeddings_patient_visit_idx ON embeddings (patient_id, visit_id);

CREATE INDEX embeddings_vector_idx
    ON embeddings USING hnsw (embedding vector_cosine_ops);
