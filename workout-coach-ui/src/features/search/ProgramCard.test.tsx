import { render, screen, fireEvent } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import { ProgramCard } from "./ProgramCard";
import type { VaultItem } from "../../types/vault";

const mockProgram: VaultItem = {
  id: "abc-123",
  name: "Push Pull Legs Hypertrophy",
  goal: "Build muscle with progressive overload",
  durationWeeks: 12,
  equipmentProfile: ["Barbell", "Dumbbells", "Cable Machine"],
  contentSource: "AI_GENERATED",
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-15T00:00:00Z",
};

describe("ProgramCard", () => {
  it("renders program name", () => {
    render(<ProgramCard program={mockProgram} />);
    expect(screen.getByText("Push Pull Legs Hypertrophy")).toBeInTheDocument();
  });

  it("renders program goal", () => {
    render(<ProgramCard program={mockProgram} />);
    expect(
      screen.getByText("Build muscle with progressive overload")
    ).toBeInTheDocument();
  });

  it("renders duration in weeks", () => {
    render(<ProgramCard program={mockProgram} />);
    expect(screen.getByText("12 weeks")).toBeInTheDocument();
  });

  it("renders singular week for 1-week program", () => {
    const singleWeek: VaultItem = { ...mockProgram, durationWeeks: 1 };
    render(<ProgramCard program={singleWeek} />);
    expect(screen.getByText("1 week")).toBeInTheDocument();
  });

  it("renders content source as metadata", () => {
    render(<ProgramCard program={mockProgram} />);
    expect(screen.getByText("ai generated")).toBeInTheDocument();
  });

  it("renders equipment tags", () => {
    render(<ProgramCard program={mockProgram} />);
    expect(screen.getByText("Barbell")).toBeInTheDocument();
    expect(screen.getByText("Dumbbells")).toBeInTheDocument();
    expect(screen.getByText("Cable Machine")).toBeInTheDocument();
  });

  it("does not render tags section when equipmentProfile is empty", () => {
    const noEquipment: VaultItem = { ...mockProgram, equipmentProfile: [] };
    render(<ProgramCard program={noEquipment} />);
    expect(screen.queryByText("Barbell")).not.toBeInTheDocument();
  });

  it("calls onClick when card is clicked", () => {
    const handleClick = vi.fn();
    render(<ProgramCard program={mockProgram} onClick={handleClick} />);

    fireEvent.click(screen.getByRole("button"));
    expect(handleClick).toHaveBeenCalledTimes(1);
  });

  it("calls onClick when Enter key is pressed", () => {
    const handleClick = vi.fn();
    render(<ProgramCard program={mockProgram} onClick={handleClick} />);

    fireEvent.keyDown(screen.getByRole("button"), { key: "Enter" });
    expect(handleClick).toHaveBeenCalledTimes(1);
  });

  it("calls onClick when Space key is pressed", () => {
    const handleClick = vi.fn();
    render(<ProgramCard program={mockProgram} onClick={handleClick} />);

    fireEvent.keyDown(screen.getByRole("button"), { key: " " });
    expect(handleClick).toHaveBeenCalledTimes(1);
  });

  it("has accessible label with program name", () => {
    render(<ProgramCard program={mockProgram} />);
    expect(
      screen.getByLabelText("View program: Push Pull Legs Hypertrophy")
    ).toBeInTheDocument();
  });

  it("does not render goal paragraph when goal is empty", () => {
    const noGoal: VaultItem = { ...mockProgram, goal: "" };
    render(<ProgramCard program={noGoal} />);
    expect(
      screen.queryByText("Build muscle with progressive overload")
    ).not.toBeInTheDocument();
  });
});
