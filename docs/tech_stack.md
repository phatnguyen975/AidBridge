# AidBridge Project - Tech Stack

This project is a Monorepo containing an Android mobile app (Frontend) and a Spring Boot application (Backend). The system focuses on disaster relief coordination using real-time geospatial data.

## 1. Frontend (Android Mobile App)

- **Language:** Strictly Java 17 (DO NOT use Kotlin).
- **UI:** Pure XML Layouts (DO NOT use Jetpack Compose).
- **Architecture:** MVVM (Model-View-ViewModel) + Clean Architecture principles.
- **Networking:** Retrofit2 + OkHttp3.
- **Maps & Location:** Google Maps SDK for Android, Google Location Services API (for polyline routing and live tracking).
- **Real-time Communication:** Firebase Cloud Messaging (FCM) or standard WebSocket client.
- **Local Storage (Offline mode):** Room Database.
- **Dependency Injection:** Dagger-Hilt.

## 2. Backend (Spring Boot)

- **Framework:** Spring Boot 4.x with Java 25.
- **Database:** Supabase (PostgreSQL with PostGIS extension enabled for geospatial/radius queries).
- **ORM:** Spring Data JPA + Hibernate Spatial.
- **Security:** Spring Security + JWT authentication.
- **Real-time Communication:** Spring WebSocket (STOMP) for chat and live tracking.
- **Design Patterns:** Emphasize Strategy Pattern for task coordination algorithms and Observer Pattern for inventory updates.
