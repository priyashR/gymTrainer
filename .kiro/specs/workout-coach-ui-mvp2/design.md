# Technical Design — Workout Coach UI MVP2 (UI Redesign)

## Overview

This design covers the frontend-only UI redesign of the HybridStrength workout coach application. It introduces a centralised dark mode theme system, restructures the Landing Page, adds a Create Program screen (assigning Vault workouts and external activities to days), redesigns Theater Mode for large tablets with a tier-based layout, redesigns the Search Screen with filter chips and a card grid, adds a Manual Input screen for raw JSON entry, and adds an External Activity Logging screen. The existing React 18 SPA architecture (Vite + React Router v6 + TypeScript) is preserved.

---

## 1. Theme System (Dark Mode)

### 1.1 Design Tokens

A centralised CSS custom properties file defines all theme tokens. Components reference these tokens exclusively — no hardcoded colours.

```
File: src/styles/theme.css

--color-bg-primary: #121212
--color-bg-surface: #1e1e1e
--color-bg-card: #2a2a2a
--color-bg-editor: #0d1117
--color-text-primary: #f5f5f5
--color-text-secondary: #a0a0a0
--color-accent: #4fc3f7
--color-accent-hover: #29b6f6
--color-success: #66bb6a
--color-error: #ef5350
--color-warning: #ffa726
--color-crossfit: #ff7043
--color-border: #333333
--color-fab-bg: #4fc3f7
--color-fab-text: #121212

--radius-sm: 8px
--radius-md: 12px
--radius-lg: 16px
--radius-full: 50%

--spacing-xs: 4px
--spacing-sm: 8px
--spacing-md: 16px
--spacing-lg: 24px
--spacing-xl: 32px

--font-mono: 'JetBrains Mono', 'Fira Code', 'Consolas', monospace
--font-sans: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif

--tap-target-min: 44px
--tap-target-preferred: 48px
--fab-size: 56px
```

### 1.2 Application Strategy

- Import `theme.css` in `main.tsx` (global)
- Set `body { background: var(--color-bg-primary); color: var(--color-text-primary); }` globally
- All existing inline styles in components are migrated to use CSS modules or styled components referencing tokens
- WCAG AA compliance: all text/background combinations meet 4.5:1 for normal text, 3:1 for large text

---

## 2. Routing Changes

### 2.1 New Routes

| Route | Component | Guard |
|-------|-----------|-------|
| `/` | `LandingPage` (replaces old `Home`) | ProtectedRoute |
| `/programs/create` | `CreateProgramPage` | ProtectedRoute |
| `/manual-input` | `ManualInputPage` | ProtectedRoute |
| `/log-activity` | `ActivityLogPage` | ProtectedRoute |
| `/vault/search` | `SearchPage` (redesigned) | ProtectedRoute |
| `/workout/session/:sessionId` | `TheaterModePage` (redesigned) | ProtectedRoute |

### 2.2 Updated App.tsx Structure

```tsx
<Routes>
  {/* Public */}
  <Route path="/login" element={<PublicOnly><Login /></PublicOnly>} />
  <Route path="/register" element={<PublicOnly><Register /></PublicOnly>} />

  {/* Protected */}
  <Route path="/" element={<ProtectedRoute><LandingPage /></ProtectedRoute>} />
  <Route path="/programs/create" element={<ProtectedRoute><CreateProgramPage /></ProtectedRoute>} />
  <Route path="/manual-input" element={<ProtectedRoute><ManualInputPage /></ProtectedRoute>} />
  <Route path="/log-activity" element={<ProtectedRoute><ActivityLogPage /></ProtectedRoute>} />
  <Route path="/vault/search" element={<ProtectedRoute><SearchPage /></ProtectedRoute>} />
  <Route path="/vault/programs/:id" element={<ProtectedRoute><ProgramDetailPage /></ProtectedRoute>} />
  <Route path="/upload" element={<ProtectedRoute><UploadPage /></ProtectedRoute>} />
  <Route path="/workout/session/:sessionId" element={<ProtectedRoute><TheaterModePage /></ProtectedRoute>} />
  <Route path="/new-workout" element={<ProtectedRoute><ComingSoon title="AI Generation" /></ProtectedRoute>} />
  <Route path="/coming-soon/:feature" element={<ProtectedRoute><ComingSoon /></ProtectedRoute>} />
</Routes>
```

---

## 3. Landing Page Design

### 3.1 Component Tree

```
LandingPage
├── ResumeWorkoutCard          (conditional — shown if active/paused session exists)
├── NewWorkoutButton           (prominent CTA)
├── LogActivityButton          (navigates to /log-activity)
├── WeeklyStatsChart           (bar chart — 7 day bars)
├── QuickActionsGrid           (Upload JSON, AI Gen, Manage Programs, Upload Pic)
├── PerformanceDashboard
│   ├── TopExercisesChart      (top 10 exercises × weight, 12 months)
│   └── MonthlyFrequencyChart  (workouts per month, 12 months)
└── FAB                        (fixed bottom-right, opens action sheet)
```

### 3.2 Data Fetching

| Data | Source | Fallback |
|------|--------|----------|
| Active session | `GET /api/v1/sessions/active` | Hide ResumeWorkoutCard |
| Active enrollment | `GET /api/v1/enrollments/active` | Hide Next Step |
| Weekly stats | Progress Tracker Service (future) | Placeholder empty state |
| Top exercises | Progress Tracker Service (future) | Placeholder empty state |
| Monthly frequency | Progress Tracker Service (future) | Placeholder empty state |

### 3.3 FAB Action Sheet

When tapped, the FAB presents a bottom sheet with options:
- "Create Program" → navigates to `/programs/create`
- "Manual JSON Input" → navigates to `/manual-input`
- "Start Workout from Vault" → navigates to `/vault/search`

### 3.4 QuickActionsGrid

A 4-column grid (2-column on mobile) with icon + label navigation cards:
- Upload JSON → `/upload`
- AI Gen → `/new-workout`
- Manage Programs → `/vault/search`
- Upload Pic → `/coming-soon/upload-pic`

---

## 4. Create Program Screen Design

### 4.1 Component Tree

```
CreateProgramPage
├── BackLink                   ("← Home")
├── ProgramNameInput           (text input)
├── DayTilesGrid
│   ├── DayTile[]             (shows assigned workout/activity name)
│   └── AddDayTile            (+ Add Day button)
├── DayAssignmentModal         (opened when a DayTile is tapped)
│   ├── AssignmentTabs         ("Workout" | "Activity")
│   ├── WorkoutSelector        (Vault search with cards + Assign button)
│   └── ActivitySelector       (grid of activity type tiles)
└── SaveButton
```

### 4.2 State Management

```ts
interface CreateProgramState {
  programName: string;
  days: DayAssignment[];
  activeDayIndex: number | null;    // which day's modal is open
  saving: boolean;
  error: string | null;
}

interface DayAssignment {
  type: 'workout' | 'activity' | null;
  workoutId?: string;
  workoutName?: string;
  activityType?: string;
}
```

### 4.3 Save API Call

```
POST /api/v1/vault/programs (or a new manual-create endpoint)

Request body:
{
  "programName": "PPL Hypertrophy",
  "days": [
    { "dayNumber": 1, "type": "workout", "workoutId": "uuid-..." },
    { "dayNumber": 2, "type": "workout", "workoutId": "uuid-..." },
    { "dayNumber": 3, "type": "activity", "activityType": "Soccer" },
    ...
  ]
}
```

**Trade-off:** This requires a new backend endpoint on the Workout Creator Service. The existing upload endpoint expects Upload_Schema JSON; the manual program builder needs a simpler payload. Alternatively, the frontend could construct Upload_Schema-compliant JSON and use the existing upload endpoint — but this couples the UI to a complex schema. A dedicated endpoint is cleaner.

---

## 5. Theater Mode Redesign

### 5.1 Layout (Tablet, Landscape ≥900px)

```
┌────────────────────────────────────────────────────────────┐
│ HEADER                                                      │
│ [Stopwatch 96px]          [◀ Prev] Tier 1: Compound [Next ▶]│
├──────────────────────────────┬─────────────────────────────-┤
│ EXERCISE LIST (left panel)   │ LOGGING PANEL (right panel)   │
│                              │                               │
│ ☐ Back Squat                 │ Romanian Deadlift             │
│   4 × 6 @ 120kg · RPE 8     │ Prescribed: 3 × 10 @ 80kg    │
│                              │                               │
│ ☐ Romanian Deadlift  [Move]  │ [Reps] [Weight] [RPE]        │
│   3 × 10 @ 80kg · RPE 7     │ [✓ Log Set]                  │
│                              │                               │
│ ☐ Hip Thrust                 │ Sets Logged:                  │
│   3 × 12 @ 100kg            │ #1: 10 × 80kg RPE 7          │
│                              │ #2: 10 × 80kg RPE 7.5        │
└──────────────────────────────┴───────────────────────────────┘
```

### 5.2 CrossFit Variant Layout

```
┌────────────────────────────────────────────────────────────┐
│ HEADER                                                      │
│ [Stopwatch 96px ORANGE]    [◀ Prev] AMRAP "Fran" [Next ▶]  │
├──────────────────────────────┬─────────────────────────────-┤
│ WOD DETAILS (left panel)     │ ROUND COUNTER (right panel)   │
│                              │                               │
│ ┌─ AMRAP 12:00 ──────────┐  │     ROUNDS COMPLETED          │
│ │ 12 min time cap         │  │           3                   │
│ └─────────────────────────┘  │       [−]  [＋]               │
│                              │                               │
│ Movements (per round):       │ ─────────────────────────────│
│ ☐ Thrusters — 21 @ 43kg     │ LOG SCORE                     │
│ ☐ Pull-ups — 21             │ Rounds: [3]  + Reps: [15]     │
│ ☐ Thrusters — 15 @ 43kg     │ [Submit Score: 3 + 15]        │
│ ☐ Pull-ups — 15             │                               │
└──────────────────────────────┴───────────────────────────────┘
```

### 5.3 Component Tree (Redesigned)

```
TheaterModePage
├── TheaterHeader
│   ├── ElapsedStopwatch       (96px, left-aligned)
│   └── TierNavigator          (tier label + prev/next buttons)
├── TheaterMain (CSS Grid: 2 columns on ≥900px, 1 column below)
│   ├── ExercisePanel          (left — scrollable exercise list)
│   │   ├── AMRAPInfoCard?     (conditional — for CrossFit sections)
│   │   └── ExerciseRow[]      (checkbox + name + prescription + Move btn)
│   └── LoggingPanel           (right — performance inputs)
│       ├── CurrentExerciseHeader
│       ├── SetLogForm         (reps, weight, RPE inputs + Log Set btn)
│       ├── SetLogHistory      (logged sets list)
│       ├── RoundCounter?      (conditional — for AMRAP sections)
│       └── CrossFitScoreForm? (conditional — for AMRAP/EMOM/ForTime)
├── RestTimerOverlay           (modal overlay when rest is active)
├── FinishWorkoutPrompt        (shown when all exercises complete)
└── EmptySessionGuard          (shown when ending with no data)
```

### 5.4 Wake Lock Integration

```ts
// src/hooks/useWakeLock.ts
export function useWakeLock() {
  const wakeLockRef = useRef<WakeLockSentinel | null>(null);

  const acquire = async () => {
    if ('wakeLock' in navigator) {
      try {
        wakeLockRef.current = await navigator.wakeLock.request('screen');
      } catch { /* silently fail */ }
    }
  };

  const release = async () => {
    if (wakeLockRef.current) {
      await wakeLockRef.current.release();
      wakeLockRef.current = null;
    }
  };

  return { acquire, release };
}
```

Called in `TheaterModePage`:
- `acquire()` on mount
- `release()` on unmount (cleanup), pause, or finish

### 5.5 Responsive Breakpoints

| Viewport | Layout |
|----------|--------|
| ≥900px | Side-by-side panels (exercise left, logging right) |
| <900px | Stacked (exercise list top, logging below) |
| Font sizes | 22px exercise names, 18px prescriptions on tablet; 16px/14px on mobile |
| Touch targets | 48px minimum on tablet, 44px on mobile |

---

## 6. Search Screen Redesign

### 6.1 Component Tree

```
SearchPage
├── BackLink                   ("← Home")
├── SearchBar                  (text input with debounce 300ms)
├── FilterChipsRow
│   ├── FilterChip ("Focus Area")   — dropdown: All, Push, Pull, Legs, Full Body, Metcon
│   ├── FilterChip ("Modality")     — dropdown: All, CrossFit, Hypertrophy, Strength
│   └── FilterChip ("Days")         — dropdown: All, 3, 4, 5, 6, 7
├── CreateProgramLink          (link to /programs/create)
├── ResultsGrid                (2-column, 1-column <600px)
│   └── ProgramCard[]          (name, goal, metadata, tags)
└── EmptyState                 (shown when no results)
```

### 6.2 API Integration

Uses the existing `GET /api/v1/vault/programs/search?q=&focusArea=&modality=` endpoint. The "Days" filter is applied client-side (filtering by `durationWeeks` field on the response).

---

## 7. Manual Input Screen Design

### 7.1 Component Tree

```
ManualInputPage
├── BackLink
├── ModeTabs                   ("Edit" | "Preview")
├── EditorView (shown in Edit mode)
│   ├── JsonEditor             (monospace textarea, dark theme)
│   ├── ValidationErrors       (inline field errors on failure)
│   └── ActionButtons          ("Validate & Preview", "Clear")
└── PreviewView (shown in Preview mode)
    ├── ProgramPreviewCard     (structured display of parsed workout)
    └── ActionButtons          ("Upload to Vault", "← Back to Edit")
```

### 7.2 State Machine

```
idle → editing → validating → preview | validation_error
preview → uploading → success | upload_error
validation_error → editing (user fixes and retries)
upload_error → preview (retry)
```

### 7.3 API Integration

- Validate: `POST /api/v1/uploads/programs/validate` (existing)
- Upload: `POST /api/v1/uploads/programs` (existing)

No new backend endpoints required.

---

## 8. Activity Log Screen Design

### 8.1 Component Tree

```
ActivityLogPage
├── BackLink
├── ActivityTypeGrid           (3-col grid of emoji + name tiles)
├── DatePicker                 (defaults to today)
├── FormFields
│   ├── DurationInput          (minutes)
│   ├── CaloriesInput          (kcal)
│   ├── DistanceInput          (number + km/miles toggle)
│   └── NotesTextarea          (free text)
└── SubmitButton               ("Save Activity")
```

### 8.2 Activity Types

Predefined list with emoji icons:
```ts
const ACTIVITY_TYPES = [
  { id: 'soccer', emoji: '⚽', name: 'Soccer' },
  { id: 'squash', emoji: '🏸', name: 'Squash' },
  { id: 'running', emoji: '🏃', name: 'Running' },
  { id: 'padel', emoji: '🎾', name: 'Padel' },
  { id: 'golf', emoji: '⛳', name: 'Golf' },
  { id: 'swimming', emoji: '🏊', name: 'Swimming' },
  { id: 'cycling', emoji: '🚴', name: 'Cycling' },
  { id: 'hiking', emoji: '🥾', name: 'Hiking' },
  { id: 'basketball', emoji: '🏀', name: 'Basketball' },
  { id: 'tennis', emoji: '🎾', name: 'Tennis' },
  { id: 'other', emoji: '✏️', name: 'Other' },
] as const;
```

### 8.3 Persistence Strategy

**Primary:** `POST /api/v1/activities` (new endpoint — to be built on a backend service, likely Progress Tracker Service or a new Activity Service).

**Fallback:** If the endpoint returns 404 or is unavailable, store in `localStorage` under key `hybridstrength_pending_activities` as a JSON array. On next successful connection, sync pending entries.

```ts
interface ActivityLogEntry {
  id: string;            // client-generated UUID
  activityType: string;
  date: string;          // ISO date
  durationMinutes?: number;
  calories?: number;
  distance?: number;
  distanceUnit?: 'km' | 'miles';
  notes?: string;
  syncedAt?: string;     // null until synced to backend
}
```

---

## 9. New File Structure

```
src/
├── styles/
│   └── theme.css                    # CSS custom properties (design tokens)
├── pages/
│   ├── LandingPage.tsx              # Redesigned home (replaces Home.tsx)
│   ├── CreateProgramPage.tsx        # Manual program builder
│   ├── ManualInputPage.tsx          # JSON editor + validate + upload
│   ├── ActivityLogPage.tsx          # External activity logging
│   ├── SearchPage.tsx               # Redesigned search (replaces VaultSearchPage)
│   └── ComingSoon.tsx               # Unchanged
├── features/
│   ├── theater/
│   │   ├── TheaterModePage.tsx      # Redesigned — tier-based tablet layout
│   │   ├── TheaterHeader.tsx        # NEW — stopwatch + tier nav
│   │   ├── ExercisePanel.tsx        # NEW — left panel exercise list
│   │   ├── LoggingPanel.tsx         # NEW — right panel logging inputs
│   │   ├── AMRAPInfoCard.tsx        # NEW — CrossFit AMRAP header card
│   │   ├── ExerciseRow.tsx          # NEW — single exercise with checkbox + prescription
│   │   └── ... (existing components retained as needed)
│   ├── program/
│   │   ├── DayTilesGrid.tsx         # NEW — grid of day assignment tiles
│   │   ├── DayTile.tsx              # NEW — single day tile component
│   │   ├── DayAssignmentModal.tsx   # NEW — workout/activity picker modal
│   │   ├── WorkoutSelector.tsx      # NEW — vault search within modal
│   │   └── ActivitySelector.tsx     # NEW — activity type grid
│   ├── activity/
│   │   ├── ActivityTypeGrid.tsx     # NEW — selectable activity tiles
│   │   └── ActivityLogForm.tsx      # NEW — form fields for activity logging
│   ├── landing/
│   │   ├── ResumeWorkoutCard.tsx    # NEW — conditional resume card
│   │   ├── WeeklyStatsChart.tsx     # NEW — bar chart component
│   │   ├── PerformanceDashboard.tsx # NEW — chart section
│   │   ├── QuickActionsGrid.tsx     # NEW — navigation grid
│   │   └── FAB.tsx                  # NEW — floating action button
│   └── search/
│       ├── FilterChipsRow.tsx       # NEW — filter chip controls
│       ├── ProgramCard.tsx          # NEW — result card in grid
│       └── ResultsGrid.tsx          # NEW — 2-column responsive grid
├── hooks/
│   ├── useWakeLock.ts               # NEW — screen wake lock for Theater Mode
│   ├── useWeeklyStats.ts           # NEW — fetch weekly stats with fallback
│   ├── usePerformanceData.ts       # NEW — fetch dashboard data with fallback
│   └── useLocalActivityStore.ts    # NEW — localStorage fallback for activities
└── components/
    └── ui/
        ├── FABButton.tsx            # NEW — reusable FAB component
        ├── FilterChip.tsx           # NEW — reusable filter chip
        └── EmptyState.tsx           # NEW — reusable placeholder component
```

---

## 10. Backend Endpoint Requirements

### 10.1 New Endpoints Needed

| Endpoint | Service | Purpose |
|----------|---------|---------|
| `POST /api/v1/programs/manual` | Workout Creator Service | Save manually assembled program (day-to-workout mappings) |
| `POST /api/v1/activities` | TBD (Progress Tracker or new service) | Persist external activity logs |
| `GET /api/v1/stats/weekly` | Progress Tracker Service | Weekly workout count for stats chart |
| `GET /api/v1/stats/top-exercises` | Progress Tracker Service | Top 10 exercises by weight (12 months) |
| `GET /api/v1/stats/monthly-frequency` | Progress Tracker Service | Monthly workout count (12 months) |

### 10.2 Graceful Degradation

All Progress Tracker endpoints are optional. The frontend wraps calls in try/catch and renders `EmptyState` on failure. The activity log uses localStorage as a write-through cache when the backend is unavailable.

---

## 11. Migration Strategy

### 11.1 Incremental Approach

1. **Phase 1:** Add `theme.css` and apply dark mode globally (all existing components)
2. **Phase 2:** Build new pages (LandingPage, CreateProgram, ManualInput, ActivityLog) alongside existing ones
3. **Phase 3:** Redesign Theater Mode components (new layout, wake lock)
4. **Phase 4:** Redesign Search Screen (filter chips, grid)
5. **Phase 5:** Update routing — swap old Home for LandingPage, old VaultSearchPage for SearchPage
6. **Phase 6:** Remove deprecated components

### 11.2 Backwards Compatibility

- Existing API clients (`authApi.ts`, `sessionApi.ts`, `vaultApi.ts`) are reused unchanged
- Existing hooks (`useSession`, `useRecommendations`) are reused in the redesigned Theater Mode
- Upload flow (`uploadApi.ts`) reused by Manual Input screen

---

## 12. Key Design Decisions and Trade-offs

| Decision | Rationale | Trade-off |
|----------|-----------|-----------|
| CSS custom properties (not CSS-in-JS library) | Zero runtime cost, works with existing inline styles during migration, easy to override | Less type-safety than styled-components/emotion |
| Separate pages vs. tabbed single page | Each screen has distinct purpose and route — better for navigation history and deep linking | More route entries in App.tsx |
| Wake Lock API (not a library) | Native browser API, no dependency, good tablet support | Not supported on Firefox (graceful fallback) |
| localStorage fallback for activities | Allows feature to work before backend exists | Requires sync logic when backend is available |
| New `/programs/manual` endpoint vs. reusing upload | Upload_Schema is complex for simple day-to-workout mapping; a dedicated endpoint is simpler and decoupled | Requires backend work on Workout Creator Service |
| Timer on left side (Theater Mode) | Primary information during workout — eyes naturally go top-left; large size (96px) visible from distance | Pushes tier nav to the right |
