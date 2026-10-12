-- 기존 기록을 보존하고 수정 충돌 감지 및 최신순 조회를 지원한다.
ALTER TABLE health_records ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
CREATE INDEX health_records_active_patient_created_idx
    ON health_records(patient_id, created_at DESC, id DESC) WHERE deleted_at IS NULL;
