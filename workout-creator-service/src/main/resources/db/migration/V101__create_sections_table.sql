CREATE TABLE sections (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workout_id            UUID NOT NULL REFERENCES workouts(id) ON DELETE CASCADE,
    name                  VARCHAR(255) NOT NULL,
    section_type          VARCHAR(50) NOT NULL,
    sort_order            INT NOT NULL,
    time_cap_minutes      INT,
    interval_seconds      INT,
    total_rounds          INT,
    work_interval_seconds INT,
    rest_interval_seconds INT,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_sections_workout_id ON sections(workout_id);
