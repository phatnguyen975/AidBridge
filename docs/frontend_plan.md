# AidBridge Android Frontend Plan (Rebased Roadmap)

> Scope: `drc-app` Android app  
> Language/UI constraints: Java 17 + XML only  
> Architecture: MVVM + Clean Architecture + Navigation + Hilt

## Planning Principles

1. No Google Maps, GPS tracking, realtime map marker, or route polyline work before the final phase.
2. Build stable authentication and shell navigation first to avoid expensive rework.
3. Keep Fragments thin and move all logic into ViewModels/UseCases/Repositories.
4. Keep each phase releasable with smoke-test checklists.

## Phase 1: Authentication and Onboarding

### Goals

- Deliver complete auth funnel: Guest entry, Login, Register, OTP, Forgot Password.
- Stabilize session bootstrap from Splash to Auth/Main shell.
- Standardize validation, loading/error states, and one-time event handling.

### Feature Scope

- Guest shell and entry actions to login/register.
- Login with email/password.
- Register with role selection and form validation.
- OTP verification flow with resend countdown and auto-submit behavior.
- Forgot password flow: email -> OTP -> new password.
- Session persistence with token and user metadata.

### Architecture Tasks

- Finalize `AuthInputValidator` contracts for all auth forms.
- Keep `Transformations.switchMap` trigger pattern in auth ViewModels.
- Unify NetworkResult handling via `BaseFragment.resultObserver()`.
- Keep auth repository contract stable while backend endpoints are toggled between mock/live.

### Deliverables

- Production-ready auth navigation graph.
- UX consistency for input errors, button loading states, and retry messaging.
- Session guard in splash (`hasActiveSession`) with deterministic routing.

### Definition of Done

- All auth screens reachable and recoverable via back navigation rules.
- No duplicate submit on fast taps.
- Manual QA checklist passed: login, register, OTP, forgot-password happy/edge paths.

## Phase 2: Core UI and Navigation

### Goals

- Build the application shell for role-based experience without map dependency.
- Lock down bottom navigation behavior and role graph boundaries.

### Feature Scope

- MainActivity shell and role-specific home placeholders.
- Bottom navigation menus per role.
- Guest tabs and role tabs using role graph isolation.
- Safe navigation utilities for action checks and debounce.

### Architecture Tasks

- Consolidate graph ownership rules:
  - Auth/Guest flows in guest graph.
  - Role flows in role-specific nav graphs.
- Establish shared base UI components for loading/error/empty states.
- Define screen-level contract: what belongs in Fragment vs ViewModel.

### Deliverables

- Stable role entry points (Victim/Volunteer/Sponsor/Staff/Admin).
- Predictable back-stack policy from auth to main shell.
- Shared navigation conventions documented for all new screens.

### Definition of Done

- No `IllegalArgumentException`/`IllegalStateException` from navigation in smoke tests.
- All role menu tabs route to valid destinations.
- Auth -> Main transition cannot back-navigate into auth stack.

## Phase 3: Profile and Settings

### Goals

- Implement user self-service and preference management.
- Introduce account and security controls independent of map features.

### Feature Scope

- Profile screen per role (user info summary, role badge).
- Edit basic user info and avatar upload pipeline (UI and API contract level).
- Account security actions: logout, session clear, password update entry.
- App settings: notification preference toggles and display preferences.

### Architecture Tasks

- Define profile repository/usecases and DTO mapping.
- Add local persistence for simple app settings (Room or SharedPreferences by responsibility).
- Standardize confirmation dialogs and destructive-action patterns.

### Deliverables

- Profile module integrated into role shell navigation.
- Settings module with persistent state.
- Unified logout/clear-session flow from any role.

### Definition of Done

- Profile and settings survive process recreation.
- Logout clears session and always routes back to Auth flow.
- Form validations and API error states follow shared auth UX standards.

## Phase 4: Non-Map Features

### Goals

- Deliver high-value operational flows that do not require map rendering or live GPS.
- Build role business workflows and messaging/notification infrastructure first.

### Feature Scope

- SOS/relief forms as structured non-map requests (address/manual location text allowed).
- Request/task list screens (assigned, pending, completed).
- Request detail/status timeline components.
- In-app notifications center (list/read state).
- Chat module scaffolding and message list UI (without location overlays).
- Sponsor/staff non-map flows: donation registration, QR workflow UI, inventory list/status.

### Architecture Tasks

- Introduce feature packages per role with clear boundaries.
- Add pagination/state reducers for list-heavy screens.
- Define offline-first behavior for critical lists and request history.

### Deliverables

- End-to-end non-map role workflows reachable from main shell.
- Notification and chat UI contracts integrated with backend APIs/stubs.
- Consistent loading/empty/error states in list-based modules.

### Definition of Done

- Core non-map business flows demoable for all active roles.
- No map SDK dependency required to complete Phase 4 acceptance tests.
- List/detail/create flows covered by regression checklist.

## Phase 5 (Final): Map Integration and Realtime Tracking

### Goals

- Add all geospatial and realtime features after business flows are stable.
- Integrate map rendering, route overlays, live tracking, and proximity updates.

### Feature Scope

- Google Maps integration for guest and role-specific map screens.
- Marker sets: hubs, shelters, mission points.
- Safe path polylines and heatmap layers.
- FusedLocation location capture and periodic updates.
- Realtime transport via WebSocket/STOMP for tracking and mission updates.
- Foreground location service behavior for volunteer mission tracking.

### Architecture Tasks

- Introduce map renderer abstraction to isolate SDK-specific code.
- Define location permission state machine and fallback UX.
- Add throttling/debouncing for realtime UI updates.

### Deliverables

- Full map-enabled production flow with realtime status updates.
- Permission-safe location lifecycle handling.
- Operational observability for location and socket failures.

### Definition of Done

- Tracking and map modules pass battery/performance smoke checks.
- Permission denial and reconnect scenarios are gracefully handled.
- End-to-end mission simulation verified with realtime updates.

## Cross-Phase Quality Gates

1. Architecture gate: Fragment -> ViewModel -> UseCase -> Repository only.
2. Navigation gate: all new actions validated against current destination before navigate.
3. UX gate: loading, empty, and error states are mandatory for each screen.
4. Security gate: token/session data only through secure storage wrappers.
5. Release gate: each phase has a smoke test script and rollback notes.

## Suggested Sprint Ordering

1. Sprint 1-2: Phase 1
2. Sprint 3: Phase 2
3. Sprint 4: Phase 3
4. Sprint 5-6: Phase 4
5. Sprint 7-8: Phase 5
