import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { DayTilesGrid } from "../features/program/DayTilesGrid";
import { DayAssignmentModal } from "../features/program/DayAssignmentModal";
import { DayAssignment } from "../features/program/DayTile";
import apiClient from "../lib/apiClient";

interface CreateProgramState {
  programName: string;
  days: DayAssignment[];
  activeDayIndex: number | null;
  saving: boolean;
  error: string | null;
}

const INITIAL_DAYS: DayAssignment[] = [
  { type: null },
  { type: null },
  { type: null },
];

const styles: Record<string, React.CSSProperties> = {
  page: {
    display: "flex",
    flexDirection: "column",
    gap: "var(--spacing-lg)",
    padding: "var(--spacing-lg)",
    maxWidth: "800px",
    margin: "0 auto",
    width: "100%",
  },
  backLink: {
    display: "inline-flex",
    alignItems: "center",
    gap: "var(--spacing-xs)",
    color: "var(--color-accent)",
    textDecoration: "none",
    fontSize: "15px",
    fontWeight: 500,
    minHeight: "var(--tap-target-min)",
    transition: "color 0.15s ease",
  },
  heading: {
    fontSize: "24px",
    fontWeight: 700,
    color: "var(--color-text-primary)",
    margin: 0,
  },
  inputGroup: {
    display: "flex",
    flexDirection: "column",
    gap: "var(--spacing-xs)",
  },
  label: {
    fontSize: "14px",
    fontWeight: 500,
    color: "var(--color-text-secondary)",
  },
  input: {
    width: "100%",
    padding: "var(--spacing-sm) var(--spacing-md)",
    borderRadius: "var(--radius-md)",
    border: "1px solid var(--color-border)",
    background: "var(--color-bg-surface)",
    color: "var(--color-text-primary)",
    fontSize: "16px",
    minHeight: "var(--tap-target-min)",
    outline: "none",
    transition: "border-color 0.15s ease",
    boxSizing: "border-box",
  },
  inputError: {
    width: "100%",
    padding: "var(--spacing-sm) var(--spacing-md)",
    borderRadius: "var(--radius-md)",
    border: "1px solid var(--color-error)",
    background: "var(--color-bg-surface)",
    color: "var(--color-text-primary)",
    fontSize: "16px",
    minHeight: "var(--tap-target-min)",
    outline: "none",
    transition: "border-color 0.15s ease",
    boxSizing: "border-box",
  },
  validationError: {
    fontSize: "13px",
    color: "var(--color-error)",
    margin: 0,
  },
  sectionTitle: {
    fontSize: "18px",
    fontWeight: 600,
    color: "var(--color-text-primary)",
    margin: 0,
  },
  saveButton: {
    width: "100%",
    padding: "var(--spacing-md) var(--spacing-lg)",
    borderRadius: "var(--radius-md)",
    border: "none",
    background: "var(--color-accent)",
    color: "var(--color-fab-text)",
    fontSize: "16px",
    fontWeight: 600,
    cursor: "pointer",
    minHeight: "var(--tap-target-preferred)",
    transition: "background 0.15s ease, opacity 0.15s ease",
  },
  saveButtonDisabled: {
    width: "100%",
    padding: "var(--spacing-md) var(--spacing-lg)",
    borderRadius: "var(--radius-md)",
    border: "none",
    background: "var(--color-accent)",
    color: "var(--color-fab-text)",
    fontSize: "16px",
    fontWeight: 600,
    cursor: "not-allowed",
    minHeight: "var(--tap-target-preferred)",
    opacity: 0.6,
  },
  errorBanner: {
    padding: "var(--spacing-sm) var(--spacing-md)",
    borderRadius: "var(--radius-md)",
    border: "1px solid var(--color-error)",
    background: "rgba(239, 83, 80, 0.1)",
    color: "var(--color-error)",
    fontSize: "14px",
    fontWeight: 500,
  },
};

export const CreateProgramPage: React.FC = () => {
  const navigate = useNavigate();

  const [state, setState] = useState<CreateProgramState>({
    programName: "",
    days: [...INITIAL_DAYS],
    activeDayIndex: null,
    saving: false,
    error: null,
  });

  const [nameError, setNameError] = useState<string | null>(null);
  const [daysError, setDaysError] = useState<string | null>(null);

  const handleNameChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setState((prev) => ({ ...prev, programName: e.target.value }));
    if (nameError) {
      setNameError(null);
    }
  };

  const handleDayTap = (index: number) => {
    setState((prev) => ({ ...prev, activeDayIndex: index }));
  };

  const handleAddDay = () => {
    setState((prev) => ({
      ...prev,
      days: [...prev.days, { type: null }],
    }));
  };

  const handleRemoveDay = (index: number) => {
    setState((prev) => ({
      ...prev,
      days: prev.days.filter((_, i) => i !== index),
    }));
    if (daysError) {
      setDaysError(null);
    }
  };

  const handleAssignment = (assignment: DayAssignment) => {
    setState((prev) => {
      if (prev.activeDayIndex === null) return prev;
      const updatedDays = [...prev.days];
      updatedDays[prev.activeDayIndex] = assignment;
      return {
        ...prev,
        days: updatedDays,
        activeDayIndex: null,
      };
    });
    if (daysError) {
      setDaysError(null);
    }
  };

  const handleModalClose = () => {
    setState((prev) => ({ ...prev, activeDayIndex: null }));
  };

  const validate = (): boolean => {
    let valid = true;

    if (!state.programName.trim()) {
      setNameError("Program name is required.");
      valid = false;
    } else {
      setNameError(null);
    }

    const hasAssignment = state.days.some((day) => day.type !== null);
    if (!hasAssignment) {
      setDaysError(
        "At least one day must have a workout or activity assigned."
      );
      valid = false;
    } else {
      setDaysError(null);
    }

    return valid;
  };

  const handleSave = async () => {
    if (!validate()) return;

    setState((prev) => ({ ...prev, saving: true, error: null }));

    const requestBody = {
      programName: state.programName.trim(),
      days: state.days
        .map((day, index) => {
          if (day.type === "workout") {
            return {
              dayNumber: index + 1,
              type: "workout" as const,
              workoutId: day.workoutId,
            };
          }
          if (day.type === "activity") {
            return {
              dayNumber: index + 1,
              type: "activity" as const,
              activityType: day.activityType,
            };
          }
          return null;
        })
        .filter(Boolean),
    };

    try {
      const response = await apiClient.post<{ id: string }>(
        "/vault/programs",
        requestBody
      );
      const programId = response.data.id;
      navigate(`/vault/programs/${programId}`);
    } catch (err: unknown) {
      let message = "Failed to save program. Please try again.";
      if (err && typeof err === "object" && "response" in err) {
        const axiosErr = err as { response?: { data?: { message?: string } } };
        if (axiosErr.response?.data?.message) {
          message = axiosErr.response.data.message;
        }
      }
      setState((prev) => ({ ...prev, saving: false, error: message }));
    }
  };

  return (
    <main style={styles.page} data-testid="create-program-page">
      {/* BackLink */}
      <Link to="/" style={styles.backLink} data-testid="back-link">
        ← Home
      </Link>

      <h1 style={styles.heading}>Create Program</h1>

      {/* Program Name Input */}
      <div style={styles.inputGroup}>
        <label htmlFor="program-name" style={styles.label}>
          Program Name
        </label>
        <input
          id="program-name"
          type="text"
          placeholder="e.g., PPL Hypertrophy"
          value={state.programName}
          onChange={handleNameChange}
          style={nameError ? styles.inputError : styles.input}
          aria-invalid={!!nameError}
          aria-describedby={nameError ? "program-name-error" : undefined}
          data-testid="program-name-input"
        />
        {nameError && (
          <p
            id="program-name-error"
            style={styles.validationError}
            role="alert"
            data-testid="program-name-error"
          >
            {nameError}
          </p>
        )}
      </div>

      {/* Day Tiles Grid */}
      <section>
        <h2 style={styles.sectionTitle}>Training Days</h2>
        <div style={{ marginTop: "var(--spacing-md)" }}>
          <DayTilesGrid
            days={state.days}
            onDayTap={handleDayTap}
            onAddDay={handleAddDay}
            onRemoveDay={handleRemoveDay}
          />
        </div>
        {daysError && (
          <p
            style={{ ...styles.validationError, marginTop: "var(--spacing-sm)" }}
            role="alert"
            data-testid="days-assignment-error"
          >
            {daysError}
          </p>
        )}
      </section>

      {/* Error Banner */}
      {state.error && (
        <div
          style={styles.errorBanner}
          role="alert"
          data-testid="save-error-banner"
        >
          {state.error}
        </div>
      )}

      {/* Save Button */}
      <button
        type="button"
        style={state.saving ? styles.saveButtonDisabled : styles.saveButton}
        onClick={handleSave}
        disabled={state.saving}
        aria-busy={state.saving}
        data-testid="save-program-button"
      >
        {state.saving ? "Saving…" : "Save Program"}
      </button>

      {/* Day Assignment Modal */}
      <DayAssignmentModal
        isOpen={state.activeDayIndex !== null}
        dayNumber={state.activeDayIndex !== null ? state.activeDayIndex + 1 : 0}
        onAssign={handleAssignment}
        onClose={handleModalClose}
      />
    </main>
  );
};

export default CreateProgramPage;
