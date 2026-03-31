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
│   ├── build.gradle                               # Root Android Gradle configuration
│   ├── settings.gradle                            # Gradle module wiring
│   └── app/                                       # Main Android application module
│       ├── build.gradle                           # App-level dependencies and build types
│       └── src/
│           ├── androidTest/                       # Instrumented tests (device/emulator)
│           ├── test/                              # Local JVM unit tests
│           └── main/
│               ├── AndroidManifest.xml            # App manifest, activity declarations, permissions
│               ├── java/com/drc/aidbridge/        # Java source root (MVVM + Clean Architecture)
│               │   ├── AidBridgeApplication.java  # Application entry point annotated with @HiltAndroidApp
│               │   ├── data/                      # Data layer implementation boundary
│               │   │   ├── local/                 # Local data source (Room)
│               │   │   │   ├── AppDatabase.java   # Room database configuration and DAO registry
│               │   │   │   ├── dao/               # Room DAO contracts
│               │   │   │   └── entity/            # Local persistence entities
│               │   │   ├── mapper/                # DTO/entity to domain mappers
│               │   │   ├── remote/                # Network transport layer (Retrofit + OkHttp)
│               │   │   │   ├── api/               # Retrofit service interfaces
│               │   │   │   ├── dto/               # Request/response DTO container
│               │   │   │   │   ├── request/       # Request payload models
│               │   │   │   │   └── response/      # Response payload models (includes BaseResponse)
│               │   │   │   ├── interceptor/       # Auth and token refresh interceptors
│               │   │   │   └── NetworkResultWrapper.java # UI-safe async result wrapper
│               │   │   └── repository/            # Repository implementations (remote/local orchestration)
│               │   ├── di/                        # Hilt modules (provide/bind dependency graph)
│               │   │   ├── AppModule.java         # App-scoped primitive providers
│               │   │   ├── NetworkModule.java     # OkHttp/Retrofit providers
│               │   │   ├── ApiModule.java         # Retrofit API interface providers
│               │   │   ├── DatabaseModule.java    # Room database and DAO providers
│               │   │   └── RepositoryModule.java  # Interface-to-implementation bindings
│               │   ├── domain/                    # Domain layer (framework-agnostic contracts)
│               │   │   ├── enums/                 # Domain enums (example: role types)
│               │   │   ├── model/                 # Pure domain models
│               │   │   ├── repository/            # Repository interfaces consumed by use cases
│               │   │   └── usecase/               # Business use cases
│               │   │       ├── auth/              # Authentication use case set
│               │   │       └── validation/        # Input validation contracts and results
│               │   ├── ui/                        # Presentation layer (Fragment/ViewModel)
│               │   │   ├── auth/                  # Authentication feature module
│               │   │   │   ├── AuthActivity.java  # Auth host activity
│               │   │   │   ├── fragment/          # Login/register/OTP/forgot/guest auth fragments
│               │   │   │   └── viewmodel/         # Auth screen viewmodels
│               │   │   ├── base/                  # BaseActivity/BaseFragment/BaseViewModel abstractions
│               │   │   ├── common/                # Shared UI controllers/components
│               │   │   ├── main/                  # Post-auth main host and role modules
│               │   │   │   ├── MainActivity.java  # Main host activity containing role navigation
│               │   │   │   ├── adapter/           # Recycler adapters grouped by role
│               │   │   │   │   ├── sponsor/       # Sponsor adapters
│               │   │   │   │   ├── staff/         # Staff adapters (inventory/tasks/detail)
│               │   │   │   │   └── victim/        # Victim adapters
│               │   │   │   ├── fragment/          # Role-specific fragments (admin/sponsor/staff/victim/volunteer)
│               │   │   │   │   ├── admin/         # Admin main fragments
│               │   │   │   │   ├── sponsor/       # Sponsor main fragments
│               │   │   │   │   ├── staff/         # Staff main fragments and bottom sheets
│               │   │   │   │   ├── victim/        # Victim main fragments and bottom sheets
│               │   │   │   │   └── volunteer/     # Volunteer main fragments
│               │   │   │   └── viewmodel/         # Main-role viewmodels
│               │   │   │       └── volunteer/     # Volunteer viewmodel package
│               │   │   ├── map/                   # Role map presentation module
│               │   │   │   └── fragment/          # Map fragments per role
│               │   │   └── splash/                # App launch and session routing
│               │   └── utils/                     # Cross-cutting helpers (constants/network/token/permission)
│               ├── res/                           # Shared Android resources (base)
│               │   ├── navigation/                # Role-specific navigation graphs
│               │   ├── menu/                      # Bottom navigation menus per role
│               │   ├── values/                    # Global colors/dimens/strings/themes
│               │   └── xml/                       # Backup and data extraction rules
│               ├── res-auth/                      # Auth-only layouts/drawables/values
│               ├── res-guest/                     # Guest-only layouts/drawables/values
│               ├── res-core/                      # Core host layouts (splash/main containers)
│               ├── res-common-ui/                 # Shared drawables across features
│               ├── res-role-admin/                # Admin role resources
│               │   └── layout/                    # Admin screen layouts
│               ├── res-role-sponsor/              # Sponsor role resources
│               │   └── layout/                    # Sponsor screen layouts
│               ├── res-role-victim/               # Victim role resources
│               │   ├── drawable/                  # Victim-specific drawables
│               │   ├── layout/                    # Victim screen layouts
│               │   └── values/                    # Victim strings/dimens/colors
│               ├── res-role-volunteer/            # Volunteer role resources
│               │   ├── drawable/                  # Volunteer-specific drawables
│               │   ├── layout/                    # Volunteer screen layouts
│               │   └── values/                    # Volunteer strings/dimens/colors
│               └── res-role-staff/                # Staff role resources
│                   ├── color/                     # Staff selectors for chips/inputs
│                   ├── drawable/                  # Staff-specific drawable assets and badges
│                   ├── layout/                    # Staff screens (profile/inventory/scanner/tasks/detail)
│                   └── values/                    # Staff strings/dimens/colors/styles
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
