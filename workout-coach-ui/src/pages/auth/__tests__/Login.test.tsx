import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { AxiosError, type AxiosResponse } from "axios";
import { AuthContext, type AuthContextValue } from "../../../features/auth/AuthContext";
import Login from "../Login";

// Requirements: 1.2

const noop = () => {};
const noopAsync = async () => ({} as never);

function renderLogin(overrides: Partial<AuthContextValue> = {}) {
  const defaultCtx: AuthContextValue = {
    accessToken: null,
    isAuthenticated: false,
    isLoading: false,
    login: vi.fn().mockResolvedValue(undefined),
    register: noopAsync,
    logout: noop,
    ...overrides,
  };

  return {
    ctx: defaultCtx,
    ...render(
      <AuthContext.Provider value={defaultCtx}>
        <MemoryRouter>
          <Login />
        </MemoryRouter>
      </AuthContext.Provider>
    ),
  };
}

describe("Login page", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should submit successfully and call login with email and password", async () => {
    const user = userEvent.setup();
    const loginFn = vi.fn().mockResolvedValue(undefined);
    renderLogin({ login: loginFn });

    await user.type(screen.getByLabelText(/email/i), "user@test.com");
    await user.type(screen.getByLabelText(/password/i), "mypassword123");
    await user.click(screen.getByRole("button", { name: /log in/i }));

    await waitFor(() => {
      expect(loginFn).toHaveBeenCalledWith("user@test.com", "mypassword123");
    });
  });

  it("should redirect to / when already authenticated", () => {
    renderLogin({ isAuthenticated: true, accessToken: "some-token" });

    // Login form should not be rendered
    expect(screen.queryByRole("button", { name: /log in/i })).not.toBeInTheDocument();
  });

  it("should display server error on invalid credentials", async () => {
    const user = userEvent.setup();
    const axiosError = new AxiosError(
      "Request failed",
      "ERR_BAD_REQUEST",
      undefined,
      undefined,
      {
        data: {
          status: 401,
          error: "Unauthorised",
          message: "Invalid email or password",
          path: "/api/v1/auth/login",
          timestamp: "2026-01-01T00:00:00Z",
        },
        status: 401,
        statusText: "Unauthorised",
        headers: {},
        config: { headers: {} as never },
      } as AxiosResponse
    );
    const loginFn = vi.fn().mockRejectedValue(axiosError);

    renderLogin({ login: loginFn });

    await user.type(screen.getByLabelText(/email/i), "user@test.com");
    await user.type(screen.getByLabelText(/password/i), "wrongpassword");
    await user.click(screen.getByRole("button", { name: /log in/i }));

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent("Invalid email or password");
    });
  });

  it("should show validation error when email is empty", async () => {
    const user = userEvent.setup();
    const loginFn = vi.fn();
    renderLogin({ login: loginFn });

    await user.type(screen.getByLabelText(/password/i), "somepassword");
    await user.click(screen.getByRole("button", { name: /log in/i }));

    expect(screen.getByText("Email is required")).toBeInTheDocument();
    expect(loginFn).not.toHaveBeenCalled();
  });

  it("should show validation error when password is empty", async () => {
    const user = userEvent.setup();
    const loginFn = vi.fn();
    renderLogin({ login: loginFn });

    await user.type(screen.getByLabelText(/email/i), "user@test.com");
    await user.click(screen.getByRole("button", { name: /log in/i }));

    expect(screen.getByText("Password is required")).toBeInTheDocument();
    expect(loginFn).not.toHaveBeenCalled();
  });

  it("should show validation error for invalid email format", async () => {
    const user = userEvent.setup();
    const loginFn = vi.fn();
    renderLogin({ login: loginFn });

    await user.type(screen.getByLabelText(/email/i), "not-an-email");
    await user.type(screen.getByLabelText(/password/i), "somepassword");
    await user.click(screen.getByRole("button", { name: /log in/i }));

    expect(screen.getByText("Must be a valid email address")).toBeInTheDocument();
    expect(loginFn).not.toHaveBeenCalled();
  });

  it("should show loading state while auth is initializing", () => {
    renderLogin({ isLoading: true });

    expect(screen.getByText("Loading…")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /log in/i })).not.toBeInTheDocument();
  });

  it("should display network error when server is unreachable", async () => {
    const user = userEvent.setup();
    const networkError = new Error("Network Error");
    const loginFn = vi.fn().mockRejectedValue(networkError);

    renderLogin({ login: loginFn });

    await user.type(screen.getByLabelText(/email/i), "user@test.com");
    await user.type(screen.getByLabelText(/password/i), "somepassword");
    await user.click(screen.getByRole("button", { name: /log in/i }));

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(
        "Unable to connect. Please check your connection and try again."
      );
    });
  });
});
