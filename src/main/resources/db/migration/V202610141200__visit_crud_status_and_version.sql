-- 기존 방문과 하위 자료 FK를 유지하며 API 상태값을 정렬한다.
ALTER TABLE visits DROP CONSTRAINT visits_status_check;
UPDATE visits SET status = CASE
    WHEN status = 'PLANNED' THEN 'SCHEDULED'
    WHEN status = 'COMPLETED' THEN 'DONE'
    WHEN status = 'CANCELLED' THEN 'CANCELED'
    WHEN status = 'OPEN' AND visited_on > (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul')::date THEN 'SCHEDULED'
    WHEN status = 'OPEN' THEN 'DONE'
    ELSE status END;
ALTER TABLE visits ALTER COLUMN status SET DEFAULT 'SCHEDULED';
ALTER TABLE visits ADD CONSTRAINT visits_status_check CHECK(status IN ('SCHEDULED','DONE','CANCELED'));
ALTER TABLE visits ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
CREATE INDEX visits_active_patient_date_idx ON visits(patient_id,visited_on DESC,id DESC) WHERE deleted_at IS NULL;
