import { renderHook, waitFor, act } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { useRecommendations } from "../useRecommendations";
import type { RecommendationsResponse } from "../../types/recommendation";

// Mock the auth hook
vi.mock("../../features/auth/useAuth", () => ({
  useAuth: () => ({ accessToken: "test-jwt-token" }),
}));

// Use vi.hoisted to ensure mock variables are available in the hoisted vi.mock factory
const {
  mockActivate,
  mockDeactivate,
  mockSubscribe,
  getSubscriptionCallback,
  resetCaptured,
} = vi.hoisted(() => {
  let subscriptionCallback: ((message: { body: string }) => void) | null = null;

  const mockSubscribe = vi.fn().mockImplementation((_topic: string, callback: (message: { body: string }) => void) => {
    subscriptionCallback = callback;
    return { unsubscribe: vi.fn() };
  });

  const mockDeactivate = vi.fn().mockResolvedValue(undefined);
  const mockActivate = vi.fn();

  return {
    mockActivate,
    mockDeactivate,
    mockSubscribe,
    getSubscriptionCallback: () => subscriptionCallback,
    resetCaptured: () => {
      subscriptionCallback = null;
    },
  };
});

// Mock @stomp/stompjs
vi.mock("@stomp/stompjs", () => {
  return {
    Client: class MockClient {
      onConnect: (() => void) | null = null;
      onStompError: (() => void) | null = null;

      subscribe = mockSubscribe;
      deactivate = mockDeactivate;

      activate() {
        mockActivate();
        if (this.onConnect) {
          this.onConnect();
        }
      }
    },
  };
});

// Mock apiClient
const mockGet = vi.fn();
vi.mock("../../lib/apiClient", () => ({
  default: {
    get: (...args: unknown[]) => mockGet(...args),
  },
}));

const mockRecommendationsResponse: RecommendationsResponse = {
  sections: [
    {
      sectionIndex: 0,
      exercises: [
        { exerciseIndex: 0, prescribedWeight: "80kg", prescribedReps: "8-10", prescribedSets: 4 },
        { exerciseIndex: 1, prescribedWeight: "60kg", prescribedReps: "12", prescribedSets: 3 },
      ],
    },
    {
      sectionIndex: 1,
      exercises: [
        { exerciseIndex: 0, prescribedWeight: "40kg", prescribedReps: null, prescribedSets: null },
      ],
    },
  ],
};

describe("useRecommendations", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    resetCaptured();
    mockGet.mockResolvedValue({ data: mockRecommendationsResponse });
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  describe("REST fetch on mount", () => {
    it("fetches recommendations and populates state with correct section exercises", async () => {
      const { result } = renderHook(() => useRecommendations("session-123", 0));

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(mockGet).toHaveBeenCalledWith("/sessions/session-123/recommendations");
      expect(result.current.recommendations).toEqual([
        { exerciseIndex: 0, prescribedWeight: "80kg", prescribedReps: "8-10", prescribedSets: 4 },
        { exerciseIndex: 1, prescribedWeight: "60kg", prescribedReps: "12", prescribedSets: 3 },
      ]);
      expect(result.current.error).toBeNull();
    });

    it("returns exercises for the requested section index", async () => {
      const { result } = renderHook(() => useRecommendations("session-123", 1));

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.recommendations).toEqual([
        { exerciseIndex: 0, prescribedWeight: "40kg", prescribedReps: null, prescribedSets: null },
      ]);
    });

    it("returns empty array when section index is not found in response", async () => {
      const { result } = renderHook(() => useRecommendations("session-123", 99));

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.recommendations).toEqual([]);
      expect(result.current.error).toBeNull();
    });
  });

  describe("loading state transitions", () => {
    it("starts with isLoading true", () => {
      const { result } = renderHook(() => useRecommendations("session-123", 0));

      expect(result.current.isLoading).toBe(true);
    });

    it("transitions isLoading to false after successful fetch", async () => {
      const { result } = renderHook(() => useRecommendations("session-123", 0));

      expect(result.current.isLoading).toBe(true);

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });
    });

    it("transitions isLoading to false after failed fetch", async () => {
      mockGet.mockRejectedValue({ response: { data: { message: "Not found" } } });

      const { result } = renderHook(() => useRecommendations("session-123", 0));

      expect(result.current.isLoading).toBe(true);

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });
    });
  });

  describe("error handling", () => {
    it("sets error message and empty recommendations on fetch failure", async () => {
      mockGet.mockRejectedValue({
        response: { data: { message: "Session not found" } },
      });

      const { result } = renderHook(() => useRecommendations("session-123", 0));

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.error).toBe("Session not found");
      expect(result.current.recommendations).toEqual([]);
    });

    it("sets generic error message when error has no response data", async () => {
      mockGet.mockRejectedValue(new Error("Network error"));

      const { result } = renderHook(() => useRecommendations("session-123", 0));

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.error).toBe("Failed to load recommendations");
      expect(result.current.recommendations).toEqual([]);
    });
  });

  describe("WebSocket update merges", () => {
    it("updates recommendations when SESSION_STATE_UPDATE message arrives with recommendations", async () => {
      const { result } = renderHook(() => useRecommendations("session-123", 0));

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      // Wait for WebSocket subscription to be established
      await waitFor(() => {
        expect(getSubscriptionCallback()).not.toBeNull();
      });

      // Simulate a SESSION_STATE_UPDATE message with recommendations
      const wsMessage = {
        body: JSON.stringify({
          type: "SESSION_STATE_UPDATE",
          payload: {
            recommendations: [
              { exerciseIndex: 0, prescribedWeight: "90kg", prescribedReps: "6-8", prescribedSets: 5 },
            ],
          },
        }),
      };

      act(() => {
        getSubscriptionCallback()!(wsMessage);
      });

      expect(result.current.recommendations).toEqual([
        { exerciseIndex: 0, prescribedWeight: "90kg", prescribedReps: "6-8", prescribedSets: 5 },
      ]);
    });

    it("ignores WebSocket messages without SESSION_STATE_UPDATE type", async () => {
      const { result } = renderHook(() => useRecommendations("session-123", 0));

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      await waitFor(() => {
        expect(getSubscriptionCallback()).not.toBeNull();
      });

      const originalRecommendations = result.current.recommendations;

      const wsMessage = {
        body: JSON.stringify({
          type: "SESSION_COMPLETED",
          payload: { sessionId: "session-123" },
        }),
      };

      act(() => {
        getSubscriptionCallback()!(wsMessage);
      });

      expect(result.current.recommendations).toEqual(originalRecommendations);
    });

    it("ignores WebSocket messages with malformed JSON", async () => {
      const { result } = renderHook(() => useRecommendations("session-123", 0));

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      await waitFor(() => {
        expect(getSubscriptionCallback()).not.toBeNull();
      });

      const originalRecommendations = result.current.recommendations;

      const wsMessage = { body: "not valid json" };

      act(() => {
        getSubscriptionCallback()!(wsMessage);
      });

      expect(result.current.recommendations).toEqual(originalRecommendations);
    });

    it("ignores SESSION_STATE_UPDATE messages without recommendations array", async () => {
      const { result } = renderHook(() => useRecommendations("session-123", 0));

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      await waitFor(() => {
        expect(getSubscriptionCallback()).not.toBeNull();
      });

      const originalRecommendations = result.current.recommendations;

      const wsMessage = {
        body: JSON.stringify({
          type: "SESSION_STATE_UPDATE",
          payload: { status: "IN_PROGRESS" },
        }),
      };

      act(() => {
        getSubscriptionCallback()!(wsMessage);
      });

      expect(result.current.recommendations).toEqual(originalRecommendations);
    });
  });
});
