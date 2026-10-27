-- Spring이 소유하는 음성 작업 큐. AI worker는 lease가 일치하는 미확정 결과만 갱신한다.
CREATE TABLE voice_transcriptions (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patients(id),
    uploader_user_id UUID NOT NULL REFERENCES users(id),
    visit_id UUID REFERENCES visits(id),
    purpose VARCHAR(20) NOT NULL CHECK (purpose IN ('HEALTH_RECORD','CONSULTATION')),
    idempotency_key VARCHAR(100) NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    storage_key VARCHAR(300) NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    size_bytes BIGINT NOT NULL CHECK (size_bytes > 0 AND size_bytes <= 20971520),
    consent_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('UPLOADED','PROCESSING','DRAFT_READY','CONFIRMED','FAILED','CANCELED')),
    draft_text TEXT,
    error_code VARCHAR(50),
    record_id UUID REFERENCES health_records(id),
    confirmed_text TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    attempts INTEGER NOT NULL DEFAULT 0,
    lease_token UUID,
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE(uploader_user_id,idempotency_key),
    CHECK (purpose <> 'CONSULTATION' OR visit_id IS NOT NULL),
    CHECK ((status = 'CONFIRMED') = (record_id IS NOT NULL)),
    CHECK (status <> 'DRAFT_READY' OR draft_text IS NOT NULL)
);
CREATE INDEX voice_transcriptions_queue_idx ON voice_transcriptions(status,updated_at);
CREATE INDEX voice_transcriptions_patient_idx ON voice_transcriptions(patient_id,purpose,created_at DESC);
CREATE INDEX voice_transcriptions_visit_idx ON voice_transcriptions(visit_id,created_at DESC);
