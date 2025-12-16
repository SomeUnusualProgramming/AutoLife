CREATE TABLE calendar_entries (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    event_id BIGINT,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    type VARCHAR(50) NOT NULL DEFAULT 'EVENT',
    reminder BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_calendar_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_calendar_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE SET NULL,
    CONSTRAINT check_valid_calendar_type CHECK (type IN ('EVENT', 'APPOINTMENT', 'MEDICAL', 'REMINDER'))
);

CREATE INDEX idx_cal_user_id ON calendar_entries(user_id);
CREATE INDEX idx_cal_event_id ON calendar_entries(event_id);
CREATE INDEX idx_cal_date ON calendar_entries(start_date);
