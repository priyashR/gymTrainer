import { render, screen } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import { AMRAPInfoCard } from "./AMRAPInfoCard";

describe("AMRAPInfoCard", () => {
  it("renders time cap and description", () => {
    render(<AMRAPInfoCard timeCap="12:00" description="12 min time cap" />);

    expect(screen.getByText("12:00")).toBeInTheDocument();
    expect(screen.getByText("12 min time cap")).toBeInTheDocument();
  });

  it("has data-testid for testing", () => {
    render(<AMRAPInfoCard timeCap="8:00" description="8 min time cap" />);

    expect(screen.getByTestId("amrap-info-card")).toBeInTheDocument();
  });

  it("has accessible region role with aria-label", () => {
    render(<AMRAPInfoCard timeCap="15:00" description="15 min AMRAP" />);

    const region = screen.getByRole("region", {
      name: /AMRAP information: 15:00/i,
    });
    expect(region).toBeInTheDocument();
  });

  it("applies orange accent styling via inline styles", () => {
    render(<AMRAPInfoCard timeCap="10:00" description="10 min time cap" />);

    const card = screen.getByTestId("amrap-info-card");
    expect(card).toHaveStyle({
      background: "rgba(255, 112, 67, 0.1)",
      border: "1px solid var(--color-crossfit)",
      borderRadius: "var(--radius-md)",
    });
  });
});
