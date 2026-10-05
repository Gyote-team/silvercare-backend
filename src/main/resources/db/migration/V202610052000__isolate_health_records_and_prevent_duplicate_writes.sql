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
