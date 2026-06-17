# Implementation Plan: Workout Coach UI MVP2 (UI Redesign)

## Overview

This plan implements the full UI redesign in 6 phases following the migration strategy from the design document: theme system first, then new pages (Landing, Create Program, Manual Input, Activity Log), then Theater Mode redesign, then Search Screen redesign, then routing updates, then cleanup. Each phase builds on the previous and ends with all tests passing.

## Tasks

- [ ] 1. Theme system and dark mode foundation
  - [ ] 1.1 Create `src/styles/theme.css` with all CSS custom property tokens
    - Define colour tokens: `--color-bg-primary`, `--color-bg-surface`, `--color-bg-card`, `--color-bg-editor`, `--color-text-primary`, `--color-text-secondary`, `--color-accent`, `--color-accent-hover`, `--color-success`, `--color-error`, `--color-warning`, `--color-crossfit`, `--color-border`, `--color-fab-bg`, `--color-fab-text`
    - Define spacing tokens: `--spacing-xs` (4px), `--spacing-sm` (8px), `--spacing-md` (16px), `--spacing-lg` (24px), `--spacing-xl` (32px)
    - Define radius tokens: `--radius-sm` (8px), `--radius-md` (12px), `--radius-lg` (16px), `--radius-full` (50%)
    - Define font tokens: `--font-mono`, `--font-sans`
    - Define interaction tokens: `--tap-target-min` (44px), `--tap-target-preferred` (48px), `--fab-size` (56px)
    - _Requirements: 1.1, 1.2, 1.4_

  - [ ] 1.2 Import `theme.css` in `src/main.tsx` and apply global body styles
    - Set `body { background: var(--color-bg-primary); color: var(--color-text-primary); font-family: var(--font-sans); }`
    - Reset default link and button colours to use theme tokens
    - _Requirements: 1.1, 1.3_

  - [ ] 1.3 Create `src/components/ui/EmptyState.tsx` reusable placeholder component
    - Props: `icon` (optional emoji/icon), `title`, `subtitle` (optional)
    - Styled with dark mode tokens, centred layout
    - _Requirements: 10.1, 10.2_

  - [ ] 1.4 Create `src/components/ui/FABButton.tsx` reusable floating action button
    - Props: `onClick`, `icon` (default "+"), `ariaLabel`
    - Fixed position bottom-right, 56×56px, accent background
    - _Requirements: 2.4, 6.2_

  - [ ] 1.5 Create `src/components/ui/FilterChip.tsx` reusable filter chip component
    - Props: `label`, `options` (array), `value`, `onChange`, `active` (boolean)
    - Pill-shaped, dark surface background, accent border when active
    - _Requirements: 5.2, 5.3_

- [ ] 2. Checkpoint — Theme and shared components compile
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 3. Landing Page implementation
  - [ ] 3.1 Create `src/features/landing/ResumeWorkoutCard.tsx`
    - Props: `sessionId`, `workoutName`, `status`, `progress`, `onResume`
    - Gradient card with resume button, conditionally rendered
    - _Requirements: 2.1, 2.2_

  - [ ] 3.2 Create `src/features/landing/WeeklyStatsChart.tsx`
    - Props: `completedCount`, `goalCount`, `dailyData` (array of 7 booleans/values)
    - Bar chart with 7 bars (M–S), accent fill for completed, border-only for remaining
    - Render EmptyState when data unavailable
    - _Requirements: 2.5, 10.1_

  - [ ] 3.3 Create `src/features/landing/PerformanceDashboard.tsx`
    - Contains two chart placeholder sections: top 10 exercises and monthly frequency
    - Render EmptyState placeholders when Progress Tracker Service unavailable
    - _Requirements: 2.6, 2.7, 10.2_

  - [ ] 3.4 Create `src/features/landing/QuickActionsGrid.tsx`
    - 4-item grid (2-col on mobile): Upload JSON, AI Gen, Manage Programs, Upload Pic
    - Each item: icon + label, navigates to appropriate route
    - _Requirements: 2.8, 2.9, 2.10, 2.11, 2.12_

  - [ ] 3.5 Create `src/hooks/useWeeklyStats.ts`
    - Fetch weekly stats from Progress Tracker Service (future endpoint)
    - Return `{ data, isLoading, error }` — error returns null data (graceful fallback)
    - _Requirements: 2.5, 10.1_

  - [ ] 3.6 Create `src/hooks/usePerformanceData.ts`
    - Fetch top exercises and monthly frequency from Progress Tracker Service
    - Return `{ topExercises, monthlyFrequency, isLoading, error }` — error returns null (graceful fallback)
    - _Requirements: 2.6, 10.2_

  - [ ] 3.7 Create `src/pages/LandingPage.tsx`
    - Compose: ResumeWorkoutCard (conditional), NewWorkoutButton, LogActivityButton, WeeklyStatsChart, QuickActionsGrid, PerformanceDashboard, FABButton
    - Fetch active session and active enrollment on mount (reuse existing API calls)
    - FAB opens action sheet with: Create Program, Manual JSON Input, Start from Vault
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 9.1_

  - [ ] 3.8 Write unit tests for LandingPage
    - Test ResumeWorkoutCard shown/hidden based on session state
    - Test QuickActionsGrid renders 4 navigation items with correct links
    - Test FAB opens action sheet on tap
    - Test WeeklyStatsChart renders placeholder when data unavailable
    - _Requirements: 2.1, 2.2, 2.5, 2.7, 2.8_

- [ ] 4. Checkpoint — Landing page renders correctly
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 5. Create Program Screen implementation
  - [ ] 5.1 Create `src/features/program/DayTile.tsx`
    - Props: `dayNumber`, `assignment` (workout name, activity type, or null), `onClick`, `onRemove`
    - Shows day number, assigned name (or "No assignment"), Change/Remove actions
    - _Requirements: 3.3, 3.8, 3.9, 3.10, 3.11_

  - [ ] 5.2 Create `src/features/program/DayTilesGrid.tsx`
    - Props: `days` (array of DayAssignment), `onDayTap`, `onAddDay`, `onRemoveDay`
    - Responsive grid layout with Add Day tile at the end
    - _Requirements: 3.3, 3.4, 6.3_

  - [ ] 5.3 Create `src/features/program/WorkoutSelector.tsx`
    - Fetches user's Vault workouts via existing `listPrograms` API
    - Search input, list of workout cards with "Assign" button
    - _Requirements: 3.5, 3.6, 3.8_

  - [ ] 5.4 Create `src/features/program/ActivitySelector.tsx`
    - Grid of activity type tiles (emoji + name) from predefined ACTIVITY_TYPES list
    - Includes "Other" option with custom text input
    - _Requirements: 3.5, 3.7, 3.9_

  - [ ] 5.5 Create `src/features/program/DayAssignmentModal.tsx`
    - Tabbed modal: "💪 Workout" tab shows WorkoutSelector, "🏃 Activity" tab shows ActivitySelector
    - Returns selected assignment to parent on selection
    - _Requirements: 3.5, 3.6, 3.7_

  - [ ] 5.6 Create `src/pages/CreateProgramPage.tsx`
    - Compose: BackLink, ProgramNameInput, DayTilesGrid, DayAssignmentModal, SaveButton
    - State: programName, days (DayAssignment[]), activeDayIndex, saving, error
    - Save calls Workout Creator Service API; validation on name and at least one assignment
    - Navigate to program detail on success
    - _Requirements: 3.1, 3.2, 3.12, 3.13, 3.14, 3.15, 3.16_

  - [ ] 5.7 Write unit tests for CreateProgramPage
    - Test program name validation (empty → error)
    - Test adding/removing day tiles
    - Test assigning a workout from Vault to a day
    - Test assigning an activity to a day
    - Test save with zero assignments shows error
    - Test successful save navigates to Vault detail
    - _Requirements: 3.2, 3.4, 3.8, 3.9, 3.13, 3.14, 3.16_

- [ ] 6. Checkpoint — Create Program page renders and validates
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 7. Manual Input Screen implementation
  - [ ] 7.1 Create `src/pages/ManualInputPage.tsx`
    - Compose: BackLink, ModeTabs (Edit/Preview), JsonEditor, ValidationErrors, PreviewView, ActionButtons
    - State machine: idle → editing → validating → preview | validation_error → uploading → success | upload_error
    - Validate calls `POST /api/v1/uploads/programs/validate` (existing endpoint)
    - Upload calls `POST /api/v1/uploads/programs` (existing endpoint)
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7, 8.8, 8.9, 8.10, 8.11, 8.12_

  - [ ] 7.2 Write unit tests for ManualInputPage
    - Test validate → preview transition on valid JSON
    - Test validate → error display on invalid JSON
    - Test JSON parse error displayed inline
    - Test upload → success with confirmation message
    - Test upload → error preserves editor content
    - Test upload button disabled during request
    - _Requirements: 8.3, 8.4, 8.5, 8.6, 8.8, 8.9, 8.10, 8.11_

- [ ] 8. Activity Log Screen implementation
  - [ ] 8.1 Create `src/features/activity/ActivityTypeGrid.tsx`
    - Grid of selectable activity tiles from ACTIVITY_TYPES constant
    - Props: `selectedType`, `onSelect`
    - "Other" shows a text input for custom name
    - _Requirements: 9.2, 9.3_

  - [ ] 8.2 Create `src/features/activity/ActivityLogForm.tsx`
    - Fields: duration (minutes), calories (kcal), distance (number + km/miles toggle), notes (textarea)
    - All fields optional except activity type and date (validated by parent)
    - _Requirements: 9.2, 9.6_

  - [ ] 8.3 Create `src/hooks/useLocalActivityStore.ts`
    - Read/write to `localStorage` key `hybridstrength_pending_activities`
    - `saveActivity(entry)`: append to local store
    - `getPendingActivities()`: return unsynced entries
    - `markSynced(id)`: mark entry as synced
    - _Requirements: 9.8_

  - [ ] 8.4 Create `src/pages/ActivityLogPage.tsx`
    - Compose: BackLink, ActivityTypeGrid, DatePicker (default today), ActivityLogForm, SubmitButton
    - Validate mandatory fields (activity type + date) before submission
    - Submit to backend; fallback to localStorage if endpoint unavailable
    - On success navigate to Landing Page
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7, 9.8, 9.9, 9.10_

  - [ ] 8.5 Write unit tests for ActivityLogPage
    - Test mandatory field validation (missing activity type → error, missing date → error)
    - Test successful submission navigates home
    - Test localStorage fallback when API returns 404
    - Test activity type grid selection (including "Other" custom input)
    - _Requirements: 9.3, 9.5, 9.7, 9.8_

- [ ] 9. Checkpoint — All new pages compile and render
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 10. Theater Mode redesign
  - [ ] 10.1 Create `src/hooks/useWakeLock.ts`
    - `acquire()`: request screen wake lock via `navigator.wakeLock.request('screen')`
    - `release()`: release wake lock sentinel
    - Graceful no-op when API not supported
    - _Requirements: 4.12, 4.13, 4.14_

  - [ ] 10.2 Create `src/features/theater/TheaterHeader.tsx`
    - Left: elapsed stopwatch (96px font, left-aligned)
    - Right: tier label + section type + Prev/Next buttons
    - Prev disabled on first tier, Next disabled on last
    - _Requirements: 4.2, 4.3, 4.9_

  - [ ] 10.3 Create `src/features/theater/ExerciseRow.tsx`
    - Props: `exercise`, `recommendation`, `isActive`, `isCompleted`, `onCheck`, `onMove`
    - Checkbox (28px), exercise name (22px), prescription text (18px), Move button
    - Completed state: dimmed with checkmark
    - _Requirements: 4.4, 4.5, 4.7, 4.11_

  - [ ] 10.4 Create `src/features/theater/ExercisePanel.tsx`
    - Props: `exercises`, `currentExerciseIndex`, `sectionType`, `timerConfig`
    - Scrollable list of ExerciseRow components
    - Conditional AMRAPInfoCard at top for CrossFit sections
    - _Requirements: 4.4, 4.7_

  - [ ] 10.5 Create `src/features/theater/AMRAPInfoCard.tsx`
    - Props: `timeCap`, `description`
    - Orange accent card showing time cap and format description
    - Only rendered for AMRAP/ForTime section types
    - _Requirements: 4.4_

  - [ ] 10.6 Create `src/features/theater/LoggingPanel.tsx`
    - Strength variant: current exercise header, SetLogForm, SetLogHistory
    - CrossFit variant: RoundCounter, CrossFitScoreForm
    - Switches based on current section type
    - _Requirements: 4.5, 4.6_

  - [ ] 10.7 Refactor `src/features/theater/TheaterModePage.tsx` to tier-based layout
    - Replace existing layout with CSS Grid: 2 columns on ≥900px, stacked on <900px
    - Compose: TheaterHeader, ExercisePanel (left), LoggingPanel (right)
    - Integrate useWakeLock: acquire on mount, release on unmount/pause/finish
    - Retain existing useSession and useRecommendations hooks
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.10, 4.11, 4.12, 4.13, 6.4_

  - [ ] 10.8 Write unit tests for Theater Mode redesign
    - Test tier navigation (prev disabled on first, next advances)
    - Test exercise completion checkbox persists data
    - Test wake lock acquired on mount and released on unmount
    - Test layout switches to side-by-side at ≥900px viewport
    - Test CrossFit variant shows RoundCounter and AMRAPInfoCard
    - _Requirements: 4.2, 4.6, 4.8, 4.9, 4.12, 6.4_

- [ ] 11. Checkpoint — Theater Mode renders in new layout
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 12. Search Screen redesign
  - [ ] 12.1 Create `src/features/search/FilterChipsRow.tsx`
    - Row of 3 FilterChip components: Focus Area, Modality, Days
    - Manages filter state and emits combined onChange
    - _Requirements: 5.2, 5.3, 5.4_

  - [ ] 12.2 Create `src/features/search/ProgramCard.tsx`
    - Props: `program` (VaultItem)
    - Card showing name, goal, metadata (days/week, duration), tags
    - Dark surface background, border, hover state
    - _Requirements: 5.5, 5.8_

  - [ ] 12.3 Create `src/features/search/ResultsGrid.tsx`
    - Props: `programs` (array), `onCardClick`
    - CSS Grid: 2 columns on ≥600px, 1 column below
    - _Requirements: 5.5, 6.1_

  - [ ] 12.4 Create `src/pages/SearchPage.tsx`
    - Compose: BackLink, SearchBar, FilterChipsRow, CreateProgramLink, ResultsGrid, EmptyState
    - Reuse existing `searchPrograms` API from `vaultApi.ts`
    - Debounce search input by 300ms
    - "Days" filter applied client-side on durationWeeks field
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8_

  - [ ] 12.5 Write unit tests for SearchPage
    - Test search input debounces and triggers API call
    - Test filter chips update results
    - Test 2-column grid renders on wide viewport
    - Test empty state shown when no results
    - Test "Create Program" link navigates to /programs/create
    - _Requirements: 5.1, 5.3, 5.5, 5.6, 5.7_

- [ ] 13. Checkpoint — Search Screen renders with filters and grid
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 14. Routing and integration
  - [ ] 14.1 Update `src/App.tsx` with new route definitions
    - Add routes: `/` → LandingPage, `/programs/create` → CreateProgramPage, `/manual-input` → ManualInputPage, `/log-activity` → ActivityLogPage, `/vault/search` → SearchPage
    - Keep existing routes: `/vault/programs/:id`, `/upload`, `/workout/session/:sessionId`, `/login`, `/register`
    - Add catch-all for `/coming-soon/:feature`
    - _Requirements: 2.8, 3.1, 8.1, 9.1_

  - [ ] 14.2 Apply dark mode theme to existing pages (Login, Register, ProgramDetail, Upload)
    - Update inline styles in Login.tsx, Register.tsx to use theme tokens
    - Update ProgramDetailPage and UploadPage colour references
    - Ensure all pages pass WCAG AA contrast
    - _Requirements: 1.1, 1.3, 1.5_

  - [ ] 14.3 Remove deprecated `src/pages/Home.tsx` and update imports
    - Delete old Home.tsx (replaced by LandingPage.tsx)
    - Verify no remaining references to removed file
    - _Requirements: 2.1_

- [ ] 15. Checkpoint — Full app routes correctly with dark mode
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 16. Property-based tests
  - [ ] 16.1 Write property test: Dark mode tokens produce WCAG AA contrast
    - Use `fast-check` to generate all token pair combinations (text on background)
    - Assert contrast ratio ≥ 4.5:1 for normal text, ≥ 3:1 for large text
    - Minimum 100 iterations
    - _Requirements: 1.5_

  - [ ] 16.2 Write property test: Filter chips produce correct AND-logic results
    - Generate arbitrary filter combinations (focus area, modality, days)
    - Assert every returned program satisfies ALL active filters
    - Minimum 100 iterations
    - _Requirements: 5.3, 5.4_

  - [ ] 16.3 Write property test: Activity log mandatory field validation
    - Generate arbitrary form states (some with missing activity type or date)
    - Assert submission is blocked iff either mandatory field is missing
    - Minimum 100 iterations
    - _Requirements: 9.5_

  - [ ] 16.4 Write property test: Create Program save validation
    - Generate arbitrary program states (empty name, zero assignments, valid states)
    - Assert save is blocked iff name is empty OR all days have no assignment
    - Minimum 100 iterations
    - _Requirements: 3.13, 3.14_

- [ ] 17. Final checkpoint — All tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Each task references specific acceptance criteria from the requirements document
- Checkpoints ensure incremental validation at natural break points
- The theme system (Phase 1) must be completed first as all subsequent components depend on it
- Theater Mode redesign (Phase 3) reuses existing `useSession` and `useRecommendations` hooks unchanged
- Manual Input reuses existing upload/validate API endpoints — no new backend work required for that screen
- Create Program and Activity Log require new backend endpoints (documented in design.md section 10)
- Property tests use `fast-check` with minimum 100 iterations per steering docs
- The migration is additive — new pages are built alongside existing ones before the routing swap in Phase 5
