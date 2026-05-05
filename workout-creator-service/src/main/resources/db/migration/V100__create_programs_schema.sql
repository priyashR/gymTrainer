-- V100: Create core Vault schema for workout-creator-service
-- Programs, Weeks, Days, Sections, Exercises, WarmCoolEntries

CREATE TABLE programs (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name             VARCHAR(255)             NOT NULL,
    duration_weeks   INTEGER                  NOT NULL,
    goal             VARCHAR(255)             NOT NULL,
    equipment_profile TEXT                    NOT NULL,
    owner_user_id    VARCHAR(255)             NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_programs_owner ON programs (owner_user_id);

CREATE TABLE weeks (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    program_id  UUID    NOT NULL REFERENCES programs (id) ON DELETE CASCADE,
    week_number INTEGER NOT NULL,
    UNIQUE (program_id, week_number)
);

CREATE INDEX idx_weeks_program ON weeks (program_id);

CREATE TABLE days (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    week_id            UUID         NOT NULL REFERENCES weeks (id) ON DELETE CASCADE,
    day_number         INTEGER      NOT NULL,
    day_label          VARCHAR(50)  NOT NULL,
    focus_area         VARCHAR(100) NOT NULL,
    modality           VARCHAR(20)  NOT NULL,
    methodology_source VARCHAR(255),
    UNIQUE (week_id, day_number)
);

CREATE INDEX idx_days_week ON days (week_id);

CREATE TABLE sections (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    day_id       UUID         NOT NULL REFERENCES days (id) ON DELETE CASCADE,
    name         VARCHAR(255) NOT NULL,
    section_type VARCHAR(20)  NOT NULL,
    format       VARCHAR(50),
    time_cap     INTEGER,
    sort_order   INTEGER      NOT NULL
);

CREATE INDEX idx_sections_day ON sections (day_id);

CREATE TABLE exercises (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    section_id           UUID         NOT NULL REFERENCES sections (id) ON DELETE CASCADE,
    exercise_name        VARCHAR(255) NOT NULL,
    modality_type        VARCHAR(20),
    prescribed_sets      INTEGER      NOT NULL,
    prescribed_reps      VARCHAR(50)  NOT NULL,
    prescribed_weight    VARCHAR(100),
    rest_interval_seconds INTEGER,
    notes                TEXT,
    sort_order           INTEGER      NOT NULL
);

CREATE INDEX idx_exercises_section ON exercises (section_id);

CREATE TABLE warm_cool_entries (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    day_id      UUID         NOT NULL REFERENCES days (id) ON DELETE CASCADE,
    entry_type  VARCHAR(10)  NOT NULL, -- 'WARM_UP' or 'COOL_DOWN'
    movement    VARCHAR(255) NOT NULL,
    instruction TEXT         NOT NULL,
    sort_order  INTEGER      NOT NULL
);

CREATE INDEX idx_warm_cool_day ON warm_cool_entries (day_id);
