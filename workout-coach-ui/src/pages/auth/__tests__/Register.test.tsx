import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { AxiosError, type AxiosResponse } from "axios";
import { AuthContext, type AuthContextValue } from "../../../features/auth/AuthContext";
import Register from "../Register";

// Requirements: 1.1

const noop = () => {};
const noopAsync = async () => ({} as never);

const mockNavigate = vi.fn();
vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

function renderRegister(overrides: Partial<AuthContextValue> = {}) {
  const defaultCtx: AuthContextValue = {
    accessToken: null,
    isAuthenticated: false,
    isLoading: false,
    login: noopAsync,
    register: vi.fn().mockResolvedValue({
      id: "uuid-123",
      email: "user@test.com",
      createdAt: "2026-01-01T00:00:00Z",
    }),
    logout: noop,
    ...overrides,
  };

  return {
    ctx: defaultCtx,
    ...render(
      <AuthContext.Provider value={defaultCtx}>
        <MemoryRouter>
          <Register />
        </MemoryRouter>
      </AuthContext.Provider>
    ),
  };
}

describe("Register page", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should submit successfully and redirect to /login", async () => {
    const user = userEvent.setup();
    const registerFn = vi.fn().mockResolvedValue({
      id: "uuid-123",
      email: "user@test.com",
      createdAt: "2026-01-01T00:00:00Z",
    });
    renderRegister({ register: registerFn });

    await user.type(screen.getByLabelText(/email/i), "user@test.com");
    await user.type(screen.getByLabelText(/password/i), "longpassword123");
    await user.click(screen.getByRole("button", { name: /register/i }));

    await waitFor(() => {
      expect(registerFn).toHaveBeenCalledWith("user@test.com", "longpassword123");
      expect(mockNavigate).toHaveBeenCalledWith("/login", { state: { registered: true } });
    });
  });

  it("should display server error on duplicate email", async () => {
    const user = userEvent.setup();
    const axiosError = new AxiosError(
      "Request failed",
      "ERR_BAD_REQUEST",
      undefined,
      undefined,
      {
        data: {
          status: 409,
          error: "Conflict",
          message: "An account with this email already exists",
          path: "/api/v1/auth/register",
          timestamp: "2026-01-01T00:00:00Z",
        },
        status: 409,
        statusText: "Conflict",
        headers: {},
        config: { headers: {} as never },
      } as AxiosResponse
    );
    const registerFn = vi.fn().mockRejectedValue(axiosError);

    renderRegister({ register: registerFn });

    await user.type(screen.getByLabelText(/email/i), "user@test.com");
    await user.type(screen.getByLabelText(/password/i), "longpassword123");
    await user.click(screen.getByRole("button", { name: /register/i }));

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(
        "An account with this email already exists"
      );
    });
  });

  it("should show validation error for invalid email", async () => {
    const user = userEvent.setup();
    const registerFn = vi.fn();
    renderRegister({ register: registerFn });

    await user.type(screen.getByLabelText(/email/i), "not-an-email");
    await user.type(screen.getByLabelText(/password/i), "longpassword123");
    await user.click(screen.getByRole("button", { name: /register/i }));

    expect(screen.getByText("Must be a valid email address")).toBeInTheDocument();
    expect(registerFn).not.toHaveBeenCalled();
  });

  it("should show validation error for short password", async () => {
    const user = userEvent.setup();
    const registerFn = vi.fn();
    renderRegister({ register: registerFn });

    await user.type(screen.getByLabelText(/email/i), "user@test.com");
    await user.type(screen.getByLabelText(/password/i), "short");
    await user.click(screen.getByRole("button", { name: /register/i }));

    expect(screen.getByText(/password must be at least 8 characters/i)).toBeInTheDocument();
    expect(registerFn).not.toHaveBeenCalled();
  });

  it("should show validation error when email is empty", async () => {
    const user = userEvent.setup();
    const registerFn = vi.fn();
    renderRegister({ register: registerFn });

    await user.type(screen.getByLabelText(/password/i), "longpassword123");
    await user.click(screen.getByRole("button", { name: /register/i }));

    expect(screen.getByText("Email is required")).toBeInTheDocument();
    expect(registerFn).not.toHaveBeenCalled();
  });

  it("should show validation error when password is empty", async () => {
    const user = userEvent.setup();
    const registerFn = vi.fn();
    renderRegister({ register: registerFn });

    await user.type(screen.getByLabelText(/email/i), "user@test.com");
    await user.click(screen.getByRole("button", { name: /register/i }));

    expect(screen.getByText("Password is required")).toBeInTheDocument();
    expect(registerFn).not.toHaveBeenCalled();
  });

  it("should redirect to / when already authenticated", () => {
    renderRegister({ isAuthenticated: true, accessToken: "some-token" });

    expect(screen.queryByRole("button", { name: /register/i })).not.toBeInTheDocument();
  });

  it("should show loading state while auth is initializing", () => {
    renderRegister({ isLoading: true });

    expect(screen.getByText("Loading…")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /register/i })).not.toBeInTheDocument();
  });

  it("should display network error when server is unreachable", async () => {
    const user = userEvent.setup();
    const networkError = new Error("Network Error");
    const registerFn = vi.fn().mockRejectedValue(networkError);

    renderRegister({ register: registerFn });

    await user.type(screen.getByLabelText(/email/i), "user@test.com");
    await user.type(screen.getByLabelText(/password/i), "longpassword123");
    await user.click(screen.getByRole("button", { name: /register/i }));

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(
        "Unable to connect. Please check your connection and try again."
      );
    });
  });

  it("should display server-side field validation errors", async () => {
    const user = userEvent.setup();
    const axiosError = new AxiosError(
      "Request failed",
      "ERR_BAD_REQUEST",
      undefined,
      undefined,
      {
        data: {
          status: 400,
          error: "Validation Failed",
          errors: [
            { field: "email", message: "must be a valid email address" },
            { field: "password", message: "must be at least 8 characters" },
          ],
          path: "/api/v1/auth/register",
          timestamp: "2026-01-01T00:00:00Z",
        },
        status: 400,
        statusText: "Bad Request",
        headers: {},
        config: { headers: {} as never },
      } as AxiosResponse
    );
    const registerFn = vi.fn().mockRejectedValue(axiosError);

    renderRegister({ register: registerFn });

    await user.type(screen.getByLabelText(/email/i), "user@test.com");
    await user.type(screen.getByLabelText(/password/i), "longpassword123");
    await user.click(screen.getByRole("button", { name: /register/i }));

    await waitFor(() => {
      expect(screen.getByText("must be a valid email address")).toBeInTheDocument();
      expect(screen.getByText("must be at least 8 characters")).toBeInTheDocument();
    });
  });
});
