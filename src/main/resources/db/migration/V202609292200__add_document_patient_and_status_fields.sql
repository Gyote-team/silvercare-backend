-- 문서 목록·권한 조회용 patient_id와 응답 계약용 상태 필드를 추가한다.
ALTER TABLE documents
    ADD COLUMN patient_id UUID;

UPDATE documents d
SET patient_id = v.patient_id
FROM visits v
WHERE d.visit_id = v.id;

ALTER TABLE documents
    ALTER COLUMN patient_id SET NOT NULL;

ALTER TABLE documents
    ADD CONSTRAINT documents_patient_fk FOREIGN KEY (patient_id) REFERENCES patients (id);

ALTER TABLE documents
    ADD COLUMN status_changed_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE documents
    ADD COLUMN request_id VARCHAR(64);

CREATE INDEX documents_patient_created_idx ON documents (patient_id, created_at, id);
