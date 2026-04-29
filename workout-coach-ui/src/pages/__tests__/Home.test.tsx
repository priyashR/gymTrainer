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
  it("should render three action cards with correct labels and links", () => {
    renderHome();

    const newWorkout = screen.getByRole("link", { name: /new workout/i });
    const myPerformance = screen.getByRole("link", { name: /my performance/i });
    const workout = screen.getByRole("link", { name: /^workout$/i });

    expect(newWorkout).toHaveAttribute("href", "/new-workout");
    expect(myPerformance).toHaveAttribute("href", "/my-performance");
    expect(workout).toHaveAttribute("href", "/workout");
  });

  it("should call logout when the logout button is clicked", async () => {
    const user = userEvent.setup();
    const logoutFn = vi.fn();
    renderHome({ logout: logoutFn });

    await user.click(screen.getByRole("button", { name: /log out/i }));

    expect(logoutFn).toHaveBeenCalledOnce();
  });
});
