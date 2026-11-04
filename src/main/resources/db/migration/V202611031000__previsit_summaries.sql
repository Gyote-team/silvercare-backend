-- 요청 시 허용된 건강기록 스냅샷과 비동기 요약 작업을 Spring이 소유한다.
CREATE TABLE previsit_summaries (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patients(id),
    requested_by_user_id UUID NOT NULL REFERENCES users(id),
    idempotency_key VARCHAR(100) NOT NULL,
    window_days INTEGER NOT NULL CHECK (window_days BETWEEN 1 AND 30),
    window_start TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    execution_id UUID,
    summary_id UUID UNIQUE,
    job_status VARCHAR(20) NOT NULL CHECK (job_status IN ('QUEUED','RUNNING','SUCCEEDED','FAILED')),
    result_status VARCHAR(20) CHECK (result_status IN ('COMPLETE','INSUFFICIENT')),
    summary TEXT,
    used_record_ids TEXT,
    error_code VARCHAR(60),
    retryable BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE(requested_by_user_id,idempotency_key),
    CHECK ((job_status = 'SUCCEEDED') = (summary_id IS NOT NULL))
);
CREATE TABLE previsit_summary_sources (
    id UUID PRIMARY KEY,
    request_id UUID NOT NULL REFERENCES previsit_summaries(id),
    health_record_id UUID NOT NULL REFERENCES health_records(id),
    record_version BIGINT NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL,
    content TEXT NOT NULL,
    UNIQUE(request_id,health_record_id)
);
CREATE INDEX previsit_summaries_queue_idx ON previsit_summaries(job_status,created_at);
CREATE INDEX previsit_summaries_patient_idx ON previsit_summaries(patient_id,created_at DESC);
