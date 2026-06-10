import { renderHook, waitFor, act } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { useSession } from "../useSession";
import * as sessionApi from "../../lib/sessionApi";
import type { SessionResponse } from "../../types/session";

// Mock the auth hook
vi.mock("../../features/auth/useAuth", () => ({
  useAuth: () => ({ accessToken: "test-jwt-token" }),
}));

// Mock the session API
vi.mock("../../lib/sessionApi");

// Use vi.hoisted to ensure mock variables are available in the hoisted vi.mock factory
const {
  mockActivate,
  mockDeactivate,
  mockSubscribe,
  getSubscriptionCallback,
  getSubscriptionTopic,
  resetCaptured,
} = vi.hoisted(() => {
  let subscriptionCallback: ((message: { body: string }) => void) | null = null;
  let subscriptionTopic: string | null = null;

  const mockSubscribe = vi.fn().mockImplementation((topic: string, callback: (message: { body: string }) => void) => {
    subscriptionTopic = topic;
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
    getSubscriptionTopic: () => subscriptionTopic,
    resetCaptured: () => {
      subscriptionCallback = null;
      subscriptionTopic = null;
    },
  };
});

// Mock @stomp/stompjs — the factory can reference hoisted variables
vi.mock("@stomp/stompjs", () => {
  return {
    Client: class MockClient {
      onConnect: (() => void) | null = null;
      onStompError: (() => void) | null = null;

      subscribe = mockSubscribe;
      deactivate = mockDeactivate;

      activate() {
        mockActivate();
        // Trigger onConnect synchronously to simulate connection established
        if (this.onConnect) {
          this.onConnect();
        }
      }
    },
  };
});

const mockSession: SessionResponse = {
  id: "session-123",
  status: "IN_PROGRESS",
  currentSectionIndex: 0,
  sectionProgresses: [
    {
      sectionIndex: 0,
      sectionName: "Strength",
      sectionType: "STRENGTH",
      exerciseLogs: [
        { exerciseIndex: 0, exerciseName: "Squat", completed: false, completedAt: null, setLogs: [] },
        { exerciseIndex: 1, exerciseName: "Bench", completed: false, completedAt: null, setLogs: [] },
      ],
      completed: false,
      crossFitScore: null,
      roundCount: 0,
    },
  ],
  workoutSnapshot: { sections: [] },
  startedAt: "2026-01-15T10:00:00Z",
  pausedAt: null,
  completedAt: null,
  durationSeconds: null,
};

describe("useSession", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    resetCaptured();
    vi.mocked(sessionApi.getSession).mockResolvedValue(mockSession);
    vi.mocked(sessionApi.completeExercise).mockResolvedValue({
      ...mockSession,
      sectionProgresses: [
        {
          ...mockSession.sectionProgresses[0],
          exerciseLogs: [
            { exerciseIndex: 0, exerciseName: "Squat", completed: true, completedAt: "2026-01-15T10:30:00Z", setLogs: [] },
            { exerciseIndex: 1, exerciseName: "Bench", completed: false, completedAt: null, setLogs: [] },
          ],
        },
      ],
    });
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it("fetches session state on mount via REST", async () => {
    const { result } = renderHook(() => useSession("session-123"));

    expect(result.current.loading).toBe(true);

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(sessionApi.getSession).toHaveBeenCalledWith("session-123");
    expect(result.current.session).toEqual(mockSession);
    expect(result.current.error).toBeNull();
  });

  it("sets error state when REST fetch fails", async () => {
    vi.mocked(sessionApi.getSession).mockRejectedValue({
      response: { data: { message: "Session not found" } },
    });

    const { result } = renderHook(() => useSession("bad-id"));

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.error).toBe("Session not found");
    expect(result.current.session).toBeNull();
  });

  it("sets generic error message when error has no response data", async () => {
    vi.mocked(sessionApi.getSession).mockRejectedValue(new Error("Network error"));

    const { result } = renderHook(() => useSession("session-123"));

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.error).toBe("Failed to load session");
  });

  it("establishes WebSocket connection with access token", async () => {
    renderHook(() => useSession("session-123"));

    await waitFor(() => {
      expect(mockActivate).toHaveBeenCalled();
    });
  });

  it("deactivates WebSocket on unmount", async () => {
    const { unmount } = renderHook(() => useSession("session-123"));

    await waitFor(() => {
      expect(mockActivate).toHaveBeenCalled();
    });

    unmount();

    expect(mockDeactivate).toHaveBeenCalled();
  });

  it("performs optimistic update on exercise checkoff", async () => {
    const { result } = renderHook(() => useSession("session-123"));

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    // Trigger completeExercise
    await act(async () => {
      await result.current.completeExercise(0, 0);
    });

    expect(sessionApi.completeExercise).toHaveBeenCalledWith("session-123", {
      sectionIndex: 0,
      exerciseIndex: 0,
    });

    // After server response, session should be updated
    expect(result.current.session?.sectionProgresses[0].exerciseLogs[0].completed).toBe(true);
  });

  it("reverts optimistic update on API failure", async () => {
    vi.mocked(sessionApi.completeExercise).mockRejectedValue(new Error("Server error"));
    // getSession is called again to revert
    vi.mocked(sessionApi.getSession).mockResolvedValue(mockSession);

    const { result } = renderHook(() => useSession("session-123"));

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    // Trigger completeExercise — should throw
    await expect(
      act(async () => {
        await result.current.completeExercise(0, 0);
      })
    ).rejects.toThrow("Server error");

    // Session should be reverted to the fresh fetch
    expect(result.current.session?.sectionProgresses[0].exerciseLogs[0].completed).toBe(false);
  });

  it("calls advanceSection API and updates session", async () => {
    const advancedSession: SessionResponse = {
      ...mockSession,
      currentSectionIndex: 1,
    };
    vi.mocked(sessionApi.advanceSection).mockResolvedValue(advancedSession);

    const { result } = renderHook(() => useSession("session-123"));

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    await act(async () => {
      await result.current.advanceSection(1);
    });

    expect(sessionApi.advanceSection).toHaveBeenCalledWith("session-123", {
      targetSectionIndex: 1,
    });
    expect(result.current.session?.currentSectionIndex).toBe(1);
  });

  it("calls pauseSession API and updates session", async () => {
    const pausedSession: SessionResponse = {
      ...mockSession,
      status: "PAUSED",
      pausedAt: "2026-01-15T10:30:00Z",
    };
    vi.mocked(sessionApi.pauseSession).mockResolvedValue(pausedSession);

    const { result } = renderHook(() => useSession("session-123"));

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    await act(async () => {
      await result.current.pauseSession();
    });

    expect(sessionApi.pauseSession).toHaveBeenCalledWith("session-123");
    expect(result.current.session?.status).toBe("PAUSED");
  });

  it("calls endSession API and updates session", async () => {
    const completedSession: SessionResponse = {
      ...mockSession,
      status: "COMPLETED",
      completedAt: "2026-01-15T11:00:00Z",
    };
    vi.mocked(sessionApi.endSession).mockResolvedValue(completedSession);

    const { result } = renderHook(() => useSession("session-123"));

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    await act(async () => {
      await result.current.endSession();
    });

    expect(sessionApi.endSession).toHaveBeenCalledWith("session-123");
    expect(result.current.session?.status).toBe("COMPLETED");
  });

  describe("WebSocket message handling", () => {
    it("subscribes to the correct topic /topic/sessions/{sessionId}", async () => {
      renderHook(() => useSession("session-123"));

      await waitFor(() => {
        expect(getSubscriptionTopic()).toBe("/topic/sessions/session-123");
      });
    });

    it("updates session state on SESSION_STATE_UPDATE message", async () => {
      const { result } = renderHook(() => useSession("session-123"));

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      // Wait for WebSocket subscription to be established
      await waitFor(() => {
        expect(getSubscriptionCallback()).not.toBeNull();
      });

      // Simulate a SESSION_STATE_UPDATE message arriving
      const updateMessage = {
        body: JSON.stringify({
          type: "SESSION_STATE_UPDATE",
          payload: {
            sessionId: "session-123",
            status: "IN_PROGRESS",
            currentSectionIndex: 1,
            sectionProgresses: [
              {
                sectionIndex: 0,
                sectionName: "Strength",
                sectionType: "STRENGTH",
                exerciseLogs: [
                  { exerciseIndex: 0, exerciseName: "Squat", completed: true, completedAt: "2026-01-15T10:30:00Z" },
                  { exerciseIndex: 1, exerciseName: "Bench", completed: true, completedAt: "2026-01-15T10:35:00Z" },
                ],
                completed: true,
              },
            ],
            lastPersistedAt: "2026-01-15T10:35:00Z",
          },
        }),
      };

      act(() => {
        getSubscriptionCallback()!(updateMessage);
      });

      expect(result.current.session?.currentSectionIndex).toBe(1);
      expect(result.current.session?.sectionProgresses[0].exerciseLogs[0].completed).toBe(true);
      expect(result.current.session?.sectionProgresses[0].exerciseLogs[1].completed).toBe(true);
      expect(result.current.session?.sectionProgresses[0].completed).toBe(true);
    });

    it("updates session status and completedAt on SESSION_COMPLETED message", async () => {
      const { result } = renderHook(() => useSession("session-123"));

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      // Wait for WebSocket subscription to be established
      await waitFor(() => {
        expect(getSubscriptionCallback()).not.toBeNull();
      });

      // Simulate a SESSION_COMPLETED message arriving
      const completedMessage = {
        body: JSON.stringify({
          type: "SESSION_COMPLETED",
          payload: {
            sessionId: "session-123",
            completedAt: "2026-01-15T11:00:00Z",
          },
        }),
      };

      act(() => {
        getSubscriptionCallback()!(completedMessage);
      });

      expect(result.current.session?.status).toBe("COMPLETED");
      expect(result.current.session?.completedAt).toBe("2026-01-15T11:00:00Z");
    });
  });
});
