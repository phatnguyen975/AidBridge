# Domain-Driven Design (DDD) Refactoring Guide

**Date**: March 28, 2026
**Project**: AidBridge Disaster Relief Coordinator
**Current Architecture**: Layered (Package-by-Layer)
**Target Architecture**: Domain-Driven Design

---

## Table of Contents
1. [Current Architecture Analysis](#current-architecture-analysis)
2. [Architecture Comparison: Hexagonal vs Pragmatic DDD](#architecture-comparison)
3. [Recommended Approach](#recommended-approach)
4. [Domain Identification](#domain-identification)
5. [Folder Structure & Organization](#folder-structure--organization)
6. [Refactoring Strategy](#refactoring-strategy)
7. [Implementation Checklist](#implementation-checklist)

---

## Current Architecture Analysis

### Current Structure (Layered/N-tier)
```
spring-backend/src/main/java/com/drc/aidbridge/
├── controller/              ← HTTP layer (Request handling)
├── service/                 ← Business logic (All domains mixed)
├── repository/              ← Data access layer
├── entity/                  ← Domain models (JPA entities - no encapsulation)
├── dto/                     ← Data transfer objects
│   ├── request/
│   └── response/
├── exception/               ← Custom exceptions
├── config/                  ← Application configuration
├── security/                ← JWT, authentication
└── redis/                   ← Caching layer
```

### Problems with Layered Architecture
❌ **Low Cohesion**: Services mix multiple business domains (VolunteerService handles profiles + status + location)
❌ **Hard to Scale**: Adding new domains requires modifying all layers
❌ **Business Logic Scattered**: DTOs, mappers, and business rules across multiple layers
❌ **Testing Difficulty**: Service tests require many mocks (repository, repository, other services)
❌ **Unclear Boundaries**: No clear separation between different business domains
❌ **Anemic Models**: Entities are just data containers (no behavior encapsulation)

---

## Architecture Comparison

### 🔷 Hexagonal (Ports & Adapters) Architecture

**Structure**:
```
domain/
├── core/
│   └── volunteer/
│       ├── domain/          ← Pure business logic (no framework dependencies)
│       ├── application/     ← Use cases & DTOs
│       ├── ports/           ← Interfaces (contracts)
│       │   ├── VolunteerRepositoryPort
│       │   └── NotificationPort
│       └── adapters/        ← Framework implementations
│           ├── database/
│           ├── http/
│           └── cache/
```

**Characteristics**:
- ✅ **Complete Isolation**: Core domain has zero Spring dependencies
- ✅ **Framework Agnostic**: Swap Spring for Quarkus/Micronaut with minimal changes
- ✅ **Excellent for Large Teams**: Clear contracts and boundaries
- ✅ **Testability**: Domain layer testable with plain Java (no mocks needed)
- ❌ **Complexity**: More boilerplate (ports, adapters, mappers)
- ❌ **Over-engineering**: Overkill for small domains or simple CRUD

---

### 🔷 Pragmatic DDD Architecture

**Structure**:
```
domain/
├── volunteer/
│   ├── domain/              ← Entities, Value Objects, Aggregates
│   ├── application/         ← Application services, DTOs
│   ├── adapter/             ← Controllers, repositories, external calls
│   └── config/              ← Domain-specific configuration
├── mission/
├── sos_request/
└── inventory/
```

**Characteristics**:
- ✅ **Balanced**: Clean separation without excessive boilerplate
- ✅ **Pragmatic**: Uses Spring annotations in application/adapter layers (not core domain)
- ✅ **Scalable**: Easy to add new domains
- ✅ **Clear Ownership**: Each team owns a `domain/`  package
- ✅ **Practical**: Faster development than full hexagonal
- ⚠️ **Domain Layer**: Still depends on Spring annotations (entities, services)

---

## Recommended Approach

### 🎯 **PRAGMATIC DDD** (Recommended for AidBridge)

**Why Pragmatic DDD for AidBridge?**

| Criterion | Hexagonal | Pragmatic DDD | Winner |
|-----------|-----------|---------------|--------|
| **Team Size** | 5-10+ | 2-5 | **Pragmatic** ✅ |
| **Project Stage** | Mature | Growth | **Pragmatic** ✅ |
| **Development Speed** | Slower (more boilerplate) | Faster | **Pragmatic** ✅ |
| **Onboarding** | Steep learning curve | Gentle | **Pragmatic** ✅ |
| **Scalability** | Extreme | Great | **Pragmatic** ✅ |
| **Testing** | Superior | Good | Hexagonal |
| **Framework Lock-in** | Protected | Exposed | Hexagonal |

**Verdict**: Use **Pragmatic DDD** with strategic hexagonal patterns for critical domains (e.g., Mission matching algorithm).

---

## Domain Identification

Based on analysis of requirements and database schema, identify **6-8 strategic domains**:

### 1. **Volunteer Domain** 🧑‍💼
- **Responsibilities**: Volunteer profiles, availability, ratings, location tracking
- **Core Entities**: `Volunteer`, `VolunteerRating`
- **Key Operations**: Profile management, status toggle, location updates
- **File**: `domain/volunteer/`

### 2. **Mission Domain** 🎯
- **Responsibilities**: Mission creation, assignment, completion, matching
- **Core Entities**: `Mission`, `MissionTask`
- **Key Operations**: Accept mission, complete task, calculate ETA
- **File**: `domain/mission/`

### 3. **SOS Request Domain** 🆘
- **Responsibilities**: Emergency request creation, status tracking, prioritization
- **Core Entities**: `SosRequest`, `SosRequestDetail`
- **Key Operations**: Create request, assign volunteer, mark resolved
- **File**: `domain/sos_request/`

### 4. **Hub Domain** 🏪
- **Responsibilities**: Disaster hub management, staff assignment, category management
- **Core Entities**: `Hub`, `HubStaff`, `HubAcceptedCategory`
- **Key Operations**: Hub registration, staff management, category assignment
- **File**: `domain/hub/`

### 5. **Inventory Domain** 📦
- **Responsibilities**: Donation tracking, inventory management, distribution
- **Core Entities**: `Donation`, `InventoryLog`, `ItemCategory`
- **Key Operations**: Add donation, consume item, track quantity
- **File**: `domain/inventory/`

### 6. **Notification Domain** 📬
- **Responsibilities**: Push notifications, event notifications
- **Core Entities**: `Notification`
- **Key Operations**: Send notification, track read status
- **File**: `domain/notification/`

### 7. **User & Auth Domain** 👤
- **Responsibilities**: User registration, authentication, role management
- **Core Entities**: `User`, `Role`
- **Key Operations**: Login, register, role assignment
- **File**: `domain/user/`

### 8. **Shared Kernel** 🔧
- **Responsibilities**: Cross-domain utilities, value objects, common exceptions
- **File**: `domain/shared/`

---

## Folder Structure & Organization

### 📐 Complete Folder Structure

```
spring-backend/src/main/java/com/drc/aidbridge/
│
├── domain/                          ← DDD domains
│   ├── shared/                      ← Cross-domain (Value Objects, Exceptions, etc.)
│   │   ├── entity/
│   │   │   ├── BaseEntity.java      ← Abstract base with id, createdAt, updatedAt
│   │   │   └── AuditableEntity.java
│   │   ├── exception/
│   │   │   ├── DomainException.java
│   │   │   ├── ResourceNotFoundException.java
│   │   │   └── InvalidOperationException.java
│   │   ├── value_object/
│   │   │   ├── Location.java        ← Value Object (lat, lng)
│   │   │   ├── Rating.java          ← Value Object (score, count)
│   │   │   └── ContactInfo.java     ← Value Object (phone, email)
│   │   ├── event/
│   │   │   └── DomainEvent.java     ← Base for all domain events
│   │   └── util/
│   │       └── GeoUtils.java        ← Utility functions
│   │
│   ├── user/                        ← User & Auth Domain
│   │   ├── domain/
│   │   │   ├── User.java            ← User aggregate root
│   │   │   ├── Role.java            ← Role enum/value object
│   │   │   └── exceptions/
│   │   │       ├── UserAlreadyExistsException.java
│   │   │       └── InvalidCredentialsException.java
│   │   ├── application/
│   │   │   ├── dtos/
│   │   │   │   ├── request/
│   │   │   │   │   └── RegisterUserRequestDto.java
│   │   │   │   └── response/
│   │   │   │       └── UserResponseDto.java
│   │   │   ├── UserService.java     ← Application service (use case)
│   │   │   ├── UserMapper.java      ← DTO mappings
│   │   │   └── queries/
│   │   │       └── GetUserByIdQuery.java
│   │   ├── adapter/
│   │   │   ├── controller/
│   │   │   │   └── UserController.java
│   │   │   ├── repository/
│   │   │   │   ├── UserJpaRepository.java  ← Spring Data interface
│   │   │   │   └── UserRepositoryAdapter.java ← Implements UserRepository
│   │   │   ├── external/
│   │   │   │   └── JwtTokenProvider.java
│   │   │   └── config/
│   │   │       └── UserDomainConfig.java
│   │   └── UserDomainModule.java    ← Domain module facade
│   │
│   ├── volunteer/                   ← Volunteer Domain
│   │   ├── domain/
│   │   │   ├── Volunteer.java       ← Volunteer aggregate root
│   │   │   ├── VolunteerRating.java ← Rating entity within aggregate
│   │   │   ├── VehicleType.java     ← Enum
│   │   │   ├── VolunteerStatus.java ← Value Object (online/offline)
│   │   │   ├── repository/
│   │   │   │   └── VolunteerRepository.java ← Port interface
│   │   │   ├── service/
│   │   │   │   └── VolunteerDomainService.java ← Domain logic
│   │   │   ├── event/
│   │   │   │   └── VolunteerLocationUpdatedEvent.java
│   │   │   └── exceptions/
│   │   │       ├── VolunteerNotFoundException.java
│   │   │       └── InvalidLocationException.java
│   │   ├── application/
│   │   │   ├── dtos/
│   │   │   │   ├── request/
│   │   │   │   │   ├── UpdateVolunteerProfileRequestDto.java
│   │   │   │   │   └── UpdateVolunteerLocationRequestDto.java
│   │   │   │   └── response/
│   │   │   │       ├── VolunteerProfileDto.java
│   │   │   │       └── VolunteerProfileResponseDto.java
│   │   │   ├── VolunteerApplicationService.java ← Use cases
│   │   │   ├── VolunteerDtoMapper.java
│   │   │   └── queries/
│   │   │       └── GetVolunteerProfileQuery.java
│   │   ├── adapter/
│   │   │   ├── controller/
│   │   │   │   └── VolunteerController.java
│   │   │   ├── repository/
│   │   │   │   ├── VolunteerJpaRepository.java
│   │   │   │   └── VolunteerRepositoryAdapter.java
│   │   │   ├── external/
│   │   │   │   └── LocationServiceAdapter.java
│   │   │   └── config/
│   │   │       └── VolunteerDomainConfig.java
│   │   └── VolunteerDomainModule.java
│   │
│   ├── mission/                     ← Mission Domain
│   │   ├── domain/
│   │   │   ├── Mission.java         ← Aggregate root
│   │   │   ├── MissionTask.java
│   │   │   ├── MissionStatus.java   ← Value Object
│   │   │   ├── repository/
│   │   │   │   └── MissionRepository.java
│   │   │   ├── service/
│   │   │   │   ├── MissionDomainService.java
│   │   │   │   └── MissionMatchingService.java ← Business logic
│   │   │   └── exceptions/
│   │   │       └── MissionMatchingException.java
│   │   ├── application/
│   │   │   ├── dtos/...
│   │   │   └── MissionApplicationService.java
│   │   ├── adapter/
│   │   │   ├── controller/
│   │   │   ├── repository/
│   │   │   └── config/
│   │   └── MissionDomainModule.java
│   │
│   ├── sos_request/                 ← SOS Request Domain
│   │   ├── domain/
│   │   │   ├── SosRequest.java
│   │   │   ├── SosRequestDetail.java
│   │   │   ├── Priority.java        ← Value Object
│   │   │   ├── repository/
│   │   │   │   └── SosRequestRepository.java
│   │   │   ├── service/
│   │   │   │   └── SosRequestDomainService.java
│   │   │   └── exceptions/
│   │   │       └── InvalidSosStatusTransitionException.java
│   │   ├── application/
│   │   │   ├── dtos/...
│   │   │   └── SosRequestApplicationService.java
│   │   ├── adapter/
│   │   │   ├── controller/
│   │   │   ├── repository/
│   │   │   └── config/
│   │   └── SosRequestDomainModule.java
│   │
│   ├── hub/                         ← Hub Domain
│   │   ├── domain/
│   │   │   ├── Hub.java
│   │   │   ├── HubStaff.java
│   │   │   ├── repository/
│   │   │   │   └── HubRepository.java
│   │   │   ├── service/
│   │   │   │   └── HubDomainService.java
│   │   │   └── exceptions/
│   │   └── application/
│   │   ├── adapter/
│   │   └── HubDomainModule.java
│   │
│   ├── inventory/                   ← Inventory Domain
│   │   ├── domain/
│   │   │   ├── Donation.java
│   │   │   ├── InventoryLog.java
│   │   │   ├── ItemCategory.java
│   │   │   ├── repository/
│   │   │   │   └── DonationRepository.java
│   │   │   ├── service/
│   │   │   │   ├── InventoryDomainService.java
│   │   │   │   └── StockCalculationService.java ← Complex logic
│   │   │   └── exceptions/
│   │   │       ├── InsufficientStockException.java
│   │   │       └── InvalidCategoryException.java
│   │   ├── application/
│   │   ├── adapter/
│   │   └── InventoryDomainModule.java
│   │
│   ├── notification/                ← Notification Domain
│   │   ├── domain/
│   │   │   ├── Notification.java
│   │   │   ├── NotificationType.java
│   │   │   ├── service/
│   │   │   │   └── NotificationDomainService.java
│   │   │   └── port/
│   │   │       └── NotificationGateway.java ← Output port
│   │   ├── application/
│   │   └── adapter/
│   │       ├── external/
│   │       │   ├── FcmNotificationGateway.java ← FCM implementation
│   │       │   └── EmailNotificationGateway.java
│   │       └── config/
│   │
│   └── DomainRegistry.java          ← Central registry (optional)
│
├── infrastructure/                  ← Cross-cutting infrastructure
│   ├── persistence/
│   │   ├── config/
│   │   │   └── JpaConfig.java
│   │   └── event/
│   │       └── DomainEventPublisher.java
│   ├── cache/
│   │   └── RedisCacheConfig.java
│   ├── security/
│   │   ├── JwtTokenProvider.java
│   │   └── SecurityConfig.java
│   └── exception/
│       └── GlobalExceptionHandler.java
│
└── api/                             ← Application entry point
    ├── AidBridgeApplication.java
    └── config/
        └── ApplicationConfig.java
```

---

### 📋 Folder Descriptions

#### **`domain/`** (Core of DDD)
- Each `domain/` subdirectory is a **Bounded Context**
- **Ownership**: One team/person owns one domain package
- **Independence**: Minimal cross-domain dependencies

#### **`domain/{domain_name}/domain/`** (Domain Layer - Pure Logic)
- **Purpose**: Business logic with NO framework dependencies
- **Contains**:
  - `Entity` classes (e.g., `Volunteer.java`) - Aggregate roots
  - `Value Object` classes (e.g., `VehicleType`, `Location`)
  - Domain `Service` classes (complex business logic)
  - Domain `Repository` interfaces (output ports)
  - Domain `Event` classes (domain events)
  - Domain-specific `Exception` classes
- **Rules**:
  - ❌ NO `@Entity`, `@Service`, `@Repository` annotations
  - ❌ NO dependency on `org.springframework.*`
  - ✅ Pure Java + domain logic

#### **`domain/{domain_name}/application/`** (Application Layer - Use Cases)
- **Purpose**: Implement use cases, orchestrate domain operations
- **Contains**:
  - `ApplicationService` (use case implementation)
  - `Query` classes (read operations)
  - `Command` classes (write operations)
  - `Dto` subdirectories (request/response mappings)
  - `Mapper` classes (domain ↔ DTO conversions)
- **Can Use**: Spring annotations (`@Service`, `@Transactional`)

#### **`domain/{domain_name}/adapter/`** (Adapter Layer - Technical Details)
- **Purpose**: Interface with external systems (DB, HTTP, external APIs)
- **Contains**:
  - `controller/` - REST endpoints (`@RestController`)
  - `repository/` - JPA repositories & adapters
  - `external/` - External service calls, gateways
  - `config/` - Domain-specific Spring configuration
- **Rules**:
  - Converts between domain objects and external formats
  - Handles all Spring framework concerns
  - No business logic here

#### **`infrastructure/`** (Cross-Cutting Concerns)
- Shared infrastructure for ALL domains
- **Contains**:
  - Cache configuration
  - Security configuration
  - Database connection pooling
  - Exception handling
  - Event publishing

#### **`api/`** (Application Entry Point)
- Main Spring Boot class
- Global configuration
- Cross-cutting aspect setup

---

## Refactoring Strategy

### Phase 1: Setup (1-2 days)
1. Create domain folder structure
2. Create `BaseEntity` and shared value objects
3. Create application configuration files

### Phase 2: Migrate Volunteer Domain (2-3 days)
1. Move `Volunteer.java` to `domain/volunteer/domain/`
2. Create `VolunteerRepository` interface in domain
3. Create `VolunteerApplicationService` in application layer
4. Create `VolunteerRepositoryAdapter` in adapter layer
5. Move `VolunteerController` to adapter layer
6. Update DTOs and mappers
7. Create domain events (`VolunteerStatusChangedEvent`, etc.)

### Phase 3: Migrate Remaining Domains (3-4 days)
- Repeat Phase 2 for each domain
- Establish cross-domain communication patterns

### Phase 4: Implement Event-Driven Architecture (1-2 days)
- Set up `DomainEventPublisher`
- Publish events from domain services
- Create `EventListener` in adapter layer

### Phase 5: Testing & Documentation (1 day)
- Update test structure
- Document domain boundaries
- Create CQRS pattern for complex queries

---

## Implementation Checklist

### Setup Phase
- [ ] Create `domain/` folder structure
- [ ] Create shared base classes (`BaseEntity`, `DomainException`)
- [ ] Create value objects (`Location`, `Rating`)
- [ ] Setup `DomainRegistry` or module pattern

### Volunteer Domain
- [ ] Move `Volunteer.java` → `domain/volunteer/domain/`
- [ ] Update annotations (remove `@Entity`, use package-scoped)
- [ ] Create `VolunteerRepository` port interface
- [ ] Create `VolunteerApplicationService`
- [ ] Create `VolunteerRepositoryAdapter`
- [ ] Move `VolunteerController` to adapter layer
- [ ] Create DTOs in application layer
- [ ] Create domain events
- [ ] Write domain logic tests
- [ ] Update dependency injections

### Cross-Domain Communication
- [ ] Setup event publishing
- [ ] Create inter-domain event listeners
- [ ] Document domain boundaries
- [ ] Create `SharedKernel` module

### Cleanup
- [ ] Remove old structure
- [ ] Update all imports
- [ ] Verify no circular dependencies
- [ ] Run full test suite
- [ ] Update documentation

---

## Code Patterns

### Pattern 1: Aggregate Root
```java
// domain/volunteer/domain/Volunteer.java
public class Volunteer {  // NO annotations here
    private UUID id;
    private UUID userId;
    private VolunteerStatus status;  // Value Object
    private Location currentLocation; // Value Object

    // Domain logic
    public void updateLocation(Location newLocation) {
        this.currentLocation = newLocation;
        // Publish domain event
        this.recordEvent(new VolunteerLocationUpdatedEvent(this.id, newLocation));
    }
}
```

### Pattern 2: Application Service
```java
// domain/volunteer/application/VolunteerApplicationService.java
@Service
@RequiredArgsConstructor
public class VolunteerApplicationService {
   private final VolunteerRepository repository;
    private final DomainEventPublisher eventPublisher;

    @Transactional
    public VolunteerProfileResponseDto getProfile(UUID userId) {
        Volunteer volunteer = repository.findByUserId(userId)
            .orElseThrow(/* exception */);
        return VolunteerDtoMapper.toResponse(volunteer);
    }
}
```

### Pattern 3: Repository Adapter
```java
// domain/volunteer/adapter/repository/VolunteerRepositoryAdapter.java
@RequiredArgsConstructor
public class VolunteerRepositoryAdapter implements VolunteerRepository {
    private final VolunteerJpaRepository jpaRepository;

    @Override
    public Optional<Volunteer> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId)
            .map(VolunteerMapper::toDomain);
    }
}
```

### Pattern 4: Controller (Thin Layer)
```java
// domain/volunteer/adapter/controller/VolunteerController.java
@RestController
@RequestMapping("/api/volunteers")
@RequiredArgsConstructor
public class VolunteerController {
    private final VolunteerApplicationService applicationService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<VolunteerProfileResponseDto>> getProfile(
            Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        VolunteerProfileResponseDto result = applicationService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("", result));
    }
}
```

---

## Migration Example: Volunteer Domain

### Before (Layered)
```
controller/
  └── VolunteerController.java
service/
  └── VolunteerService.java
repository/
  └── VolunteerRepository.java
entity/
  └── Volunteer.java
dto/
  ├── request/
  │   └── UpdateVolunteerProfileRequestDto.java
  └── response/
      └── VolunteerProfileResponseDto.java
```

### After (Pragmatic DDD)
```
domain/volunteer/
├── domain/
│   ├── Volunteer.java                    ← Pure entity (no @Entity)
│   ├── VehicleType.java                  ← Value Object
│   ├── VolunteerStatus.java              ← Value Object
│   ├── repository/
│   │   └── VolunteerRepository.java      ← Port interface
│   ├── service/
│   │   └── VolunteerDomainService.java   ← Domain logic
│   ├── event/
│   │   └── VolunteerLocationUpdatedEvent.java
│   └── exceptions/
│       └── VolunteerNotFoundException.java
├── application/
│   ├── dtos/
│   │   ├── request/
│   │   │   └── UpdateVolunteerProfileRequestDto.java
│   │   └── response/
│   │       └── VolunteerProfileResponseDto.java
│   ├── VolunteerApplicationService.java   ← Use case implementation
│   └── VolunteerDtoMapper.java
├── adapter/
│   ├── controller/
│   │   └── VolunteerController.java       ← REST endpoint
│   ├── repository/
│   │   ├── VolunteerJpaRepository.java    ← Spring Data JPA
│   │   └── VolunteerRepositoryAdapter.java ← Implements port
│   └── config/
│       └── VolunteerDomainConfig.java
└── VolunteerDomainModule.java             ← Domain module
```

---

## Benefits After Refactoring

| Aspect | Before | After |
|--------|--------|-------|
| **Code Organization** | By layer (scattered across 5 folders) | By domain (one folder per feature) |
| **Adding Feature** | Modify controller + service + repo (3 files) | One domain folder (isolated) |
| **Testing** | Hard to test service without mocks | Pure domain logic testable without mocks |
| **Team Scalability** | Developers step on each other | Each team owns a domain |
| **Cross-Domain Communication** | Direct service calls (tight coupling) | Event-driven (loose coupling) |
| **Dependency Management** | Circular dependencies common | Explicit, acyclic dependencies |
| **Framework Lock-in** | High (JPA annotations everywhere) | Low (domain is pure Java) |

---

## Next Steps

1. **Review this document** with your team
2. **Start Phase 1** (setup domain structure)
3. **Pilot with Volunteer Domain** (smallest, clear scope)
4. **Iterate and refine** before migrating other domains
5. **Document domain boundaries** as you progress
6. **Establish communication patterns** (events, services)

---

## References

- Eric Evans - Domain-Driven Design (Blue Book)
- Vaughn Vernon - Implementing Domain-Driven Design (Red Book)
- Robert C. Martin - Clean Architecture
- CQRS Pattern: https://martinfowler.com/bliki/CQRS.html
- Event Sourcing: https://martinfowler.com/eaaDev/EventSourcing.html
