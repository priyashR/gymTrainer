import { describe, it, expect, vi, beforeEach } from "vitest";
import { renderHook, act, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import type { ReactNode } from "react";
import { AuthProvider } from "../AuthContext";
import { useAuth } from "../useAuth";

// Requirements: 1.3, 1.4, 1.5

vi.mock("../../../lib/authApi", () => ({
  login: vi.fn(),
  register: vi.fn(),
  refresh: vi.fn(),
}));

vi.mock("../../../lib/apiClient", () => ({
  default: {},
  setAccessTokenGetter: vi.fn(),
  setOnRefreshFailure: vi.fn(),
  setOnTokenRefreshed: vi.fn(),
}));

import * as authApi from "../../../lib/authApi";

function wrapper({ children }: { children: ReactNode }) {
  return (
    <MemoryRouter>
      <AuthProvider>{children}</AuthProvider>
    </MemoryRouter>
  );
}

describe("AuthContext", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(authApi.refresh).mockRejectedValue(new Error("no session"));
  });

  it("should set token and isAuthenticated after login", async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      accessToken: "my-token-123",
      tokenType: "Bearer",
      expiresIn: 900,
    });

    const { result } = renderHook(() => useAuth(), { wrapper });

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    await act(async () => {
      await result.current.login("[email]@test.com", "password123");
    });

    expect(result.current.isAuthenticated).toBe(true);
    expect(result.current.accessToken).toBe("my-token-123");
  });

  it("should clear token and isAuthenticated after logout", async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      accessToken: "my-token-123",
      tokenType: "Bearer",
      expiresIn: 900,
    });

    const { result } = renderHook(() => useAuth(), { wrapper });

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    await act(async () => {
      await result.current.login("[email]@test.com", "password123");
    });

    expect(result.current.isAuthenticated).toBe(true);

    act(() => {
      result.current.logout();
    });

    expect(result.current.accessToken).toBeNull();
    expect(result.current.isAuthenticated).toBe(false);
  });

  it("should set authenticated state when silent refresh on mount succeeds", async () => {
    vi.mocked(authApi.refresh).mockResolvedValue({
      accessToken: "refreshed-token",
      tokenType: "Bearer",
      expiresIn: 900,
    });

    const { result } = renderHook(() => useAuth(), { wrapper });

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.isAuthenticated).toBe(true);
    expect(result.current.accessToken).toBe("refreshed-token");
  });

  it("should remain unauthenticated when silent refresh on mount fails", async () => {
    vi.mocked(authApi.refresh).mockRejectedValue(new Error("expired"));

    const { result } = renderHook(() => useAuth(), { wrapper });

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.isAuthenticated).toBe(false);
    expect(result.current.accessToken).toBeNull();
  });
});
