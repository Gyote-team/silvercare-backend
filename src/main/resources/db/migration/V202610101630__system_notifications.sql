-- 복약 발송용 notifications와 분리한 앱 내 시스템 활동 알림.
CREATE TABLE system_notifications (
    id UUID PRIMARY KEY,
    recipient_user_id UUID NOT NULL REFERENCES users(id),
    type VARCHAR(255) NOT NULL,
    message VARCHAR(255) NOT NULL,
    path VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ(6) NOT NULL,
    read_at TIMESTAMPTZ(6)
);
CREATE INDEX system_notifications_recipient_time_idx ON system_notifications(recipient_user_id,created_at DESC,id DESC);
CREATE INDEX system_notifications_unread_idx ON system_notifications(recipient_user_id) WHERE read_at IS NULL;
