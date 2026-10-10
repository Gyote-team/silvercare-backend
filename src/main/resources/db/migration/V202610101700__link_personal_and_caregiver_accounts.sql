-- 한 사람이 개인 계정과 보호자 계정을 따로 두고 전환할 수 있도록 같은 사람의 계정을 하나의 그룹으로 묶는다.
-- 기존 계정은 각자 자신의 id를 그룹 id로 쓴다.
ALTER TABLE users
    ADD COLUMN account_group_id UUID;

UPDATE users
SET account_group_id = id;

ALTER TABLE users
    ALTER COLUMN account_group_id SET NOT NULL;

CREATE INDEX users_account_group_idx ON users (account_group_id);

-- 한 사람은 역할마다 활성 계정을 하나만 가진다.
CREATE UNIQUE INDEX users_account_group_role_active_uidx
    ON users (account_group_id, role)
    WHERE status = 'ACTIVE' AND role IN ('PATIENT', 'CAREGIVER');
