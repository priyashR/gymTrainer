-- V104: Add copied_day support to day_assignments table
-- Widens assignment_type to accommodate COPIED_DAY enum value.
-- Adds snapshot_data (JSONB) for storing copied day structure and
-- provenance columns for tracking the source program/week/day.
-- source_program_id intentionally has NO foreign key to programs table
-- to ensure snapshot independence from the source program lifecycle.

ALTER TABLE day_assignments
    ALTER COLUMN assignment_type TYPE VARCHAR(30);

ALTER TABLE day_assignments
    ADD COLUMN snapshot_data JSONB,
    ADD COLUMN source_program_id UUID,
    ADD COLUMN source_week_number INTEGER,
    ADD COLUMN source_day_number INTEGER;

COMMENT ON COLUMN day_assignments.snapshot_data IS 'JSON snapshot of the copied day structure (label, focus area, modality, warm-up, sections, exercises, cool-down). Null for activity-type assignments.';
COMMENT ON COLUMN day_assignments.source_program_id IS 'UUID of the program from which the day was copied. Provenance only — no FK constraint.';
COMMENT ON COLUMN day_assignments.source_week_number IS 'Week number in the source program at time of copy.';
COMMENT ON COLUMN day_assignments.source_day_number IS 'Day number within the source week at time of copy.';
