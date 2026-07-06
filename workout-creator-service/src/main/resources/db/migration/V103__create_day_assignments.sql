CREATE TABLE day_assignments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    program_id      UUID         NOT NULL REFERENCES programs (id) ON DELETE CASCADE,
    day_number      INTEGER      NOT NULL,
    assignment_type VARCHAR(20)  NOT NULL,
    workout_id      UUID,
    activity_type   VARCHAR(100),
    UNIQUE (program_id, day_number)
);

CREATE INDEX idx_day_assignments_program ON day_assignments (program_id);
