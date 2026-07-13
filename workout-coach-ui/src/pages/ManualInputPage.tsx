import React, { useState } from "react";
import { Link } from "react-router-dom";
import { validateProgram, uploadProgram } from "../features/upload/uploadApi";
import type {
  ParsedProgram,
  UploadProgramResponse,
  ValidateUploadResponse,
} from "../types/upload";

// --- State Machine ---
type PageState =
  | "idle"
  | "editing"
  | "validating"
  | "preview"
  | "validation_error"
  | "uploading"
  | "success"
  | "upload_error";

type ActiveTab = "edit" | "preview";

interface ValidationError {
  field: string;
  message: string;
}

// --- Styles ---
const styles: Record<string, React.CSSProperties> = {
  page: {
    display: "flex",
    flexDirection: "column",
    gap: "var(--spacing-lg)",
    padding: "var(--spacing-lg)",
    maxWidth: "900px",
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
  tabRow: {
    display: "flex",
    gap: "0",
    borderBottom: "1px solid var(--color-border)",
  },
  tab: {
    padding: "var(--spacing-sm) var(--spacing-lg)",
    border: "none",
    background: "transparent",
    color: "var(--color-text-secondary)",
    fontSize: "15px",
    fontWeight: 500,
    cursor: "pointer",
    minHeight: "var(--tap-target-min)",
    borderBottom: "2px solid transparent",
    transition: "color 0.15s ease, border-color 0.15s ease",
  },
  tabActive: {
    padding: "var(--spacing-sm) var(--spacing-lg)",
    border: "none",
    background: "transparent",
    color: "var(--color-accent)",
    fontSize: "15px",
    fontWeight: 600,
    cursor: "pointer",
    minHeight: "var(--tap-target-min)",
    borderBottom: "2px solid var(--color-accent)",
    transition: "color 0.15s ease, border-color 0.15s ease",
  },
  editor: {
    width: "100%",
    minHeight: "400px",
    padding: "var(--spacing-md)",
    fontFamily: "var(--font-mono)",
    fontSize: "14px",
    lineHeight: "1.6",
    background: "var(--color-bg-editor)",
    color: "var(--color-text-primary)",
    border: "1px solid var(--color-border)",
    borderRadius: "var(--radius-md)",
    resize: "vertical",
    outline: "none",
    boxSizing: "border-box",
    transition: "border-color 0.15s ease",
  },
  editorError: {
    width: "100%",
    minHeight: "400px",
    padding: "var(--spacing-md)",
    fontFamily: "var(--font-mono)",
    fontSize: "14px",
    lineHeight: "1.6",
    background: "var(--color-bg-editor)",
    color: "var(--color-text-primary)",
    border: "1px solid var(--color-error)",
    borderRadius: "var(--radius-md)",
    resize: "vertical",
    outline: "none",
    boxSizing: "border-box",
    transition: "border-color 0.15s ease",
  },
  errorList: {
    display: "flex",
    flexDirection: "column",
    gap: "var(--spacing-xs)",
    padding: "var(--spacing-sm) var(--spacing-md)",
    borderRadius: "var(--radius-sm)",
    background: "rgba(239, 83, 80, 0.08)",
    border: "1px solid var(--color-error)",
  },
  errorItem: {
    fontSize: "13px",
    color: "var(--color-error)",
    margin: 0,
  },
  errorField: {
    fontWeight: 600,
    fontFamily: "var(--font-mono)",
  },
  buttonRow: {
    display: "flex",
    gap: "var(--spacing-md)",
    flexWrap: "wrap",
  },
  primaryButton: {
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
  primaryButtonDisabled: {
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
  secondaryButton: {
    padding: "var(--spacing-md) var(--spacing-lg)",
    borderRadius: "var(--radius-md)",
    border: "1px solid var(--color-border)",
    background: "transparent",
    color: "var(--color-text-primary)",
    fontSize: "16px",
    fontWeight: 500,
    cursor: "pointer",
    minHeight: "var(--tap-target-preferred)",
    transition: "background 0.15s ease, border-color 0.15s ease",
  },
  uploadButton: {
    padding: "var(--spacing-md) var(--spacing-lg)",
    borderRadius: "var(--radius-md)",
    border: "none",
    background: "var(--color-success)",
    color: "#fff",
    fontSize: "16px",
    fontWeight: 600,
    cursor: "pointer",
    minHeight: "var(--tap-target-preferred)",
    transition: "background 0.15s ease, opacity 0.15s ease",
  },
  uploadButtonDisabled: {
    padding: "var(--spacing-md) var(--spacing-lg)",
    borderRadius: "var(--radius-md)",
    border: "none",
    background: "var(--color-success)",
    color: "#fff",
    fontSize: "16px",
    fontWeight: 600,
    cursor: "not-allowed",
    minHeight: "var(--tap-target-preferred)",
    opacity: 0.6,
  },
  previewCard: {
    background: "var(--color-bg-surface)",
    border: "1px solid var(--color-border)",
    borderRadius: "var(--radius-md)",
    padding: "var(--spacing-lg)",
    display: "flex",
    flexDirection: "column",
    gap: "var(--spacing-md)",
  },
  previewTitle: {
    fontSize: "20px",
    fontWeight: 700,
    color: "var(--color-text-primary)",
    margin: 0,
  },
  previewMeta: {
    display: "grid",
    gridTemplateColumns: "auto 1fr",
    gap: "var(--spacing-xs) var(--spacing-md)",
    fontSize: "14px",
  },
  metaLabel: {
    fontWeight: 600,
    color: "var(--color-text-secondary)",
  },
  metaValue: {
    color: "var(--color-text-primary)",
    margin: 0,
  },
  sectionBlock: {
    borderLeft: "3px solid var(--color-accent)",
    paddingLeft: "var(--spacing-md)",
    marginTop: "var(--spacing-sm)",
  },
  sectionTitle: {
    fontSize: "15px",
    fontWeight: 600,
    color: "var(--color-text-primary)",
    margin: "0 0 var(--spacing-xs) 0",
  },
  exerciseRow: {
    fontSize: "13px",
    color: "var(--color-text-secondary)",
    margin: "2px 0",
    fontFamily: "var(--font-mono)",
  },
  successCard: {
    background: "rgba(102, 187, 106, 0.1)",
    border: "1px solid var(--color-success)",
    borderRadius: "var(--radius-md)",
    padding: "var(--spacing-lg)",
    display: "flex",
    flexDirection: "column",
    gap: "var(--spacing-md)",
    alignItems: "center",
    textAlign: "center",
  },
  successTitle: {
    fontSize: "20px",
    fontWeight: 700,
    color: "var(--color-success)",
    margin: 0,
  },
  successText: {
    fontSize: "15px",
    color: "var(--color-text-primary)",
    margin: 0,
  },
  vaultLink: {
    color: "var(--color-accent)",
    textDecoration: "underline",
    fontSize: "15px",
    fontWeight: 500,
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

// --- Preview Component ---
function ProgramPreviewCard({ program }: { program: ParsedProgram }) {
  const { program_metadata, program_structure } = program;

  return (
    <div style={styles.previewCard} data-testid="program-preview-card">
      <h2 style={styles.previewTitle}>{program_metadata.program_name}</h2>

      <div style={styles.previewMeta}>
        <span style={styles.metaLabel}>Goal</span>
        <span style={styles.metaValue}>{program_metadata.goal}</span>
        <span style={styles.metaLabel}>Duration</span>
        <span style={styles.metaValue}>
          {program_metadata.duration_weeks}{" "}
          {program_metadata.duration_weeks === 1 ? "week" : "weeks"}
        </span>
        <span style={styles.metaLabel}>Equipment</span>
        <span style={styles.metaValue}>
          {program_metadata.equipment_profile.join(", ")}
        </span>
      </div>

      {program_structure.map((week) => (
        <div key={week.week_number}>
          <h3
            style={{
              fontSize: "16px",
              fontWeight: 600,
              color: "var(--color-text-primary)",
              margin: "var(--spacing-sm) 0 var(--spacing-xs) 0",
            }}
          >
            Week {week.week_number}
          </h3>
          {week.days.map((day) => (
            <div key={day.day_number} style={styles.sectionBlock}>
              <p style={styles.sectionTitle}>
                Day {day.day_number}: {day.day_label} — {day.focus_area} (
                {day.modality})
              </p>
              {day.blocks.map((block, bi) => (
                <div key={bi} style={{ marginBottom: "var(--spacing-xs)" }}>
                  <p
                    style={{
                      fontSize: "13px",
                      fontWeight: 500,
                      color: "var(--color-text-secondary)",
                      margin: "var(--spacing-xs) 0 2px 0",
                    }}
                  >
                    {block.block_type} — {block.format}
                    {block.time_cap_minutes != null &&
                      ` (${block.time_cap_minutes} min cap)`}
                  </p>
                  {block.movements.map((m, mi) => (
                    <p key={mi} style={styles.exerciseRow}>
                      {m.exercise_name}: {m.prescribed_sets} × {m.prescribed_reps}
                      {m.prescribed_weight ? ` @ ${m.prescribed_weight}` : ""}
                    </p>
                  ))}
                </div>
              ))}
            </div>
          ))}
        </div>
      ))}
    </div>
  );
}

// --- Main Page Component ---
export const ManualInputPage: React.FC = () => {

  const [pageState, setPageState] = useState<PageState>("idle");
  const [activeTab, setActiveTab] = useState<ActiveTab>("edit");
  const [jsonContent, setJsonContent] = useState<string>("");
  const [validationErrors, setValidationErrors] = useState<ValidationError[]>(
    []
  );
  const [parseError, setParseError] = useState<string | null>(null);
  const [parsedProgram, setParsedProgram] = useState<ParsedProgram | null>(
    null
  );
  const [uploadResult, setUploadResult] =
    useState<UploadProgramResponse | null>(null);
  const [uploadError, setUploadError] = useState<string | null>(null);

  // --- Handlers ---

  const handleJsonChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setJsonContent(e.target.value);
    if (pageState === "idle" || pageState === "validation_error") {
      setPageState("editing");
    }
    // Clear errors as user types
    if (parseError) setParseError(null);
    if (validationErrors.length > 0) setValidationErrors([]);
  };

  const handleClear = () => {
    setJsonContent("");
    setPageState("idle");
    setValidationErrors([]);
    setParseError(null);
    setParsedProgram(null);
    setUploadError(null);
  };

  const handleValidate = async () => {
    // First check for JSON parse errors client-side
    setValidationErrors([]);
    setParseError(null);
    setUploadError(null);

    if (!jsonContent.trim()) {
      setParseError("JSON content cannot be empty.");
      setPageState("validation_error");
      return;
    }

    try {
      JSON.parse(jsonContent);
    } catch (err: unknown) {
      const message =
        err instanceof SyntaxError
          ? `JSON syntax error: ${err.message}`
          : "Invalid JSON format.";
      setParseError(message);
      setPageState("validation_error");
      return;
    }

    // Call validate endpoint
    setPageState("validating");

    try {
      const result: ValidateUploadResponse =
        await validateProgram(jsonContent);

      if (result.valid) {
        // Parse the JSON to display in preview
        const parsed: ParsedProgram = JSON.parse(jsonContent);
        setParsedProgram(parsed);
        setPageState("preview");
        setActiveTab("preview");
      } else {
        setValidationErrors(result.errors);
        setPageState("validation_error");
      }
    } catch (err: unknown) {
      let message = "Validation request failed. Please try again.";
      if (err && typeof err === "object" && "response" in err) {
        const axiosErr = err as {
          response?: { data?: { message?: string; errors?: ValidationError[] } };
        };
        if (axiosErr.response?.data?.errors) {
          setValidationErrors(axiosErr.response.data.errors);
          setPageState("validation_error");
          return;
        }
        if (axiosErr.response?.data?.message) {
          message = axiosErr.response.data.message;
        }
      }
      setParseError(message);
      setPageState("validation_error");
    }
  };

  const handleUpload = async () => {
    setPageState("uploading");
    setUploadError(null);

    try {
      const result = await uploadProgram(jsonContent);
      setUploadResult(result);
      setPageState("success");
    } catch (err: unknown) {
      let message = "Upload failed. Please try again.";
      if (err && typeof err === "object" && "response" in err) {
        const axiosErr = err as { response?: { data?: { message?: string } } };
        if (axiosErr.response?.data?.message) {
          message = axiosErr.response.data.message;
        }
      }
      setUploadError(message);
      setPageState("upload_error");
    }
  };

  const handleBackToEdit = () => {
    setActiveTab("edit");
    if (pageState === "upload_error") {
      setPageState("editing");
    } else {
      setPageState("editing");
    }
  };

  const handleTabSwitch = (tab: ActiveTab) => {
    if (tab === "preview" && pageState !== "preview" && pageState !== "uploading" && pageState !== "upload_error") {
      // Can't switch to preview without validating first
      return;
    }
    setActiveTab(tab);
  };

  // --- Computed ---
  const isValidating = pageState === "validating";
  const isUploading = pageState === "uploading";
  const hasErrors = validationErrors.length > 0 || !!parseError;
  const canSwitchToPreview =
    pageState === "preview" ||
    pageState === "uploading" ||
    pageState === "upload_error";

  // --- Render ---
  return (
    <main style={styles.page} data-testid="manual-input-page">
      {/* BackLink */}
      <Link to="/" style={styles.backLink} data-testid="back-link">
        ← Home
      </Link>

      <h1 style={styles.heading}>Manual JSON Input</h1>

      {/* Success State */}
      {pageState === "success" && uploadResult && (
        <div style={styles.successCard} data-testid="upload-success">
          <h2 style={styles.successTitle}>✓ Upload Successful</h2>
          <p style={styles.successText}>
            <strong>{uploadResult.programName}</strong> has been saved to your
            Vault.
          </p>
          <Link
            to={`/vault/programs/${uploadResult.id}`}
            style={styles.vaultLink}
            data-testid="vault-link"
          >
            View in Vault →
          </Link>
          <button
            type="button"
            style={styles.secondaryButton}
            onClick={() => {
              handleClear();
              setActiveTab("edit");
            }}
            data-testid="new-input-button"
          >
            Enter Another Program
          </button>
        </div>
      )}

      {/* Main Editor / Preview */}
      {pageState !== "success" && (
        <>
          {/* Mode Tabs */}
          <div style={styles.tabRow} role="tablist" aria-label="Editor modes">
            <button
              type="button"
              role="tab"
              aria-selected={activeTab === "edit"}
              style={activeTab === "edit" ? styles.tabActive : styles.tab}
              onClick={() => handleTabSwitch("edit")}
              data-testid="tab-edit"
            >
              Edit
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={activeTab === "preview"}
              aria-disabled={!canSwitchToPreview}
              style={activeTab === "preview" ? styles.tabActive : styles.tab}
              onClick={() => handleTabSwitch("preview")}
              data-testid="tab-preview"
            >
              Preview
            </button>
          </div>

          {/* Edit Tab Content */}
          {activeTab === "edit" && (
            <div
              style={{ display: "flex", flexDirection: "column", gap: "var(--spacing-md)" }}
              role="tabpanel"
              aria-label="JSON Editor"
              data-testid="editor-view"
            >
              {/* JSON Editor Textarea */}
              <label
                htmlFor="json-editor"
                style={{
                  fontSize: "14px",
                  fontWeight: 500,
                  color: "var(--color-text-secondary)",
                }}
              >
                Paste or type your workout JSON below
              </label>
              <textarea
                id="json-editor"
                value={jsonContent}
                onChange={handleJsonChange}
                disabled={isValidating}
                placeholder='{\n  "program_metadata": {\n    "program_name": "My Workout",\n    ...\n  },\n  "program_structure": [...]\n}'
                spellCheck={false}
                aria-invalid={hasErrors}
                aria-describedby={hasErrors ? "validation-errors" : undefined}
                style={hasErrors ? styles.editorError : styles.editor}
                data-testid="json-editor"
              />

              {/* Validation Errors (Req 8.5) */}
              {validationErrors.length > 0 && (
                <div
                  id="validation-errors"
                  style={styles.errorList}
                  role="alert"
                  data-testid="validation-errors"
                >
                  {validationErrors.map((err, i) => (
                    <p key={i} style={styles.errorItem}>
                      <span style={styles.errorField}>{err.field}</span>:{" "}
                      {err.message}
                    </p>
                  ))}
                </div>
              )}

              {/* Parse Error (Req 8.6) */}
              {parseError && (
                <div
                  id="validation-errors"
                  style={styles.errorList}
                  role="alert"
                  data-testid="parse-error"
                >
                  <p style={styles.errorItem}>{parseError}</p>
                </div>
              )}

              {/* Upload Error Banner (Req 8.10) */}
              {uploadError && (
                <div
                  style={styles.errorBanner}
                  role="alert"
                  data-testid="upload-error"
                >
                  {uploadError}
                </div>
              )}

              {/* Action Buttons */}
              <div style={styles.buttonRow}>
                <button
                  type="button"
                  style={
                    isValidating
                      ? styles.primaryButtonDisabled
                      : styles.primaryButton
                  }
                  onClick={handleValidate}
                  disabled={isValidating}
                  aria-busy={isValidating}
                  data-testid="validate-button"
                >
                  {isValidating ? "Validating…" : "Validate & Preview"}
                </button>
                <button
                  type="button"
                  style={styles.secondaryButton}
                  onClick={handleClear}
                  disabled={isValidating}
                  data-testid="clear-button"
                >
                  Clear
                </button>
              </div>
            </div>
          )}

          {/* Preview Tab Content (Req 8.4, 8.7) */}
          {activeTab === "preview" && parsedProgram && (
            <div
              style={{ display: "flex", flexDirection: "column", gap: "var(--spacing-md)" }}
              role="tabpanel"
              aria-label="Program Preview"
              data-testid="preview-view"
            >
              <ProgramPreviewCard program={parsedProgram} />

              {/* Upload Error in Preview (Req 8.10) */}
              {uploadError && (
                <div
                  style={styles.errorBanner}
                  role="alert"
                  data-testid="upload-error"
                >
                  {uploadError}
                </div>
              )}

              {/* Action Buttons (Req 8.7, 8.8, 8.11) */}
              <div style={styles.buttonRow}>
                <button
                  type="button"
                  style={
                    isUploading
                      ? styles.uploadButtonDisabled
                      : styles.uploadButton
                  }
                  onClick={handleUpload}
                  disabled={isUploading}
                  aria-busy={isUploading}
                  data-testid="upload-button"
                >
                  {isUploading ? "Uploading…" : "Upload to Vault"}
                </button>
                <button
                  type="button"
                  style={styles.secondaryButton}
                  onClick={handleBackToEdit}
                  disabled={isUploading}
                  data-testid="back-to-edit-button"
                >
                  ← Back to Edit
                </button>
              </div>
            </div>
          )}
        </>
      )}
    </main>
  );
};

export default ManualInputPage;
