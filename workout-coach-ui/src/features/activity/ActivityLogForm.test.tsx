import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi } from "vitest";
import { ActivityLogForm, ActivityLogFormProps } from "./ActivityLogForm";

function renderComponent(overrides: Partial<ActivityLogFormProps> = {}) {
  const defaultProps: ActivityLogFormProps = {
    duration: "",
    calories: "",
    distance: "",
    distanceUnit: "km",
    notes: "",
    onDurationChange: vi.fn(),
    onCaloriesChange: vi.fn(),
    onDistanceChange: vi.fn(),
    onDistanceUnitChange: vi.fn(),
    onNotesChange: vi.fn(),
    ...overrides,
  };

  return {
    props: defaultProps,
    ...render(<ActivityLogForm {...defaultProps} />),
  };
}

describe("ActivityLogForm", () => {
  it("renders all form fields", () => {
    renderComponent();

    expect(screen.getByTestId("activity-log-form")).toBeInTheDocument();
    expect(screen.getByTestId("duration-input")).toBeInTheDocument();
    expect(screen.getByTestId("calories-input")).toBeInTheDocument();
    expect(screen.getByTestId("distance-input")).toBeInTheDocument();
    expect(screen.getByTestId("unit-km")).toBeInTheDocument();
    expect(screen.getByTestId("unit-miles")).toBeInTheDocument();
    expect(screen.getByTestId("notes-textarea")).toBeInTheDocument();
  });

  it("renders duration input with correct label and type", () => {
    renderComponent();

    const input = screen.getByTestId("duration-input");
    expect(input).toHaveAttribute("type", "number");
    expect(screen.getByLabelText(/duration in minutes/i)).toBeInTheDocument();
    expect(screen.getByText("Duration (minutes)")).toBeInTheDocument();
  });

  it("renders calories input with correct label and type", () => {
    renderComponent();

    const input = screen.getByTestId("calories-input");
    expect(input).toHaveAttribute("type", "number");
    expect(screen.getByLabelText(/calories in kcal/i)).toBeInTheDocument();
    expect(screen.getByText("Calories (kcal)")).toBeInTheDocument();
  });

  it("renders distance input with correct type", () => {
    renderComponent();

    const input = screen.getByTestId("distance-input");
    expect(input).toHaveAttribute("type", "number");
    expect(screen.getByText("Distance")).toBeInTheDocument();
  });

  it("renders notes textarea", () => {
    renderComponent();

    const textarea = screen.getByTestId("notes-textarea");
    expect(textarea.tagName.toLowerCase()).toBe("textarea");
    expect(screen.getByText("Notes")).toBeInTheDocument();
  });

  it("displays current values from props", () => {
    renderComponent({
      duration: "45",
      calories: "300",
      distance: "5.5",
      distanceUnit: "miles",
      notes: "Morning jog",
    });

    expect(screen.getByTestId("duration-input")).toHaveValue(45);
    expect(screen.getByTestId("calories-input")).toHaveValue(300);
    expect(screen.getByTestId("distance-input")).toHaveValue(5.5);
    expect(screen.getByTestId("notes-textarea")).toHaveValue("Morning jog");
  });

  it("calls onDurationChange when duration value changes", async () => {
    const user = userEvent.setup();
    const onDurationChange = vi.fn();
    renderComponent({ onDurationChange });

    const input = screen.getByTestId("duration-input");
    await user.type(input, "30");

    // Controlled component: each keystroke fires with its individual character
    expect(onDurationChange).toHaveBeenCalledTimes(2);
    expect(onDurationChange).toHaveBeenCalledWith("3");
    expect(onDurationChange).toHaveBeenCalledWith("0");
  });

  it("calls onCaloriesChange when calories value changes", async () => {
    const user = userEvent.setup();
    const onCaloriesChange = vi.fn();
    renderComponent({ onCaloriesChange });

    const input = screen.getByTestId("calories-input");
    await user.type(input, "250");

    expect(onCaloriesChange).toHaveBeenCalledTimes(3);
    expect(onCaloriesChange).toHaveBeenCalledWith("2");
    expect(onCaloriesChange).toHaveBeenCalledWith("5");
    expect(onCaloriesChange).toHaveBeenCalledWith("0");
  });

  it("calls onDistanceChange when distance value changes", async () => {
    const user = userEvent.setup();
    const onDistanceChange = vi.fn();
    renderComponent({ onDistanceChange });

    const input = screen.getByTestId("distance-input");
    await user.type(input, "10");

    expect(onDistanceChange).toHaveBeenCalledTimes(2);
    expect(onDistanceChange).toHaveBeenCalledWith("1");
    expect(onDistanceChange).toHaveBeenCalledWith("0");
  });

  it("calls onNotesChange when notes textarea changes", async () => {
    const user = userEvent.setup();
    const onNotesChange = vi.fn();
    renderComponent({ onNotesChange });

    const textarea = screen.getByTestId("notes-textarea");
    await user.type(textarea, "Hi");

    // Each keystroke fires independently since value isn't accumulating (controlled)
    expect(onNotesChange).toHaveBeenCalledTimes(2);
    expect(onNotesChange).toHaveBeenCalledWith("H");
    expect(onNotesChange).toHaveBeenCalledWith("i");
  });

  it("shows km unit as active by default", () => {
    renderComponent({ distanceUnit: "km" });

    const kmBtn = screen.getByTestId("unit-km");
    const milesBtn = screen.getByTestId("unit-miles");

    expect(kmBtn).toHaveAttribute("aria-pressed", "true");
    expect(milesBtn).toHaveAttribute("aria-pressed", "false");
  });

  it("shows miles unit as active when distanceUnit is miles", () => {
    renderComponent({ distanceUnit: "miles" });

    const kmBtn = screen.getByTestId("unit-km");
    const milesBtn = screen.getByTestId("unit-miles");

    expect(kmBtn).toHaveAttribute("aria-pressed", "false");
    expect(milesBtn).toHaveAttribute("aria-pressed", "true");
  });

  it("calls onDistanceUnitChange with 'miles' when miles button is clicked", async () => {
    const user = userEvent.setup();
    const onDistanceUnitChange = vi.fn();
    renderComponent({ distanceUnit: "km", onDistanceUnitChange });

    await user.click(screen.getByTestId("unit-miles"));

    expect(onDistanceUnitChange).toHaveBeenCalledWith("miles");
  });

  it("calls onDistanceUnitChange with 'km' when km button is clicked", async () => {
    const user = userEvent.setup();
    const onDistanceUnitChange = vi.fn();
    renderComponent({ distanceUnit: "miles", onDistanceUnitChange });

    await user.click(screen.getByTestId("unit-km"));

    expect(onDistanceUnitChange).toHaveBeenCalledWith("km");
  });

  it("ensures inputs have minimum tap target size", () => {
    renderComponent();

    const durationInput = screen.getByTestId("duration-input");
    const caloriesInput = screen.getByTestId("calories-input");
    const distanceInput = screen.getByTestId("distance-input");

    expect(durationInput.style.minHeight).toBe("var(--tap-target-min)");
    expect(caloriesInput.style.minHeight).toBe("var(--tap-target-min)");
    expect(distanceInput.style.minHeight).toBe("var(--tap-target-min)");
  });

  it("ensures unit toggle buttons have minimum tap target size", () => {
    renderComponent();

    const kmBtn = screen.getByTestId("unit-km");
    const milesBtn = screen.getByTestId("unit-miles");

    expect(kmBtn.style.minWidth).toBe("var(--tap-target-min)");
    expect(kmBtn.style.minHeight).toBe("var(--tap-target-min)");
    expect(milesBtn.style.minWidth).toBe("var(--tap-target-min)");
    expect(milesBtn.style.minHeight).toBe("var(--tap-target-min)");
  });

  it("renders distance unit toggle as a group with aria-label", () => {
    renderComponent();

    const unitGroup = screen.getByRole("group", { name: /distance unit/i });
    expect(unitGroup).toBeInTheDocument();
  });
});
