import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import fc from "fast-check";
import { AuthContext, type AuthContextValue } from "../../../features/auth/AuthContext";
import { ProtectedRoute } from "../ProtectedRoute";

// Feature: workout-coach-ui-mvp1, Property 5: Protected routes redirect unauthenticated users

const noop = () => {};
const noopAsync = async () => ({} as never);

function makeUnauthenticatedContext(): AuthContextValue {
  return {
    accessToken: null,
    isAuthenticated: false,
    isLoading: false,
    login: noopAsync,
    register: noopAsync,
    logout: noop,
  };
}

/**
 * Arbitrary that generates URL-safe route paths excluding /login and /register.
 * Produces paths like "/dashboard", "/a/b/c", "/workout", etc.
 */
const urlSafeChar = fc.constantFrom(
  ...("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_".split(""))
);
const protectedPathArb = fc
  .array(
    fc.stringOf(urlSafeChar, { minLength: 1, maxLength: 12 }),
    { minLength: 1, maxLength: 4 }
  )
  .map((segments) => "/" + segments.join("/"))
  .filter((path) => path !== "/login" && path !== "/register");

describe("ProtectedRoute — redirect for unauthenticated users", () => {
  it("should redirect to /login for any protected route when unauthenticated", () => {
    fc.assert(
      fc.property(protectedPathArb, (path) => {
        const context = makeUnauthenticatedContext();

        const { unmount } = render(
          <AuthContext.Provider value={context}>
            <MemoryRouter initialEntries={[path]}>
              <Routes>
                <Route
                  path="*"
                  element={
                    <ProtectedRoute>
                      <div data-testid="protected-content">Secret</div>
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/login"
                  element={<div data-testid="login-page">Login</div>}
                />
              </Routes>
            </MemoryRouter>
          </AuthContext.Provider>
        );

        // Should have redirected to /login
        expect(screen.getByTestId("login-page")).toBeDefined();
        // Protected content should NOT be rendered
        expect(screen.queryByTestId("protected-content")).toBeNull();

        unmount();
      }),
      { numRuns: 100 }
    );
  });
});
