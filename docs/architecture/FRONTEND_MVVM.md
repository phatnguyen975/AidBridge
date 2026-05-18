# AidBridge - Frontend Architecture

## 1. Purpose and Scope

This document defines the canonical frontend architecture for AidBridge Android in `drc-app/`.

Scope:

1. Java-only Android app (Java 17).
2. XML + ViewBinding UI stack.
3. MVVM + Clean Architecture with role-based isolation.
4. Integration contract for wrapped backend responses (`BaseResponse<T>`).

### 1.1 Core Stack

AidBridge frontend is standardized on the following core stack:

1. Java 17 (no Kotlin).
2. XML Layout + ViewBinding (no Jetpack Compose).
3. AndroidX ViewModel + LiveData.
4. Jetpack Navigation (role-based nav graphs).
5. Dagger Hilt (DI).
6. Retrofit + OkHttp + Gson.
7. Room (local storage).
8. Firebase Messaging (FCM).
9. RxJava3 + STOMP client (realtime channel foundation).
10. Glide (image loading).
11. CameraX + ML Kit Barcode (QR workflows).
12. Google Maps + Fused Location.

This is a developer-facing architecture guide (theory + implementation rules + examples).

## 2. Architectural Principles

### 2.1 Non-Negotiable Constraints

1. Language is Java 17 only.
2. UI is XML + ViewBinding only.
3. Dependency Injection uses Hilt.
4. Network transport uses Retrofit/OkHttp.
5. Local persistence uses Room.
6. Presentation state uses LiveData.

### 2.2 Why MVVM + Clean Architecture Here

The project spans multiple role flows (Guest, Victim, Volunteer, Sponsor, Staff, Admin) and combines real-time, geospatial, and transactional workflows. To keep complexity manageable, architecture must:

1. Separate presentation concerns from business rules.
2. Keep data source concerns out of UI.
3. Support independent role evolution without tight coupling.
4. Make API contract changes localized to data/repository boundaries.

### 2.3 Hilt DI Package Convention (`/di`)

All application-level Dependency Injection declarations must be centralized in:

```text
app/src/main/java/com/drc/aidbridge/di/
```

Current canonical modules and responsibilities:

1. `AppModule`: app-wide primitives (for example secure/shared preferences).
2. `NetworkModule`: `OkHttpClient`, interceptors, and `Retrofit`.
3. `ApiModule`: Retrofit service interfaces (`AuthApiService`, etc.).
4. `DatabaseModule`: Room database + DAO providers.
5. `RepositoryModule`: interface-to-implementation bindings (`@Binds`).

Mandatory DI rules:

1. New global providers/bindings must be added to the correct module in `/di`, not scattered in feature packages.
2. Install scope must match lifecycle need (`SingletonComponent` for app-wide singletons).
3. Repository interfaces are consumed by UseCase/ViewModel; concrete implementations are wired in DI modules only.
4. Feature classes should request dependencies via constructor injection (`@Inject`), not manual instantiation.

## 3. Canonical Data Flow

The required data flow is:

```text
-------------------- Client Action Path --------------------
[Client Action]
       |
       v
[Fragment]
   - Collect input
   - Forward intent
       |
       v
[ViewModel]
   - Trigger by MutableLiveData
   - Execute via Transformations.switchMap
       |
       v
[UseCase]
   - Business validation/orchestration
       |
       v
[Repository Interface]
   - Domain contract only
       |
       v
[Repository Impl]
   - Choose source (Remote-first / Local-first)
   - Parse BaseResponse<T>
   - Map DTO/Entity -> Domain
   - Emit NetworkResultWrapper<T>
      /                        \
     v                          v
[API (Retrofit)]         [Local (Room/Cache)]

-------------------- API/Local Result Path --------------------
API/Local -> Repository Impl -> ViewModel (LiveData) -> Fragment (render)
```

### 3.1 Fragment (UI Only)

1. Bind views and collect UI input.
2. Forward intents/actions to ViewModel.
3. Observe LiveData and render state.
4. Handle one-time events through `BaseFragment.resultObserver(...)`.

Fragment must not:

1. Implement business validation rules.
2. Call repository/API/local DB directly.
3. Perform DTO-domain mapping.

### 3.2 ViewModel (State Orchestration)

1. Expose immutable LiveData to UI.
2. Hold trigger LiveData (`MutableLiveData<Params>`).
3. Execute use case via `Transformations.switchMap(trigger, ...)`.
4. Publish loading/success/error states for rendering.

### 3.3 UseCase (Business Logic)

1. Owns business rules and orchestration.
2. Coordinates validation and repository call order.
3. Remains framework-light and testable.

### 3.4 Repository Interface (Domain Contract)

1. Defines use-case-facing data operations.
2. Hides transport/storage details.

### 3.5 Repository Implementation (Integration Boundary)

1. Calls Retrofit/Room or other data sources.
2. Parses transport responses.
3. Maps DTO to Domain model.
4. Normalizes results into `NetworkResultWrapper<T>`.

### 3.6 API/Local (Data Source Layer Behavior)

At the API/Local end of the flow, repository implementation must treat remote and local as explicit data-source responsibilities, not as interchangeable shortcuts.

1. API is the source for server-authoritative data and mutation endpoints (login/register/profile updates, remote task states, etc.).
2. Local is the source for cached/offline-friendly reads, session snapshots, and persistence required for startup continuity.
3. Repository decides source order per use case:
   - Remote-first when data freshness is mandatory.
   - Local-first when fast rendering is required, then remote sync/refresh.
4. Any remote response must still pass the `BaseResponse<T>` parsing sequence before updating local cache.
5. Local entities/DTOs must be mapped to domain models before emitting to ViewModel (no raw storage/network models above repository layer).
6. Error strategy should degrade gracefully:
   - If remote fails but valid local cache exists, emit local data with clear stale-state signaling when needed.
   - If both remote and local fail/empty, emit normalized error through `NetworkResultWrapper.Error`.

## 4. `BaseResponse<T>` to `NetworkResultWrapper<T>` Mapping Pattern

AidBridge backend returns wrapped JSON with outer fields:

1. `success`
2. `message`
3. `data`

Android parsing wrapper:

- `BaseResponse<T>`

UI state wrapper:

- `NetworkResultWrapper<T>` with `Loading`, `Success`, `Error`

### 4.1 Canonical Repository Parsing Sequence

For each API call, repository implementation must execute in this order:

1. Emit `NetworkResultWrapper.loading()`.
2. Check HTTP-level success (`response.isSuccessful()`).
3. Check body non-null.
4. Check API-level success (`body.isSuccess()`).
5. Read `body.getData()` with null-safety.
6. Read `body.getMessage()` with null-safety for error fallback.
7. Map DTO -> Domain.
8. Emit `NetworkResultWrapper.success(domainData)`.
9. On any failure path, emit `NetworkResultWrapper.error(message, code?)`.

### 4.2 Null-Safety Notes

Because `getData()` and `getMessage()` can be nullable:

1. Never assume `data` is present on success without checking.
2. Always provide fallback error messages when `message` is null/blank.
3. Avoid leaking `BaseResponse` above repository boundary.

## 5. One-Time Event Handling Mechanism

AidBridge uses one-time event consumption to prevent duplicated UI actions after lifecycle recreation.

Key mechanism:

1. `NetworkResultWrapper.hasBeenHandled()`
2. `NetworkResultWrapper.markAsHandled()`
3. `BaseFragment.resultObserver(...)`

### 5.1 Expected Behavior

1. Loading state can re-render freely.
2. Success/Error events are consumed once.
3. Re-observation after backstack/rotation must not retrigger the same navigation/toast/dialog.

### 5.2 Practical Rule

Fragment observers should use shared observer helpers from `BaseFragment` for consistent consumption behavior.

## 6. Strict Role Isolation Strategy

AidBridge uses physical isolation by role in both Java packages and resource source-sets.

### 6.1 Code Isolation

1. Role fragments: `ui/main/fragment/<role_name>/`
2. Role viewmodels: `ui/main/viewmodel/<role_name>/`
3. Role adapters: `ui/main/adapter/<role_name>/`
4. Shared role-agnostic logic should move to domain/usecase/repository/helper layers.

### 6.2 Resource Isolation

1. Role layouts: `app/src/main/res-role-<role>/layout/`
2. Role drawables: `app/src/main/res-role-<role>/drawable/`
3. Role strings: `app/src/main/res-role-<role>/values/strings_<role>.xml`
4. Role dimens: `app/src/main/res-role-<role>/values/dimens_<role>.xml`
5. Role colors: `app/src/main/res-role-<role>/values/colors_<role>.xml`

Do not place role-specific resources in global `res/values` unless they are truly shared across roles.

### 6.3 Navigation Isolation

1. Keep each role flow in its own role graph located at `app/src/main/res/navigation/nav_graph_<role>.xml`.
2. Avoid direct cross-role navigation dependencies.
3. In Fragments, use safe helpers from `BaseFragment` (`navigateSafely`, `navigateToDestinationSafely`, `popBackStackSafely`) to guard fast-click and stale-destination errors.

## 7. No-Hardcoding Resource Law

All UI-facing values must come from resources.

1. Strings: `@string/...` and `getString(...)`
2. Dimensions: `@dimen/...`
3. Colors: `@color/...`
4. Styles/themes: reuse theme styles before creating new one-off styles

Never hardcode:

1. User-facing text.
2. Hex colors.
3. Spacing/sizing values in XML or Java.

## 8. Auth Flow Reference (Concrete Example)

A representative production pattern in current codebase:

1. `LoginFragment` forwards email/password intent.
2. `LoginViewModel` validates inputs and triggers execution by `Transformations.switchMap`.
3. `LoginUseCase` orchestrates validation and repository contract call.
4. `AuthRepositoryImpl` parses wrapped API/local data and emits `NetworkResultWrapper<User>`.
5. Fragment renders states through `BaseFragment.resultObserver(...)`.

This pattern is the baseline for all future features.

## 9. Definition of Done for Frontend Changes

A feature is architecture-complete only when all checks pass:

1. Fragment is dumb (no business/validation/data-source logic).
2. ViewModel exposes LiveData and uses trigger-based `Transformations.switchMap` where applicable.
3. UseCase contains business orchestration.
4. Repository implementation maps `BaseResponse<T>` correctly and returns `NetworkResultWrapper<T>`.
5. One-time events are consumed safely with `hasBeenHandled()` pattern.
6. Role code/resources are placed in correct role locations.
7. Navigation uses safe helper methods from `BaseFragment`.
8. No hardcoded strings/colors/dimensions in feature code.
