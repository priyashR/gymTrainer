import { useEffect, useRef, useState } from "react";
import { Client } from "@stomp/stompjs";
import { useAuth } from "../features/auth/useAuth";
import apiClient from "../lib/apiClient";
import type {
  ExerciseRecommendationDto,
  RecommendationsResponse,
} from "../types/recommendation";

export interface UseRecommendationsResult {
  recommendations: ExerciseRecommendationDto[];
  isLoading: boolean;
  error: string | null;
}

/**
 * Fetches exercise recommendations for the current section via REST on mount
 * and section change, and merges with real-time WebSocket updates from the
 * STOMP session state messages.
 */
export function useRecommendations(
  sessionId: string,
  currentSectionIndex: number
): UseRecommendationsResult {
  const { accessToken } = useAuth();
  const [recommendations, setRecommendations] = useState<ExerciseRecommendationDto[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const clientRef = useRef<Client | null>(null);

  // Fetch recommendations via REST on mount and when section changes
  useEffect(() => {
    let cancelled = false;

    setIsLoading(true);
    setError(null);

    apiClient
      .get<RecommendationsResponse>(`/sessions/${sessionId}/recommendations`)
      .then((res) => {
        if (!cancelled) {
          const section = res.data.sections.find(
            (s) => s.sectionIndex === currentSectionIndex
          );
          setRecommendations(section?.exercises ?? []);
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err?.response?.data?.message ?? "Failed to load recommendations");
          setRecommendations([]);
        }
      })
      .finally(() => {
        if (!cancelled) {
          setIsLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [sessionId, currentSectionIndex]);

  // Subscribe to STOMP WebSocket for real-time recommendation updates
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
          const parsed = JSON.parse(message.body);

          if (
            parsed.type === "SESSION_STATE_UPDATE" &&
            Array.isArray(parsed.payload?.recommendations)
          ) {
            setRecommendations(parsed.payload.recommendations);
          }
        } catch {
          // Ignore malformed messages
        }
      });
    };

    client.onStompError = (frame) => {
      console.error("STOMP error (recommendations):", frame.headers["message"]);
    };

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
      clientRef.current = null;
    };
  }, [accessToken, sessionId]);

  return { recommendations, isLoading, error };
}
