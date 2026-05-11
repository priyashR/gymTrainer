import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { AuthContext, type AuthContextValue } from "../../features/auth/AuthContext";
import Home from "../Home";

// Requirements: 2.1, 2.2

const noopAsync = async () => ({} as never);

function renderHome(overrides: Partial<AuthContextValue> = {}) {
  const defaultCtx: AuthContextValue = {
    accessToken: "test-token",
    isAuthenticated: true,
    isLoading: false,
    login: noopAsync,
    register: noopAsync,
    logout: vi.fn(),
    ...overrides,
  };

  return {
    ctx: defaultCtx,
    ...render(
      <AuthContext.Provider value={defaultCtx}>
        <MemoryRouter>
          <Home />
        </MemoryRouter>
      </AuthContext.Provider>
    ),
  };
}

describe("Home page", () => {
  it("should render expandable menus and action cards with correct labels", async () => {
    const user = userEvent.setup();
    renderHome();

    // "New Workout" is an expandable button
    const newWorkoutBtn = screen.getByRole("button", { name: /new workout/i });
    expect(newWorkoutBtn).toHaveAttribute("aria-expanded", "false");

    // "Workout" is an expandable button
    const workoutBtn = screen.getByRole("button", { name: /^workout/i });
    expect(workoutBtn).toHaveAttribute("aria-expanded", "false");

    // "My Performance" is still a link
    const myPerformance = screen.getByRole("link", { name: /my performance/i });
    expect(myPerformance).toHaveAttribute("href", "/my-performance");

    // Expand "New Workout" — sub-options appear
    await user.click(newWorkoutBtn);
    expect(newWorkoutBtn).toHaveAttribute("aria-expanded", "true");
    expect(screen.getByRole("link", { name: /ask gemini/i })).toHaveAttribute("href", "/new-workout");
    expect(screen.getByRole("link", { name: /upload program/i })).toHaveAttribute("href", "/upload");

    // Expand "Workout" — sub-options appear
    await user.click(workoutBtn);
    expect(workoutBtn).toHaveAttribute("aria-expanded", "true");
    expect(screen.getByRole("link", { name: /continue with program/i })).toHaveAttribute("href", "/workout/continue");
    expect(screen.getByRole("link", { name: /search for a workout or program/i })).toHaveAttribute("href", "/vault/search");
  });

  it("should call logout when the logout button is clicked", async () => {
    const user = userEvent.setup();
    const logoutFn = vi.fn();
    renderHome({ logout: logoutFn });

    await user.click(screen.getByRole("button", { name: /log out/i }));

    expect(logoutFn).toHaveBeenCalledOnce();
  });
});
