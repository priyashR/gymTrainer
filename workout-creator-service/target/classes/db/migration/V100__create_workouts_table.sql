CREATE TABLE workouts (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL,
    name                VARCHAR(255) NOT NULL,
    description         TEXT,
    training_style      VARCHAR(50) NOT NULL,
    raw_gemini_response TEXT,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_workouts_user_id ON workouts(user_id);
