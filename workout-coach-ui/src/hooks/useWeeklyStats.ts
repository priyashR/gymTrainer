import { useEffect, useState } from "react";
import apiClient from "../lib/apiClient";

export interface WeeklyStatsData {
  completedCount: number;
  goalCount: number;
  dailyData: boolean[];
}

export interface UseWeeklyStatsResult {
  data: WeeklyStatsData | null;
  isLoading: boolean;
  error: string | null;
}

/**
 * Sample data used when the Progress Tracker Service is unavailable.
 * TODO: Remove once GET /api/v1/stats/weekly is implemented.
 */
const SAMPLE_WEEKLY_STATS: WeeklyStatsData = {
  completedCount: 4,
  goalCount: 5,
  dailyData: [true, true, false, true, true, false, false], // M-T-W-T-F-S-S
};

/**
 * Fetches weekly workout stats from the Progress Tracker Service.
 *
 * The endpoint (`GET /api/v1/stats/weekly`) does not exist yet.
 * Falls back to sample data when the service is unavailable.
 */
export function useWeeklyStats(): UseWeeklyStatsResult {
  const [data, setData] = useState<WeeklyStatsData | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    setIsLoading(true);
    setError(null);

    apiClient
      .get<WeeklyStatsData>("/stats/weekly")
      .then((res) => {
        if (!cancelled) {
          const d = res.data;
          if (d && typeof d === "object" && "completedCount" in d) {
            setData(d);
          } else {
            // Invalid response shape — use sample data
            setData(SAMPLE_WEEKLY_STATS);
          }
        }
      })
      .catch(() => {
        if (!cancelled) {
          // Service unavailable — use sample data for demo purposes
          setData(SAMPLE_WEEKLY_STATS);
          setError(null);
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
  }, []);

  return { data, isLoading, error };
}
