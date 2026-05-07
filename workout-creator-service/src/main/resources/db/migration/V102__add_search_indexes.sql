-- V102: Add indexes for vault search and filter operations
-- Supports keyword search on name/goal and filtering by focus_area/modality

CREATE INDEX idx_programs_name_lower ON programs (LOWER(name));
CREATE INDEX idx_programs_goal_lower ON programs (LOWER(goal));
CREATE INDEX idx_days_focus_area_lower ON days (LOWER(focus_area));
CREATE INDEX idx_days_modality ON days (modality);
