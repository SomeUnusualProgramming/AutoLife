CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    calendar_id BIGINT,
    title VARCHAR(255) NOT NULL,
    message TEXT,
    scheduled_time TIMESTAMP NOT NULL,
    sent_at TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    type VARCHAR(50) NOT NULL DEFAULT 'REMINDER',
    minutes_before INTEGER DEFAULT 15,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_notification_calendar FOREIGN KEY (calendar_id) REFERENCES calendar_entries(id) ON DELETE SET NULL,
    CONSTRAINT check_valid_notification_status CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'CANCELLED')),
    CONSTRAINT check_valid_notification_type CHECK (type IN ('REMINDER', 'ALERT', 'EVENT_CREATED', 'SCHEDULED_CHECK'))
);

CREATE INDEX idx_notif_user_id ON notifications(user_id);
CREATE INDEX idx_notif_calendar_id ON notifications(calendar_id);
CREATE INDEX idx_notif_scheduled_time ON notifications(scheduled_time);
