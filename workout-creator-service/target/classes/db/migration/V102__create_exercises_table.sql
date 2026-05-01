CREATE TABLE exercises (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    section_id   UUID NOT NULL REFERENCES sections(id) ON DELETE CASCADE,
    name         VARCHAR(255) NOT NULL,
    sets         INT NOT NULL,
    reps         VARCHAR(50) NOT NULL,
    weight       VARCHAR(100),
    rest_seconds INT,
    sort_order   INT NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_exercises_section_id ON exercises(section_id);
