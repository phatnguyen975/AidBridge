# AidBridge - Frontend Instruction

This file is the strict execution rulebook for AI coding assistants working in `drc-app/`.

## MANDATORY PREREQUISITE (READ FIRST - BLOCKING)

1. STOP. Before generating any code or answering any question, you MUST read and ingest all files below:
   - `docs/frontend_architecture.md` (read full file).
   - `docs/main/requirements.md` (read full file).
   - `docs/main/project_structure.md` (read only the `drc-app/` frontend section).
   - `docs/main/tech_stack.md` (read only the Frontend section).
2. If you cannot access these files automatically, you MUST ask the user to provide their contents before proceeding.
3. Do not continue implementation until this prerequisite is completed.

## 1. General Coding and UI Rules

1. Write idiomatic, maintainable Java with clear naming and small focused methods.
2. Keep architecture boundaries strict: UI code in Fragment, state in ViewModel, business rules in UseCase, data orchestration in Repository.
3. Null safety is mandatory:
   - Always guard nullable inputs/outputs from API, Bundle, and DB.
   - Do not assume non-null for `BaseResponse.getData()` and `BaseResponse.getMessage()`.
   - Use explicit fallback messages for null/blank error text.
4. UI responsiveness is mandatory:
   - Prefer `ConstraintLayout` for complex screens to reduce deep view nesting.
   - Use `RecyclerView` for dynamic lists; avoid heavy nested layouts.
   - Do not block the main thread with network or database operations.
5. Use Material Design 3 components and project themes/styles:
   - Prefer `com.google.android.material.*` widgets for buttons, tabs, text fields, sheets.
   - Reuse existing styles before creating new ones.
6. Standard error handling is required:
   - Normalize errors in repository layer.
   - Surface UI-safe messages through `NetworkResultWrapper` states.
   - Avoid leaking transport/exception details directly to UI.

## 2. Zero-Tolerance Tech Rules

1. Use Java 17 only.
2. Kotlin is forbidden.
3. Use XML Layout + ViewBinding only.
4. Jetpack Compose is forbidden.
5. Use existing stack: Hilt, Retrofit, Room, LiveData, Navigation Component.

Any violation is a blocking failure.

## 3. Mandatory Architecture Flow

Follow this exact flow:

```text
Fragment -> ViewModel -> UseCase -> Repository Interface -> Repository Impl -> API/Local
```

Rules:

1. Fragments are dumb UI only.
2. Fragments must not contain business rules, validation rules, or data mapping.
3. Fragments must not call Retrofit/Room/Repository directly.
4. ViewModels must expose state through LiveData.
5. ViewModels must trigger use cases via `Transformations.switchMap` (trigger-based pattern).
6. Repository implementation must unwrap `BaseResponse<T>` and map to `NetworkResultWrapper<T>`.
7. One-time UI events must use `NetworkResultWrapper.hasBeenHandled()` and `BaseFragment.resultObserver()`.

## 4. Navigation Safety (Critical)

Never call raw `navController.navigate()` in Fragments.

Use only safe helpers from `BaseFragment`:

1. `navigateSafely(...)`
2. `navigateToDestinationSafely(...)`
3. `popBackStackSafely(...)`

Do not bypass these helpers. This mandate exists to prevent fast-click and stale-backstack crashes.

## 5. Role Isolation Requirements

1. Role fragments -> `ui/main/fragment/<role_name>/`
2. Role viewmodels -> `ui/main/viewmodel/<role_name>/`
3. Role adapters -> `ui/main/adapter/<role_name>/`
4. Role navigation must remain in its dedicated role graph.
5. Do not implement direct cross-role feature coupling.

## 6. No Hardcoding Law

Do not hardcode UI values in Java or XML.

1. No hardcoded user-facing text.
2. No hardcoded dimensions.
3. No hardcoded hex colors.

Use resources only:

1. Strings -> `@string/...` / `getString(...)`
2. Dimensions -> `@dimen/...`
3. Colors -> `@color/...`

Role-specific resources must stay in role source-sets:

1. Layouts: `app/src/main/res-role-<role>/layout/`
2. Drawables: `app/src/main/res-role-<role>/drawable/`
3. Strings: `app/src/main/res-role-<role>/values/strings_<role>.xml`
4. Dimens: `app/src/main/res-role-<role>/values/dimens_<role>.xml`

Do not move role-specific resources to global `res/values` unless they are truly shared by all roles.

## 7. API Wrapper Enforcement

1. Retrofit interfaces must use `BaseResponse<T>` as the server wrapper.
2. Repository implementation must check in order:
   - HTTP success.
   - `body != null`.
   - `body.isSuccess()`.
   - `body.getData()` / `body.getMessage()` with null safety.
3. Repository outputs to presentation must use `NetworkResultWrapper` states.

## 8. Output and Explanation Rules

1. Explain changes briefly and clearly in Vietnamese.
2. For complex UseCase/Repository logic, include standard JavaDoc/comments in English.
3. Keep edits minimal and architecture-safe.
