# Modular Monolith Architecture for Spring Boot

**Project**: AidBridge | **Date**: March 29, 2026 | **Pattern**: Modular Monolith + Facade + UseCase + Events

---

## Architecture Diagram

### High-Level System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Spring Boot Application                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  MODULES LAYER:                                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ volunteer/   │  │ mission/     │  │ sos_request/ │  ...     │
│  │              │  │              │  │              │          │
│  │ - Facade ✓   │  │ - Facade ✓   │  │ - Facade ✓   │          │
│  │ - DTO ✓      │  │ - DTO ✓      │  │ - DTO ✓      │          │
│  │ - Events ✓   │  │ - Events ✓   │  │ - Events ✓   │          │
│  │ - internal/  │  │ - internal/  │  │ - internal/  │          │
│  │   ├─ web/    │  │   ├─ web/    │  │   ├─ web/    │          │
│  │   ├─ usecase/│  │   ├─ usecase/│  │   ├─ usecase/│          │
│  │   ├─ listener│  │   ├─ listener│  │   ├─ listener│          │
│  │   ├─ mapper/ │  │   ├─ mapper/ │  │   ├─ mapper/ │          │
│  │   ├─ entity/ │  │   ├─ entity/ │  │   ├─ entity/ │          │
│  │   ├─ repo/   │  │   ├─ repo/   │  │   ├─ repo/   │          │
│  │   └─ config/ │  │   └─ config/ │  │   └─ config/ │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
│                                                                   │
│  INFRASTRUCTURE LAYER:                                           │
│  ├── Database (PostgreSQL + PostGIS)                            │
│  ├── Event Bus (Spring Events / Kafka)                          │
│  ├── Cache (Redis)                                              │
│  ├── Security (JWT)                                             │
│  └── Config (Spring, AOP, etc)                                  │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                   HTTP Clients (External)                        │
│            Mobile App | Web App | Third-party APIs              │
└─────────────────────────────────────────────────────────────────┘
```

### Module Communication Patterns

```
PATTERN 1: Synchronous (Facade Call)

  MissionService
    ↓ calls
  VolunteerFacade
    ↓ returns
  VolunteerDTO (immediate response)

  Use: When you need immediate result


PATTERN 2: Asynchronous (Event-Driven)

  Volunteer Module
    ↓ publishes
  VolunteerStatusChangedEvent
    ↓ consumed by
  Mission Module
    ↓ listener (MissionVolunteerStatusListener)

  Use: When modules should react without knowing each other
```

---

## Folder Structure

### Complete Project Layout

```
spring-backend/src/main/java/com/drc/aidbridge/
│
├── modules/                         ← ALL MODULES
│   ├── volunteer/
│   ├── mission/
│   ├── sos_request/
│   ├── hub/
│   ├── inventory/
│   ├── notification/
│   ├── user/
│   └── shared/
│
├── infrastructure/                  ← Cross-cutting
│   ├── config/
│   ├── security/
│   ├── exception/
│   ├── dto/
│   └── event/                       (Event bus config)
│
└── AidBridgeApplication.java
```

### Single Module Structure (Template)

```
modules/volunteer/
│
├── VolunteerFacade.java             ✓ PUBLIC interface
├── VolunteerDTO.java                ✓ PUBLIC data transfer object
├── VolunteerStatusChangedEvent.java  ✓ PUBLIC event (Broadcast to others)
├── VolunteerNotFoundException.java   ✓ PUBLIC exception
│
└── internal/                        ✗ PRIVATE (don't import from other modules)
    │
    ├── web/
    │   ├── VolunteerController.java
    │   └── dto/
    │       ├── CreateVolunteerRequest.java
    │       ├── UpdateProfileRequest.java
    │       └── VolunteerProfileResponse.java
    │
    ├── usecase/                     ← EACH USE CASE = ONE CLASS
    │   ├── GetVolunteerProfileUseCase.java
    │   ├── UpdateVolunteerProfileUseCase.java
    │   ├── ToggleVolunteerStatusUseCase.java
    │   └── UpdateVolunteerLocationUseCase.java
    │
    ├── listener/                    ← NEW: React to other modules' events
    │   ├── MissionEventListener.java
    │   └── SosRequestEventListener.java
    │
    ├── mapper/
    │   └── VolunteerMapper.java
    │
    ├── entity/                      ← RENAMED: Was "core/" for clarity
    │   └── Volunteer.java           (JPA @Entity)
    │
    ├── repository/
    │   └── VolunteerJpaRepository.java
    │
    ├── config/
    │   └── VolunteerModuleConfig.java
    │
    └── VolunteerFacadeImpl.java      (Delegates to use cases + publishes events)
```

### 8 Strategic Modules

```
1. volunteer/            Profile, availability, location, ratings
2. mission/              Create, assign, match, complete missions
3. sos_request/          Emergency requests, tracking, priority
4. hub/                  Disaster hubs, staff, category management
5. inventory/            Donations, stock tracking, distribution
6. notification/         Push, email, event notifications
7. user/                 Authentication, roles, verification
8. shared/               Cross-module: exceptions, base DTOs, configs
```

---

## Data Transfer Flow

### Flow 1: HTTP Request → Response

```
HTTP Request
  ↓ POST /api/volunteers/profile
  ↓
Controller.updateProfile():
  ✓ Parse UpdateProfileRequest
  ✓ Extract userId from JWT
  ✓ Call: volunteerFacade.updateProfile(userId, request)
  ↓
VolunteerFacadeImpl.updateProfile():
  ✓ Delegate to UpdateVolunteerProfileUseCase.execute()
  ↓
UpdateVolunteerProfileUseCase.execute():
  ✓ repository.findByUserId(userId)
  ✓ entity.setVehicleType(...)
  ✓ repository.save(entity)
  ✓ mapper.toDTO(entity)
  ✓ Publish: VolunteerStatusChangedEvent
  ✓ Return VolunteerDTO
  ↓
Controller receives VolunteerDTO
  ✓ Wrap: ApiResponse.success("Updated", volunteerDTO)
  ↓
HTTP 200 + JSON Response
```

### Flow 2: Inter-Module Communication (Synchronous)

```
MissionUseCase needs Volunteer data:

  MissionAssignUseCase.execute():
    ✓ Call: volunteerFacade.getProfile(volunteerId)
    ↓ (Import only: volunteer.VolunteerFacade)
    ↓ (Import only: volunteer.VolunteerDTO)
    ✓ Get: VolunteerDTO {id, userId, isOnline, ...}
    ✓ Continue with DTO

  KEY RULES:
    ✓ import volunteer.VolunteerFacade;     (OK)
    ✓ import volunteer.VolunteerDTO;        (OK)
    ✗ import volunteer.internal.usecase.*;  (NO)
    ✗ import volunteer.internal.repository.*; (NO)
```

### Flow 3: Event-Driven Communication (Asynchronous)

```
Volunteer Module publishes event:

  ToggleVolunteerStatusUseCase.execute():
    ✓ volunteer.setOnline(status)
    ✓ repository.save(volunteer)
    ✓ mapper.toDTO(volunteer)
    ✓ Publish event: new VolunteerStatusChangedEvent(volunteerId, isOnline)
    ↓
  Spring Event Bus (Application Events)
    ↓ distributes to all listeners
    ↓

Other modules listen:

  Mission Module:
    MissionVolunteerStatusListener.onVolunteerStatusChanged(event)
      ✓ Check: is volunteer assigned to any missions?
      ✓ Update mission status accordingly

  Notification Module:
    NotificationVolunteerStatusListener.onVolunteerStatusChanged(event)
      ✓ Send push: "Volunteer offline"

  KEY BENEFIT:
    - Volunteer module doesn't know about Mission/Notification
    - Mission/Notification react to Volunteer events
    - Loose coupling, high scalability
```

### Flow 4: Data Transformation Chain

```
Request DTO → Controller → Facade
  ↓
UseCase.execute()
  ↓
Repository → Entity → Mapper
  ↓
VolunteerDTO + Event
  ↓
Controller (wrap in ApiResponse)
  ↓
HTTP Response + EventBus (publishes event)
```

---

## Module Boundaries

### ✅ CORRECT (Between Modules)

```java
// In MissionUseCase
import volunteer.VolunteerFacade;            // OK
import volunteer.VolunteerDTO;               // OK
import volunteer.VolunteerStatusChangedEvent; // OK

VolunteerDTO vol = volunteerFacade.getProfile(id);
```

### ❌ WRONG (Between Modules)

```java
import volunteer.internal.usecase.*;     // NO
import volunteer.internal.repository.*;  // NO
import volunteer.internal.entity.*;      // NO
import volunteer.internal.listener.*;    // NO
```

### ✅ ALWAYS OK (Within Module)

```java
// Inside volunteer/internal/...
import volunteer.internal.usecase.*;     // OK
import volunteer.internal.repository.*;  // OK
import volunteer.internal.entity.*;      // OK
import volunteer.internal.listener.*;    // OK
```

---

## Use Cases Pattern

| Aspect | Service | UseCase |
|--------|---------|---------|
| **Responsibility** | Multiple operations | Single operation |
| **Testability** | Hard with many mocks | Simple, focused |
| **Clear Intent** | Generic `VolunteerService` | Explicit `GetProfileUseCase` |
| **Maintainability** | Mixed concerns | Isolated concerns |

---

## Events Pattern

### Why Events?

```
WITHOUT Events (Tight Coupling):

  Volunteer Service
    → calls Mission Service
    → calls Notification Service
    → calls Analytics Service

  Problem: Circular dependencies, hard to test, high coupling


WITH Events (Loose Coupling):

  Volunteer publishes event:
    VolunteerStatusChangedEvent

  Others listen:
    - Mission: MissionVolunteerStatusListener
    - Notification: NotificationVolunteerStatusListener
    - Analytics: AnalyticsVolunteerStatusListener

  Benefit: Volunteer doesn't know about listeners, scales easily
```

### Publishing Events

```java
// Inside usecase
public class ToggleVolunteerStatusUseCase {
  private final ApplicationEventPublisher eventPublisher;

  public void execute(UUID userId, ToggleStatusRequest request) {
    volunteer.setOnline(request.isOnline());
    repository.save(volunteer);

    // Publish event
    eventPublisher.publishEvent(
      new VolunteerStatusChangedEvent(volunteerId, volunteer.isOnline())
    );
  }
}
```

### Listening to Events

```java
// In listener/ folder
@Component
public class MissionVolunteerStatusListener {
  private final MissionRepository missionRepository;

  @EventListener
  public void onVolunteerStatusChanged(VolunteerStatusChangedEvent event) {
    if (event.isOnline()) {
      // Activate pending missions for this volunteer
    }
  }
}
```

---

## Implementation Checklist

**Setup**
- [ ] Create modules/ directory structure
- [ ] Create shared/ with ApiResponse, exceptions, configs
- [ ] Setup Spring Event Bus configuration in infrastructure/

**For Each Module (Repeat for all 8)**
- [ ] Create {Module}Facade.java (interface)
- [ ] Create {Module}DTO.java (public)
- [ ] Create {Module}*Event.java (public events)
- [ ] Create {Module}Exception.java (public exceptions)
- [ ] Create internal/web/{Module}Controller.java
- [ ] Create internal/web/dto/* (REST DTOs)
- [ ] Create internal/usecase/*UseCase.java (per operation)
- [ ] Create internal/listener/*Listener.java (listen to external events)
- [ ] Create internal/mapper/{Module}Mapper.java
- [ ] Create internal/entity/{Module}.java (JPA entity)
- [ ] Create internal/repository/{Module}JpaRepository.java
- [ ] Create internal/config/{Module}ModuleConfig.java
- [ ] Create internal/{Module}FacadeImpl.java (delegates to use cases)
- [ ] Publish events from use cases
- [ ] Setup event listeners for dependent operations
- [ ] Test: No internal/ imports between modules
- [ ] Test: Events fire correctly

**Verification**
- [ ] All modules follow structure
- [ ] No circular dependencies (verify with IDE)
- [ ] All tests pass
- [ ] Only Facade + DTO + Event imported between modules
- [ ] Events are published and consumed correctly
