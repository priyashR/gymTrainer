CREATE TABLE programs (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL,
    name                VARCHAR(255) NOT NULL,
    description         TEXT,
    scope               VARCHAR(50) NOT NULL,
    training_styles     VARCHAR(255) NOT NULL,
    raw_gemini_response TEXT,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_programs_user_id ON programs(user_id);
