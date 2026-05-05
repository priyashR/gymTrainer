// --- Upload Schema Types ---
// These types mirror the Upload_Schema JSON structure used by the backend parser/formatter.

export interface ProgramMetadata {
  program_name: string;
  duration_weeks: 1 | 4;
  goal: string;
  equipment_profile: string[];
  version: '1.0';
}

export interface WarmCoolEntry {
  movement: string;
  instruction: string;
}

export interface Movement {
  exercise_name: string;
  /** Required when the parent Day modality is 'CrossFit'; optional for 'Hypertrophy'. */
  modality_type?: 'Engine' | 'Gymnastics' | 'Weightlifting';
  prescribed_sets: number;
  prescribed_reps: string;
  prescribed_weight?: string;
  rest_interval_seconds?: number;
  notes?: string;
}

export interface Block {
  block_type: string;
  format: string;
  time_cap_minutes?: number;
  movements: Movement[];
}

export interface Day {
  day_number: number;
  day_label: string;
  focus_area: string;
  modality: 'CrossFit' | 'Hypertrophy';
  warm_up: WarmCoolEntry[];
  blocks: Block[];
  cool_down: WarmCoolEntry[];
  methodology_source?: string;
}

export interface Week {
  week_number: number;
  days: Day[];
}

/** The full parsed program structure as returned by the Upload_Schema. */
export interface ParsedProgram {
  program_metadata: ProgramMetadata;
  program_structure: Week[];
}

// --- API Response DTOs ---

/** Response body from POST /api/v1/uploads/programs (201 Created). */
export interface UploadProgramResponse {
  id: string;
  programName: string;
  durationWeeks: number;
  goal: string;
  equipmentProfile: string[];
  /** Always "UPLOADED" for programs created via the upload endpoint. */
  contentSource: 'UPLOADED';
  createdAt: string; // ISO-8601
}

/** Response body from POST /api/v1/uploads/programs/validate (200 OK). */
export interface ValidateUploadResponse {
  valid: boolean;
  /** Empty array when valid; contains field-level errors when invalid. */
  errors: Array<{ field: string; message: string }>;
}
