// --- Session Domain Types ---

export type SessionStatus = 'IN_PROGRESS' | 'PAUSED' | 'COMPLETED';

export type SectionType = 'STRENGTH' | 'AMRAP' | 'EMOM' | 'TABATA' | 'FOR_TIME';

export interface SetLog {
  setNumber: number;
  weight: number; // kg
  repetitions: number;
  rpe: number | null; // 1.0–10.0 in 0.5 increments
  loggedAt: string; // ISO-8601
}

export interface CrossFitScore {
  rounds: number;
  additionalReps: number;
  totalTimeSeconds: number | null;
  loggedAt: string; // ISO-8601
}

export interface ExerciseLog {
  exerciseIndex: number;
  exerciseName: string;
  completed: boolean;
  completedAt: string | null; // ISO-8601
  setLogs: SetLog[];
}

export interface TimerConfig {
  sectionType: SectionType;
  durationSeconds?: number; // AMRAP countdown
  workSeconds?: number; // Tabata/EMOM work interval
  restSeconds?: number; // Tabata/EMOM rest interval
  rounds?: number; // Tabata/EMOM round count
}

export interface SectionProgress {
  sectionIndex: number;
  sectionName: string;
  sectionType: SectionType;
  exerciseLogs: ExerciseLog[];
  completed: boolean;
  crossFitScore: CrossFitScore | null;
  roundCount: number;
}

export interface Session {
  id: string;
  userId: string;
  programId: string | null;
  enrollmentId: string | null;
  weekNumber: number;
  dayNumber: number;
  status: SessionStatus;
  currentSectionIndex: number;
  sectionProgresses: SectionProgress[];
  workoutSnapshot: unknown; // JSON snapshot of the day definition
  startedAt: string; // ISO-8601
  pausedAt: string | null; // ISO-8601
  completedAt: string | null; // ISO-8601
  lastPersistedAt: string; // ISO-8601
}

// --- Enrollment / Progression Domain Types ---

export type EnrollmentStatus = 'ACTIVE' | 'COMPLETED' | 'REPLACED';

export interface SkipRecord {
  weekNumber: number;
  dayNumber: number;
  skippedAt: string; // ISO-8601
}

export interface NextDayInfo {
  programName: string;
  weekNumber: number;
  dayNumber: number;
  dayLabel: string;
}

export interface ProgramEnrollment {
  id: string;
  userId: string;
  programId: string;
  programName: string;
  currentWeek: number;
  currentDay: number;
  totalWeeks: number;
  totalDaysPerWeek: number;
  status: EnrollmentStatus;
  enrolledAt: string; // ISO-8601
  completedAt: string | null; // ISO-8601
  skips: SkipRecord[];
}

// --- Request DTOs ---

export interface StartSessionRequest {
  programId: string;
  weekNumber: number;
  dayNumber: number;
  standalone: boolean;
}

export interface CompleteExerciseRequest {
  sectionIndex: number;
  exerciseIndex: number;
}

export interface AdvanceSectionRequest {
  targetSectionIndex: number;
}

export interface EnrollRequest {
  programId: string;
  programName: string;
  totalWeeks: number;
  totalDaysPerWeek: number;
}

export interface LogSetRequest {
  sectionIndex: number;
  exerciseIndex: number;
  weight: number;
  repetitions: number;
  rpe: number | null;
}

export interface LogCrossFitScoreRequest {
  sectionIndex: number;
  rounds: number;
  additionalReps: number;
  totalTimeSeconds: number | null;
}

// --- Response DTOs ---

export interface SectionProgressResponse {
  sectionIndex: number;
  sectionName: string;
  sectionType: SectionType;
  exerciseLogs: ExerciseLog[];
  completed: boolean;
  crossFitScore: CrossFitScore | null;
  roundCount: number;
}

export interface SessionResponse {
  id: string;
  status: SessionStatus;
  currentSectionIndex: number;
  sectionProgresses: SectionProgressResponse[];
  workoutSnapshot: unknown;
  startedAt: string; // ISO-8601
  pausedAt: string | null; // ISO-8601
  completedAt: string | null; // ISO-8601
  durationSeconds: number | null;
}

export interface EnrollmentResponse {
  id: string;
  programId: string;
  programName: string;
  currentWeek: number;
  currentDay: number;
  totalWeeks: number;
  status: EnrollmentStatus;
  enrolledAt: string; // ISO-8601
  nextDay: NextDayInfo | null;
}

// --- WebSocket Message Types ---

export interface SessionStateUpdateMessage {
  type: 'SESSION_STATE_UPDATE';
  payload: {
    sessionId: string;
    status: SessionStatus;
    currentSectionIndex: number;
    sectionProgresses: SectionProgressResponse[];
    lastPersistedAt: string;
  };
}

export interface SessionCompletedMessage {
  type: 'SESSION_COMPLETED';
  payload: {
    sessionId: string;
    completedAt: string;
  };
}

export type SessionWebSocketMessage =
  | SessionStateUpdateMessage
  | SessionCompletedMessage;
