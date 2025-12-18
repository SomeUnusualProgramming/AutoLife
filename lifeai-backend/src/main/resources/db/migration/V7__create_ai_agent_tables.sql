-- AI Agent Clarification Tables
-- Phase 1: Pending Events and Clarification Sessions

-- Table for pending clarification events
CREATE TABLE pending_clarification_events (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    session_id UUID NOT NULL UNIQUE,
    event_type VARCHAR(50) NOT NULL,
    raw_input TEXT NOT NULL,
    parsed_data JSONB,
    confidence_score FLOAT,
    status VARCHAR(50) DEFAULT 'PENDING_CLARIFICATION',
    clarification_round INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table for clarification sessions
CREATE TABLE clarification_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL REFERENCES users(id),
    pending_event_id BIGINT REFERENCES pending_clarification_events(id),
    conversation_context JSONB,
    total_rounds INT DEFAULT 0,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

-- Table for clarification Q&A rounds
CREATE TABLE clarification_qa_rounds (
    id BIGSERIAL PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES clarification_sessions(id),
    round_number INT,
    ai_question TEXT,
    user_response TEXT,
    ai_analysis JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX idx_pending_events_user_id ON pending_clarification_events(user_id);
CREATE INDEX idx_pending_events_session_id ON pending_clarification_events(session_id);
CREATE INDEX idx_pending_events_status ON pending_clarification_events(status);
CREATE INDEX idx_clarification_sessions_user_id ON clarification_sessions(user_id);
CREATE INDEX idx_clarification_sessions_status ON clarification_sessions(status);
CREATE INDEX idx_clarification_qa_session_id ON clarification_qa_rounds(session_id);
