import { describe, it, expect, vi, beforeEach } from "vitest";
import { renderHook, act, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes, useNavigate } from "react-router-dom";
import fc from "fast-check";
import { useEffect, type ReactNode } from "react";
import { AuthProvider } from "../AuthContext";
import { useAuth } from "../useAuth";

// Mock authApi — must be before any import that triggers it
vi.mock("../../../lib/authApi", () => ({
  login: vi.fn(),
  register: vi.fn(),
  refresh: vi.fn(),
}));

// Mock apiClient wiring functions — AuthProvider calls these on mount
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

// Feature: workout-coach-ui-mvp1, Property 2: Successful login stores token and sets authenticated state

describe("Auth state — login", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // Silent refresh on mount fails so isLoading settles to false
    vi.mocked(authApi.refresh).mockRejectedValue(new Error("no session"));
  });

  it("should set isAuthenticated to true and store the access token after a successful login", async () => {
    // Arbitrary that produces valid AccessTokenResponse values
    const accessTokenResponseArb = fc.record({
      accessToken: fc.string({ minLength: 1, maxLength: 200 }),
      tokenType: fc.constant("Bearer"),
      expiresIn: fc.integer({ min: 1, max: 86400 }),
    });

    // Pre-generate 100 samples to avoid rendering 100 separate React trees
    const samples = fc.sample(accessTokenResponseArb, 100);

    const { result } = renderHook(() => useAuth(), { wrapper });

    // Wait for the initial silent-refresh to settle
    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    for (const tokenResponse of samples) {
      vi.mocked(authApi.login).mockResolvedValue(tokenResponse);

      await act(async () => {
        await result.current.login("[email]", "[password]");
      });

      // Post-condition: authenticated with the correct token
      expect(result.current.isAuthenticated).toBe(true);
      expect(result.current.accessToken).toBe(tokenResponse.accessToken);
    }
  }, 30_000);
});

// Feature: workout-coach-ui-mvp1, Property 4: Logout clears auth state

describe("Auth state — logout", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(authApi.refresh).mockRejectedValue(new Error("no session"));
  });

  it("should clear accessToken and set isAuthenticated to false after logout", async () => {
    const accessTokenArb = fc.string({ minLength: 1, maxLength: 200 });
    const expiresInArb = fc.integer({ min: 1, max: 86400 });

    const samples = fc.sample(
      fc.record({
        accessToken: accessTokenArb,
        tokenType: fc.constant("Bearer"),
        expiresIn: expiresInArb,
      }),
      100
    );

    const { result } = renderHook(() => useAuth(), { wrapper });

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    for (const tokenResponse of samples) {
      // Establish authenticated state
      vi.mocked(authApi.login).mockResolvedValue(tokenResponse);

      await act(async () => {
        await result.current.login("[email]", "[password]");
      });

      // Precondition: must be authenticated with a token
      expect(result.current.isAuthenticated).toBe(true);
      expect(result.current.accessToken).toBe(tokenResponse.accessToken);

      // Exercise: logout
      act(() => {
        result.current.logout();
      });

      // Post-condition: auth state is cleared
      expect(result.current.accessToken).toBeNull();
      expect(result.current.isAuthenticated).toBe(false);
    }
  }, 30_000);
});

// Feature: workout-coach-ui-mvp1, Property 6: Auth state persists across navigation

/**
 * Helper component that exposes a navigate function via a ref-like callback.
 * This lets the test imperatively navigate within the MemoryRouter.
 */
function NavigationHelper({
  onNavigate,
}: {
  onNavigate: (nav: ReturnType<typeof useNavigate>) => void;
}) {
  const navigate = useNavigate();
  useEffect(() => {
    onNavigate(navigate);
  }, [navigate, onNavigate]);
  return null;
}

describe("Auth state — persists across navigation", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(authApi.refresh).mockRejectedValue(new Error("no session"));
  });

  it("should not change isAuthenticated or accessToken when navigating between protected routes", async () => {
    const protectedRoutes = ["/", "/new-workout", "/my-performance", "/workout"];

    // Generate 100 pairs of (from, to) protected route paths
    const routePairArb = fc.record({
      from: fc.constantFrom(...protectedRoutes),
      to: fc.constantFrom(...protectedRoutes),
    });
    const samples = fc.sample(routePairArb, 100);

    let navigateFn: ReturnType<typeof useNavigate> | null = null;

    function testWrapper({ children }: { children: ReactNode }) {
      return (
        <MemoryRouter initialEntries={["/"]}>
          <AuthProvider>
            <NavigationHelper
              onNavigate={(nav) => {
                navigateFn = nav;
              }}
            />
            <Routes>
              {protectedRoutes.map((path) => (
                <Route key={path} path={path} element={children} />
              ))}
            </Routes>
          </AuthProvider>
        </MemoryRouter>
      );
    }

    const { result } = renderHook(() => useAuth(), { wrapper: testWrapper });

    // Wait for silent refresh to settle
    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    // Log in with a fixed token
    const token = "test-access-token-for-navigation";
    vi.mocked(authApi.login).mockResolvedValue({
      accessToken: token,
      tokenType: "Bearer",
      expiresIn: 900,
    });

    await act(async () => {
      await result.current.login("[email]", "[password]");
    });

    expect(result.current.isAuthenticated).toBe(true);
    expect(result.current.accessToken).toBe(token);

    // For each generated route pair, navigate and assert auth state is unchanged
    for (const { from, to } of samples) {
      act(() => {
        navigateFn!(from);
      });

      act(() => {
        navigateFn!(to);
      });

      // Post-condition: auth state must be identical after navigation
      expect(result.current.isAuthenticated).toBe(true);
      expect(result.current.accessToken).toBe(token);
    }
  }, 30_000);
});
