-- 다수 보호자가 같은 개인을 관리해도 기록이 섞이거나 재시도로 중복 저장되지 않도록 한다.
ALTER TABLE health_records
    ADD COLUMN idempotency_key VARCHAR(64),
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE UNIQUE INDEX health_records_patient_author_idempotency_key_idx
    ON health_records (patient_id, author_user_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE INDEX health_records_patient_recorded_idx
    ON health_records (patient_id, recorded_at DESC, id DESC)
    WHERE deleted_at IS NULL;

-- 문서 업로드 구현에서 원본 해시를 기록해 같은 파일의 중복 업로드를 감지할 수 있게 한다.
ALTER TABLE documents
    ADD COLUMN content_sha256 VARCHAR(64);

CREATE INDEX documents_patient_content_sha256_idx
    ON documents (patient_id, content_sha256)
    WHERE content_sha256 IS NOT NULL AND deleted_at IS NULL;

-- 일정 수정 시 낙관적 잠금을 적용할 기반 컬럼이다.
ALTER TABLE schedules
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
