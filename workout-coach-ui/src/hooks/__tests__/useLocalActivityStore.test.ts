import { describe, it, expect, beforeEach, vi } from "vitest";
import {
  saveActivity,
  getPendingActivities,
  markSynced,
  ActivityLogEntry,
} from "../useLocalActivityStore";

const STORAGE_KEY = "hybridstrength_pending_activities";

function makeEntry(overrides: Partial<ActivityLogEntry> = {}): ActivityLogEntry {
  return {
    id: "test-id-1",
    activityType: "Running",
    date: "2025-01-15",
    ...overrides,
  };
}

describe("useLocalActivityStore", () => {
  beforeEach(() => {
    localStorage.clear();
  });

  describe("saveActivity", () => {
    it("stores an entry in localStorage", () => {
      const entry = makeEntry();
      saveActivity(entry);

      const stored = JSON.parse(localStorage.getItem(STORAGE_KEY)!);
      expect(stored).toHaveLength(1);
      expect(stored[0]).toEqual(entry);
    });

    it("appends to existing entries", () => {
      const entry1 = makeEntry({ id: "id-1", activityType: "Running" });
      const entry2 = makeEntry({ id: "id-2", activityType: "Swimming" });

      saveActivity(entry1);
      saveActivity(entry2);

      const stored = JSON.parse(localStorage.getItem(STORAGE_KEY)!);
      expect(stored).toHaveLength(2);
      expect(stored[0].id).toBe("id-1");
      expect(stored[1].id).toBe("id-2");
    });

    it("handles corrupted localStorage gracefully by starting fresh", () => {
      localStorage.setItem(STORAGE_KEY, "not valid json{{{");

      const entry = makeEntry();
      saveActivity(entry);

      const stored = JSON.parse(localStorage.getItem(STORAGE_KEY)!);
      expect(stored).toHaveLength(1);
      expect(stored[0]).toEqual(entry);
    });
  });

  describe("getPendingActivities", () => {
    it("returns only entries without syncedAt", () => {
      const pending = makeEntry({ id: "pending-1" });
      const synced = makeEntry({ id: "synced-1", syncedAt: "2025-01-15T10:00:00Z" });

      saveActivity(pending);
      saveActivity(synced);

      const result = getPendingActivities();
      expect(result).toHaveLength(1);
      expect(result[0].id).toBe("pending-1");
    });

    it("returns empty array when localStorage is empty", () => {
      const result = getPendingActivities();
      expect(result).toEqual([]);
    });

    it("returns empty array when localStorage has corrupted data", () => {
      localStorage.setItem(STORAGE_KEY, "corrupted!!");

      const result = getPendingActivities();
      expect(result).toEqual([]);
    });

    it("returns empty array when stored value is not an array", () => {
      localStorage.setItem(STORAGE_KEY, JSON.stringify({ not: "an array" }));

      const result = getPendingActivities();
      expect(result).toEqual([]);
    });
  });

  describe("markSynced", () => {
    it("sets syncedAt on the correct entry", () => {
      const entry1 = makeEntry({ id: "id-1" });
      const entry2 = makeEntry({ id: "id-2" });

      saveActivity(entry1);
      saveActivity(entry2);

      vi.useFakeTimers();
      vi.setSystemTime(new Date("2025-06-01T12:00:00Z"));

      markSynced("id-1");

      vi.useRealTimers();

      const stored = JSON.parse(localStorage.getItem(STORAGE_KEY)!);
      expect(stored[0].syncedAt).toBe("2025-06-01T12:00:00.000Z");
      expect(stored[1].syncedAt).toBeUndefined();
    });

    it("does nothing when id does not exist", () => {
      const entry = makeEntry({ id: "id-1" });
      saveActivity(entry);

      markSynced("nonexistent-id");

      const stored = JSON.parse(localStorage.getItem(STORAGE_KEY)!);
      expect(stored[0].syncedAt).toBeUndefined();
    });

    it("handles empty localStorage gracefully", () => {
      // Should not throw
      expect(() => markSynced("any-id")).not.toThrow();
    });
  });
});
