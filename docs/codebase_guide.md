# AidBridge Android — Codebase Reading Guide

> A step-by-step guide for navigating and understanding the `drc-app/` codebase.  
> Read the files in the order listed below for the fastest mental model of the system.

---

## Table of Contents

1. [Project Layout at a Glance](#1-project-layout-at-a-glance)
2. [Step 1 — Configuration & Entry Points](#2-step-1--configuration--entry-points)
3. [Step 2 — Architecture Foundations (Base Classes)](#3-step-2--architecture-foundations-base-classes)
4. [Step 3 — Domain Layer (Pure Business Logic)](#4-step-3--domain-layer-pure-business-logic)
5. [Step 4 — Data Layer (Network & Storage)](#5-step-4--data-layer-network--storage)
6. [Step 5 — Dependency Injection Wiring](#6-step-5--dependency-injection-wiring)
7. [Step 6 — UI Layer: Auth Flow](#7-step-6--ui-layer-auth-flow)
8. [Step 7 — UI Layer: Post-Auth Shell](#8-step-7--ui-layer-post-auth-shell)
9. [Screen Flow Reference](#9-screen-flow-reference)
10. [Key Patterns & Conventions](#10-key-patterns--conventions)
11. [TODO Integration Checklist (Phase 3)](#11-todo-integration-checklist-phase-3)

---

## 1. Project Layout at a Glance

```
drc-app/app/src/main/
├── java/com/drc/aidbridge/
│   ├── AidBridgeApplication.java       ← App entry point (Hilt)
│   ├── data/
│   │   ├── local/                      ← Room DB (AppDatabase, entities)
│   │   ├── remote/                     ← Retrofit interfaces, interceptors, DTOs
│   │   └── repository/                 ← Concrete repository implementations
│   ├── di/                             ← Hilt modules (wiring all singletons)
│   ├── domain/
│   │   ├── model/                      ← Pure data classes (User, UserRole)
│   │   ├── repository/                 ← Repository interfaces
│   │   └── usecase/auth/               ← Business logic use cases
│   ├── ui/
│   │   ├── base/                       ← BaseActivity, BaseFragment, BaseViewModel
│   │   ├── splash/                     ← SplashActivity
│   │   ├── auth/                       ← Auth flow (7 Fragments + ViewModels)
│   │   ├── main/                       ← Post-auth shell (MainActivity, HomeFragment, MapFragment)
│   │   └── guide/                      ← UserGuideActivity
│   └── utils/                          ← Constants, TokenManager, PermissionHelper, NetworkUtils
└── res/
    ├── layout/                         ← All XML layouts (activity_* and fragment_*)
    ├── navigation/
    │   ├── auth_nav_graph.xml          ← Auth flow nav graph (start: GuestFragment)
    │   └── main_nav_graph.xml          ← Post-auth nav graph (start: HomeFragment)
    ├── menu/
    │   ├── bottom_nav_guest_menu.xml   ← Guest BottomNav (Cứu hộ / Bản đồ)
    │   └── bottom_nav_menu.xml         ← Main BottomNav (Trang chủ / Bản đồ)
    └── values/                         ← strings.xml, colors.xml, dimens.xml, themes.xml
```

---

## 2. Step 1 — Configuration & Entry Points

Start here to understand how the app initializes.

### `AidBridgeApplication.java`
- Annotated with `@HiltAndroidApp` — triggers Hilt's code generation and sets up the DI component tree.
- This is the first class Android instantiates. Every singleton injected anywhere in the app lives here.

### `AndroidManifest.xml`
- Declares 4 activities: `SplashActivity` (launcher), `AuthActivity`, `MainActivity`, `UserGuideActivity`.
- `SplashActivity` has the `LAUNCHER` intent filter — it is always the first screen.

### `utils/Constants.java`
- **Read this early.** Contains every magic number, SharedPreferences key, and endpoint path constant.
- Key constants: `BASE_URL`, `SPLASH_DELAY_MS`, `MOCK_OTP_SUCCESS`, `PASSWORD_MIN_LENGTH`, `OTP_COUNTDOWN_SEC`.

### `gradle/libs.versions.toml`
- Single source of truth for all library versions (Version Catalog pattern).
- Check here when adding a new dependency.

---

## 3. Step 2 — Architecture Foundations (Base Classes)

These three classes are extended by every screen in the app.

### `ui/base/BaseActivity.java`
- Extends `AppCompatActivity`.
- Provides `showLoading()`, `hideLoading()`, `showToast()`, `showError()` helpers.
- Subclasses must implement `getLoadingView()` to return their `ProgressBar` (or `null`).

### `ui/base/BaseFragment.java`
- Extends `Fragment`.
- Same helper methods as `BaseActivity` — avoids duplicating toast/loading code in every Fragment.

### `ui/base/BaseViewModel.java`
- Extends `ViewModel`.
- Holds a `MutableLiveData<Boolean> loading` that Fragments can observe to drive the loading state.
- All ViewModels extend this to share `setLoading()` / `isLoading`.

---

## 4. Step 3 — Domain Layer (Pure Business Logic)

The domain layer has **no Android dependencies** — it is plain Java.

### `domain/model/User.java`
- Core entity: `id`, `name`, `email`, `phone`, `role` (enum `UserRole`).

### `domain/model/UserRole.java`
- Enum: `VICTIM`, `VOLUNTEER`, `SPONSOR`, `STAFF`, `ADMIN`.
- Used for role-based UI behavior (card selection in Register, display label in Home).

### `domain/repository/AuthRepository.java`
- Interface that defines the contract: `login()`, `register()`, `verifyOtp()`, `resendOtp()`.
- The domain layer depends only on this interface, never on `AuthRepositoryImpl`.

### `domain/usecase/auth/LoginUseCase.java`
- Validates email format and password length **before** hitting the repository.
- Returns `LiveData<NetworkResultWrapper<User>>`.
- Calling `execute()` with invalid inputs returns an error `LiveData` immediately (no network call).

### `domain/usecase/auth/RegisterUseCase.java`
- Split into `validate()` + `execute()`.
- `validate()` checks name, email, phone format (`^0[0-9]{9,10}$`), password length, and role selection.
- `execute()` builds a `RegisterRequest` DTO and calls the repository.

### `domain/usecase/auth/VerifyOtpUseCase.java`
- Validates that the OTP is exactly 6 numeric digits.
- `execute()` delegates to `AuthRepository.verifyOtp()`.

---

## 5. Step 4 — Data Layer (Network & Storage)

### `data/remote/NetworkResultWrapper.java`
- **Read this before any ViewModel.** It is the universal async result type.
- Three concrete subclasses: `Success<T>` (holds `data`), `Error<T>` (holds `message` + `code`), `Loading<T>`.
- Factory methods: `NetworkResultWrapper.success(data)`, `.error(message)`, `.loading()`.

### `data/remote/AuthApiService.java`
- Retrofit interface declaring all auth endpoints (`/auth/login`, `/auth/register`, etc.).
- Currently stubbed — calls are replaced by mock data in `AuthRepositoryImpl`.

### `data/remote/AuthInterceptor.java`
- OkHttp interceptor that attaches `Authorization: Bearer <token>` to every request **except** `/auth/**` endpoints.

### `data/remote/TokenRefreshInterceptor.java`
- OkHttp interceptor that intercepts `401 Unauthorized` responses.
- Calls `POST /auth/refresh-token` with the stored refresh token, then retries the original request.
- On refresh failure, calls `tokenManager.clearAll()` to force logout.

### `utils/TokenManager.java`
- Wrapper around `EncryptedSharedPreferences`.
- Key methods:
  - `saveTokens(access, refresh)` — called after successful login/OTP.
  - `saveUserInfo(id, name, email, role)` — caches profile data locally.
  - `getAccessToken()` — used by `SplashActivity` and `AuthInterceptor`.
  - `clearAll()` — called on logout or token refresh failure.

### `data/repository/AuthRepositoryImpl.java`
- Implements `AuthRepository`. **All methods currently use MOCK data** (simulated 800–1500ms delay).
- Each method has a `// TODO API INTEGRATION` comment block showing exactly where to replace the mock with a real Retrofit `enqueue()` call.
- On mock login success: calls `tokenManager.saveTokens()` + `tokenManager.saveUserInfo()`.

### `data/local/AppDatabase.java`
- Room database definition. Currently only holds `AppSettingsEntity`.
- Will grow in Phase 3 to cache SOS requests, hub inventory, etc.

---

## 6. Step 5 — Dependency Injection Wiring

Read the `di/` package to understand how everything is connected.

### `di/AppModule.java`
- Provides `EncryptedSharedPreferences` (used by `TokenManager`).
- Provides `TokenManager` as a `@Singleton`.

### `di/NetworkModule.java`
- Provides `OkHttpClient` (with logging + auth interceptors), `Retrofit`, `AuthApiService`, `ApiService`.

### `di/RepositoryModule.java`
- Binds `AuthRepository` interface → `AuthRepositoryImpl` concrete class.
- This is the **only place** the binding is declared — all use cases receive `AuthRepository` (not the impl).

### `di/DatabaseModule.java`
- Provides `AppDatabase` and its DAOs.

---

## 7. Step 6 — UI Layer: Auth Flow

The auth flow lives entirely inside `AuthActivity` and its nav graph.

### `ui/auth/AuthActivity.java`
- Thin container — just inflates `activity_auth.xml` which holds the `NavHostFragment`.
- All navigation is handled by `auth_nav_graph.xml`.

### `res/navigation/auth_nav_graph.xml`
- **Start destination:** `GuestFragment`.
- Declares all navigation actions between auth fragments.
- Critical action: `action_loginFragment_to_mainActivity` — uses `popUpTo="@id/auth_nav_graph"` + `popUpToInclusive="true"` to clear the entire auth back stack on login success.

### `ui/auth/GuestFragment.java`
- The first screen every unauthenticated user sees.
- Has its own `BottomNavigationView` with 2 tabs managed by layout visibility toggling:
  - `nav_rescue` → shows `layout_rescue` (SOS button, Login/Register buttons, GPS badge).
  - `nav_map_view` → shows `layout_map_view` (Phase 3 Google Maps placeholder).
- Info icon (ⓘ) opens `UserGuideActivity` via explicit `Intent`.

### `ui/auth/LoginFragment.java` + `LoginViewModel.java`
- Fragment handles validation display + navigation decisions.
- ViewModel calls `LoginUseCase.execute()` and exposes `loginResult: LiveData<NetworkResultWrapper<User>>`.
- On success: calls `navigateToMain()` → `NavController.navigate(action_loginFragment_to_mainActivity)` + `requireActivity().finish()`.

### `ui/auth/RegisterFragment.java` + `RegisterViewModel.java`
- Role selection updates `selectedRole: LiveData<UserRole>` which drives card border highlights.
- On success: navigates to `OtpFragment` passing `email` as a Safe Args argument.

### `ui/auth/OtpFragment.java` + `OtpViewModel.java`
- 6 `EditText` boxes with:
  - `TextWatcher` for auto-advance on digit entry.
  - `OnKeyListener` for back-navigation on DELETE from empty box.
- `OtpViewModel` starts a `CountDownTimer` (59s) on init; re-enables a "Gửi lại" resend button at 0.
- On OTP success: shows a dialog → user taps Continue → `navigateToLogin()` (pops to `GuestFragment` exclusive, then navigates to `LoginFragment`).
- Mock OTP: `Constants.MOCK_OTP_SUCCESS = "111111"`.

### Forgot Password Flow (3 fragments)
| Fragment | Screen | Key Logic |
|----------|--------|-----------|
| `ForgotEmailFragment` | Enter email | `POST /auth/forgot-password` (mocked) |
| `ForgotOtpFragment` | Enter OTP | Receives `email` via Safe Args. `POST /auth/verify-reset-otp` (mocked) |
| `ForgotNewPasswordFragment` | Set new password | Client-side password match. `POST /auth/reset-password` (mocked) → `popBackStack` to `LoginFragment` |

---

## 8. Step 7 — UI Layer: Post-Auth Shell

### `ui/splash/SplashActivity.java`
- Shows logo + app name + tagline with a staggered fade-in animation.
- After `Constants.SPLASH_DELAY_MS` (2000ms), checks:
  ```java
  if (tokenManager.getAccessToken() != null && !tokenManager.getAccessToken().isEmpty()) {
      // → MainActivity
  } else {
      // → AuthActivity
  }
  ```
- In Phase 2, `getAccessToken()` always returns `null` (no real login has been completed).

### `ui/main/MainActivity.java`
- Post-auth shell. Hosts `BottomNavigationView` with 2 tabs:
  - `nav_home` → `HomeFragment` (default on launch)
  - `nav_map` → `MapFragment`
- Uses `FragmentManager.replace()` (not NavController) for tab switching.
- Config change safety: `savedInstanceState != null` guard prevents recreating the Fragment on rotation.

### `ui/main/HomeFragment.java`
- Reads user info directly from `TokenManager` (pre-cached during login).
- Displays name, email, and a Vietnamese-label role badge.
- Logout: `tokenManager.clearAll()` → starts `AuthActivity` with `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK` → `finish()`.

### `ui/main/MapFragment.java`
- Phase 3 placeholder with a dimmed map icon and explainer text.

### `ui/guide/UserGuideActivity.java`
- Static informational screen. Uses `CollapsingToolbar` with 4 expandable sections (SOS, Map, Auth, Roles).
- No ViewModels — purely presentational.

---

## 9. Screen Flow Reference

### Flow A — First Launch (unauthenticated)
```
SplashActivity (2s)
  └─ no token → AuthActivity
       └─ GuestFragment [start destination]
            ├─ btn ĐĂNG NHẬP → LoginFragment
            │    └─ login success → MainActivity (auth stack cleared)
            │         └─ HomeFragment (default tab)
            └─ link Đăng ký → RegisterFragment
                 └─ register success → OtpFragment
                      └─ OTP "111111" → (pop to GuestFragment) → LoginFragment
```

### Flow B — Returning user (token present, Phase 3+)
```
SplashActivity (2s)
  └─ token found → MainActivity
       └─ HomeFragment (default tab)
```

### Flow C — Forgot Password
```
LoginFragment
  └─ "Quên mật khẩu?" → ForgotEmailFragment
       └─ email submitted → ForgotOtpFragment
            └─ OTP correct → ForgotNewPasswordFragment
                 └─ save → popBackStack → LoginFragment
```

### Flow D — Logout
```
HomeFragment (inside MainActivity)
  └─ btn ĐĂNG XUẤT → tokenManager.clearAll()
       └─ AuthActivity (CLEAR_TASK → all previous screens destroyed)
            └─ GuestFragment [start destination]
```

### Flow E — Help / User Guide
```
GuestFragment
  └─ ⓘ icon → UserGuideActivity (independent Activity)
       └─ Back → GuestFragment
```

---

## 10. Key Patterns & Conventions

### MVVM Data Flow
```
Fragment/Activity
      │  observe LiveData
      ▼
  ViewModel
      │  calls UseCase
      ▼
  UseCase (validates inputs)
      │  calls Repository interface
      ▼
  Repository Implementation
      │  Retrofit enqueue() OR Mock
      ▼
  NetworkResultWrapper<T>  →  posted back via LiveData
```

### `NetworkResultWrapper<T>` states in Fragments
Every ViewModel result LiveData follows this three-state pattern:
```java
viewModel.getSomeResult().observe(getViewLifecycleOwner(), result -> {
    if (result instanceof NetworkResultWrapper.Loading) {
        // show spinner, disable buttons
    } else if (result instanceof NetworkResultWrapper.Success) {
        // navigate or update UI
    } else if (result instanceof NetworkResultWrapper.Error) {
        // show error message
    }
});
```

### Back Stack Management
- `popUpTo` + `popUpToInclusive="true"` in nav graph actions → wipes entire back stack.
- `requireActivity().finish()` after navigation → closes the Activity container.
- Used on: Login success (clears all auth fragments + AuthActivity).

### Token Lifecycle
```
AuthRepositoryImpl.login() → saveTokens() + saveUserInfo()
SplashActivity              → getAccessToken() (routing decision)
AuthInterceptor             → getAccessToken() (HTTP header)
TokenRefreshInterceptor     → getRefreshToken() + saveTokens() (auto-refresh)
HomeFragment / logout       → clearAll()
```

### Hilt Injection Points
- Activities: `@AndroidEntryPoint` on the class.
- Fragments: `@AndroidEntryPoint` on the class (host Activity must also be annotated).
- ViewModels: `@HiltViewModel` + `@Inject` constructor.
- Other classes: `@Inject` constructor (e.g., `TokenManager`, `AuthRepositoryImpl`).

---

## 11. TODO Integration Checklist (Phase 3)

When replacing mocks with real API calls, follow this order:

1. **`Constants.java`** — Update `BASE_URL` to production server URL.
2. **`AuthApiService.java`** — Uncomment/verify all Retrofit interface methods.
3. **`AuthRepositoryImpl.java`** — Replace each `// ======== MOCK DATA ========` block with the real `authApiService.xxx().enqueue(...)` call. Each method has a `TODO API INTEGRATION` comment showing the exact replacement.
4. **`SplashActivity.java`** — The token check (`getAccessToken() != null`) already works; no changes needed once real login saves tokens.
5. **`TokenRefreshInterceptor.java`** — Verify the refresh endpoint path matches the real server.
6. **`NetworkModule.java`** — Set logging interceptor to `Level.NONE` for release builds.
7. Remove `Constants.MOCK_OTP_SUCCESS` usage and delete the constant.
