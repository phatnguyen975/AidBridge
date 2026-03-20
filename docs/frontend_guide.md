# AidBridge - Frontend Guide

## 1. Project Overview & Tech Stack

AidBridge Android frontend is built as a Java/XML app with MVVM + Clean Architecture, role-based navigation, and Hilt dependency injection.

### Core Stack

- Java 17 (no Kotlin)
- XML Layout + ViewBinding (no Jetpack Compose)
- AndroidX ViewModel + LiveData
- Jetpack Navigation (role-based nav graphs)
- Dagger Hilt (DI)
- Retrofit + OkHttp + Gson
- Room (local storage)
- Firebase Messaging (FCM)
- RxJava3 + STOMP client (realtime channel foundation)
- Glide (image loading)
- CameraX + ML Kit Barcode (QR workflows)
- Google Maps + Fused Location (already in dependencies, currently feature-phased)

### Build and Module Notes

- Module: `app`
- Build: Gradle 8.x, Android Gradle Plugin with Java 17 compile options
- Resource source-sets are split by feature:
  - `res`, `res-core`, `res-auth`, `res-guest`, `res-common-ui`, `res-role-*`

## 2. Folder Structure Explanation

Below is the current `app/src` tree (summarized at package level, based on current codebase).

```text
app/src/
├── androidTest/                                      # Instrumented tests
├── test/                                             # Local JVM tests
└── main/
    ├── AndroidManifest.xml                           # App manifest, activity declarations, permissions
    ├── java/com/drc/aidbridge/
    │   ├── AidBridgeApplication.java                 # @HiltAndroidApp entry point
    │   ├── data/                                     # Data layer implementation boundary
    │   │   ├── local/                                # Local data access root
    │   │   │   ├── dao/                              # Room DAO interfaces
    │   │   │   └── entity/                           # Room entity models
    │   │   ├── mapper/                               # DTO <-> Domain mappers
    │   │   ├── remote/                               # Network models and transport utilities
    │   │   │   ├── api/                              # Retrofit service interfaces
    │   │   │   ├── dto/request/                      # Request payload DTOs
    │   │   │   ├── dto/response/                     # Response payload DTOs
    │   │   │   └── interceptor/                      # OkHttp interceptors (auth/refresh)
    │   │   └── repository/                           # Repository implementations (data orchestration)
    │   ├── di/                                       # Hilt modules (provide/bind dependencies)
    │   ├── domain/                                   # Domain layer (framework-agnostic business contracts)
    │   │   ├── enums/                                # Domain enums (example: roles)
    │   │   ├── model/                                # Domain models (pure POJO)
    │   │   ├── repository/                           # Repository interfaces
    │   │   └── usecase/                              # Use cases (business rules)
    │   │       ├── auth/                             # Auth-specific use cases
    │   │       └── validation/                       # Input validation contracts and results
    │   ├── ui/                                       # Presentation layer root
    │   │   ├── auth/                                 # Authentication screens and auth activity host
    │   │   │   ├── fragment/                         # Auth fragments (login/register/otp/forgot/guest shell)
    │   │   │   └── viewmodel/                        # Auth viewmodels
    │   │   ├── base/                                 # BaseActivity/BaseFragment/BaseViewModel abstractions
    │   │   ├── common/                               # Reusable presentation controllers/widgets (example: OTP input)
    │   │   ├── main/                                 # Post-auth shell activity and role home fragments
    │   │   │   └── fragment/                         # Role-specific home modules (admin/staff/victim/volunteer/sponsor)
    │   │   ├── map/                                  # Map fragment entry points per role
    │   │   └── splash/                               # Launch and session routing
    │   └── utils/                                    # Cross-cutting helpers/constants/token manager
    ├── res/                                          # Base resources (menu, navigation, values, launcher, animations)
    ├── res-auth/                                     # Auth layouts and auth drawables
    ├── res-guest/                                    # Guest layouts and guest UI assets
    ├── res-core/                                     # App-level core layouts (splash/main activity)
    ├── res-common-ui/                                # Shared drawable assets across modules
    ├── res-role-victim/                              # Victim-specific layouts
    ├── res-role-volunteer/                           # Volunteer-specific layouts
    ├── res-role-sponsor/                             # Sponsor-specific layouts
    ├── res-role-staff/                               # Staff-specific layouts
    └── res-role-admin/                               # Admin-specific layouts
```

## 3. The MVVM + Clean Architecture Rules

### Canonical Dependency Direction

`Fragment -> ViewModel -> UseCase -> Repository Interface -> Repository Impl -> API/Local`

### Layer Responsibilities

- Fragment:
  - Render UI and forward user intents only.
  - Observe `LiveData` from ViewModel.
  - Never contain business validation, mapping rules, or networking logic.

- ViewModel:
  - Hold UI state as `LiveData`.
  - Trigger use cases and expose result streams.
  - Convert one-shot results into event-safe handling pattern.

- UseCase:
  - Own business rules and validation orchestration.
  - Coordinate repository calls.
  - Remain independent from Android UI classes.

- Repository:
  - Abstract data source choice (remote/local/cache).
  - Normalize response/error into `NetworkResultWrapper`.
  - Persist session/token side effects through dedicated managers.

### Strict Rule Set for Team

- Fragments must be dumb:
  - No validation logic besides basic UI field extraction.
  - No Retrofit, Room, or token access directly in Fragment.

- ViewModels must use LiveData:
  - UI observes immutable `LiveData` outputs.
  - Trigger patterns should use `MutableLiveData` + `Transformations.switchMap` for execution.

- Event wrappers are mandatory for one-time actions:
  - Use the existing `NetworkResultWrapper.hasBeenHandled` pattern with `BaseFragment.resultObserver()`.
  - Never trigger navigation or toast repeatedly after configuration changes.

- Navigation safety is mandatory:
  - Use base safe navigation helpers to avoid stale-destination crashes.
  - Respect debounce policy before navigating.

### Standardized Network Parsing (DTO vs Wrapper)

To handle the Backend's standardized JSON response format, we strictly separate the parsing layer from the UI state layer:

1. **`BaseResponse<T>` (Data Layer):** - All Retrofit API calls MUST return `Call<BaseResponse<T>>`.
   - This wrapper handles the outer JSON layer (`success`, `message`, `data`).
   - It is strictly used for Gson parsing and must NOT leak into the Domain or UI layers.

2. **`NetworkResultWrapper<T>` (UI/Presentation Layer):**
   - The UI only understands `NetworkResultWrapper` (`LOADING`, `SUCCESS`, `ERROR`).
   - The Repository is responsible for unwrapping `BaseResponse`, mapping the inner DTO to a Domain Model, and packing it into `NetworkResultWrapper` to post to the ViewModel.

## 4. Detailed Flow Walkthrough (The Authentication Flow)

Golden path: `LoginFragment -> LoginViewModel -> LoginUseCase -> AuthInputValidator -> AuthRepositoryImpl`

### Step-by-step

1. Fragment collects input and sends intent.

- `LoginFragment` reads email/password from EditText fields.
- On login click, it calls `viewModel.login(email, password)`.
- Fragment itself does not call repository/API.

2. Fragment observes ViewModel state.

- `validationError` LiveData:
  - Fragment clears old errors, then binds field-level error to `TextInputLayout`.
- `loginResult` LiveData:
  - Fragment uses shared `resultObserver(...)` from `BaseFragment`.
  - Loading state disables login button and toggles progress.
  - Success triggers navigation to `MainActivity` with task-clearing flags.
  - Error shows user message.

3. ViewModel gates execution via `Transformations.switchMap`.

- `LoginViewModel` defines:
  - `loginTrigger: MutableLiveData<LoginParams>`
  - `loginResult = Transformations.switchMap(loginTrigger, params -> loginUseCase.execute(...))`
- This ensures the use case executes only when trigger changes after passing validation.

4. UseCase delegates validation to `AuthInputValidator`.

- `LoginUseCase.validate(email, password)` calls:
  - `requireValidEmail(...)`
  - `requirePassword(...)`
- If validation fails, ViewModel emits the invalid result and does not set trigger.
- If valid, ViewModel sets trigger and starts execution.

5. UseCase delegates data work to repository.

- `LoginUseCase.execute(...)` normalizes email and creates `LoginRequest`.
- It calls `authRepository.login(request)`.

6. Repository handles API/session side effects.

- `AuthRepositoryImpl.login(...)` posts `Loading` first.
- Current implementation has API code commented and uses mock success data for navigation/testing.
- On success path it saves user session metadata using `TokenManager.saveUserInfo(...)`.
- Live backend mode (when uncommented) persists tokens via `persistAuthData(...)` then maps `UserDto -> User` and returns `Success`.

7. Event-safe consumption.

- `NetworkResultWrapper` contains `hasBeenHandled`.
- `BaseFragment.resultObserver()` checks and marks handled on Success/Error.
- This prevents duplicate toasts/navigation during lifecycle changes.

### Why this flow is correct

- Validation is centralized in domain, not duplicated in UI.
- Fragment stays presentation-only.
- UseCase remains business-centric and testable.
- Repository is the only place that knows transport/local persistence details.

## 5. File Placement Cheat Sheet (Where to put what)

Use this checklist whenever adding a new feature screen.

### A. New Screen UI

- `Fragment.java`:
  - Role-based screens: place in `app/src/main/java/com/drc/aidbridge/ui/main/fragment/<role_name>/`
  - Non-role feature screens (auth/guest/common): place in `app/src/main/java/com/drc/aidbridge/ui/<feature>/fragment/`
- `fragment_<name>.xml`:
  - Place in feature resource set:
    - Auth screens: `app/src/main/res-auth/layout/`
    - Guest screens: `app/src/main/res-guest/layout/`
    - Role screens: `app/src/main/res-role-<role>/layout/`
    - Core shell screens: `app/src/main/res-core/layout/`

### B. Screen State and Logic

- `ViewModel.java`:
  - Role-based screens: place in `app/src/main/java/com/drc/aidbridge/ui/main/viewmodel/<role_name>/`
  - Non-role feature screens (auth/guest/common): place in `app/src/main/java/com/drc/aidbridge/ui/<feature>/viewmodel/`
- `UseCase.java`:
  - Place in `app/src/main/java/com/drc/aidbridge/domain/usecase/<feature>/`
- Input validators:
  - Reuse or extend `domain/usecase/validation/`

### C. Data Layer

- Repository interface:
  - Place in `app/src/main/java/com/drc/aidbridge/domain/repository/`
- Repository implementation:
  - Place in `app/src/main/java/com/drc/aidbridge/data/repository/`
- API interface (Retrofit):
  - Place in `app/src/main/java/com/drc/aidbridge/data/remote/api/`
- DTOs:
  - Request: `data/remote/dto/request/`
  - Response: `data/remote/dto/response/`
- Local persistence:
  - Entity: `data/local/entity/`
  - DAO: `data/local/dao/`

### D. Navigation

- Add new destination/action in the graph where flow belongs:
  - Auth/guest flow: `app/src/main/res/navigation/nav_graph_guest.xml`
  - Guest tab host: `app/src/main/res/navigation/nav_graph_guest_tabs.xml`
  - Role flows: corresponding `nav_graph_<role>.xml` in `app/src/main/res/navigation/`
- In Fragment, call safe navigation helper from `BaseFragment`.

### E. Theme, Colors, Icons, and Shared UI Assets

- Colors, strings, dimensions, theme:
  - `app/src/main/res/values/`
- Night theme overrides:
  - `app/src/main/res/values-night/`
- Shared icons/drawables:
  - `app/src/main/res-common-ui/drawable/`
- Feature-specific icons:
  - Auth: `app/src/main/res-auth/drawable/`
  - Guest: `app/src/main/res-guest/drawable/`
  - Role-specific assets in corresponding `res-role-*` folder when role-bound.

### F. Dependency Injection Registration

- If adding new repository implementation:
  - Bind interface to implementation in `di/RepositoryModule`.
- If adding new service/provider:
  - Add provider methods in `di/ApiModule`, `di/AppModule`, `di/DatabaseModule`, or `di/NetworkModule` by responsibility.

### G. Definition of Ready Before PR

- Fragment has no business logic.
- ViewModel exposes only `LiveData` to UI.
- UseCase owns validation/business rules.
- Repository returns `NetworkResultWrapper` and handles errors consistently.
- Navigation action exists and is reachable in correct nav graph.
- Layout and drawables are placed in correct feature source-set.
