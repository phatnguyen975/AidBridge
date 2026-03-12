# AidBridge Android — Frontend Summary

> **Phase:** 2 (Auth UI + Navigation Refactor)  
> **Stack:** Java 17 · XML Layouts · MVVM + Clean Architecture · Hilt DI · Navigation Component 2.9.0

---

## 1. Navigation Flow

> Source: [diagrams/01_navigation_flow.mmd](diagrams/01_navigation_flow.mmd)

```mermaid
%%{init: {"flowchart": {"htmlLabels": false}}}%%
```

<!-- Render with: npx @mermaid-js/mermaid-cli -i docs/diagrams/01_navigation_flow.mmd -->

![Navigation Flow](diagrams/01_navigation_flow.mmd)

---

## 2. Screen Structure

> Source: [diagrams/02_screen_structure.mmd](diagrams/02_screen_structure.mmd)

![Screen Structure](diagrams/02_screen_structure.mmd)

---

## 3. Class Diagram

> Source: [diagrams/03_class_diagram.mmd](diagrams/03_class_diagram.mmd)

![Class Diagram](diagrams/03_class_diagram.mmd)

---

## 4. API Integration Reference

All Retrofit2 call blocks are **commented out** in Phase 2. To activate them:

1. Remove the `// ======== MOCK DATA ========` block
2. Uncomment the `// TODO API INTEGRATION` block below it

| Screen            | Endpoint                 | Method | Key Fields                                   |
| ----------------- | ------------------------ | ------ | -------------------------------------------- |
| Login             | `/auth/login`            | POST   | `email`, `password`                          |
| Register          | `/auth/register`         | POST   | `name`, `phone`, `email`, `password`, `role` |
| OTP Verify        | `/auth/verify-otp`       | POST   | `email`, `otp`                               |
| OTP Resend        | `/auth/resend-otp`       | POST   | `email`                                      |
| Forgot PW — Email | `/auth/forgot-password`  | POST   | `email`                                      |
| Forgot PW — OTP   | `/auth/verify-reset-otp` | POST   | `email`, `otp`                               |
| Forgot PW — Reset | `/auth/reset-password`   | POST   | `email`, `otp`, `newPassword`                |

All requests require:

```
Content-Type: application/json
```

Authenticated requests (Phase 3+) also require:

```
Authorization: Bearer <access_token>
```

---

## 5. Design Tokens

| Token            | Value     | Usage                             |
| ---------------- | --------- | --------------------------------- |
| `bg_primary`     | `#0F172A` | Activity/Fragment root background |
| `bg_surface`     | `#1E293B` | Cards, input fields               |
| `color_primary`  | `#288DFA` | Buttons, links, tints             |
| `sos_red`        | `#EF4444` | SOS button, emergency indicators  |
| `safe_green`     | `#34D399` | Success messages, GPS active      |
| `text_primary`   | `#F1F5F9` | Main body text                    |
| `text_secondary` | `#94A3B8` | Hints, subtitles, icon tints      |

---

## 6. Responsive Design Rules

- All layouts use `ConstraintLayout` with **Guideline** margins (typically 5–6% horizontal)
- Text sizes use **sp** units; dimensions use **dp** units
- No hardcoded pixel values
- Role card icons: `ImageView` with `app:tint` (replaces emoji TextViews)
- CheckBox uses `@color/cb_tint` state list (blue when checked, gray when unchecked)

---

## 7. Activity & Fragment Inventory (Phase 2)

### Activities
| Class | Purpose |
|-------|---------|
| `SplashActivity` | Launch screen. Checks `TokenManager.getAccessToken()` after 2s delay. Routes to `MainActivity` (token present) or `AuthActivity` (no token). |
| `AuthActivity` | Thin NavHostFragment container for the entire auth flow. |
| `MainActivity` | Post-auth shell. Hosts `BottomNavigationView` with 2 tabs: `HomeFragment` (nav_home) and `MapFragment` (nav_map). |
| `UserGuideActivity` | Static help content with `CollapsingToolbar`. Opened from `GuestFragment` info icon. |

### Auth Fragments (inside `AuthActivity`)
| Class | Purpose |
|-------|---------|
| `GuestFragment` | Start destination. SOS button + bottom tab for map. Entry points to Login/Register. |
| `LoginFragment` | Email + password auth. On success → `action_loginFragment_to_mainActivity` (popUpTo inclusive). |
| `RegisterFragment` | Registration form with role selection cards (VICTIM / VOLUNTEER / SPONSOR). |
| `OtpFragment` | 6-box OTP with auto-advance, countdown timer, resend. On success → popUpTo GuestFragment then → LoginFragment. |
| `ForgotEmailFragment` | Step 1 of forgot-password flow. |
| `ForgotOtpFragment` | Step 2 — verifies reset OTP. Receives `email` as Safe Args argument. |
| `ForgotNewPasswordFragment` | Step 3 — sets new password with confirmation match. |

### Main Fragments (inside `MainActivity`)
| Class | Purpose |
|-------|---------|
| `HomeFragment` | Reads cached user info from `TokenManager`. Displays name, email, role. Logout button. |
| `MapFragment` | Placeholder for Phase 3 Google Maps integration. |

---

## 8. Mock Values (Phase 2)

| Mock             | Value               | Source                       |
| ---------------- | ------------------- | ---------------------------- |
| OTP success code | `111111`            | `Constants.MOCK_OTP_SUCCESS` |
| User ID          | `mock-user-id-001`  | `AuthRepositoryImpl`         |
| Access token     | `mock_access_token` | `AuthRepositoryImpl`         |
| Network delay    | 800–1500ms          | `Handler.postDelayed()`      |
| isLoggedIn       | always `false`      | `SplashActivity` — token check against empty prefs |
