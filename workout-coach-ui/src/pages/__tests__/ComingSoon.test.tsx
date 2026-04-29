import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import ComingSoon from "../ComingSoon";

// Requirements: 2.2

function renderComingSoon(props: { title?: string } = {}, routeState?: unknown) {
  const entries = routeState
    ? [{ pathname: "/test", state: routeState }]
    : ["/test"];

  return render(
    <MemoryRouter initialEntries={entries}>
      <ComingSoon {...props} />
    </MemoryRouter>
  );
}

describe("ComingSoon page", () => {
  it("should render 'Coming Soon' message", () => {
    renderComingSoon({ title: "New Workout" });

    expect(screen.getByText("Coming Soon")).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "New Workout" })).toBeInTheDocument();
  });

  it("should render a link back to home", () => {
    renderComingSoon({ title: "New Workout" });

    const homeLink = screen.getByRole("link", { name: /back to home/i });
    expect(homeLink).toHaveAttribute("href", "/");
  });

  it("should read title from route state when no prop is provided", () => {
    renderComingSoon({}, { title: "My Performance" });

    expect(screen.getByRole("heading", { name: "My Performance" })).toBeInTheDocument();
  });

  it("should fall back to 'This feature' when no title is provided", () => {
    renderComingSoon();

    expect(screen.getByRole("heading", { name: "This feature" })).toBeInTheDocument();
    expect(screen.getByText("Coming Soon")).toBeInTheDocument();
  });
});
