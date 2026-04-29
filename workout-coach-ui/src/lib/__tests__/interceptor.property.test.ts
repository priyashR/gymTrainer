import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import fc from "fast-check";
import MockAdapter from "axios-mock-adapter";

// Feature: workout-coach-ui-mvp1, Property 3: 401 interceptor triggers refresh and retries the original request

import apiClient, {
  setAccessTokenGetter,
  setOnRefreshFailure,
  setOnTokenRefreshed,
} from "../apiClient";

describe("401 interceptor — triggers refresh and retries", () => {
  let mock: MockAdapter;

  beforeEach(() => {
    mock = new MockAdapter(apiClient);
  });

  afterEach(() => {
    mock.restore();
    setAccessTokenGetter(() => null);
    setOnRefreshFailure(() => {});
    setOnTokenRefreshed(() => {});
  });

  /**
   * Arbitrary that generates URL-safe protected endpoint paths,
   * excluding auth endpoints.
   */
  const protectedPathArb = fc
    .array(
      fc.stringOf(
        fc.constantFrom(..."abcdefghijklmnopqrstuvwxyz0123456789".split("")),
        { minLength: 1, maxLength: 8 }
      ),
      { minLength: 1, maxLength: 3 }
    )
    .map((segments) => "/" + segments.join("/"))
    .filter(
      (path) =>
        !path.includes("/auth/login") &&
        !path.includes("/auth/register") &&
        !path.includes("/auth/refresh")
    );

  const newTokenArb = fc.stringOf(
    fc.constantFrom(..."abcdefghijklmnopqrstuvwxyz0123456789".split("")),
    { minLength: 10, maxLength: 32 }
  );

  // **Validates: Requirements 1.4**
  it("should call refresh and retry the original request with the new token on 401", async () => {
    await fc.assert(
      fc.asyncProperty(protectedPathArb, newTokenArb, async (path, newToken) => {
        const tokenRefreshed = vi.fn();
        const refreshFailure = vi.fn();

        setAccessTokenGetter(() => "expired-token");
        setOnRefreshFailure(refreshFailure);
        setOnTokenRefreshed(tokenRefreshed);

        mock.reset();

        let callCount = 0;
        // Use regex to match any GET request — the mock adapter prepends baseURL
        mock.onGet(new RegExp(path.replace(/[.*+?^${}()|[\]\\]/g, "\\$&") + "$")).reply(() => {
          callCount++;
          if (callCount === 1) {
            return [401, { message: "Unauthorized" }];
          }
          return [200, { data: "success" }];
        });

        mock.onPost("/auth/refresh").reply(200, { accessToken: newToken });

        const response = await apiClient.get(path);

        expect(response.status).toBe(200);
        expect(response.data).toEqual({ data: "success" });
        expect(tokenRefreshed).toHaveBeenCalledWith(newToken);
        expect(callCount).toBe(2);
        expect(refreshFailure).not.toHaveBeenCalled();
      }),
      { numRuns: 100 }
    );
  });
});
