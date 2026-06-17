# Requirements Document — Workout Coach UI MVP2 (UI Redesign)

## Introduction

This document specifies the requirements for the HybridStrength Workout Coach UI redesign (MVP2). The redesign introduces a dark mode theme as the default, restructures the Landing Page with a weekly stats chart, performance dashboards, and streamlined navigation, adds a brand-new Create Program screen for manual program assembly, adds a Manual Input screen for typing raw JSON workouts with validation and preview, redesigns the Theater Mode (active workout execution) with a tier-based layout optimised for large tablets, and redesigns the Search screen with filter chips and a 2-column card grid. All changes are purely frontend except for one new backend endpoint: saving a manually created program to the Vault. The application continues to be a React 18 SPA using Vite, TypeScript, and React Router v6.

---

## Glossary

- **Workout_Coach_UI**: The React 18 Single Page Application serving all user-facing views for HybridStrength
- **Dark_Mode_Theme**: A visual theme using dark backgrounds (charcoal/near-black) with high-contrast light text and accent colours, applied as the default
- **Landing_Page**: The home screen displayed after a User authenticates successfully
- **Resume_Workout_Card**: A conditional card on the Landing_Page that appears only when an active or paused workout Session exists
- **FAB**: Floating Action Button — a circular "+" button fixed in a prominent position for primary creation actions
- **Weekly_Stats_Chart**: A bar chart component displaying the count of workouts completed in the current week relative to a weekly goal (e.g., "3 of 7")
- **Performance_Dashboard**: A section on the Landing_Page displaying top-10 exercise weight trends and monthly workout frequency over 12 months
- **Progress_Tracker_Service**: The backend microservice responsible for analytics, benchmarks, and heat maps — not yet built at the time of this spec
- **Create_Program_Screen**: A dedicated screen for manual program creation through a day-tile interface
- **Day_Tile**: A clickable tile representing a single training day within a program under construction
- **Day_Editor**: A sub-view opened when a Day_Tile is tapped, allowing exercise search, selection, and addition to that day
- **Theater_Mode**: The distraction-free active workout execution interface, redesigned for large tablet use
- **Tier**: A logical grouping of exercises within a workout (e.g., "Tier 1: Compound", "Tier 2: Accessory")
- **Move_Button**: A button in Theater_Mode that advances focus to the next exercise within the current tier
- **Search_Screen**: A redesigned view for browsing and filtering programs/workouts with a grid layout
- **Filter_Chip**: A compact filter control (chip or dropdown) for narrowing search results by a specific dimension
- **Program**: A structured collection of workouts spanning one or more training days/weeks
- **Exercise**: A single movement within a workout section (e.g., Back Squat, Box Jump)
- **Session**: An active workout execution instance
- **User**: An authenticated individual using the HybridStrength platform
- **Workout_Creator_Service**: The backend service responsible for program CRUD, AI generation, and Vault management
- **Manual_Input_Screen**: A dedicated screen where a User can type raw JSON representing a single workout, validate it against the Upload_Schema, preview the parsed result, and upload it to the Vault
- **Upload_Schema**: The JSON schema that defines the valid structure for uploaded programs/workouts (defined in the workout-creator-service-upload sub-spec)
- **External_Activity**: A physical activity performed outside the HybridStrength app (e.g., soccer, squash, running, padel, golf, swimming, cycling) that a User wants to log for tracking purposes
- **Activity_Log_Screen**: A dedicated screen for manually logging External_Activity sessions with metadata such as date, duration, calories, and distance

---

## Requirements

### Requirement 1: Dark Mode Theme

**User Story:** As a User, I want the application to use a dark mode colour scheme by default, so that I can train in low-light environments without eye strain and the UI looks modern during workouts.

#### Acceptance Criteria

1. THE Workout_Coach_UI SHALL render all screens, components, and UI elements using the Dark_Mode_Theme as the default visual style.
2. THE Dark_Mode_Theme SHALL use dark background colours (charcoal or near-black) with high-contrast light foreground text to ensure readability during workouts.
3. THE Workout_Coach_UI SHALL apply the Dark_Mode_Theme consistently to all screens including the Landing_Page, Create_Program_Screen, Theater_Mode, and Search_Screen.
4. THE Workout_Coach_UI SHALL define theme tokens (background, surface, text-primary, text-secondary, accent, error, border) in a centralised theme configuration so that all components reference shared values.
5. THE Dark_Mode_Theme SHALL ensure sufficient colour contrast between text and background to meet WCAG AA contrast ratio guidelines (minimum 4.5:1 for normal text, 3:1 for large text).

---

### Requirement 2: Landing Page Redesign

**User Story:** As a User, I want a redesigned home screen with quick stats, performance insights, and streamlined navigation, so that I can see my training progress at a glance and take action without friction.

#### Acceptance Criteria

1. WHEN an active or paused Session exists for the User, THE Workout_Coach_UI SHALL display a Resume_Workout_Card at the top of the Landing_Page showing the workout name, session status, and a resume action that navigates to Theater_Mode.
2. WHEN no active or paused Session exists, THE Workout_Coach_UI SHALL hide the Resume_Workout_Card from the Landing_Page.
3. THE Workout_Coach_UI SHALL display a "New Workout" button prominently on the Landing_Page for quick access to starting a new workout.
4. THE Workout_Coach_UI SHALL display a FAB (floating "+" button) on the Landing_Page that, when tapped, presents options to add a new Workout or create a new Program.
5. THE Workout_Coach_UI SHALL display a Weekly_Stats_Chart (bar chart) on the Landing_Page showing the number of workouts completed in the current week relative to a weekly goal (e.g., "3 of 7").
6. THE Workout_Coach_UI SHALL display a Performance_Dashboard section on the Landing_Page containing: a chart of the top 10 exercises by weight over the last 12 months, and a chart of workouts completed per month over the last 12 months.
7. IF the Progress_Tracker_Service is unavailable or returns an error, THEN THE Workout_Coach_UI SHALL display a graceful empty state in the Performance_Dashboard and Weekly_Stats_Chart sections (e.g., placeholder graphics or "Data not available yet" message) without breaking the page layout.
8. THE Workout_Coach_UI SHALL display a navigation menu on the Landing_Page with the following options: Upload JSON, AI Gen, Manage Programs, and Upload Pic.
9. WHEN a User taps "Upload JSON" in the navigation menu, THE Workout_Coach_UI SHALL navigate to the existing JSON upload page.
10. WHEN a User taps "AI Gen" in the navigation menu, THE Workout_Coach_UI SHALL navigate to the AI workout generation page.
11. WHEN a User taps "Manage Programs" in the navigation menu, THE Workout_Coach_UI SHALL navigate to the Vault search page.
12. WHEN a User taps "Upload Pic" in the navigation menu, THE Workout_Coach_UI SHALL navigate to a photo-based program upload page (or a "Coming Soon" placeholder if not yet implemented).

---

### Requirement 3: Create Program Screen (Manual Program Builder)

**User Story:** As a User, I want to manually create a program by assigning existing workouts from my Vault to training days, so that I can organise my weekly schedule without rebuilding workouts from scratch.

#### Acceptance Criteria

1. THE Workout_Coach_UI SHALL provide a Create_Program_Screen accessible via the FAB on the Landing_Page and via a "Create Program" link on the Search_Screen.
2. THE Create_Program_Screen SHALL display a text input field at the top for entering the Program name.
3. THE Create_Program_Screen SHALL display a grid of Day_Tiles (labelled "Day 1", "Day 2", "Day 3", etc.) representing training days in the program.
4. THE Create_Program_Screen SHALL provide an "Add Day" button that appends a new Day_Tile to the grid, allowing the User to define as many training days as needed.
5. WHEN a User taps a Day_Tile, THE Workout_Coach_UI SHALL open a day assignment view that presents two options: "Assign Workout" (from the Vault) and "Assign Activity" (an external activity type).
6. WHEN the User selects "Assign Workout", THE Workout_Coach_UI SHALL display the User's Vault workouts with search and filter functionality, allowing the User to browse and assign a full workout to the day.
7. WHEN the User selects "Assign Activity", THE Workout_Coach_UI SHALL display the list of common activity types (Soccer, Squash, Running, Padel, Golf, Swimming, Cycling, Hiking, Tennis, Basketball, and custom "Other"), allowing the User to assign an external activity to the day.
8. WHEN a User selects a workout from the Vault, THE Workout_Coach_UI SHALL assign that entire workout to the selected day and display the workout name on the Day_Tile as confirmation.
9. WHEN a User selects an external activity, THE Workout_Coach_UI SHALL assign that activity to the selected day and display the activity name (with its emoji icon) on the Day_Tile as confirmation.
10. THE Day_Tile SHALL display the assigned workout or activity name and a summary after an assignment has been made.
11. THE Day_Tile SHALL provide a "Change" action to replace the assignment and a "Remove" action to clear it.
12. THE Create_Program_Screen SHALL display a "Save" button that persists the entire program (name and day-to-workout/activity assignments) via a POST request to the Workout_Creator_Service API.
13. IF a User taps "Save" without entering a program name, THEN THE Workout_Coach_UI SHALL display a validation error indicating the program name is required.
14. IF a User taps "Save" with zero assignments across all days, THEN THE Workout_Coach_UI SHALL display a validation error indicating at least one workout or activity must be assigned to save the program.
15. IF the save request fails due to a server error, THEN THE Workout_Coach_UI SHALL display an error message and allow the User to retry without losing entered data.
16. WHEN the save request succeeds, THE Workout_Coach_UI SHALL display a success message and navigate to the program detail page in the Vault.

---

### Requirement 4: Theater Mode Redesign (Tier-Based Tablet Layout)

**User Story:** As a User training with a large tablet, I want a tier-based workout execution interface with clear exercise logging fields and larger touch targets, so that I can focus on completing each tier and accurately log my performance without squinting or mis-tapping.

#### Acceptance Criteria

1. THE Workout_Coach_UI SHALL render Theater_Mode with a layout optimised for large tablets in landscape orientation, using increased font sizes, larger touch targets, and wider spacing compared to phone-sized layouts.
2. WHEN a User enters Theater_Mode, THE Workout_Coach_UI SHALL display a header showing the current Tier label (e.g., "Tier 1: Compound"), the section type (e.g., "Strength"), and navigation buttons for "Previous tier" and "Next tier".
3. THE Workout_Coach_UI SHALL display an elapsed time stopwatch (format MM:SS or HH:MM:SS) prominently below the header, showing time elapsed since the session started, along with the current section/tier name.
4. THE Workout_Coach_UI SHALL display an exercise list for the current tier, with each exercise showing: a completion checkbox, the exercise name, and the recommendation/prescription text (prescribed weight, reps, and sets).
5. THE Workout_Coach_UI SHALL display per-exercise performance logging input fields: actual reps completed, actual load/weight used, and a notes field or RPE indicator.
6. WHEN a User checks the completion checkbox for an exercise, THE Workout_Coach_UI SHALL record the exercise as complete and persist the logged performance data to the Session_Service.
7. THE Workout_Coach_UI SHALL display a "Move" button that advances focus to the next exercise within the current tier's exercise list.
8. WHEN the User taps "Next tier", THE Workout_Coach_UI SHALL advance to the next tier and update the header to reflect the new tier label and section type.
9. WHEN the User is on the first tier, THE Workout_Coach_UI SHALL disable the "Previous tier" navigation button.
10. WHEN the User is on the last tier and all exercises are completed, THE Workout_Coach_UI SHALL present the option to finish the workout session.
11. THE Workout_Coach_UI SHALL ensure all interactive elements in Theater_Mode (buttons, checkboxes, input fields) have a minimum tap target size of 48x48px for comfortable touch interaction on a tablet.
12. WHEN a User enters Theater_Mode, THE Workout_Coach_UI SHALL request a screen wake lock (via the Screen Wake Lock API) to prevent the device from sleeping during the active workout session.
13. WHEN a User exits Theater_Mode (by finishing, pausing, or navigating away), THE Workout_Coach_UI SHALL release the screen wake lock to allow normal device sleep behaviour.
14. IF the Screen Wake Lock API is not supported by the browser, THEN THE Workout_Coach_UI SHALL continue to function normally without wake lock and SHALL NOT display an error.

---

### Requirement 5: Search Screen Redesign

**User Story:** As a User, I want a visually reorganised search screen with filter chips and a card grid, so that I can quickly browse and find programs matching my training preferences.

#### Acceptance Criteria

1. THE Workout_Coach_UI SHALL display a search bar at the top of the Search_Screen for text-based program/workout search.
2. THE Workout_Coach_UI SHALL display Filter_Chips below the search bar for the following dimensions: Focus Area, Modality, and Days.
3. WHEN a User selects a Filter_Chip value, THE Workout_Coach_UI SHALL update the search results to show only programs matching the selected filter criteria.
4. WHEN multiple Filter_Chips are active, THE Workout_Coach_UI SHALL apply all selected filters using AND logic to narrow the results.
5. THE Workout_Coach_UI SHALL display search results in a 2-column grid layout of program/workout cards showing the program name, goal, and key metadata.
6. THE Workout_Coach_UI SHALL display a "Create Program" action link on the Search_Screen that navigates to the Create_Program_Screen.
7. WHEN a search returns no results, THE Workout_Coach_UI SHALL display an empty state message indicating no matching programs were found.
8. WHEN a User taps a program card in the results grid, THE Workout_Coach_UI SHALL navigate to the program detail page.

---

### Requirement 6: Responsive Layout and Tablet Optimisation

**User Story:** As a User, I want the redesigned screens to work well on my phone at the gym and on a large tablet mounted on a stand during workouts, so that the experience adapts to my device.

#### Acceptance Criteria

1. THE Workout_Coach_UI SHALL render the 2-column grid on the Search_Screen responsively, collapsing to a single column on viewport widths below 600px.
2. THE Workout_Coach_UI SHALL size the FAB on the Landing_Page with a minimum tap target of 56x56px and position it in the bottom-right corner.
3. THE Workout_Coach_UI SHALL render the Day_Tiles grid on the Create_Program_Screen responsively, adjusting the number of columns based on available viewport width.
4. THE Workout_Coach_UI SHALL render Theater_Mode in a landscape-optimised layout when the viewport width exceeds 900px, using side-by-side panels for exercise list and logging inputs.
5. THE Workout_Coach_UI SHALL ensure all interactive elements across all screens meet a minimum tap target of 44x44px for touch accessibility.

---

### Requirement 8: Manual Input Screen (JSON Workout Editor)

**User Story:** As a User, I want to manually type a workout in JSON format, validate it, preview the structured result, and then upload it to my Vault, so that I can quickly create a single workout using a format I already know without needing a file picker.

#### Acceptance Criteria

1. THE Workout_Coach_UI SHALL provide a Manual_Input_Screen accessible via the navigation menu on the Landing_Page (as an option under the FAB or navigation) and via a direct route.
2. THE Manual_Input_Screen SHALL display a full-screen JSON text editor area where the User can type or paste raw JSON representing a single workout conforming to the Upload_Schema.
3. THE Manual_Input_Screen SHALL display a "Validate" or "Submit" button that sends the entered JSON to the Workout_Creator_Service validate-only endpoint (`POST /api/v1/uploads/programs/validate`).
4. WHEN the JSON passes schema validation, THE Workout_Coach_UI SHALL transition to a preview mode displaying the parsed workout in a structured, readable format showing the program name, sections, exercises, sets, reps, and prescribed weights.
5. WHEN the JSON fails schema validation, THE Workout_Coach_UI SHALL display the field-level validation errors inline below the editor without clearing the editor content, allowing the User to correct the errors and resubmit.
6. WHEN the JSON is not valid JSON (parse error), THE Workout_Coach_UI SHALL display an inline error message indicating the JSON syntax error position without clearing the editor content.
7. WHILE in preview mode, THE Workout_Coach_UI SHALL display an "Upload to Vault" button and a "Back to Edit" button.
8. WHEN a User taps "Upload to Vault" in preview mode, THE Workout_Coach_UI SHALL submit the JSON to the Workout_Creator_Service upload endpoint (`POST /api/v1/uploads/programs`) and display a loading indicator while the request is in progress.
9. WHEN the upload succeeds, THE Workout_Coach_UI SHALL display a success confirmation message with the program name and provide a link to view it in the Vault.
10. WHEN the upload fails, THE Workout_Coach_UI SHALL display the error message and return the User to the editor with their JSON content preserved.
11. WHILE an upload request is in progress, THE Workout_Coach_UI SHALL disable the "Upload to Vault" button to prevent duplicate submissions.
12. THE Manual_Input_Screen SHALL apply the Dark_Mode_Theme to the JSON editor, using a monospace font with syntax-appropriate colouring for readability.

---

### Requirement 9: External Activity Logging

**User Story:** As a User, I want to log workouts and activities I do outside the app (like soccer, running, padel, or golf), so that I can track all my physical activity in one place and see a complete picture of my training load.

#### Acceptance Criteria

1. THE Workout_Coach_UI SHALL display a "Log Activity" button on the Landing_Page that navigates to the Activity_Log_Screen.
2. THE Activity_Log_Screen SHALL display a form with the following fields: Activity type (mandatory), Date (mandatory), Duration, Calories burned, Distance, and a free-text Notes field for any additional stats.
3. THE Activity_Log_Screen SHALL provide a selectable list of common activity types including but not limited to: Soccer, Squash, Running, Padel, Golf, Swimming, Cycling, Hiking, Tennis, Basketball, and a custom "Other" option that allows the User to type a custom activity name.
4. THE Activity_Log_Screen SHALL default the Date field to today's date and allow the User to select a past date via a date picker.
5. THE Workout_Coach_UI SHALL validate that Activity type and Date are provided before allowing submission; IF either mandatory field is missing, THEN THE Workout_Coach_UI SHALL display a validation error indicating the missing field.
6. THE Duration field SHALL accept a value in minutes; THE Calories field SHALL accept a numeric value in kcal; THE Distance field SHALL accept a numeric value with a unit selector (km or miles).
7. WHEN a User submits the activity log, THE Workout_Coach_UI SHALL persist the entry via a POST request to the appropriate backend endpoint and display a success confirmation.
8. IF the backend endpoint for activity logging is not yet implemented, THEN THE Workout_Coach_UI SHALL store the activity log locally and display a message indicating that the entry has been saved locally and will sync when the feature is fully available.
9. WHEN the submission succeeds, THE Workout_Coach_UI SHALL return the User to the Landing_Page and the logged activity SHALL be reflected in the Weekly_Stats_Chart (counting toward total activity for the week).
10. THE Activity_Log_Screen SHALL apply the Dark_Mode_Theme consistently with the rest of the application.

---

### Requirement 10: Graceful Degradation for Missing Backend Services

**User Story:** As a User, I want the app to remain usable even when some backend services are not yet available, so that I can use the features that are ready without encountering broken screens.

#### Acceptance Criteria

1. IF the Progress_Tracker_Service is unavailable or returns an error when fetching weekly stats, THEN THE Workout_Coach_UI SHALL display a placeholder state in the Weekly_Stats_Chart area without crashing or showing a full-page error.
2. IF the Progress_Tracker_Service is unavailable or returns an error when fetching performance dashboard data, THEN THE Workout_Coach_UI SHALL display a placeholder state in the Performance_Dashboard section (e.g., "Performance data coming soon") without affecting other Landing_Page features.
3. IF the "Upload Pic" feature backend is not implemented, THEN THE Workout_Coach_UI SHALL navigate to a "Coming Soon" placeholder page when the menu item is tapped.
4. THE Workout_Coach_UI SHALL isolate API call failures for optional features (stats, dashboard, photo upload) so that critical features (resume workout, navigation, search, create program) remain fully functional.
