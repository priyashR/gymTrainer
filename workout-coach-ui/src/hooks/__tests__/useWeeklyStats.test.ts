import { renderHook, waitFor } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { useWeeklyStats } from "../useWeeklyStats";
import type { WeeklyStatsData } from "../useWeeklyStats";

// Mock apiClient
const mockGet = vi.fn();
vi.mock("../../lib/apiClient", () => ({
  default: {
    get: (...args: unknown[]) => mockGet(...args),
  },
}));

const mockWeeklyStats: WeeklyStatsData = {
  completedCount: 3,
  goalCount: 7,
  dailyData: [true, true, false, true, false, false, false],
};

describe("useWeeklyStats", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("successful fetch", () => {
    it("fetches weekly stats and returns data", async () => {
      mockGet.mockResolvedValue({ data: mockWeeklyStats });

      const { result } = renderHook(() => useWeeklyStats());

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(mockGet).toHaveBeenCalledWith("/stats/weekly");
      expect(result.current.data).toEqual(mockWeeklyStats);
      expect(result.current.error).toBeNull();
    });
  });

  describe("loading state", () => {
    it("starts with isLoading true and data null", () => {
      mockGet.mockResolvedValue({ data: mockWeeklyStats });

      const { result } = renderHook(() => useWeeklyStats());

      expect(result.current.isLoading).toBe(true);
      expect(result.current.data).toBeNull();
    });

    it("transitions isLoading to false after successful fetch", async () => {
      mockGet.mockResolvedValue({ data: mockWeeklyStats });

      const { result } = renderHook(() => useWeeklyStats());

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });
    });

    it("transitions isLoading to false after failed fetch", async () => {
      mockGet.mockRejectedValue(new Error("Network error"));

      const { result } = renderHook(() => useWeeklyStats());

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });
    });
  });

  describe("error handling (graceful fallback)", () => {
    it("returns sample data when service returns an error response", async () => {
      mockGet.mockRejectedValue({
        response: { data: { message: "Service unavailable" } },
      });

      const { result } = renderHook(() => useWeeklyStats());

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      // Falls back to sample data when API is unavailable
      expect(result.current.data).not.toBeNull();
      expect(result.current.data?.completedCount).toBeDefined();
      expect(result.current.data?.goalCount).toBeDefined();
      expect(result.current.data?.dailyData).toHaveLength(7);
      expect(result.current.error).toBeNull();
    });

    it("returns sample data on network failure", async () => {
      mockGet.mockRejectedValue(new Error("Network error"));

      const { result } = renderHook(() => useWeeklyStats());

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      // Falls back to sample data when API is unavailable
      expect(result.current.data).not.toBeNull();
      expect(result.current.data?.completedCount).toBeDefined();
      expect(result.current.error).toBeNull();
    });

    it("returns sample data when endpoint returns 404 (not yet built)", async () => {
      mockGet.mockRejectedValue({
        response: { status: 404, data: { message: "Not Found" } },
      });

      const { result } = renderHook(() => useWeeklyStats());

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      // Falls back to sample data when API is unavailable
      expect(result.current.data).not.toBeNull();
      expect(result.current.error).toBeNull();
    });
  });
});
