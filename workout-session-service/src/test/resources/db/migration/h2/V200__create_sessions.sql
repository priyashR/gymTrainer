CREATE TABLE sessions (
    id                    UUID PRIMARY KEY,
    user_id               VARCHAR(255) NOT NULL,
    program_id            UUID,
    enrollment_id         UUID,
    week_number           INT NOT NULL,
    day_number            INT NOT NULL,
    status                VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    current_section_index INT NOT NULL DEFAULT 0,
    workout_snapshot      CLOB NOT NULL,
    section_progresses    CLOB NOT NULL DEFAULT '[]',
    started_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    paused_at             TIMESTAMP WITH TIME ZONE,
    completed_at          TIMESTAMP WITH TIME ZONE,
    last_persisted_at     TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT chk_session_status CHECK (status IN ('IN_PROGRESS', 'PAUSED', 'COMPLETED'))
);

CREATE INDEX idx_sessions_user_id ON sessions(user_id);
CREATE INDEX idx_sessions_user_status ON sessions(user_id, status);
