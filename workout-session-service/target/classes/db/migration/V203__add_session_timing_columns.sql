ALTER TABLE sessions ADD COLUMN total_paused_seconds BIGINT NOT NULL DEFAULT 0;
ALTER TABLE sessions ADD COLUMN duration_seconds INTEGER;
