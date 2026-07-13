// --- Vault Domain Types ---

export interface VaultItem {
  id: string;
  name: string;
  goal: string;
  durationWeeks: number;
  equipmentProfile: string[];
  contentSource: 'AI_GENERATED' | 'UPLOADED' | 'MANUAL';
  createdAt: string; // ISO-8601
  updatedAt: string; // ISO-8601
}

export interface PaginatedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

// --- Program Detail Types ---

export interface VaultProgramDetail {
  id: string;
  name: string;
  goal: string;
  durationWeeks: number;
  equipmentProfile: string[];
  contentSource: 'AI_GENERATED' | 'UPLOADED' | 'MANUAL';
  createdAt: string; // ISO-8601
  updatedAt: string; // ISO-8601
  weeks: VaultWeek[];
  dayAssignments?: DayAssignmentDetail[];
}

/** API response shape for a day assignment within a manual program detail. */
export interface DayAssignmentDetail {
  dayNumber: number;
  type: string;
  workoutId?: string;
  activityType?: string;
  snapshotData?: SnapshotData;
  sourceProgramId?: string;
  sourceProgramName?: string;
  sourceWeekNumber?: number;
  sourceDayNumber?: number;
}

/** Snapshot structure for a copied day. */
export interface SnapshotData {
  dayNumber?: number;
  label?: string;
  focusArea?: string;
  modality?: string;
  warmUp?: SnapshotWarmCoolEntry[];
  sections?: SnapshotSection[];
  coolDown?: SnapshotWarmCoolEntry[];
}

export interface SnapshotWarmCoolEntry {
  movement: string;
  instruction: string;
}

export interface SnapshotSection {
  name?: string;
  sectionType?: string;
  format?: string;
  timeCap?: number | null;
  exercises?: SnapshotExercise[];
}

export interface SnapshotExercise {
  name: string;
  modalityType?: string | null;
  sets?: number;
  reps?: string;
  weight?: string | null;
  restSeconds?: number | null;
  notes?: string | null;
}

export interface VaultWeek {
  weekNumber: number;
  days: VaultDay[];
}

export interface VaultDay {
  dayNumber: number;
  label: string;
  focusArea: string;
  modality: 'CROSSFIT' | 'HYPERTROPHY';
  warmUp: WarmCoolEntry[];
  sections: VaultSection[];
  coolDown: WarmCoolEntry[];
  methodologySource?: string;
}

export interface WarmCoolEntry {
  movement: string;
  instruction: string;
}

export interface VaultSection {
  name: string;
  sectionType: string;
  format?: string;
  timeCap?: number;
  exercises: VaultExercise[];
}

export interface VaultExercise {
  name: string;
  modalityType?: string;
  sets: number;
  reps: string;
  weight?: string;
  restSeconds?: number;
  notes?: string;
}

// --- Day Assignment Types ---

export interface DayAssignment {
  type: "workout" | "activity" | "copied_day" | null;
  workoutId?: string;
  workoutName?: string;
  activityType?: string;
  // Copied day fields
  sourceProgramId?: string;
  sourceProgramName?: string;
  sourceWeekNumber?: number;
  sourceDayNumber?: number;
  dayLabel?: string;
  focusArea?: string;
  snapshotData?: any;
}
