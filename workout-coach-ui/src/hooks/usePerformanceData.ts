import { useEffect, useState } from "react";
import apiClient from "../lib/apiClient";

export interface TopExerciseEntry {
  exerciseName: string;
  maxWeight: number;
  unit: string;
}

export interface MonthlyFrequencyEntry {
  month: string;
  count: number;
}

export interface UsePerformanceDataResult {
  topExercises: TopExerciseEntry[] | null;
  monthlyFrequency: MonthlyFrequencyEntry[] | null;
  isLoading: boolean;
  error: null;
}

/**
 * Sample data used when the Progress Tracker Service is unavailable.
 * TODO: Remove once the real endpoints are implemented.
 */
const SAMPLE_TOP_EXERCISES: TopExerciseEntry[] = [
  { exerciseName: "Back Squat", maxWeight: 140, unit: "kg" },
  { exerciseName: "Deadlift", maxWeight: 180, unit: "kg" },
  { exerciseName: "Bench Press", maxWeight: 100, unit: "kg" },
  { exerciseName: "Overhead Press", maxWeight: 65, unit: "kg" },
  { exerciseName: "Barbell Row", maxWeight: 95, unit: "kg" },
  { exerciseName: "Romanian Deadlift", maxWeight: 120, unit: "kg" },
  { exerciseName: "Hip Thrust", maxWeight: 160, unit: "kg" },
  { exerciseName: "Front Squat", maxWeight: 110, unit: "kg" },
  { exerciseName: "Incline Bench Press", maxWeight: 80, unit: "kg" },
  { exerciseName: "Pull-ups (weighted)", maxWeight: 30, unit: "kg" },
];

const SAMPLE_MONTHLY_FREQUENCY: MonthlyFrequencyEntry[] = [
  { month: "Jul", count: 12 },
  { month: "Aug", count: 15 },
  { month: "Sep", count: 18 },
  { month: "Oct", count: 14 },
  { month: "Nov", count: 20 },
  { month: "Dec", count: 10 },
  { month: "Jan", count: 16 },
  { month: "Feb", count: 19 },
  { month: "Mar", count: 22 },
  { month: "Apr", count: 17 },
  { month: "May", count: 21 },
  { month: "Jun", count: 18 },
];

/**
 * Fetches performance dashboard data from the Progress Tracker Service:
 * - Top 10 exercises by weight (last 12 months)
 * - Monthly workout frequency (last 12 months)
 *
 * Falls back to sample data when the service is unavailable.
 * TODO: Remove sample fallback once real endpoints exist.
 */
export function usePerformanceData(): UsePerformanceDataResult {
  const [topExercises, setTopExercises] = useState<TopExerciseEntry[] | null>(
    null
  );
  const [monthlyFrequency, setMonthlyFrequency] = useState<
    MonthlyFrequencyEntry[] | null
  >(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    setIsLoading(true);

    const fetchData = async () => {
      const [topExercisesResult, monthlyFrequencyResult] =
        await Promise.allSettled([
          apiClient.get<TopExerciseEntry[]>("/stats/top-exercises"),
          apiClient.get<MonthlyFrequencyEntry[]>("/stats/monthly-frequency"),
        ]);

      if (!cancelled) {
        const topData =
          topExercisesResult.status === "fulfilled" &&
          Array.isArray(topExercisesResult.value.data)
            ? topExercisesResult.value.data
            : SAMPLE_TOP_EXERCISES;

        const monthlyData =
          monthlyFrequencyResult.status === "fulfilled" &&
          Array.isArray(monthlyFrequencyResult.value.data)
            ? monthlyFrequencyResult.value.data
            : SAMPLE_MONTHLY_FREQUENCY;

        setTopExercises(topData);
        setMonthlyFrequency(monthlyData);
        setIsLoading(false);
      }
    };

    fetchData();

    return () => {
      cancelled = true;
    };
  }, []);

  return { topExercises, monthlyFrequency, isLoading, error: null };
}
