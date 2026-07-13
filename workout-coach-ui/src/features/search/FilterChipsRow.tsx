import React from "react";
import { FilterChip } from "../../components/ui/FilterChip";

export interface SearchFilters {
  focusArea: string;
  modality: string;
  days: string;
}

interface FilterChipsRowProps {
  filters: SearchFilters;
  onChange: (filters: SearchFilters) => void;
}

const FOCUS_AREA_OPTIONS = ["Push", "Pull", "Legs", "Full Body", "Metcon"];
const MODALITY_OPTIONS = ["CrossFit", "Hypertrophy", "Strength"];
const DAYS_OPTIONS = ["3", "4", "5", "6", "7"];

const styles: Record<string, React.CSSProperties> = {
  row: {
    display: "flex",
    flexWrap: "wrap",
    gap: "var(--spacing-sm)",
    alignItems: "center",
  },
};

export const FilterChipsRow: React.FC<FilterChipsRowProps> = ({
  filters,
  onChange,
}) => {
  const handleFocusAreaChange = (value: string) => {
    onChange({ ...filters, focusArea: value });
  };

  const handleModalityChange = (value: string) => {
    onChange({ ...filters, modality: value });
  };

  const handleDaysChange = (value: string) => {
    onChange({ ...filters, days: value });
  };

  return (
    <div style={styles.row}>
      <FilterChip
        label="Focus Area"
        options={FOCUS_AREA_OPTIONS}
        value={filters.focusArea}
        onChange={handleFocusAreaChange}
        active={filters.focusArea !== ""}
      />
      <FilterChip
        label="Modality"
        options={MODALITY_OPTIONS}
        value={filters.modality}
        onChange={handleModalityChange}
        active={filters.modality !== ""}
      />
      <FilterChip
        label="Days"
        options={DAYS_OPTIONS}
        value={filters.days}
        onChange={handleDaysChange}
        active={filters.days !== ""}
      />
    </div>
  );
};

export default FilterChipsRow;
