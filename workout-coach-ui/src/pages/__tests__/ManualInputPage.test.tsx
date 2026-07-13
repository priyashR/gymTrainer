import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import ManualInputPage from "../ManualInputPage";

// Mock the uploadApi module
vi.mock("../../features/upload/uploadApi", () => ({
  validateProgram: vi.fn(),
  uploadProgram: vi.fn(),
}));

import { validateProgram, uploadProgram } from "../../features/upload/uploadApi";

// Valid JSON fixture matching ParsedProgram shape
const validProgramJson = JSON.stringify({
  program_metadata: {
    program_name: "Push Pull Legs",
    duration_weeks: 4,
    goal: "Hypertrophy",
    equipment_profile: ["Barbell", "Dumbbells"],
    version: "1.0",
  },
  program_structure: [
    {
      week_number: 1,
      days: [
        {
          day_number: 1,
          day_label: "Push A",
          focus_area: "Chest & Shoulders",
          modality: "Hypertrophy",
          warm_up: [],
          blocks: [
            {
              block_type: "Strength",
              format: "Standard",
              movements: [
                {
                  exercise_name: "Bench Press",
                  prescribed_sets: 4,
                  prescribed_reps: "8",
                  prescribed_weight: "80kg",
                },
              ],
            },
          ],
          cool_down: [],
        },
      ],
    },
  ],
});

function renderManualInputPage() {
  return render(
    <MemoryRouter>
      <ManualInputPage />
    </MemoryRouter>
  );
}

/** Helper to set textarea value (avoids userEvent keyboard parsing issues with braces). */
function setEditorValue(value: string) {
  const editor = screen.getByTestId("json-editor");
  fireEvent.change(editor, { target: { value } });
}

describe("ManualInputPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("Validate → Preview transition on valid JSON (Req 8.3, 8.4)", () => {
    it("transitions to preview mode when validation passes", async () => {
      const user = userEvent.setup();
      (validateProgram as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
        valid: true,
        errors: [],
      });

      renderManualInputPage();

      // Set valid JSON in the editor
      setEditorValue(validProgramJson);

      // Click validate button
      await user.click(screen.getByTestId("validate-button"));

      // Should transition to preview view
      await waitFor(() => {
        expect(screen.getByTestId("preview-view")).toBeInTheDocument();
      });

      // Preview should display the program name
      expect(screen.getByText("Push Pull Legs")).toBeInTheDocument();
    });
  });

  describe("Validate → Error display on invalid JSON (Req 8.5)", () => {
    it("displays field-level validation errors inline when validation fails", async () => {
      const user = userEvent.setup();
      (validateProgram as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
        valid: false,
        errors: [
          { field: "program_metadata.goal", message: "Goal is required" },
          { field: "program_structure[0].days[0].modality", message: "Invalid modality" },
        ],
      });

      renderManualInputPage();

      setEditorValue(validProgramJson);

      await user.click(screen.getByTestId("validate-button"));

      // Should display validation errors inline
      await waitFor(() => {
        expect(screen.getByTestId("validation-errors")).toBeInTheDocument();
      });

      expect(screen.getByText(/Goal is required/)).toBeInTheDocument();
      expect(screen.getByText(/Invalid modality/)).toBeInTheDocument();

      // Should NOT transition to preview
      expect(screen.queryByTestId("preview-view")).not.toBeInTheDocument();

      // Editor content should be preserved
      expect(screen.getByTestId("json-editor")).toHaveValue(validProgramJson);
    });
  });

  describe("JSON parse error displayed inline (Req 8.6)", () => {
    it("shows parse error when JSON is syntactically invalid", async () => {
      const user = userEvent.setup();
      renderManualInputPage();

      setEditorValue("{ invalid json }");

      await user.click(screen.getByTestId("validate-button"));

      // Should show parse error without calling the API
      expect(screen.getByTestId("parse-error")).toBeInTheDocument();
      expect(screen.getByTestId("parse-error")).toHaveTextContent("JSON syntax error");

      // Should NOT call validateProgram
      expect(validateProgram).not.toHaveBeenCalled();

      // Editor content preserved
      expect(screen.getByTestId("json-editor")).toHaveValue("{ invalid json }");
    });

    it("shows parse error when editor is empty", async () => {
      const user = userEvent.setup();
      renderManualInputPage();

      await user.click(screen.getByTestId("validate-button"));

      expect(screen.getByTestId("parse-error")).toBeInTheDocument();
      expect(screen.getByTestId("parse-error")).toHaveTextContent("cannot be empty");
      expect(validateProgram).not.toHaveBeenCalled();
    });
  });

  describe("Upload → Success with confirmation message (Req 8.8, 8.9)", () => {
    it("shows success card with program name and vault link after upload", async () => {
      const user = userEvent.setup();
      (validateProgram as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
        valid: true,
        errors: [],
      });
      (uploadProgram as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
        id: "program-123",
        programName: "Push Pull Legs",
        durationWeeks: 4,
        goal: "Hypertrophy",
        equipmentProfile: ["Barbell", "Dumbbells"],
        contentSource: "UPLOADED",
        createdAt: "2026-01-15T10:00:00Z",
      });

      renderManualInputPage();

      // Set JSON and validate
      setEditorValue(validProgramJson);
      await user.click(screen.getByTestId("validate-button"));

      // Wait for preview
      await waitFor(() => {
        expect(screen.getByTestId("preview-view")).toBeInTheDocument();
      });

      // Click upload
      await user.click(screen.getByTestId("upload-button"));

      // Should show success state
      await waitFor(() => {
        expect(screen.getByTestId("upload-success")).toBeInTheDocument();
      });

      expect(screen.getByText("Push Pull Legs")).toBeInTheDocument();
      expect(screen.getByTestId("vault-link")).toHaveAttribute(
        "href",
        "/vault/programs/program-123"
      );
    });
  });

  describe("Upload → Error preserves editor content (Req 8.10)", () => {
    it("shows error message and preserves JSON content on upload failure", async () => {
      const user = userEvent.setup();
      (validateProgram as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
        valid: true,
        errors: [],
      });
      (uploadProgram as ReturnType<typeof vi.fn>).mockRejectedValueOnce({
        response: { data: { message: "Server error: upload failed" } },
      });

      renderManualInputPage();

      // Set JSON and validate
      setEditorValue(validProgramJson);
      await user.click(screen.getByTestId("validate-button"));

      await waitFor(() => {
        expect(screen.getByTestId("preview-view")).toBeInTheDocument();
      });

      // Click upload
      await user.click(screen.getByTestId("upload-button"));

      // Should show upload error
      await waitFor(() => {
        expect(screen.getByTestId("upload-error")).toBeInTheDocument();
      });

      expect(screen.getByTestId("upload-error")).toHaveTextContent(
        "Server error: upload failed"
      );

      // Navigate back to edit and verify content is preserved
      await user.click(screen.getByTestId("back-to-edit-button"));

      expect(screen.getByTestId("json-editor")).toHaveValue(validProgramJson);
    });
  });

  describe("Upload button disabled during request (Req 8.11)", () => {
    it("disables the upload button while the upload request is in progress", async () => {
      const user = userEvent.setup();
      (validateProgram as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
        valid: true,
        errors: [],
      });

      let resolveUpload: (value: unknown) => void;
      (uploadProgram as ReturnType<typeof vi.fn>).mockReturnValueOnce(
        new Promise((resolve) => {
          resolveUpload = resolve;
        })
      );

      renderManualInputPage();

      // Set JSON and validate
      setEditorValue(validProgramJson);
      await user.click(screen.getByTestId("validate-button"));

      await waitFor(() => {
        expect(screen.getByTestId("preview-view")).toBeInTheDocument();
      });

      // Click upload
      await user.click(screen.getByTestId("upload-button"));

      // Upload button should be disabled while uploading
      expect(screen.getByTestId("upload-button")).toBeDisabled();
      expect(screen.getByTestId("upload-button")).toHaveTextContent("Uploading…");

      // Resolve the upload to clean up
      resolveUpload!({
        id: "program-123",
        programName: "Push Pull Legs",
        durationWeeks: 4,
        goal: "Hypertrophy",
        equipmentProfile: ["Barbell", "Dumbbells"],
        contentSource: "UPLOADED",
        createdAt: "2026-01-15T10:00:00Z",
      });

      await waitFor(() => {
        expect(screen.getByTestId("upload-success")).toBeInTheDocument();
      });
    });
  });
});
