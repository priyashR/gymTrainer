import { useCallback, useEffect, useRef, useState } from "react";
import { Client } from "@stomp/stompjs";
import { useAuth } from "../features/auth/useAuth";
import * as sessionApi from "../lib/sessionApi";
import type {
  AdvanceSectionRequest,
  CompleteExerciseRequest,
  LogCrossFitScoreRequest,
  LogSetRequest,
  SessionResponse,
  SessionWebSocketMessage,
} from "../types/session";

export interface UseSessionResult {
  session: SessionResponse | null;
  loading: boolean;
  error: string | null;
  completeExercise: (sectionIndex: number, exerciseIndex: number) => Promise<void>;
  advanceSection: (targetSectionIndex: number) => Promise<void>;
  pauseSession: () => Promise<void>;
  resumeSession: () => Promise<void>;
  endSession: () => Promise<void>;
  logSet: (request: LogSetRequest) => Promise<void>;
  logCrossFitScore: (request: LogCrossFitScoreRequest) => Promise<void>;
}

/**
 * Hook that manages session state via REST fetch on mount and
 * real-time updates via STOMP WebSocket subscription.
 */
export function useSession(sessionId: string): UseSessionResult {
  const { accessToken } = useAuth();
  const [session, setSession] = useState<SessionResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const clientRef = useRef<Client | null>(null);

  // Fetch session state on mount
  useEffect(() => {
    let cancelled = false;

    setLoading(true);
    setError(null);

    sessionApi
      .getSession(sessionId)
      .then((data) => {
        if (!cancelled) {
          setSession(data);
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err?.response?.data?.message ?? "Failed to load session");
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [sessionId]);

  // Establish STOMP WebSocket connection
  useEffect(() => {
    if (!accessToken || !sessionId) return;

    const wsProtocol = window.location.protocol === "https:" ? "wss:" : "ws:";
    const wsUrl = `${wsProtocol}//${window.location.host}/ws/sessions?token=${encodeURIComponent(accessToken)}`;

    const client = new Client({
      brokerURL: wsUrl,
      reconnectDelay: 3000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
    });

    client.onConnect = () => {
      client.subscribe(`/topic/sessions/${sessionId}`, (message) => {
        try {
          const parsed: SessionWebSocketMessage = JSON.parse(message.body);

          if (parsed.type === "SESSION_STATE_UPDATE") {
            setSession((prev) =>
              prev
                ? {
                    ...prev,
                    status: parsed.payload.status,
                    currentSectionIndex: parsed.payload.currentSectionIndex,
                    sectionProgresses: parsed.payload.sectionProgresses,
                  }
                : prev
            );
          } else if (parsed.type === "SESSION_COMPLETED") {
            setSession((prev) =>
              prev
                ? {
                    ...prev,
                    status: "COMPLETED",
                    completedAt: parsed.payload.completedAt,
                  }
                : prev
            );
          }
        } catch {
          // Ignore malformed messages
        }
      });
    };

    client.onStompError = (frame) => {
      console.error("STOMP error:", frame.headers["message"]);
    };

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
      clientRef.current = null;
    };
  }, [accessToken, sessionId]);

  // --- Mutation functions ---

  const completeExercise = useCallback(
    async (sectionIndex: number, exerciseIndex: number): Promise<void> => {
      const request: CompleteExerciseRequest = { sectionIndex, exerciseIndex };

      // Optimistic update
      setSession((prev) => {
        if (!prev) return prev;
        const updatedProgresses = prev.sectionProgresses.map((sp) => {
          if (sp.sectionIndex !== sectionIndex) return sp;
          return {
            ...sp,
            exerciseLogs: sp.exerciseLogs.map((log) => {
              if (log.exerciseIndex !== exerciseIndex) return log;
              return {
                ...log,
                completed: true,
                completedAt: new Date().toISOString(),
              };
            }),
          };
        });
        return { ...prev, sectionProgresses: updatedProgresses };
      });

      try {
        const updated = await sessionApi.completeExercise(sessionId, request);
        setSession(updated);
      } catch (err: unknown) {
        // Revert optimistic update by re-fetching
        const fresh = await sessionApi.getSession(sessionId);
        setSession(fresh);
        throw err;
      }
    },
    [sessionId]
  );

  const advanceSection = useCallback(
    async (targetSectionIndex: number): Promise<void> => {
      const request: AdvanceSectionRequest = { targetSectionIndex };
      const updated = await sessionApi.advanceSection(sessionId, request);
      setSession(updated);
    },
    [sessionId]
  );

  const pauseSession = useCallback(async (): Promise<void> => {
    const updated = await sessionApi.pauseSession(sessionId);
    setSession(updated);
  }, [sessionId]);

  const resumeSession = useCallback(async (): Promise<void> => {
    const updated = await sessionApi.resumeSession(sessionId);
    setSession(updated);
  }, [sessionId]);

  const endSession = useCallback(async (): Promise<void> => {
    const updated = await sessionApi.endSession(sessionId);
    setSession(updated);
  }, [sessionId]);

  const logSet = useCallback(
    async (request: LogSetRequest): Promise<void> => {
      const updated = await sessionApi.logSet(sessionId, request);
      setSession(updated);
    },
    [sessionId]
  );

  const logCrossFitScore = useCallback(
    async (request: LogCrossFitScoreRequest): Promise<void> => {
      const updated = await sessionApi.logCrossFitScore(sessionId, request);
      setSession(updated);
    },
    [sessionId]
  );

  return {
    session,
    loading,
    error,
    completeExercise,
    advanceSection,
    pauseSession,
    resumeSession,
    endSession,
    logSet,
    logCrossFitScore,
  };
}
