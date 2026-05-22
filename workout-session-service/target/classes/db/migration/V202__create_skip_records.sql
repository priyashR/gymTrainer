CREATE TABLE skip_records (
    id            UUID PRIMARY KEY,
    enrollment_id UUID NOT NULL REFERENCES program_enrollments(id),
    week_number   INT NOT NULL,
    day_number    INT NOT NULL,
    skipped_at    TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_skips_enrollment ON skip_records(enrollment_id);
