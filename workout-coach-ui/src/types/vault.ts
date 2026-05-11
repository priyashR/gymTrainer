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
