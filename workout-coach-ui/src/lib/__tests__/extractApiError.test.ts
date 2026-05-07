import { describe, it, expect } from "vitest";
import { AxiosError, type AxiosResponse } from "axios";
import { extractApiError } from "../authApi";

// Requirements: 1.1, 1.2

function makeAxiosError(responseData: unknown, status = 400): AxiosError {
  const error = new AxiosError(
    "Request failed",
    "ERR_BAD_REQUEST",
    undefined,
    undefined,
    {
      data: responseData,
      status,
      statusText: "Bad Request",
      headers: {},
      config: { headers: {} as never },
    } as AxiosResponse
  );
  return error;
}

describe("extractApiError", () => {
  it("should parse ApiErrorResponse shape and return the message", () => {
    const error = makeAxiosError({
      status: 401,
      error: "Unauthorized",
      message: "Invalid email or password",
      path: "/api/v1/auth/login",
      timestamp: "2026-01-01T00:00:00Z",
    }, 401);

    const result = extractApiError(error);

    expect(result.message).toBe("Invalid email or password");
    expect(result.fieldErrors).toBeUndefined();
  });

  it("should parse ValidationErrorResponse shape and return field errors", () => {
    const error = makeAxiosError({
      status: 400,
      error: "Validation Failed",
      errors: [
        { field: "email", message: "must be a valid email address" },
        { field: "password", message: "must be at least 8 characters" },
      ],
      path: "/api/v1/auth/register",
      timestamp: "2026-01-01T00:00:00Z",
    });

    const result = extractApiError(error);

    expect(result.message).toBe("Validation Failed");
    expect(result.fieldErrors).toEqual([
      { field: "email", message: "must be a valid email address" },
      { field: "password", message: "must be at least 8 characters" },
    ]);
  });

  it("should handle network errors with no response", () => {
    const error = new Error("Network Error");

    const result = extractApiError(error);

    expect(result.message).toBe(
      "Unable to connect. Please check your connection and try again."
    );
    expect(result.fieldErrors).toBeUndefined();
  });
});
