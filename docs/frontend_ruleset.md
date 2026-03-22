# AidBridge - Frontend Ruleset

This document is the non-negotiable standard for all Android developers implementing new role-based features in AidBridge (Victim, Volunteer, Sponsor, Staff, Admin).

Any violation is a blocking issue for review and merge.

## 1. Zero Tolerance Tech Constraints

- Java 17 only.
- Kotlin is banned.
- XML Layout + ViewBinding only.
- Jetpack Compose is banned.
- MVVM + Clean Architecture is mandatory for all new features.

Enforcement note:

- Any Kotlin file, Compose function, or mixed UI stack in a role feature is an automatic PR rejection.

## 2. Strict Role Isolation (Crucial)

Role-based implementation must be physically separated in code, layouts, and navigation.

### 2.1 Code Placement Rules

- Role-specific Fragments must be placed under:
  - ui/main/fragment/<role_name>/
- Role-specific ViewModels must be placed under:
  - ui/main/viewmodel/<role_name>/
- Shared components are allowed only when explicitly role-agnostic and placed in a shared module/package.
- Cross-role feature logic must not be copied between role folders; move common behavior to a shared layer (usecase/repository/helper), not to another role package.

### 2.2 Layout Placement Rules

- Role-specific XML layouts must be placed in the corresponding resource source-set:
  - Victim: app/src/main/res-role-victim/layout/
  - Volunteer: app/src/main/res-role-volunteer/layout/
  - Sponsor: app/src/main/res-role-sponsor/layout/
  - Staff: app/src/main/res-role-staff/layout/
  - Admin: app/src/main/res-role-admin/layout/
- Placing role-specific layouts in app/src/main/res/layout/ is banned.

### 2.3 Navigation Isolation Rules

- Each role flow must be contained in its own navigation graph:
  - nav_graph_victim.xml
  - nav_graph_volunteer.xml
  - nav_graph_sponsor.xml
  - nav_graph_staff.xml
  - nav_graph_admin.xml
- Role screen actions must be declared only in that role graph.
- Direct cross-role navigation actions are prohibited unless approved as an architecture decision and documented.

## 3. Dumb UI and Event Wrapper Mandate

### 3.1 Fragment Responsibilities (Strict)

- Fragments are presentation-only.
- Fragments must not contain business decision logic.
- Fragments must not contain validation regex or validation rules.
- Fragments must not call Retrofit/API/Repository directly.
- Fragments may only:
  - Bind views
  - Forward user actions to ViewModel
  - Observe state/events from ViewModel
  - Render UI

### 3.2 ViewModel Boundaries

- ViewModels must not hold Android Context.
- ViewModels must expose lifecycle-aware UI state via LiveData.
- Validation/business orchestration belongs to UseCase + Validator, not Fragment.

### 3.3 One-Time Event Safety (Mandatory)

- All one-time UI events (Navigation, Toast, Snackbar, dialog trigger) must be consumed through the existing event-safe flow:
  - NetworkResultWrapper.hasBeenHandled()
  - BaseFragment.resultObserver()
- Duplicate event triggers after back-press, fragment recreation, or rotation are unacceptable.

## 4. No Hardcoding UI Law

### 4.1 Colors

- Hardcoded hex colors in XML are banned.
- Use color resources via @color/... from values/colors.xml.

### 4.2 Strings

- Hardcoded user-facing text in Java/XML is banned.
- Use @string/... in XML and getString(...) in Java.
- Role-specific strings must be placed in app/src/main/res-role-<role>/values/strings_<role>.xml.
- Placing role-specific strings in app/src/main/res/values/strings.xml is prohibited.

### 4.3 Icons

- Prefer built-in Android icons first when suitable (@android:drawable/...).
- Custom vector icons are discouraged and allowed only when built-in options are not appropriate.
- Approved placement for custom icons:
  - Shared icons: app/src/main/res-common-ui/drawable/
  - Role-specific icons: corresponding res-role-<role>/drawable/

### 4.4 Theming and Reuse

- UI components must reuse project theme/styles from themes.xml.
- Feature-specific one-off styling is disallowed if an existing shared style can be reused.
- New styles must follow existing AidBridge naming conventions and be reviewed for reuse impact.

## 5. PR Definition of Done (DoD) Checklist

Before opening or approving a PR for a new screen, all items below must be true:

- [ ] Fragment is dumb: no business logic, no validation regex, no API/repository call.
- [ ] Role isolation is correct: code, layout, and navigation are placed in the correct role-specific paths/graph.
- [ ] Back-stack and navigation are safe: actions are valid in the role graph and event handling prevents duplicate navigation.
- [ ] No UI hardcoding exists: strings/colors/icons/styles follow resource and theme rules.
- [ ] ViewModel/usecase/repository layering is clean, with one-time events consumed via hasBeenHandled + BaseFragment.resultObserver.

---

Governance:

- These rules apply to all new role-based development.
- Any exception requires explicit architecture approval documented in the PR description.
- If conflict exists between speed and standards, standards win.
