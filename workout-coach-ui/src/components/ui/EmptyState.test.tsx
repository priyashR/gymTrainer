import { render, screen } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import { EmptyState } from "./EmptyState";

describe("EmptyState", () => {
  it("renders title text", () => {
    render(<EmptyState title="No data available" />);
    expect(screen.getByText("No data available")).toBeInTheDocument();
  });

  it("renders subtitle when provided", () => {
    render(
      <EmptyState title="No data" subtitle="Check back later" />
    );
    expect(screen.getByText("Check back later")).toBeInTheDocument();
  });

  it("does not render subtitle when not provided", () => {
    render(<EmptyState title="No data" />);
    expect(screen.queryByText("Check back later")).not.toBeInTheDocument();
  });

  it("renders icon when provided", () => {
    render(<EmptyState icon="📊" title="No data" />);
    expect(screen.getByText("📊")).toBeInTheDocument();
  });

  it("does not render icon when not provided", () => {
    const { container } = render(<EmptyState title="No data" />);
    const iconSpan = container.querySelector('[aria-hidden="true"]');
    expect(iconSpan).not.toBeInTheDocument();
  });

  it("has accessible role and label", () => {
    render(<EmptyState title="Performance data coming soon" />);
    expect(
      screen.getByRole("status", { name: "Performance data coming soon" })
    ).toBeInTheDocument();
  });
});
