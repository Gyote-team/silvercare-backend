-- 수동 일정과 한 후보의 여러 복약 시간대·반복 규칙을 지원한다.
ALTER TABLE schedules ALTER COLUMN action_item_id DROP NOT NULL;
ALTER TABLE schedules ADD COLUMN slot_index INTEGER NOT NULL DEFAULT 0;
WITH numbered AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY action_item_id ORDER BY created_at, id) - 1 AS slot
    FROM schedules WHERE action_item_id IS NOT NULL
)
UPDATE schedules SET slot_index = numbered.slot FROM numbered WHERE schedules.id = numbered.id;
CREATE UNIQUE INDEX ux_schedules_candidate_slot ON schedules(action_item_id, slot_index);
ALTER TABLE schedules ADD COLUMN repeat_frequency VARCHAR(10) NOT NULL DEFAULT 'NONE';
ALTER TABLE schedules ADD COLUMN repeat_interval INTEGER NOT NULL DEFAULT 1;
ALTER TABLE schedules ADD COLUMN repeat_until DATE;
ALTER TABLE schedules ADD COLUMN deleted_at TIMESTAMPTZ(6);
ALTER TABLE schedules ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE schedules ADD CONSTRAINT schedules_repeat_check CHECK (
    (repeat_frequency = 'NONE' AND repeat_interval = 1 AND repeat_until IS NULL)
    OR (schedule_type = 'MEDICATION' AND repeat_frequency IN ('DAILY','WEEKLY')
        AND repeat_interval BETWEEN 1 AND 30 AND repeat_until IS NOT NULL)
);
CREATE INDEX idx_schedules_patient_start ON schedules(patient_id, starts_at, id) WHERE deleted_at IS NULL;
