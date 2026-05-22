CREATE TABLE program_enrollments (
    id                  UUID PRIMARY KEY,
    user_id             VARCHAR(255) NOT NULL,
    program_id          UUID NOT NULL,
    program_name        VARCHAR(500) NOT NULL,
    current_week        INT NOT NULL DEFAULT 1,
    current_day         INT NOT NULL DEFAULT 1,
    total_weeks         INT NOT NULL,
    total_days_per_week INT NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    enrolled_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at        TIMESTAMP WITH TIME ZONE,

    CONSTRAINT chk_enrollment_status CHECK (status IN ('ACTIVE', 'COMPLETED', 'REPLACED'))
);

CREATE INDEX idx_enrollments_user_id ON program_enrollments(user_id);
CREATE INDEX idx_enrollments_user_status ON program_enrollments(user_id, status);
