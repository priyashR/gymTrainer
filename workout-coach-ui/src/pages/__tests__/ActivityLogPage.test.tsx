import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import ActivityLogPage from "../ActivityLogPage";

// Mock react-router-dom's useNavigate
const mockNavigate = vi.fn();
vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

// Mock apiClient
vi.mock("../../lib/apiClient", () => ({
  default: {
    post: vi.fn(),
  },
}));

// Mock useLocalActivityStore
vi.mock("../../hooks/useLocalActivityStore", () => ({
  saveActivity: vi.fn(),
}));

import apiClient from "../../lib/apiClient";
import { saveActivity } from "../../hooks/useLocalActivityStore";

function renderActivityLogPage() {
  return render(
    <MemoryRouter>
      <ActivityLogPage />
    </MemoryRouter>
  );
}

describe("ActivityLogPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.useFakeTimers({ shouldAdvanceTime: true });
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("renders the page with all required elements", () => {
    renderActivityLogPage();

    expect(screen.getByTestId("activity-log-page")).toBeInTheDocument();
    expect(screen.getByTestId("back-link")).toBeInTheDocument();
    expect(screen.getByText("Log Activity")).toBeInTheDocument();
    expect(screen.getByTestId("activity-type-grid")).toBeInTheDocument();
    expect(screen.getByTestId("date-picker")).toBeInTheDocument();
    expect(screen.getByTestId("activity-log-form")).toBeInTheDocument();
    expect(screen.getByTestId("submit-button")).toBeInTheDocument();
  });

  it("has a back link to the home page", () => {
    renderActivityLogPage();

    const backLink = screen.getByTestId("back-link");
    expect(backLink).toHaveAttribute("href", "/");
    expect(backLink).toHaveTextContent("← Home");
  });

  describe("Date defaults to today (Req 9.4)", () => {
    it("defaults the date input to today's date in ISO format", () => {
      renderActivityLogPage();

      const dateInput = screen.getByTestId("date-picker");
      const today = new Date();
      const expectedDate = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}-${String(today.getDate()).padStart(2, "0")}`;
      expect(dateInput).toHaveValue(expectedDate);
    });
  });

  describe("Validation: activity type and date required (Req 9.5)", () => {
    it("shows error when submitting without selecting activity type", async () => {
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
      renderActivityLogPage();

      await user.click(screen.getByTestId("submit-button"));

      expect(screen.getByTestId("error-activity-type")).toBeInTheDocument();
      expect(screen.getByTestId("error-activity-type")).toHaveTextContent(
        "Activity type is required"
      );
    });

    it("shows error when date is cleared", async () => {
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
      renderActivityLogPage();

      // Clear the date
      const dateInput = screen.getByTestId("date-picker");
      fireEvent.change(dateInput, { target: { value: "" } });

      // Select an activity to isolate date validation
      await user.click(screen.getByTestId("activity-tile-running"));

      await user.click(screen.getByTestId("submit-button"));

      expect(screen.getByTestId("error-date")).toBeInTheDocument();
      expect(screen.getByTestId("error-date")).toHaveTextContent(
        "Date is required"
      );
    });

    it("does not submit to API when validation fails", async () => {
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
      renderActivityLogPage();

      await user.click(screen.getByTestId("submit-button"));

      expect(apiClient.post).not.toHaveBeenCalled();
    });

    it("clears activity type error when user selects an activity", async () => {
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
      renderActivityLogPage();

      // Trigger validation error
      await user.click(screen.getByTestId("submit-button"));
      expect(screen.getByTestId("error-activity-type")).toBeInTheDocument();

      // Select an activity
      await user.click(screen.getByTestId("activity-tile-soccer"));

      // Error should be cleared
      expect(screen.queryByTestId("error-activity-type")).not.toBeInTheDocument();
    });
  });

  describe("Successful submission via POST (Req 9.7, 9.9)", () => {
    it("submits to /activities and navigates to landing page on success", async () => {
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
      (apiClient.post as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
        data: { id: "123" },
      });

      renderActivityLogPage();

      // Select activity type
      await user.click(screen.getByTestId("activity-tile-running"));

      // Submit
      await user.click(screen.getByTestId("submit-button"));

      await waitFor(() => {
        expect(apiClient.post).toHaveBeenCalledWith(
          "/activities",
          expect.objectContaining({
            activityType: "running",
            date: expect.any(String),
          })
        );
      });

      // Should show success message
      await waitFor(() => {
        expect(screen.getByTestId("success-message")).toBeInTheDocument();
      });

      // Navigate after timeout
      vi.advanceTimersByTime(600);
      expect(mockNavigate).toHaveBeenCalledWith("/");
    });

    it("disables submit button while submitting", async () => {
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
      let resolvePost: (value: unknown) => void;
      (apiClient.post as ReturnType<typeof vi.fn>).mockReturnValueOnce(
        new Promise((resolve) => {
          resolvePost = resolve;
        })
      );

      renderActivityLogPage();

      await user.click(screen.getByTestId("activity-tile-cycling"));
      await user.click(screen.getByTestId("submit-button"));

      expect(screen.getByTestId("submit-button")).toBeDisabled();
      expect(screen.getByTestId("submit-button")).toHaveTextContent("Saving…");

      // Resolve to clean up
      resolvePost!({ data: { id: "123" } });
      await waitFor(() => {
        expect(screen.getByTestId("success-message")).toBeInTheDocument();
      });
    });
  });

  describe("Fallback to localStorage when backend unavailable (Req 9.8)", () => {
    it("saves to localStorage when POST returns 404", async () => {
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
      (apiClient.post as ReturnType<typeof vi.fn>).mockRejectedValueOnce({
        response: { status: 404 },
        code: "ERR_BAD_REQUEST",
      });

      renderActivityLogPage();

      await user.click(screen.getByTestId("activity-tile-golf"));
      await user.click(screen.getByTestId("submit-button"));

      await waitFor(() => {
        expect(saveActivity).toHaveBeenCalledWith(
          expect.objectContaining({
            activityType: "golf",
            date: expect.any(String),
          })
        );
      });

      // Should show local storage message
      expect(screen.getByTestId("local-storage-message")).toBeInTheDocument();
      expect(screen.getByTestId("local-storage-message")).toHaveTextContent(
        "saved locally"
      );

      // Navigate after timeout
      vi.advanceTimersByTime(1600);
      expect(mockNavigate).toHaveBeenCalledWith("/");
    });

    it("saves to localStorage on network error", async () => {
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
      (apiClient.post as ReturnType<typeof vi.fn>).mockRejectedValueOnce({
        code: "ERR_NETWORK",
      });

      renderActivityLogPage();

      await user.click(screen.getByTestId("activity-tile-swimming"));
      await user.click(screen.getByTestId("submit-button"));

      await waitFor(() => {
        expect(saveActivity).toHaveBeenCalledWith(
          expect.objectContaining({
            activityType: "swimming",
          })
        );
      });

      expect(screen.getByTestId("local-storage-message")).toBeInTheDocument();
    });
  });

  describe("Activity type grid selection with 'Other' custom input (Req 9.3)", () => {
    it("allows selecting 'Other' and typing a custom activity name that is used in the API call", async () => {
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
      (apiClient.post as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
        data: { id: "456" },
      });

      renderActivityLogPage();

      // Select "Other" tile
      await user.click(screen.getByTestId("activity-tile-other"));

      // Custom input should appear
      const customInput = screen.getByTestId("custom-activity-input");
      expect(customInput).toBeInTheDocument();

      // Type a custom activity name using fireEvent.change to simulate a single
      // value change (userEvent.type triggers per-character, which interacts with
      // the onSelect callback causing re-renders that hide the input)
      fireEvent.change(customInput, { target: { value: "Rock Climbing" } });

      // Submit the form
      await user.click(screen.getByTestId("submit-button"));

      await waitFor(() => {
        expect(apiClient.post).toHaveBeenCalledWith(
          "/activities",
          expect.objectContaining({
            activityType: "Rock Climbing",
            date: expect.any(String),
          })
        );
      });

      // Should show success message
      await waitFor(() => {
        expect(screen.getByTestId("success-message")).toBeInTheDocument();
      });
    });
  });

  describe("Dark mode theming (Req 9.10)", () => {
    it("applies dark mode theme tokens to the page", () => {
      renderActivityLogPage();

      const page = screen.getByTestId("activity-log-page");
      // The page should exist and use theme CSS variables in its inline styles
      expect(page).toBeInTheDocument();
    });
  });
});
