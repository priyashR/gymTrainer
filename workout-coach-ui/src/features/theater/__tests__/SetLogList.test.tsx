import { render, screen } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import { SetLogList } from "../SetLogList";
import type { SetLog } from "../../../types/session";

describe("SetLogList", () => {
  it("renders empty state when no sets are logged", () => {
    render(<SetLogList setLogs={[]} />);

    expect(screen.getByText("No sets logged yet")).toBeInTheDocument();
  });

  it("renders logged sets with correct set number, weight, reps, and RPE", () => {
    const setLogs: SetLog[] = [
      { setNumber: 1, weight: 100, repetitions: 8, rpe: 7.5, loggedAt: "2026-01-15T10:00:00Z" },
      { setNumber: 2, weight: 105, repetitions: 6, rpe: null, loggedAt: "2026-01-15T10:01:00Z" },
      { setNumber: 3, weight: 110, repetitions: 5, rpe: 9, loggedAt: "2026-01-15T10:02:00Z" },
    ];

    render(<SetLogList setLogs={setLogs} />);

    const list = screen.getByRole("list", { name: /logged sets/i });
    expect(list).toBeInTheDocument();

    // Check set 1
    expect(screen.getByText("#1")).toBeInTheDocument();
    expect(screen.getByText("100kg × 8")).toBeInTheDocument();
    expect(screen.getByText("RPE 7.5")).toBeInTheDocument();

    // Check set 2 (no RPE)
    expect(screen.getByText("#2")).toBeInTheDocument();
    expect(screen.getByText("105kg × 6")).toBeInTheDocument();

    // Check set 3
    expect(screen.getByText("#3")).toBeInTheDocument();
    expect(screen.getByText("110kg × 5")).toBeInTheDocument();
    expect(screen.getByText("RPE 9")).toBeInTheDocument();
  });

  it("does not render RPE when it is null", () => {
    const setLogs: SetLog[] = [
      { setNumber: 1, weight: 80, repetitions: 10, rpe: null, loggedAt: "2026-01-15T10:00:00Z" },
    ];

    render(<SetLogList setLogs={setLogs} />);

    expect(screen.getByText("#1")).toBeInTheDocument();
    expect(screen.getByText("80kg × 10")).toBeInTheDocument();
    expect(screen.queryByText(/RPE/)).not.toBeInTheDocument();
  });
});
