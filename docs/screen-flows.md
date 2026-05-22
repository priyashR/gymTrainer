# HybridStrength — Screen Flows

## Navigation Map

```mermaid
flowchart TD
    LOGIN[Login Page] -->|Login success| HOME[Home Page]
    REGISTER[Register Page] -->|Register success| LOGIN
    LOGIN -->|Register link| REGISTER
    REGISTER -->|Login link| LOGIN
    HOME -->|Logout| LOGIN

    HOME --> NEW_WORKOUT[New Workout Menu]
    HOME --> SEARCH[Vault Search]
    HOME --> PERFORMANCE[My Performance]
    HOME -->|Resume Session| THEATER[Theater Mode]
    HOME -->|Next Step · Start| THEATER

    NEW_WORKOUT --> GEMINI[Ask Gemini · Coming Soon]
    NEW_WORKOUT --> UPLOAD[Upload Program]

    UPLOAD -->|Save success · View in Vault| PROGRAM_DETAIL[Program Detail]
    UPLOAD -->|Cancel / Back| HOME

    SEARCH --> PROGRAM_DETAIL
    SEARCH -->|Back| HOME

    PROGRAM_DETAIL -->|Start New Program| THEATER
    PROGRAM_DETAIL -->|Start Standalone| THEATER
    PROGRAM_DETAIL -->|Back| SEARCH
    PROGRAM_DETAIL -->|Delete success| SEARCH
    PROGRAM_DETAIL -->|Copy success| PROGRAM_DETAIL_COPY[Program Detail · Copy]

    THEATER -->|End Workout · confirmed| HOME
    THEATER -->|Leave Workout · confirmed| HOME

    PERFORMANCE -->|Back| HOME
    GEMINI -->|Back| HOME
```

## Detailed Screen Flows

### Flow 1: Registration & Login

```mermaid
sequenceDiagram
    participant U as User
    participant R as Register Page
    participant L as Login Page
    participant H as Home Page

    U->>R: Navigate to /register
    U->>R: Enter email + password
    R->>R: Submit registration
    R-->>L: Redirect to /login (success)
    Note over R,L: "Already have an account?" link also goes to Login

    U->>L: Enter credentials
    L->>L: Submit login
    L-->>H: Redirect to / (success)
    Note over L,R: "Create account" link goes to Register

    U->>H: Click "Logout"
    H-->>L: Clear token, redirect to /login
```

### Flow 2: Upload a Program

```mermaid
sequenceDiagram
    participant U as User
    participant H as Home Page
    participant UP as Upload Page
    participant PD as Program Detail

    U->>H: Click "New Workout"
    H->>H: Expand submenu
    U->>UP: Click "Upload Program"
    U->>UP: Select JSON file
    UP->>UP: Parse & show preview

    alt Save to Vault
        U->>UP: Click "Save to Vault"
        UP->>UP: Upload to backend (201 Created)
        UP-->>PD: Navigate to /vault/programs/:id (View in Vault link)
        U->>PD: Click "Back"
        PD-->>H: Navigate to / (or Vault Search)
    else Cancel upload
        U->>UP: Click "Cancel" or browser back
        UP-->>H: Navigate to /
    end
```

### Flow 3: Start a Program from Vault

```mermaid
sequenceDiagram
    participant U as User
    participant H as Home Page
    participant S as Vault Search
    participant PD as Program Detail
    participant TM as Theater Mode

    U->>H: Click "Search for a workout"
    H-->>S: Navigate to /vault/search
    U->>S: Browse or search programs
    U->>PD: Click on a program result
    S-->>PD: Navigate to /vault/programs/:id

    alt Start New Program
        U->>PD: Click "Start New Program"
        PD->>PD: Show confirmation (if active program exists)
        U->>PD: Confirm
        PD->>PD: Enroll + start session
        PD-->>TM: Navigate to /workout/session/:id
        Note over TM,H: On End/Leave → returns to Home
    else Back to search
        U->>PD: Click "Back"
        PD-->>S: Navigate to /vault/search
    else Back to home
        U->>S: Click "Back"
        S-->>H: Navigate to /
    end
```

### Flow 4: Resume or Continue a Program

```mermaid
sequenceDiagram
    participant U as User
    participant H as Home Page
    participant TM as Theater Mode

    U->>H: Open Home Page
    H->>H: Fetch active session & enrollment

    alt Active session exists (paused/in-progress)
        H->>H: Show "Resume Session" banner
        U->>H: Click "Resume Session"
        H-->>TM: Navigate to /workout/session/:id
        Note over TM,H: End/Leave → returns to Home
        TM-->>H: Navigate to / (on End or Leave)
    else Active enrollment exists (no active session)
        H->>H: Show "Next Step" with program info
        U->>H: Click "Start"
        H->>H: Create new session for next day
        H-->>TM: Navigate to /workout/session/:id
        TM-->>H: Navigate to / (on End or Leave)
    end
```

### Flow 5: Theater Mode Workout

```mermaid
sequenceDiagram
    participant U as User
    participant TM as Theater Mode
    participant H as Home Page

    U->>TM: Enter Theater Mode
    TM->>TM: Load session state (REST fetch)
    TM->>TM: Connect WebSocket (STOMP subscribe)
    TM->>TM: Show current section + exercises

    loop For each exercise
        U->>TM: Check off exercise
        TM->>TM: Show rest timer overlay
        TM->>TM: Update progress (persist to server)
    end

    U->>TM: Navigate sections (prev/next)

    alt All exercises complete
        TM->>TM: Show "Finish Workout" prompt
        U->>TM: Click "Finish Workout"
        TM->>TM: Mark session complete
        TM-->>H: Navigate to / (Home)
    else End Workout early
        U->>TM: Click "End Workout"
        TM->>TM: Show confirmation dialog
        U->>TM: Confirm
        TM->>TM: Mark session complete (partial progress preserved)
        TM-->>H: Navigate to / (Home)
    else Leave Workout (persist & exit)
        U->>TM: Click "Leave Workout"
        TM->>TM: Show confirmation dialog
        U->>TM: Confirm
        TM->>TM: Persist current state (session stays IN_PROGRESS)
        TM-->>H: Navigate to / (Home)
        Note over H: "Resume Session" banner will appear on next visit
    else Decline to leave
        U->>TM: Click "Leave Workout"
        TM->>TM: Show confirmation dialog
        U->>TM: Cancel
        TM->>TM: Stay in Theater Mode (no navigation)
    end
```

### Flow 6: Start Standalone Workout from Vault

```mermaid
sequenceDiagram
    participant U as User
    participant S as Vault Search
    participant PD as Program Detail
    participant TM as Theater Mode
    participant H as Home Page

    U->>S: Search for a program
    U->>PD: Click on program result
    S-->>PD: Navigate to /vault/programs/:id
    U->>PD: Expand a week/day
    U->>PD: Click "Start Standalone" on a day
    PD->>PD: Create standalone session
    PD-->>TM: Navigate to /workout/session/:id
    Note over TM: Program pointer NOT affected

    alt Complete or end workout
        TM-->>H: Navigate to / (Home)
    else Leave workout
        TM-->>H: Navigate to / (Home)
        Note over H: Session resumable later
    end
```

### Flow 7: Program Detail Actions (Delete, Copy, Edit)

```mermaid
sequenceDiagram
    participant U as User
    participant S as Vault Search
    participant PD as Program Detail
    participant ED as JSON Editor (inline)

    U->>PD: Viewing program detail

    alt Delete program
        U->>PD: Click "Delete"
        PD->>PD: Show confirmation
        U->>PD: Confirm delete
        PD->>PD: DELETE /api/v1/vault/programs/:id (204)
        PD-->>S: Navigate to /vault/search
    else Copy program
        U->>PD: Click "Copy"
        PD->>PD: POST copy request
        PD-->>PD: Navigate to /vault/programs/:newId (copy)
    else Edit JSON
        U->>PD: Click "Edit JSON"
        PD-->>ED: Show inline JSON editor
        U->>ED: Modify JSON
        U->>ED: Click "Save"
        ED->>ED: PUT /api/v1/vault/programs/:id
        ED-->>PD: Return to detail view (updated)
    else Cancel edit
        U->>ED: Click "Cancel"
        ED-->>PD: Return to detail view (unchanged)
    else Back
        U->>PD: Click "Back"
        PD-->>S: Navigate to /vault/search
    end
```

## Page Inventory

| Route | Page | Purpose |
|-------|------|---------|
| `/login` | Login | Email/password authentication |
| `/register` | Register | New account creation |
| `/` | Home | Dashboard with Next Step, Resume, navigation |
| `/upload` | Upload Program | JSON file upload with preview |
| `/vault/search` | Vault Search | Browse and search saved programs |
| `/vault/programs/:id` | Program Detail | View program structure, start/enroll actions |
| `/workout/session/:sessionId` | Theater Mode | Active workout execution UI |
| `/new-workout` | Coming Soon | Placeholder for AI generation |
| `/my-performance` | Coming Soon | Placeholder for analytics |
