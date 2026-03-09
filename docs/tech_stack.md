# AidBridge - Tech Stack

This project is a Monorepo containing an Android mobile app (Frontend) and a Spring Boot application (Backend). The system focuses on disaster relief coordination using real-time geospatial data.

## 1. Frontend (Android Mobile App — `drc-app/`)

| Constraint         | Value                                                                   |
| ------------------ | ----------------------------------------------------------------------- |
| Language           | **Java 17** (Kotlin is strictly forbidden)                              |
| UI Toolkit         | **Pure XML Layouts** (Jetpack Compose is strictly forbidden)            |
| Architecture       | **MVVM + Clean Architecture** (ViewModel, LiveData, Repository pattern) |
| Min SDK            | 29 (Android 10)                                                         |
| Target/Compile SDK | 36 (Android 16)                                                         |
| Build Tool         | Gradle 8.13+ with Version Catalog (`libs.versions.toml`)                |

### 1.1 Architecture & UI

| Library                                          | Version  | Purpose                                                        |
| ------------------------------------------------ | -------- | -------------------------------------------------------------- |
| `androidx.appcompat:appcompat`                   | `1.7.1`  | Backward-compatible Activity/Fragment base classes             |
| `com.google.android.material:material`           | `1.13.0` | Material Design 3 widgets (Button, TextField, BottomNav, etc.) |
| `androidx.constraintlayout:constraintlayout`     | `2.2.1`  | Flexible XML constraint-based layout system                    |
| `androidx.recyclerview:recyclerview`             | `2.9.0`  | Efficient scrollable list/grid for history and request lists   |
| `androidx.viewpager2:viewpager2`                 | `1.1.0`  | Swipeable pages for multi-step SOS/registration wizards        |
| `androidx.swiperefreshlayout:swiperefreshlayout` | `1.2.0`  | Pull-to-refresh gesture on list screens                        |
| `androidx.lifecycle:lifecycle-viewmodel`         | `2.9.0`  | ViewModel — survives config changes; holds UI state            |
| `androidx.lifecycle:lifecycle-livedata`          | `2.9.0`  | LiveData — lifecycle-aware observable for ViewModel→UI binding |
| `androidx.navigation:navigation-fragment`        | `2.9.0`  | Fragment back-stack and in-app routing (NavController)         |
| `androidx.navigation:navigation-ui`              | `2.9.0`  | Navigation Component integration with Toolbar and BottomNav    |

### 1.2 Dependency Injection

| Library                                   | Version | Purpose                                                   |
| ----------------------------------------- | ------- | --------------------------------------------------------- |
| `com.google.dagger:hilt-android`          | `2.56`  | DI container integrated with Android lifecycle components |
| `com.google.dagger:hilt-android-compiler` | `2.56`  | Annotation processor — generates DI component classes     |

### 1.3 Networking

| Library                                    | Version  | Purpose                                                       |
| ------------------------------------------ | -------- | ------------------------------------------------------------- |
| `com.squareup.retrofit2:retrofit`          | `2.11.0` | Type-safe HTTP client; maps REST endpoints to Java interfaces |
| `com.squareup.retrofit2:converter-gson`    | `2.11.0` | Gson converter for automatic JSON ↔ Java object mapping       |
| `com.squareup.okhttp3:okhttp`              | `4.12.0` | Underlying HTTP client used by Retrofit                       |
| `com.squareup.okhttp3:logging-interceptor` | `4.12.0` | Logs full HTTP request/response for debug builds              |

### 1.4 Real-time Communication (WebSocket / STOMP)

| Library                                        | Version | Purpose                                                             |
| ---------------------------------------------- | ------- | ------------------------------------------------------------------- |
| `com.github.NaikSoftware:stompprotocolandroid` | `1.6.6` | STOMP protocol over WebSocket for chat, live tracking, and dispatch |
| `io.reactivex.rxjava3:rxjava`                  | `3.1.9` | Reactive streams library (required by StompProtocolAndroid)         |
| `io.reactivex.rxjava3:rxandroid`               | `3.0.2` | Android-aware schedulers (mainThread, io) for RxJava3               |

> **Repository note:** `StompProtocolAndroid` is hosted on JitPack. The JitPack repository is declared in `settings.gradle`.

### 1.5 Push Notifications (Firebase FCM)

| Library                                  | Version            | Purpose                                                                  |
| ---------------------------------------- | ------------------ | ------------------------------------------------------------------------ |
| `com.google.firebase:firebase-bom`       | `33.9.0`           | BOM — ensures all Firebase libraries use a consistent version set        |
| `com.google.firebase:firebase-messaging` | _(managed by BOM)_ | Receives task dispatch, status updates, and broadcast alerts from server |

### 1.6 Maps & Location Services

| Library                                         | Version  | Purpose                                                                               |
| ----------------------------------------------- | -------- | ------------------------------------------------------------------------------------- |
| `com.google.android.gms:play-services-maps`     | `19.1.0` | Google Maps SDK — renders map, custom markers, polylines, and heatmap tiles           |
| `com.google.android.gms:play-services-location` | `21.2.0` | Fused Location Provider — battery-efficient GPS for live tracking and SOS coordinates |

### 1.7 QR Code Scanning (CameraX + ML Kit)

| Library                             | Version  | Purpose                                                                       |
| ----------------------------------- | -------- | ----------------------------------------------------------------------------- |
| `com.google.mlkit:barcode-scanning` | `17.3.0` | Decodes QR codes for Staff check-in (donation) and check-out (dispatch) flows |
| `androidx.camera:camera-core`       | `1.4.1`  | Camera lifecycle abstraction layer                                            |
| `androidx.camera:camera-camera2`    | `1.4.1`  | Hardware camera implementation backed by Camera2 API                          |
| `androidx.camera:camera-lifecycle`  | `1.4.1`  | Binds camera stream to Activity/Fragment lifecycle automatically              |
| `androidx.camera:camera-view`       | `1.4.1`  | Provides `PreviewView` widget for embedding camera preview in XML layouts     |

### 1.8 Local Database (Room — Offline Support)

| Library                       | Version | Purpose                                                                   |
| ----------------------------- | ------- | ------------------------------------------------------------------------- |
| `androidx.room:room-runtime`  | `2.7.0` | SQLite abstraction for offline storage (7-day request history, map cache) |
| `androidx.room:room-compiler` | `2.7.0` | Annotation processor — generates type-safe DAO implementations            |

### 1.9 Image Loading

| Library                           | Version  | Purpose                                                                       |
| --------------------------------- | -------- | ----------------------------------------------------------------------------- |
| `com.github.bumptech.glide:glide` | `4.16.0` | Disk/memory-caching image loader for SOS photos, avatars, and donation images |

### 1.10 Secure Storage

| Library                             | Version         | Purpose                                                                       |
| ----------------------------------- | --------------- | ----------------------------------------------------------------------------- |
| `androidx.security:security-crypto` | `1.1.0-alpha06` | `EncryptedSharedPreferences` for AES-256 storage of JWT access/refresh tokens |

## 2. Backend (Spring Boot — `spring-backend/`)

| Constraint | Value                                           |
| ---------- | ----------------------------------------------- |
| Language   | **Java 25**                                     |
| Framework  | **Spring Boot 4.0.3**                           |
| Build Tool | Gradle with Spring Dependency Management plugin |
| Database   | **Supabase (PostgreSQL + PostGIS extension)**   |

### 2.1 Core Spring Boot Starters

| Dependency                       | Purpose                                                                            |
| -------------------------------- | ---------------------------------------------------------------------------------- |
| `spring-boot-starter-web`        | Builds the REST API layer (Controllers, ExceptionHandlers, Filters via Spring MVC) |
| `spring-boot-starter-security`   | Authentication, authorization, CSRF protection, and Security filter chain          |
| `spring-boot-starter-validation` | Jakarta Bean Validation (`@Valid`, `@NotNull`, `@Size`) applied to DTOs            |
| `spring-boot-starter-websocket`  | Spring WebSocket + STOMP broker for real-time chat and live tracking               |
| `spring-boot-starter-actuator`   | Exposes `/actuator/health` and `/actuator/metrics` endpoints for monitoring        |

### 2.2 Data & Persistence

| Dependency                            | Version     | Purpose                                                                                                |
| ------------------------------------- | ----------- | ------------------------------------------------------------------------------------------------------ |
| `spring-boot-starter-data-jpa`        | _(managed)_ | Spring Data JPA — repository abstraction over Hibernate ORM                                            |
| `org.postgresql:postgresql`           | _(managed)_ | PostgreSQL JDBC driver — connects to Supabase                                                          |
| `org.hibernate.orm:hibernate-spatial` | _(managed)_ | Adds spatial type support (`Point`, `Polygon`) and spatial HQL functions (`ST_DWithin`, `ST_Distance`) |
| `org.locationtech.jts:jts-core`       | `1.20.0`    | JTS Topology Suite — Java geometry types used by Hibernate Spatial entities                            |

### 2.3 Cache & Session Store

| Dependency                       | Purpose                                                                                           |
| -------------------------------- | ------------------------------------------------------------------------------------------------- |
| `spring-boot-starter-data-redis` | Caches OTP codes, JWT blacklist, dispatch state machine, and pub/sub for multi-instance WebSocket |

### 2.4 Email Service

| Dependency                 | Purpose                                                                   |
| -------------------------- | ------------------------------------------------------------------------- |
| `spring-boot-starter-mail` | Sends OTP verification emails during user registration and password reset |

### 2.5 Push Notifications

| Dependency                           | Version | Purpose                                                                                                                                                             |
| ------------------------------------ | ------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `com.google.firebase:firebase-admin` | `9.4.3` | Firebase Admin SDK — sends FCM push notifications to Android app clients; used for task dispatch (30-second window), status updates, and broadcast emergency alerts |

### 2.6 JWT Authentication

| Dependency                     | Version  | Purpose                                                                  |
| ------------------------------ | -------- | ------------------------------------------------------------------------ |
| `io.jsonwebtoken:jjwt-api`     | `0.13.0` | Public API for building and parsing JSON Web Tokens                      |
| `io.jsonwebtoken:jjwt-impl`    | `0.13.0` | Runtime engine for JJWT (runtime-only)                                   |
| `io.jsonwebtoken:jjwt-jackson` | `0.13.0` | Jackson-based JSON serializer/deserializer for JWT claims (runtime-only) |

### 2.7 DTO Mapping

| Dependency                          | Version | Purpose                                                                 |
| ----------------------------------- | ------- | ----------------------------------------------------------------------- |
| `org.mapstruct:mapstruct`           | `1.6.3` | Compile-time code generator for Entity ↔ DTO mapping (zero reflection)  |
| `org.mapstruct:mapstruct-processor` | `1.6.3` | Annotation processor — generates mapper implementations at compile time |

> **Processor order:** Lombok annotationProcessor must be declared **before** MapStruct processor in `build.gradle` so that Lombok-generated getters/setters are visible to MapStruct.

### 2.8 Boilerplate Reduction

| Dependency                 | Purpose                                                                  |
| -------------------------- | ------------------------------------------------------------------------ |
| `org.projectlombok:lombok` | Provides `@Data`, `@Builder`, `@Slf4j`, `@RequiredArgsConstructor`, etc. |

### 2.9 Development Tools

| Dependency             | Purpose                                            |
| ---------------------- | -------------------------------------------------- |
| `spring-boot-devtools` | Hot reload and LiveReload during local development |

### 2.10 Testing

| Dependency                                          | Purpose                                                                             |
| --------------------------------------------------- | ----------------------------------------------------------------------------------- |
| `spring-boot-starter-test`                          | JUnit 5, Mockito, MockMvc, `@SpringBootTest`, `@DataJpaTest`                        |
| `org.springframework.security:spring-security-test` | `@WithMockUser`, `SecurityMockMvcRequestPostProcessors` for authentication in tests |

## 3. Infrastructure

| Component               | Technology                                            | Purpose                                                                                    |
| ----------------------- | ----------------------------------------------------- | ------------------------------------------------------------------------------------------ |
| Primary Database        | PostgreSQL (via Supabase) with **PostGIS** extension  | Relational data storage + geospatial radius queries                                        |
| Cache / Message Broker  | **Redis**                                             | OTP TTL, JWT blacklist, dispatch state, WebSocket pub/sub for horizontal scaling           |
| Push Notifications      | **Firebase Cloud Messaging (FCM)**                    | Server → Android push for task dispatch, status updates, and emergency broadcasts          |
| Authentication          | **Spring Security + JWT** (access + refresh token)    | Stateless authentication; tokens stored on Android with AES-256 EncryptedSharedPreferences |
| Real-time Communication | **Spring WebSocket (STOMP) ↔ StompProtocolAndroid**   | Bidirectional channel for live tracking, chat, and dispatch events                         |
| Maps                    | **Google Maps SDK** (Android) + **PostGIS** (Backend) | Client-side map rendering; server-side spatial queries for hub/volunteer discovery         |
| QR Code Workflow        | **ML Kit Barcode Scanning** + CameraX                 | Donation check-in (Sponsor → Staff) and dispatch check-out (Staff → Volunteer)             |

## 4. Design Patterns

| Pattern        | Where Applied                                                                              |
| -------------- | ------------------------------------------------------------------------------------------ |
| **Strategy**   | Task dispatch algorithm — SOS uses broadcast, supply requests use sequential batches       |
| **Observer**   | Real-time inventory updates — WebSocket notifies relevant clients on stock changes         |
| **Repository** | Data layer abstraction in both Android (Room + Retrofit) and Spring Boot (Spring Data JPA) |
| **MVVM**       | Android presentation layer — ViewModel holds state; LiveData drives UI updates             |
