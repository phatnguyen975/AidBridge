# AidBridge - Monorepo Directory Structure

The project follows a Monorepo approach. The frontend utilizes Clean Architecture + MVVM. The backend applies the standard Spring Boot layered architecture combined with Design Patterns (Strategy, Observer) to handle coordination logic.

```text
AidBridge/
├── .github/
│   └── instructions/
│       ├── global.instructions.md
│       ├── android.instructions.md
│       └── spring.instructions.md
│
├── docs/
│   ├── requirements.md             # Business logic and feature details
│   ├── tech_stack.md               # Technology stack definitions
│   └── project_structure.md        # This file
│
├── drc-app/                        # --- FRONTEND WORKSPACE ---
│   ├── app/src/main/java/com/drc/aidbridge/
│   │   ├── data/                   
│   │   │   ├── local/              # Room DB: Entities, DAOs, AppDatabase
│   │   │   ├── remote/             # Retrofit: ApiService, Interceptors
│   │   │   └── repository/         # Repository Implementations
│   │   ├── di/                     # Dagger-Hilt Modules
│   │   │   ├── NetworkModule.java
│   │   │   └── DatabaseModule.java
│   │   ├── domain/                 
│   │   │   ├── model/              # Domain Models (pure data objects only)
│   │   │   ├── repository/         # Repository Interfaces
│   │   │   └── usecase/            # Business logic (e.g., CalculateDistanceUseCase)
│   │   ├── services/               
│   │   │   ├── location/           # Background Location Tracking Service
│   │   │   └── messaging/          # Firebase Cloud Messaging Service
│   │   ├── ui/                     # Contains Views (Activities/Fragments) and ViewModels
│   │   │   ├── base/               # BaseActivity, BaseFragment, BaseViewModel
│   │   │   ├── auth/               # Login, Register screens
│   │   │   ├── map/                # Map rendering, Markers, Polylines
│   │   │   ├── sos/                # Quick SOS, SOS request form
│   │   │   ├── volunteer/          # Missions, 30s countdown, Live tracking
│   │   │   └── hub/                # Inventory management, QR scanning
│   │   └── utils/                  # Formatters, Constants, Permission Helpers
│   └── app/src/main/res/           
│       ├── layout/                 # UI XML files
│       ├── drawable/               # Custom icons (Green Marker, Shelter Flag)
│       └── values/                 # strings.xml, colors.xml, themes.xml
│
└── spring-backend/                 # --- BACKEND WORKSPACE ---
    ├── src/main/java/com/drc/aidbridge/
    │   ├── config/                 # Spring configurations (CORS, WebSocket, Swagger)
    │   ├── controller/             # REST API Endpoints
    │   ├── dto/                    
    │   │   ├── request/            # Incoming client data (e.g., SosRequestDto)
    │   │   └── response/           # Outgoing client data, common API wrappers
    │   ├── domain/                 # Core domain models
    │   │   ├── entity/             # JPA entities mapped to database tables
    │   │   └── enums/              # Domain enums used in entities and business logic
    │   ├── mapper/                 # Mapper classes for converting Entity <-> DTO
    │   ├── exception/              # GlobalExceptionHandler, Custom Exceptions
    │   ├── repository/             # Spring Data JPA (PostGIS queries)
    │   ├── security/               # JWT Request Filter, Custom UserDetails
    │   ├── service/                
    │   │   ├── impl/               # Service Implementations
    │   │   ├── strategy/           # Contains Broadcast & Sequential Batches logic
    │   │   └── observer/           # Listens to Inventory change events
    │   └── websocket/              # STOMP Handlers (Chat, Live Tracking)
    └── resources/
        ├── application.yml                 # Global configuration (Port, JPA ddl-auto)
        ├── application-local.example.yml   # Template file with empty keys (Safe to commit)
        └── application-local.yml           # Actual file containing secrets (DO NOT commit)
```
