-- 기능명세서의 초대 코드 및 관계 상태를 새 ERD 구조에 반영한다.
ALTER TABLE patients
    ADD COLUMN invite_code VARCHAR(16);

UPDATE patients
SET invite_code = UPPER(SUBSTRING(REPLACE(id::text, '-', '') FROM 1 FOR 16))
WHERE invite_code IS NULL;

ALTER TABLE patients
    ADD CONSTRAINT patients_invite_code_key UNIQUE (invite_code);

ALTER TABLE caregiver_links
    RENAME COLUMN invited_at TO requested_at;

ALTER TABLE caregiver_links
    RENAME COLUMN revoked_at TO ended_at;

ALTER TABLE caregiver_links
    DROP CONSTRAINT caregiver_links_status_check;

ALTER TABLE caregiver_links
    ALTER COLUMN status SET DEFAULT 'REQUESTED';

UPDATE caregiver_links
SET status = 'REQUESTED'
WHERE status = 'INVITED';

ALTER TABLE caregiver_links
    ADD CONSTRAINT caregiver_links_status_check
        CHECK (status IN ('REQUESTED', 'ACTIVE', 'REJECTED', 'CANCELED', 'REVOKED'));

ALTER TABLE patients
    ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE patients
    ALTER COLUMN invite_code SET NOT NULL;
