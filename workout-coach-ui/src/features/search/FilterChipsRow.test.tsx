import { render, screen, fireEvent } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import { FilterChipsRow, SearchFilters } from "./FilterChipsRow";

describe("FilterChipsRow", () => {
  const defaultFilters: SearchFilters = {
    focusArea: "",
    modality: "",
    days: "",
  };

  it("renders three filter chips: Focus Area, Modality, Days", () => {
    render(<FilterChipsRow filters={defaultFilters} onChange={() => {}} />);

    expect(screen.getByLabelText("Focus Area")).toBeInTheDocument();
    expect(screen.getByLabelText("Modality")).toBeInTheDocument();
    expect(screen.getByLabelText("Days")).toBeInTheDocument();
  });

  it("renders Focus Area options", () => {
    render(<FilterChipsRow filters={defaultFilters} onChange={() => {}} />);

    const select = screen.getByLabelText("Focus Area") as HTMLSelectElement;
    const options = Array.from(select.options).map((o) => o.text);

    expect(options).toContain("Push");
    expect(options).toContain("Pull");
    expect(options).toContain("Legs");
    expect(options).toContain("Full Body");
    expect(options).toContain("Metcon");
  });

  it("renders Modality options", () => {
    render(<FilterChipsRow filters={defaultFilters} onChange={() => {}} />);

    const select = screen.getByLabelText("Modality") as HTMLSelectElement;
    const options = Array.from(select.options).map((o) => o.text);

    expect(options).toContain("CrossFit");
    expect(options).toContain("Hypertrophy");
    expect(options).toContain("Strength");
  });

  it("renders Days options", () => {
    render(<FilterChipsRow filters={defaultFilters} onChange={() => {}} />);

    const select = screen.getByLabelText("Days") as HTMLSelectElement;
    const options = Array.from(select.options).map((o) => o.text);

    expect(options).toContain("3");
    expect(options).toContain("4");
    expect(options).toContain("5");
    expect(options).toContain("6");
    expect(options).toContain("7");
  });

  it("calls onChange with updated focusArea when Focus Area changes", () => {
    const handleChange = vi.fn();
    render(<FilterChipsRow filters={defaultFilters} onChange={handleChange} />);

    fireEvent.change(screen.getByLabelText("Focus Area"), {
      target: { value: "Pull" },
    });

    expect(handleChange).toHaveBeenCalledWith({
      focusArea: "Pull",
      modality: "",
      days: "",
    });
  });

  it("calls onChange with updated modality when Modality changes", () => {
    const handleChange = vi.fn();
    render(<FilterChipsRow filters={defaultFilters} onChange={handleChange} />);

    fireEvent.change(screen.getByLabelText("Modality"), {
      target: { value: "CrossFit" },
    });

    expect(handleChange).toHaveBeenCalledWith({
      focusArea: "",
      modality: "CrossFit",
      days: "",
    });
  });

  it("calls onChange with updated days when Days changes", () => {
    const handleChange = vi.fn();
    render(<FilterChipsRow filters={defaultFilters} onChange={handleChange} />);

    fireEvent.change(screen.getByLabelText("Days"), {
      target: { value: "5" },
    });

    expect(handleChange).toHaveBeenCalledWith({
      focusArea: "",
      modality: "",
      days: "5",
    });
  });

  it("preserves existing filter values when one filter changes", () => {
    const handleChange = vi.fn();
    const activeFilters: SearchFilters = {
      focusArea: "Legs",
      modality: "Hypertrophy",
      days: "4",
    };

    render(<FilterChipsRow filters={activeFilters} onChange={handleChange} />);

    fireEvent.change(screen.getByLabelText("Days"), {
      target: { value: "6" },
    });

    expect(handleChange).toHaveBeenCalledWith({
      focusArea: "Legs",
      modality: "Hypertrophy",
      days: "6",
    });
  });

  it("reflects current filter state in the selects", () => {
    const activeFilters: SearchFilters = {
      focusArea: "Push",
      modality: "Strength",
      days: "3",
    };

    render(<FilterChipsRow filters={activeFilters} onChange={() => {}} />);

    expect(
      (screen.getByLabelText("Focus Area") as HTMLSelectElement).value
    ).toBe("Push");
    expect(
      (screen.getByLabelText("Modality") as HTMLSelectElement).value
    ).toBe("Strength");
    expect((screen.getByLabelText("Days") as HTMLSelectElement).value).toBe(
      "3"
    );
  });
});
