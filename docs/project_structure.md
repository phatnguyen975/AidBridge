# AidBridge - Project Structure

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
│   ├── requirements.md                            # Business logic and feature details
│   ├── tech_stack.md                              # Technology stack definitions
│   └── project_structure.md                       # This file
│
├── database/                                      # --- INFRASTRUCTURE (Supabase/PostgreSQL) ---
│   ├── 01_init_postgis.sql                        # Enable PostGIS extension
│   ├── 02_schema.sql                              # DDL: Create tables (Users, Hubs, Inventory)
│   └── 03_seed_data.sql                           # DML: Insert initial mock data
│
├── drc-app/                                       # --- FRONTEND WORKSPACE ---
│   └── app/src/
│       ├── androidTest/                           # Instrumented tests
│       ├── test/                                  # Local JVM tests
│       └── main/
│           ├── AndroidManifest.xml                # App manifest, activity declarations, permissions
│           ├── java/com/drc/aidbridge/
│           │   ├── AidBridgeApplication.java      # @HiltAndroidApp entry point
│           │   ├── data/                          # Data layer implementation boundary
│           │   │   ├── local/                     # Local data access root
│           │   │   │   ├── dao/                   # Room DAO interfaces
│           │   │   │   └── entity/                # Room entity models
│           │   │   ├── mapper/                    # DTO <-> Domain mappers
│           │   │   ├── remote/                    # Network models and transport utilities
│           │   │   │   ├── api/                   # Retrofit service interfaces
│           │   │   │   ├── dto/request/           # Request payload DTOs
│           │   │   │   ├── dto/response/          # Response payload DTOs
│           │   │   │   └── interceptor/           # OkHttp interceptors (auth/refresh)
│           │   │   └── repository/                # Repository implementations (data orchestration)
│           │   ├── di/                            # Hilt modules (provide/bind dependencies)
│           │   ├── domain/                        # Domain layer (framework-agnostic business contracts)
│           │   │   ├── enums/                     # Domain enums (example: roles)
│           │   │   ├── model/                     # Domain models (pure POJO)
│           │   │   ├── repository/                # Repository interfaces
│           │   │   └── usecase/                   # Use cases (business rules)
│           │   │       ├── auth/                  # Auth-specific use cases
│           │   │       └── validation/            # Input validation contracts and results
│           │   ├── ui/                            # Presentation layer root
│           │   │   ├── auth/                      # Authentication screens and auth activity host
│           │   │   │   ├── fragment/              # Auth fragments (login/register/otp/forgot/guest shell)
│           │   │   │   └── viewmodel/             # Auth viewmodels
│           │   │   ├── base/                      # BaseActivity/BaseFragment/BaseViewModel abstractions
│           │   │   ├── common/                    # Reusable presentation controllers/widgets (example: OTP input)
│           │   │   ├── main/                      # Post-auth shell activity and role home fragments
│           │   │   │   └── fragment/              # Role-specific home modules (admin/staff/victim/volunteer/sponsor)
│           │   │   ├── map/                       # Map fragment entry points per role
│           │   │   └── splash/                    # Launch and session routing
│           │   └── utils/                         # Cross-cutting helpers/constants/token manager
│           ├── res/                               # Base resources (menu, navigation, values, launcher, animations)
│           ├── res-auth/                          # Auth layouts and auth drawables
│           ├── res-guest/                         # Guest layouts and guest UI assets
│           ├── res-core/                          # App-level core layouts (splash/main activity)
│           ├── res-common-ui/                     # Shared drawable assets across modules
│           ├── res-role-victim/                   # Victim-specific layouts
│           ├── res-role-volunteer/                # Volunteer-specific layouts
│           ├── res-role-sponsor/                  # Sponsor-specific layouts
│           ├── res-role-staff/                    # Staff-specific layouts
│           └── res-role-admin/                    # Admin-specific layouts
│
└── spring-backend/                                # --- BACKEND WORKSPACE ---
    ├── src/main/java/com/drc/aidbridge/
    │   ├── config/                                # Spring configurations (CORS, WebSocket, Swagger)
    │   ├── controller/                            # REST API Endpoints
    │   ├── dto/                    
    │   │   ├── request/                           # Incoming client data (e.g., SosRequestDto)
    │   │   └── response/                          # Outgoing client data, common API wrappers
    │   ├── domain/                                # Core domain models
    │   │   ├── entity/                            # JPA entities mapped to database tables
    │   │   └── enums/                             # Domain enums used in entities and business logic
    │   ├── mapper/                                # Mapper classes for converting Entity <-> DTO
    │   ├── exception/                             # GlobalExceptionHandler, Custom Exceptions
    │   ├── repository/                            # Spring Data JPA (PostGIS queries)
    │   ├── security/                              # JWT Request Filter, Custom UserDetails
    │   ├── service/
    │   │   ├── impl/                              # Service Implementations
    │   │   ├── strategy/                          # Contains Broadcast & Sequential Batches logic
    │   │   └── observer/                          # Listens to Inventory change events
    │   └── websocket/                             # STOMP Handlers (Chat, Live Tracking)
    └── resources/
        ├── application.yaml                       # Global configuration (Port, JPA ddl-auto)
        ├── application-local.example.yaml         # Template file with empty keys (Safe to commit)
        └── application-local.yaml                 # Actual file containing secrets (DO NOT commit)
```
