import { renderHook, waitFor } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { usePerformanceData } from "../usePerformanceData";
import type {
  TopExerciseEntry,
  MonthlyFrequencyEntry,
} from "../usePerformanceData";

// Mock apiClient
const mockGet = vi.fn();
vi.mock("../../lib/apiClient", () => ({
  default: {
    get: (...args: unknown[]) => mockGet(...args),
  },
}));

const mockTopExercises: TopExerciseEntry[] = [
  { exerciseName: "Back Squat", maxWeight: 140, unit: "kg" },
  { exerciseName: "Bench Press", maxWeight: 100, unit: "kg" },
  { exerciseName: "Deadlift", maxWeight: 180, unit: "kg" },
];

const mockMonthlyFrequency: MonthlyFrequencyEntry[] = [
  { month: "Jan", count: 12 },
  { month: "Feb", count: 15 },
  { month: "Mar", count: 10 },
];

describe("usePerformanceData", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("successful fetch", () => {
    it("fetches both endpoints concurrently and returns data", async () => {
      mockGet.mockImplementation((url: string) => {
        if (url === "/stats/top-exercises") {
          return Promise.resolve({ data: mockTopExercises });
        }
        if (url === "/stats/monthly-frequency") {
          return Promise.resolve({ data: mockMonthlyFrequency });
        }
        return Promise.reject(new Error("Unknown endpoint"));
      });

      const { result } = renderHook(() => usePerformanceData());

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(mockGet).toHaveBeenCalledWith("/stats/top-exercises");
      expect(mockGet).toHaveBeenCalledWith("/stats/monthly-frequency");
      expect(result.current.topExercises).toEqual(mockTopExercises);
      expect(result.current.monthlyFrequency).toEqual(mockMonthlyFrequency);
      expect(result.current.error).toBeNull();
    });
  });

  describe("loading state", () => {
    it("starts with isLoading true and both fields null", () => {
      mockGet.mockImplementation(() => new Promise(() => {}));

      const { result } = renderHook(() => usePerformanceData());

      expect(result.current.isLoading).toBe(true);
      expect(result.current.topExercises).toBeNull();
      expect(result.current.monthlyFrequency).toBeNull();
    });

    it("transitions isLoading to false after fetch completes", async () => {
      mockGet.mockResolvedValue({ data: [] });

      const { result } = renderHook(() => usePerformanceData());

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });
    });
  });

  describe("partial availability", () => {
    it("returns topExercises data and sample monthlyFrequency when monthly-frequency fails", async () => {
      mockGet.mockImplementation((url: string) => {
        if (url === "/stats/top-exercises") {
          return Promise.resolve({ data: mockTopExercises });
        }
        return Promise.reject(new Error("Service unavailable"));
      });

      const { result } = renderHook(() => usePerformanceData());

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.topExercises).toEqual(mockTopExercises);
      // Falls back to sample data when endpoint fails
      expect(result.current.monthlyFrequency).not.toBeNull();
      expect(result.current.monthlyFrequency!.length).toBeGreaterThan(0);
      expect(result.current.error).toBeNull();
    });

    it("returns monthlyFrequency data and sample topExercises when top-exercises fails", async () => {
      mockGet.mockImplementation((url: string) => {
        if (url === "/stats/monthly-frequency") {
          return Promise.resolve({ data: mockMonthlyFrequency });
        }
        return Promise.reject(new Error("Service unavailable"));
      });

      const { result } = renderHook(() => usePerformanceData());

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      // Falls back to sample data when endpoint fails
      expect(result.current.topExercises).not.toBeNull();
      expect(result.current.topExercises!.length).toBeGreaterThan(0);
      expect(result.current.monthlyFrequency).toEqual(mockMonthlyFrequency);
      expect(result.current.error).toBeNull();
    });
  });

  describe("error handling (graceful fallback)", () => {
    it("returns sample data for both fields when both endpoints fail", async () => {
      mockGet.mockRejectedValue(new Error("Network error"));

      const { result } = renderHook(() => usePerformanceData());

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      // Falls back to sample data when API is unavailable
      expect(result.current.topExercises).not.toBeNull();
      expect(result.current.topExercises!.length).toBeGreaterThan(0);
      expect(result.current.monthlyFrequency).not.toBeNull();
      expect(result.current.monthlyFrequency!.length).toBeGreaterThan(0);
      expect(result.current.error).toBeNull();
    });

    it("returns sample data when endpoints return 404 (not yet built)", async () => {
      mockGet.mockRejectedValue({
        response: { status: 404, data: { message: "Not Found" } },
      });

      const { result } = renderHook(() => usePerformanceData());

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      // Falls back to sample data when API is unavailable
      expect(result.current.topExercises).not.toBeNull();
      expect(result.current.monthlyFrequency).not.toBeNull();
      expect(result.current.error).toBeNull();
    });

    it("error field always returns null regardless of failures", async () => {
      mockGet.mockRejectedValue({
        response: { data: { message: "Internal Server Error" } },
      });

      const { result } = renderHook(() => usePerformanceData());

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.error).toBeNull();
    });
  });
});
