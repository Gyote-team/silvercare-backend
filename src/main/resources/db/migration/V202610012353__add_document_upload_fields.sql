-- 업로드 중복 방지 키·사용자가 고른 문서 유형과 분석 재시도·실패 기록 필드를 추가한다.
ALTER TABLE documents
    ADD COLUMN idempotency_key VARCHAR(100);

ALTER TABLE documents
    ADD COLUMN declared_doc_type VARCHAR(30);

ALTER TABLE documents
    ADD CONSTRAINT documents_declared_doc_type_check CHECK (declared_doc_type IS NULL OR declared_doc_type IN ('LAB_RESULT', 'PRESCRIPTION', 'DIAGNOSIS', 'DISCHARGE_GUIDE', 'UNKNOWN'));

-- PostgreSQL은 NULL끼리 중복을 허용하므로 idempotency_key가 없는 기존 행에는 영향이 없다.
CREATE UNIQUE INDEX documents_uploader_idempotency_key_uidx ON documents (uploader_user_id, idempotency_key);

ALTER TABLE document_analyses
    ADD COLUMN retryable BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE document_analyses
    ADD COLUMN retry_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE document_analyses
    ADD COLUMN error_code VARCHAR(100);

ALTER TABLE document_analyses
    ADD COLUMN failed_step VARCHAR(100);
