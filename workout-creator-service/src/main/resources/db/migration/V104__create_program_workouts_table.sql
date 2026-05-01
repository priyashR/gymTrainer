CREATE TABLE program_workouts (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    program_id UUID NOT NULL REFERENCES programs(id) ON DELETE CASCADE,
    workout_id UUID NOT NULL REFERENCES workouts(id) ON DELETE CASCADE,
    day_number INT NOT NULL,
    UNIQUE (program_id, day_number)
);

CREATE INDEX idx_program_workouts_program_id ON program_workouts(program_id);
