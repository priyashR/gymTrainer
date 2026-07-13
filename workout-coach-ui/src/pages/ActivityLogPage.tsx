import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ActivityTypeGrid } from "../features/activity/ActivityTypeGrid";
import {
  ActivityLogForm,
  type DistanceUnit,
} from "../features/activity/ActivityLogForm";
import { saveActivity } from "../hooks/useLocalActivityStore";
import type { ActivityLogEntry } from "../hooks/useLocalActivityStore";
import apiClient from "../lib/apiClient";

function getTodayISO(): string {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function generateId(): string {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 11)}`;
}

interface FormErrors {
  activityType?: string;
  date?: string;
}

const styles: Record<string, React.CSSProperties> = {
  page: {
    display: "flex",
    flexDirection: "column",
    gap: "var(--spacing-lg)",
    padding: "var(--spacing-lg)",
    maxWidth: "700px",
    margin: "0 auto",
    width: "100%",
    minHeight: "100vh",
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
  sectionLabel: {
    fontSize: "13px",
    fontWeight: 500,
    color: "var(--color-text-secondary)",
    margin: 0,
  },
  dateInput: {
    width: "100%",
    padding: "var(--spacing-sm) var(--spacing-md)",
    fontSize: "16px",
    fontFamily: "var(--font-sans)",
    background: "var(--color-bg-surface)",
    border: "1px solid var(--color-border)",
    borderRadius: "var(--radius-sm)",
    color: "var(--color-text-primary)",
    minHeight: "var(--tap-target-min)",
    boxSizing: "border-box",
  },
  dateInputError: {
    width: "100%",
    padding: "var(--spacing-sm) var(--spacing-md)",
    fontSize: "16px",
    fontFamily: "var(--font-sans)",
    background: "var(--color-bg-surface)",
    border: "1px solid var(--color-error)",
    borderRadius: "var(--radius-sm)",
    color: "var(--color-text-primary)",
    minHeight: "var(--tap-target-min)",
    boxSizing: "border-box",
  },
  fieldGroup: {
    display: "flex",
    flexDirection: "column",
    gap: "var(--spacing-xs)",
  },
  errorText: {
    fontSize: "13px",
    color: "var(--color-error)",
    margin: 0,
  },
  submitButton: {
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
  submitButtonDisabled: {
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
  successMessage: {
    padding: "var(--spacing-sm) var(--spacing-md)",
    borderRadius: "var(--radius-sm)",
    background: "rgba(102, 187, 106, 0.1)",
    border: "1px solid var(--color-success)",
    color: "var(--color-success)",
    fontSize: "14px",
    fontWeight: 500,
    textAlign: "center",
  },
  localStorageMessage: {
    padding: "var(--spacing-sm) var(--spacing-md)",
    borderRadius: "var(--radius-sm)",
    background: "rgba(255, 167, 38, 0.1)",
    border: "1px solid var(--color-warning)",
    color: "var(--color-warning)",
    fontSize: "14px",
    fontWeight: 500,
    textAlign: "center",
  },
};

export const ActivityLogPage: React.FC = () => {
  const navigate = useNavigate();

  // Form state
  const [selectedActivityType, setSelectedActivityType] = useState<
    string | null
  >(null);
  const [date, setDate] = useState<string>(getTodayISO());
  const [duration, setDuration] = useState<string>("");
  const [calories, setCalories] = useState<string>("");
  const [distance, setDistance] = useState<string>("");
  const [distanceUnit, setDistanceUnit] = useState<DistanceUnit>("km");
  const [notes, setNotes] = useState<string>("");

  // UI state
  const [errors, setErrors] = useState<FormErrors>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [statusMessage, setStatusMessage] = useState<{
    type: "success" | "local";
    text: string;
  } | null>(null);

  const validate = (): boolean => {
    const newErrors: FormErrors = {};

    if (!selectedActivityType) {
      newErrors.activityType = "Activity type is required";
    }
    if (!date) {
      newErrors.date = "Date is required";
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async () => {
    setStatusMessage(null);

    if (!validate()) {
      return;
    }

    setIsSubmitting(true);

    const entry: ActivityLogEntry = {
      id: generateId(),
      activityType: selectedActivityType!,
      date,
      durationMinutes: duration ? Number(duration) : undefined,
      calories: calories ? Number(calories) : undefined,
      distance: distance ? Number(distance) : undefined,
      distanceUnit: distance ? distanceUnit : undefined,
      notes: notes || undefined,
    };

    try {
      await apiClient.post("/activities", entry);
      setStatusMessage({
        type: "success",
        text: "Activity saved successfully!",
      });
      // Navigate to landing page on success
      setTimeout(() => {
        navigate("/");
      }, 500);
    } catch (err: unknown) {
      // Check if endpoint is unavailable (404 or network error)
      const isUnavailable =
        !err ||
        (typeof err === "object" &&
          "response" in err &&
          (err as { response?: { status?: number } }).response?.status ===
            404) ||
        (typeof err === "object" &&
          "code" in err &&
          (err as { code?: string }).code === "ERR_NETWORK");

      if (isUnavailable) {
        // Fallback: save to localStorage
        saveActivity(entry);
        setStatusMessage({
          type: "local",
          text: "Activity saved locally. It will sync when the service is available.",
        });
        // Navigate to landing page after showing message
        setTimeout(() => {
          navigate("/");
        }, 1500);
      } else {
        // Re-throw unexpected errors — could add error banner in future
        setIsSubmitting(false);
        setStatusMessage({
          type: "local",
          text: "An error occurred. Activity saved locally.",
        });
        saveActivity(entry);
        setTimeout(() => {
          navigate("/");
        }, 1500);
      }
    }
  };

  return (
    <main style={styles.page} data-testid="activity-log-page">
      {/* BackLink */}
      <Link to="/" style={styles.backLink} data-testid="back-link">
        ← Home
      </Link>

      <h1 style={styles.heading}>Log Activity</h1>

      {/* Activity Type Selection */}
      <div style={styles.fieldGroup}>
        <p style={styles.sectionLabel}>Activity Type *</p>
        <ActivityTypeGrid
          selectedType={selectedActivityType}
          onSelect={(type) => {
            setSelectedActivityType(type);
            if (errors.activityType) {
              setErrors((prev) => ({ ...prev, activityType: undefined }));
            }
          }}
        />
        {errors.activityType && (
          <p
            style={styles.errorText}
            role="alert"
            data-testid="error-activity-type"
          >
            {errors.activityType}
          </p>
        )}
      </div>

      {/* Date Picker */}
      <div style={styles.fieldGroup}>
        <label htmlFor="activity-date" style={styles.sectionLabel}>
          Date *
        </label>
        <input
          id="activity-date"
          type="date"
          value={date}
          max={getTodayISO()}
          onChange={(e) => {
            setDate(e.target.value);
            if (errors.date) {
              setErrors((prev) => ({ ...prev, date: undefined }));
            }
          }}
          style={errors.date ? styles.dateInputError : styles.dateInput}
          data-testid="date-picker"
          aria-label="Activity date"
        />
        {errors.date && (
          <p style={styles.errorText} role="alert" data-testid="error-date">
            {errors.date}
          </p>
        )}
      </div>

      {/* Optional Form Fields */}
      <ActivityLogForm
        duration={duration}
        calories={calories}
        distance={distance}
        distanceUnit={distanceUnit}
        notes={notes}
        onDurationChange={setDuration}
        onCaloriesChange={setCalories}
        onDistanceChange={setDistance}
        onDistanceUnitChange={setDistanceUnit}
        onNotesChange={setNotes}
      />

      {/* Status Messages */}
      {statusMessage && (
        <div
          style={
            statusMessage.type === "success"
              ? styles.successMessage
              : styles.localStorageMessage
          }
          role="status"
          data-testid={
            statusMessage.type === "success"
              ? "success-message"
              : "local-storage-message"
          }
        >
          {statusMessage.text}
        </div>
      )}

      {/* Submit Button */}
      <button
        type="button"
        style={isSubmitting ? styles.submitButtonDisabled : styles.submitButton}
        onClick={handleSubmit}
        disabled={isSubmitting}
        aria-busy={isSubmitting}
        data-testid="submit-button"
      >
        {isSubmitting ? "Saving…" : "Save Activity"}
      </button>
    </main>
  );
};

export default ActivityLogPage;
