const STORAGE_KEY = "hybridstrength_pending_activities";

export interface ActivityLogEntry {
  id: string;
  activityType: string;
  date: string; // ISO date
  durationMinutes?: number;
  calories?: number;
  distance?: number;
  distanceUnit?: "km" | "miles";
  notes?: string;
  syncedAt?: string; // null/undefined until synced to backend
}

function readEntries(): ActivityLogEntry[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];
    return parsed;
  } catch {
    return [];
  }
}

function writeEntries(entries: ActivityLogEntry[]): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(entries));
}

/**
 * Appends a new activity entry to the local store.
 */
export function saveActivity(entry: ActivityLogEntry): void {
  const entries = readEntries();
  entries.push(entry);
  writeEntries(entries);
}

/**
 * Returns all entries that have not yet been synced (syncedAt is null/undefined).
 */
export function getPendingActivities(): ActivityLogEntry[] {
  const entries = readEntries();
  return entries.filter((e) => !e.syncedAt);
}

/**
 * Marks the entry with the given id as synced by setting syncedAt to the current timestamp.
 */
export function markSynced(id: string): void {
  const entries = readEntries();
  const entry = entries.find((e) => e.id === id);
  if (entry) {
    entry.syncedAt = new Date().toISOString();
    writeEntries(entries);
  }
}
