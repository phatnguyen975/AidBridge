# DDD Refactoring Plan for AidBridge Spring Backend

## Current Layered Architecture (Package-by-Layer)
```
spring-backend/src/main/java/com/drc/aidbridge/
├── config/              # 9 infrastructure files
├── controller/          # 4 HTTP entry points
├── dto/
│   ├── request/         # 10 DTOs
│   └── response/        # 6 DTOs
├── entity/              # 3 JPA entities + 5 enums
├── exception/           # 5 exception types
├── redis/               # 4 Redis schema classes
├── repository/          # 3 Spring Data interfaces
├── security/            # JWT handling (3 files)
├── service/             # 3 business services
└── SpringBackendApplication.java
```

**Current Issues:**
- ❌ All DTOs mixed horizontally (no domain context)
- ❌ Services know about ALL domains (AuthService, SosService, EmailService)
- ❌ Hard to find related code (Sos feature scattered across 7+ locations)
- ❌ Cross-domain coupling via shared repositories/services
- ❌ Difficult to add new features without touching existing layers

## Proposed Domain-Driven Structure (Package-by-Feature)
```
spring-backend/src/main/java/com/drc/aidbridge/
├── shared/                          # Global cross-cutting concerns
│   ├── config/                      # Global configs (CORS, WebSocket, Async)
│   ├── exception/                   # Global exceptions
│   ├── security/                    # JWT, filters, RSA keys
│   ├── redis/                       # Redis schemas (shared)
│   └── util/                        # Utility helpers
│
├── auth/                            # AUTH DOMAIN
│   ├── api/
│   │   └── AuthController.java      # REST endpoints for auth
│   ├── application/                 # Use cases/business logic
│   │   ├── AuthApplicationService.java
│   │   ├── PasswordResetService.java
│   │   └── OtpService.java
│   ├── domain/
│   │   ├── model/
│   │   │   └── User.java            # Domain model (NOT JPA)
│   │   ├── port/
│   │   │   ├── UserRepository.java
│   │   │   └── EmailNotificationPort.java
│   │   └── service/
│   │       └── UserDomainService.java
│   ├── infrastructure/
│   │   ├── persistence/
│   │   │   ├── JpaUserEntity.java
│   │   │   ├── UserJpaRepository.java
│   │   │   └── UserRepositoryAdapter.java
│   │   └── notification/
│   │       └── EmailNotificationAdapter.java
│   └── dto/
│       ├── request/
│       │   ├── LoginRequest.java
│       │   ├── RegisterRequest.java
│       │   └── ResetPasswordRequest.java
│       └── response/
│           ├── AuthResponse.java
│           └── TokenResponse.java
│
├── sos/                             # SOS COORDINATION DOMAIN
│   ├── api/
│   │   ├── SosRequestController.java
│   │   └── GuestSosController.java
│   ├── application/
│   │   ├── SosApplicationService.java
│   │   ├── AI_DespatchService.java
│   │   └── SosHistoryService.java
│   ├── domain/
│   │   ├── model/
│   │   │   ├── SosRequest.java      # Core aggregate root
│   │   │   ├── SosRequestDetails.java
│   │   │   ├── UrgencyLevel.java
│   │   │   └── SosStatus.java
│   │   ├── service/
│   │   │   ├── SosDomainService.java
│   │   │   └── DispatchAlgorithmService.java
│   │   └── port/
│   │       ├── SosRepository.java
│   │       ├── LocationServicePort.java
│   │       └── NotificationPort.java
│   ├── infrastructure/
│   │   ├── persistence/
│   │   │   ├── JpaSosEntity.java
│   │   │   ├── JpaSosDetailsEntity.java
│   │   │   ├── SosJpaRepository.java
│   │   │   └── SosRepositoryAdapter.java
│   │   └── external/
│   │       └── GoogleMapsLocationService.java
│   └── dto/
│       ├── request/
│       │   ├── CreateSosRequest.java
│       │   └── CreateGuestSosRequest.java
│       └── response/
│           └── SosRequestResponse.java
│
├── mission/                         # MISSION MANAGEMENT DOMAIN
│   ├── api/
│   │   └── MissionController.java
│   ├── application/
│   │   ├── MissionApplicationService.java
│   │   └── MissionAssignmentService.java
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Mission.java
│   │   │   ├── MissionStatus.java
│   │   │   └── MissionType.java
│   │   ├── service/
│   │   │   └── MissionDomainService.java
│   │   └── port/
│   │       ├── MissionRepository.java
│   │       └── VolunteerPort.java
│   ├── infrastructure/
│   │   └── persistence/
│   │       ├── JpaMissionEntity.java
│   │       ├── MissionJpaRepository.java
│   │       └── MissionRepositoryAdapter.java
│   └── dto/
│       ├── request/
│       │   └── AssignMissionRequest.java
│       └── response/
│           └── MissionResponse.java
│
├── volunteer/                       # VOLUNTEER MANAGEMENT DOMAIN
│   ├── api/
│   │   └── VolunteerController.java
│   ├── application/
│   │   ├── VolunteerApplicationService.java
│   │   └── VolunteerRatingService.java
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Volunteer.java
│   │   │   ├── VolunteerProfile.java
│   │   │   ├── Skill.java
│   │   │   └── Availability.java
│   │   ├── service/
│   │   │   └── VolunteerDomainService.java
│   │   └── port/
│   │       ├── VolunteerRepository.java
│   │       └── RatingPort.java
│   ├── infrastructure/
│   │   └── persistence/
│   │       ├── JpaVolunteerEntity.java
│   │       ├── VolunteerJpaRepository.java
│   │       └── VolunteerRepositoryAdapter.java
│   └── dto/
│       ├── request/
│       │   └── VolunteerProfileUpdate.java
│       └── response/
│           └── VolunteerResponse.java
│
├── sponsor/                         # SPONSOR MANAGEMENT DOMAIN
│   ├── api/
│   │   └── SponsorController.java
│   ├── application/
│   │   ├── SponsorApplicationService.java
│   │   └── DonationTrackingService.java
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Sponsor.java
│   │   │   ├── Donation.java
│   │   │   └── DonationStatus.java
│   │   ├── service/
│   │   │   └── SponsorDomainService.java
│   │   └── port/
│   │       ├── SponsorRepository.java
│   │       └── DonationRepository.java
│   ├── infrastructure/
│   │   └── persistence/
│   │       ├── JpaSponsorEntity.java
│   │       ├── SponsorJpaRepository.java
│   │       └── SponsorRepositoryAdapter.java
│   └── dto/
│       └── request/
│           └── CreateDonationRequest.java
│
├── hub/                             # RELIEF HUB MANAGEMENT DOMAIN
│   ├── api/
│   │   └── HubController.java
│   ├── application/
│   │   ├── HubApplicationService.java
│   │   └── HubInventoryService.java
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Hub.java
│   │   │   ├── HubStatus.java
│   │   │   └── HubCapacity.java
│   │   ├── service/
│   │   │   └── HubDomainService.java
│   │   └── port/
│   │       └── HubRepository.java
│   ├── infrastructure/
│   │   └── persistence/
│   │       ├── JpaHubEntity.java
│   │       ├── HubJpaRepository.java
│   │       └── HubRepositoryAdapter.java
│   └── dto/
│       └── response/
│           └── HubResponse.java
│
├── inventory/                       # INVENTORY MANAGEMENT DOMAIN
│   ├── api/
│   │   └── InventoryController.java
│   ├── application/
│   │   ├── InventoryApplicationService.java
│   │   └── DistributionService.java
│   ├── domain/
│   │   ├── model/
│   │   │   ├── InventoryItem.java
│   │   │   ├── ItemCategory.java
│   │   │   └── InventoryLog.java
│   │   ├── service/
│   │   │   └── InventoryDomainService.java
│   │   └── port/
│   │       └── InventoryRepository.java
│   ├── infrastructure/
│   │   └── persistence/
│   │       ├── JpaInventoryItem.java
│   │       ├── InventoryJpaRepository.java
│   │       └── InventoryRepositoryAdapter.java
│   └── dto/
│       ├── request/
│       │   └── StockAdjustmentRequest.java
│       └── response/
│           └── InventoryResponse.java
│
└── notification/                    # NOTIFICATION DOMAIN (Shared)
    ├── api/                         # Internal API for other domains
    ├── application/
    │   ├── NotificationApplicationService.java
    │   └── NotificationDispatcher.java
    ├── domain/
    │   ├── model/
    │   │   ├── Notification.java
    │   │   └── NotificationType.java
    │   └── port/
    │       ├── NotificationRepository.java
    │       └── PushPort.java (Firebase FCM)
    ├── infrastructure/
    │   └── external/
    │       └── FirebaseNotificationAdapter.java
    └── dto/
        └── response/
            └── NotificationResponse.java
```

## Key Design Patterns Applied

### 1. **Hexagonal Architecture (Ports & Adapters)**
- **Domain Layer**: Pure business logic, NO framework dependencies
- **Application Layer**: Use cases orchestrating domain services
- **Infrastructure Layer**: Spring Data, External APIs, Adapters
- **API Layer**: REST controllers, DTO mapping

### 2. **Domain-Driven Boundaries**
- Each domain is a **bounded context**
- Domains communicate via **Published Events** or **Application Services**
- No direct cross-domain repository access

### 3. **Key Responsibilities**

| Layer | Responsibility | NEVER DO |
|-------|---|---|
| **Domain Model** | Business rules, validations, state transitions | Depend on Spring, DB, Repositories |
| **Domain Service** | Cross-aggregate orchestration | UI logic, DB queries |
| **Application Service** | Use case orchestration, transaction mgmt, DTO mapping | Business logic decisions |
| **Controller** | HTTP marshalling, input validation | Business logic |
| **Repository Adapter** | Spring Data CRUD → Domain model mapping | Business logic |

### Auth Domain Deep Dive

```java
// ---------- DOMAIN LAYER (com/drc/aidbridge/auth/domain) ----------

// Domain Model - NO Spring annotations
public class User {  // Aggregate Root
    private UserId id;
    private String email;
    private PasswordHash passwordHash;
    private UserRole role;
    private boolean emailVerified;
    private LocalDateTime createdAt;

    // Domain invariants
    public void changePassword(String newPassword) throws DomainException {
        if (newPassword.length() < 8) {
            throw new InvalidPasswordException("Min 8 chars");
        }
        this.passwordHash = PasswordHash.of(newPassword);
    }

    public void markEmailVerified() {
        this.emailVerified = true;
    }
}

// Domain Repository Port (interface only - no DB logic)
public interface UserRepository {
    void save(User user);
    Optional<User> findByEmail(String email);
    User findById(UserId id);
}

// Domain Service - pure domain logic
public class UserDomainService {
    public User registerNewUser(String email, String password, UserRole role)
        throws DomainException {
        // Business rules here
        if (password.length() < 8) throw new InvalidPasswordException();

        User user = new User(UserId.generate(), email,
            PasswordHash.of(password), role);
        return user;
    }
}

// ---------- APPLICATION LAYER (com/drc/aidbridge/auth/application) ----------

@Service
public class AuthApplicationService {
    private final UserDomainService userDomainService;
    private final UserRepository userRepository;
    private final PasswordResetService passwordReset;
    private final JwtService jwtService;

    @Transactional
    public TokenResponse register(RegisterRequest request) throws ApplicationException {
        // Orchestrate domain & infrastructure
        User newUser = userDomainService.registerNewUser(
            request.email(), request.password(), UserRole.VICTIM);

        userRepository.save(newUser);

        String token = jwtService.generateToken(newUser);
        return new TokenResponse(token);
    }
}

// ---------- INFRASTRUCTURE LAYER (com/drc/aidbridge/auth/infrastructure) ----------

// JPA Entity - separate from domain model (never mix!)
@Entity
@Table(name = "users")
public class JpaUserEntity {
    @Id private String id;
    private String email;
    private String passwordHash;
    // ... JPA-specific annotations
}

// Spring Data Repository
@Repository
public interface UserJpaRepository extends JpaRepository<JpaUserEntity, String> {
    Optional<JpaUserEntity> findByEmail(String email);
}

// Adapter - Bridges Domain Repository to JPA
@Component
public class UserRepositoryAdapter implements UserRepository {
    @Autowired private UserJpaRepository jpaRepo;

    @Override
    public void save(User user) {
        // Map User domain model → JpaUserEntity
        JpaUserEntity entity = new JpaUserEntity(
            user.getId().value(),
            user.getEmail(),
            user.getPasswordHash().value()
        );
        jpaRepo.save(entity);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepo.findByEmail(email)
            .map(this::mapToDomain);
    }

    private User mapToDomain(JpaUserEntity entity) {
        // Map JpaUserEntity → User domain model
        return new User(UserId.of(entity.getId()), ...);
    }
}

// ---------- API LAYER (com/drc/aidbridge/auth/api) ----------

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired private AuthApplicationService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<TokenResponse>> register(
        @RequestBody RegisterRequest request) {
        TokenResponse token = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success(token));
    }
}
```

---

## Refactoring Risk & Mitigation

| Risk | Mitigation |
|------|-----------|
| **Breaking existing tests** | Keep old layer until all tests migrated |
| **Circular dependencies** | Use event bus for cross-domain comms |
| **Large PR** | Migrate one domain at a time (Auth → SOS → Mission) |
| **Performance degradation** | Add caching at adapter layer (Redis) |
| **Missing business logic** | Extract domain rules BEFORE moving code |

## Phase-Based Migration Strategy

**Phase 1 (Week 1):** Setup shared layer + Migrate AUTH domain**Phase 2 (Week 2):** Migrate SOS domain + test integration**Phase 3 (Week 3):** Migrate Mission, Volunteer, Sponsor domains**Phase 4 (Week 4):** Migrate Inventory, Hub, Notification; optimize & refactor
