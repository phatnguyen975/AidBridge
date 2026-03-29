# Facade & FacadeImpl - Complete Guide

**Project**: AidBridge | **Date**: March 29, 2026

---

## Table of Contents
1. [What is Facade?](#what-is-facade)
2. [What is FacadeImpl?](#what-is-facadeimpl)
3. [Facade Pattern Diagram](#facade-pattern-diagram)
4. [Code Examples](#code-examples)
5. [Using Facade Within Same Module](#using-facade-within-same-module)
6. [Using Facade in Other Modules](#using-facade-in-other-modules)
7. [Spring Dependency Injection Setup](#spring-dependency-injection-setup)

---

## What is Facade?

### Simple Definition

**Facade** = A **PUBLIC interface** that defines **what operations** a module offers.

Think of it like a restaurant's menu:
```
Menu (Facade) shows:
  - Order Burger
  - Order Pizza
  - Order Salad

You don't care HOW the kitchen makes it (implementation details)
You just call the menu operations
```

```java
// modules/volunteer/VolunteerFacade.java (PUBLIC)
public interface VolunteerFacade {

    // What operations does volunteer module offer?
    VolunteerDTO getVolunteerProfile(UUID userId);
    VolunteerDTO updateVolunteerProfile(UUID userId, UpdateProfileRequest request);
    VolunteerDTO toggleVolunteerStatus(UUID userId, ToggleStatusRequest request);
    void updateVolunteerLocation(UUID userId, UpdateLocationRequest request);
}
```

### Key Points About Facade

```
✅ PUBLIC: Lives in module root, can be imported by other modules
✅ INTERFACE: Just defines operations (no implementation)
✅ SIMPLE: Only methods that other modules need
✅ CONTRACT: Says "I promise to do these operations"
✅ STABLE: Once published, don't change (backward compatibility)
```

---

## What is FacadeImpl?

### Simple Definition

**FacadeImpl** = The **PRIVATE implementation** of Facade interface that delegates to use cases.

```
FacadeImpl is like the KITCHEN:
  - Facade (Menu) says what operations exist
  - FacadeImpl (Kitchen) says HOW to do each operation
  - Kitchen is PRIVATE (customers don't see it)
```

```java
// modules/volunteer/internal/VolunteerFacadeImpl.java (INTERNAL/PRIVATE)
@Component
@RequiredArgsConstructor
public class VolunteerFacadeImpl implements VolunteerFacade {

    // Inject all use cases
    private final GetVolunteerProfileUseCase getProfileUseCase;
    private final UpdateVolunteerProfileUseCase updateProfileUseCase;
    private final ToggleVolunteerStatusUseCase toggleStatusUseCase;
    private final UpdateVolunteerLocationUseCase locationUseCase;

    // Implement each operation by delegating to correct use case
    @Override
    public VolunteerDTO getVolunteerProfile(UUID userId) {
        return getProfileUseCase.execute(userId);
    }

    @Override
    public VolunteerDTO updateVolunteerProfile(UUID userId, UpdateProfileRequest request) {
        return updateProfileUseCase.execute(userId, request);
    }

    @Override
    public VolunteerDTO toggleVolunteerStatus(UUID userId, ToggleStatusRequest request) {
        return toggleStatusUseCase.execute(userId, request);
    }

    @Override
    public void updateVolunteerLocation(UUID userId, UpdateLocationRequest request) {
        locationUseCase.execute(userId, request);
    }
}
```

### Key Points About FacadeImpl

```
✅ INTERNAL/PRIVATE: Lives in internal/ folder, only used within module
✅ IMPLEMENTATION: Actually does the work (delegates to use cases)
✅ @Component: Spring manages it as a bean
✅ @RequiredArgsConstructor: Spring injects all use cases
✅ Each method delegates to ONE use case
```

---

## Facade Pattern Diagram

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                   VOLUNTEER MODULE                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  PUBLIC BOUNDARY ─────────────────────────────────────────────  │
│                                                                   │
│  ┌──────────────────────────────────────────┐                  │
│  │  VolunteerFacade [INTERFACE]             │ ← Other modules  │
│  │                                          │   import this   │
│  │  + getVolunteerProfile(userId)           │                 │
│  │  + updateVolunteerProfile(...)           │                 │
│  │  + toggleVolunteerStatus(...)            │                 │
│  │  + updateVolunteerLocation(...)          │                 │
│  │                                          │                 │
│  │  ┌─ VolunteerDTO [PUBLIC DTO]            │                 │
│  │  └─ Exceptions [PUBLIC]                  │                 │
│  └──────────────┬───────────────────────────┘                 │
│                 │ (implements)                                 │
│                 ▼                                              │
│  PRIVATE BOUNDARY ────────────────────────────────────────────  │
│                                                                   │
│  ┌──────────────────────────────────────────┐                  │
│  │  VolunteerFacadeImpl [IMPLEMENTATION]     │ ← Internal only │
│  │  (internal/)                             │                 │
│  │                                          │                 │
│  │  - GetVolunteerProfileUseCase            │                 │
│  │  - UpdateVolunteerProfileUseCase         │                 │
│  │  - ToggleVolunteerStatusUseCase          │                 │
│  │  - UpdateVolunteerLocationUseCase        │                 │
│  └──────────────┬───────────────────────────┘                 │
│                 │ (uses/delegates)                            │
│                 ▼                                              │
│  ┌──────────────────────────────────────────┐                 │
│  │  All Use Cases + Repository + Mapper     │                 │
│  │  (implementation details)                │                 │
│  └──────────────────────────────────────────┘                 │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

### Information Flow

```
OTHER MODULES:
  ↓ import VolunteerFacade (PUBLIC)
  ↓ import VolunteerDTO (PUBLIC)
  ↓
  volunteerFacade.getProfile(...) ← Call through interface


SPRING:
  ↓ wires VolunteerFacade interface
  ↓ injects VolunteerFacadeImpl bean
  ↓
  Spring knows: VolunteerFacade = VolunteerFacadeImpl
  ↓
  Other modules get VolunteerFacadeImpl but see it as VolunteerFacade


INTERNAL/IMPLEMENTATION:
  ┌─ VolunteerFacadeImpl never seen outside module
  ├─ Constructor injects all use cases
  ├─ Each method delegates to correct use case
  └─ Use of case has business logic
```

---

## Code Examples

### File Structure

```
modules/volunteer/
│
├── VolunteerFacade.java              ← PUBLIC INTERFACE
├── VolunteerDTO.java
├── VolunteerStatusChangedEvent.java
├── VolunteerNotFoundException.java
│
└── internal/
    │
    ├── web/
    │   ├── VolunteerController.java
    │   └── dto/
    │
    ├── usecase/
    │   ├── GetVolunteerProfileUseCase.java
    │   ├── UpdateVolunteerProfileUseCase.java
    │   ├── ToggleVolunteerStatusUseCase.java
    │   └── UpdateVolunteerLocationUseCase.java
    │
    ├── listener/
    │   └── MissionEventListener.java
    │
    ├── mapper/
    │   └── VolunteerMapper.java
    │
    ├── entity/
    │   └── Volunteer.java
    │
    ├── repository/
    │   └── VolunteerJpaRepository.java
    │
    ├── config/
    │   └── VolunteerModuleConfig.java
    │
    └── VolunteerFacadeImpl.java        ← PRIVATE IMPLEMENTATION
```

---

## Step-by-Step Implementation

### Step 1: Define Facade Interface (PUBLIC)

```java
// modules/volunteer/VolunteerFacade.java
public interface VolunteerFacade {

    /**
     * Get volunteer profile with user information
     */
    VolunteerDTO getVolunteerProfile(UUID userId) throws VolunteerNotFoundException;

    /**
     * Update volunteer profile (vehicle type, etc)
     */
    VolunteerDTO updateVolunteerProfile(UUID userId, UpdateProfileRequest request)
            throws VolunteerNotFoundException;

    /**
     * Toggle volunteer online/offline status
     */
    VolunteerDTO toggleVolunteerStatus(UUID userId, ToggleStatusRequest request)
            throws VolunteerNotFoundException;

    /**
     * Update volunteer current location
     */
    void updateVolunteerLocation(UUID userId, UpdateLocationRequest request)
            throws VolunteerNotFoundException;
}
```

### Step 2: Create Use Cases (INTERNAL)

```java
// modules/volunteer/internal/usecase/GetVolunteerProfileUseCase.java
@Component
@RequiredArgsConstructor
public class GetVolunteerProfileUseCase {

    private final VolunteerJpaRepository repository;
    private final VolunteerMapper mapper;

    @Transactional(readOnly = true)
    public VolunteerDTO execute(UUID userId) {
        Volunteer volunteer = repository.findByUserId(userId)
            .orElseThrow(() -> new VolunteerNotFoundException("Volunteer not found"));
        return mapper.toDTO(volunteer);
    }
}

// modules/volunteer/internal/usecase/UpdateVolunteerProfileUseCase.java
@Component
@RequiredArgsConstructor
public class UpdateVolunteerProfileUseCase {

    private final VolunteerJpaRepository repository;
    private final VolunteerMapper mapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public VolunteerDTO execute(UUID userId, UpdateProfileRequest request) {
        Volunteer volunteer = repository.findByUserId(userId)
            .orElseThrow(() -> new VolunteerNotFoundException("Volunteer not found"));

        if (request.getVehicleType() != null) {
            volunteer.setVehicleType(VehicleType.valueOf(request.getVehicleType()));
        }

        volunteer = repository.save(volunteer);

        // Publish event
        eventPublisher.publishEvent(
            new VolunteerStatusChangedEvent(this, userId, volunteer.isOnline(), Instant.now())
        );

        return mapper.toDTO(volunteer);
    }
}

// modules/volunteer/internal/usecase/ToggleVolunteerStatusUseCase.java
@Component
@RequiredArgsConstructor
public class ToggleVolunteerStatusUseCase {

    private final VolunteerJpaRepository repository;
    private final VolunteerMapper mapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public VolunteerDTO execute(UUID userId, ToggleStatusRequest request) {
        Volunteer volunteer = repository.findByUserId(userId)
            .orElseThrow(() -> new VolunteerNotFoundException("Volunteer not found"));

        volunteer.setOnline(request.isOnline());

        if (request.getCurrentLat() != null) {
            volunteer.setCurrentLat(request.getCurrentLat());
        }
        if (request.getCurrentLng() != null) {
            volunteer.setCurrentLng(request.getCurrentLng());
        }

        volunteer = repository.save(volunteer);

        // Publish event for other modules
        eventPublisher.publishEvent(
            new VolunteerStatusChangedEvent(this, userId, volunteer.isOnline(), Instant.now())
        );

        return mapper.toDTO(volunteer);
    }
}

// modules/volunteer/internal/usecase/UpdateVolunteerLocationUseCase.java
@Component
@RequiredArgsConstructor
public class UpdateVolunteerLocationUseCase {

    private final VolunteerJpaRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void execute(UUID userId, UpdateLocationRequest request) {
        Volunteer volunteer = repository.findByUserId(userId)
            .orElseThrow(() -> new VolunteerNotFoundException("Volunteer not found"));

        volunteer.setCurrentLat(request.getCurrentLat());
        volunteer.setCurrentLng(request.getCurrentLng());

        repository.save(volunteer);

        // Publish location update event
        eventPublisher.publishEvent(
            new VolunteerLocationUpdatedEvent(this, userId, request.getCurrentLat(), request.getCurrentLng())
        );
    }
}
```

### Step 3: Implement FacadeImpl (INTERNAL)

```java
// modules/volunteer/internal/VolunteerFacadeImpl.java
@Component  // ← Spring registers this bean
@RequiredArgsConstructor  // ← Spring injects dependencies
public class VolunteerFacadeImpl implements VolunteerFacade {

    // Inject all use cases
    private final GetVolunteerProfileUseCase getProfileUseCase;
    private final UpdateVolunteerProfileUseCase updateProfileUseCase;
    private final ToggleVolunteerStatusUseCase toggleStatusUseCase;
    private final UpdateVolunteerLocationUseCase locationUseCase;

    // Delegate to correct use case
    @Override
    public VolunteerDTO getVolunteerProfile(UUID userId) {
        return getProfileUseCase.execute(userId);
    }

    @Override
    public VolunteerDTO updateVolunteerProfile(UUID userId, UpdateProfileRequest request) {
        return updateProfileUseCase.execute(userId, request);
    }

    @Override
    public VolunteerDTO toggleVolunteerStatus(UUID userId, ToggleStatusRequest request) {
        return toggleStatusUseCase.execute(userId, request);
    }

    @Override
    public void updateVolunteerLocation(UUID userId, UpdateLocationRequest request) {
        locationUseCase.execute(userId, request);
    }
}
```

---

## Using Facade Within Same Module

### In Controller (Same Module - Volunteer)

```java
// modules/volunteer/internal/web/VolunteerController.java
@RestController
@RequestMapping("/api/volunteers")
@RequiredArgsConstructor
public class VolunteerController {

    // Inject facade interface
    private final VolunteerFacade volunteerFacade;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<VolunteerDTO>> getProfile(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());

        // Use facade (which is actually VolunteerFacadeImpl)
        VolunteerDTO profile = volunteerFacade.getVolunteerProfile(userId);

        return ResponseEntity.ok(
            ApiResponse.success("Retrieved", profile)
        );
    }

    @PatchMapping("/profile")
    public ResponseEntity<ApiResponse<VolunteerDTO>> updateProfile(
            Authentication auth,
            @Valid @RequestBody UpdateProfileRequest request) {

        UUID userId = UUID.fromString(auth.getName());

        // Use facade
        VolunteerDTO updated = volunteerFacade.updateVolunteerProfile(userId, request);

        return ResponseEntity.ok(
            ApiResponse.success("Updated", updated)
        );
    }

    @PostMapping("/status")
    public ResponseEntity<ApiResponse<VolunteerDTO>> toggleStatus(
            Authentication auth,
            @Valid @RequestBody ToggleStatusRequest request) {

        UUID userId = UUID.fromString(auth.getName());

        // Use facade
        VolunteerDTO toggled = volunteerFacade.toggleVolunteerStatus(userId, request);

        return ResponseEntity.ok(
            ApiResponse.success("Status updated", toggled)
        );
    }

    @PostMapping("/location")
    public ResponseEntity<ApiResponse<Void>> updateLocation(
            Authentication auth,
            @Valid @RequestBody UpdateLocationRequest request) {

        UUID userId = UUID.fromString(auth.getName());

        // Use facade
        volunteerFacade.updateVolunteerLocation(userId, request);

        return ResponseEntity.ok(
            ApiResponse.success("Location updated", null)
        );
    }
}
```

### Key Points

```
✅ Controller injects: VolunteerFacade (interface)
✅ Spring provides: VolunteerFacadeImpl (implementation)
✅ Controller sees: Interface contract only
✅ Controller doesn't know: Anything about use cases, repository, etc.
```

---

## Using Facade in Other Modules

### In Mission Module's UseCase

```java
// modules/mission/internal/usecase/AssignMissionUseCase.java
@Component
@RequiredArgsConstructor
public class AssignMissionUseCase {

    private final MissionJpaRepository missionRepository;

    // IMPORTANT: Import only from public API
    private final VolunteerFacade volunteerFacade;  // ← Import from volunteer root
    private final VolunteerDTO volunteerDTO;        // ← Import from volunteer root

    @Transactional
    public MissionDTO execute(UUID missionId, UUID volunteerId) {

        // Get volunteer info via PUBLIC Facade
        VolunteerDTO volunteer = volunteerFacade.getVolunteerProfile(volunteerId);

        // Verify volunteer is online
        if (!volunteer.isOnline()) {
            throw new InvalidOperationException("Volunteer is offline");
        }

        // Get mission
        Mission mission = missionRepository.findById(missionId)
            .orElseThrow(() -> new MissionNotFoundException("Mission not found"));

        // Assign volunteer
        mission.setAssignedVolunteerId(volunteerId);
        mission.setStatus(MissionStatus.ASSIGNED);
        mission.setAssignedAt(Instant.now());

        mission = missionRepository.save(mission);

        return MissionMapper.toDTO(mission);
    }
}
```

### Imports in Other Modules

```java
// ✅ CORRECT: Import only PUBLIC API
import volunteer.VolunteerFacade;                    // OK
import volunteer.VolunteerDTO;                       // OK
import volunteer.VolunteerStatusChangedEvent;        // OK (for listeners)
import volunteer.VolunteerNotFoundException;         // OK (public exception)

// ❌ WRONG: Never import from internal
import volunteer.internal.usecase.*;                 // NO!
import volunteer.internal.repository.*;             // NO!
import volunteer.internal.entity.*;                 // NO!
import volunteer.internal.VolunteerFacadeImpl.*;     // NO!
```

### Usage Pattern

```java
// In MissionService (other module):
MissionService {
    private final VolunteerFacade volunteerFacade;  // Inject interface

    public void assignVolunteer(UUID missionId, UUID volunteerId) {
        // Use facade (don't care it's VolunteerFacadeImpl)
        VolunteerDTO vol = volunteerFacade.getVolunteerProfile(volunteerId);

        // Continue business logic with DTO
        if (vol.isOnline()) {
            assignMission(...);
        }
    }
}
```

---

## Spring Dependency Injection Setup

### Module Configuration (Optional but Recommended)

```java
// modules/volunteer/internal/config/VolunteerModuleConfig.java
@Configuration
public class VolunteerModuleConfig {

    // All use cases are auto-registered by Spring (@Component)
    // FacadeImpl is auto-registered (@Component)

    // Optional: Explicit bean registration if needed
    @Bean
    public VolunteerFacade volunteerFacade(VolunteerFacadeImpl impl) {
        return impl;  // Interface implementation
    }
}
```

### How Spring Wires It

```
1. Spring scans @Component classes:
   ├─ GetVolunteerProfileUseCase (@Component)
   ├─ UpdateVolunteerProfileUseCase (@Component)
   ├─ ToggleVolunteerStatusUseCase (@Component)
   ├─ UpdateVolunteerLocationUseCase (@Component)
   └─ VolunteerFacadeImpl (@Component implements VolunteerFacade)

2. When you inject VolunteerFacade:
   @RequiredArgsConstructor
   public class MyClass {
       private final VolunteerFacade facade;
   }

3. Spring resolves:
   "You asking for VolunteerFacade interface
    I found VolunteerFacadeImpl that implements it
    Injecting that!"

4. Result:
   - You code against interface (stable)
   - Spring provides implementation (can change)
   - Other modules never see implementation
```

---

## Complete Data Flow

### Request Lifecycle

```
HTTP Request
  │
  ├─ VolunteerController
  │  (Receives request)
  │  └─ Injects: VolunteerFacade (interface)
  │
  ├─ volunteerFacade.getVolunteerProfile(userId)
  │  (Calls interface method)
  │  │
  │  └─ Spring resolves to VolunteerFacadeImpl
  │     (Actual implementation)
  │
  └─ VolunteerFacadeImpl.getVolunteerProfile(userId)
     └─ Delegates to: GetVolunteerProfileUseCase.execute(userId)
        │
        ├─ repository.findByUserId(userId)
        │  (Queries database)
        │
        ├─ mapper.toDTO(entity)
        │  (Converts to DTO)
        │
        └─ Returns: VolunteerDTO

           └─ Returns to Controller
              └─ Wraps in ApiResponse
                 └─ Sends to Client


IMPORTANT:
  Other modules only see:
  ├─ VolunteerFacade (interface)
  └─ VolunteerDTO (data)

  Other modules never see:
  ├─ VolunteerFacadeImpl (private)
  ├─ GetVolunteerProfileUseCase (private)
  ├─ VolunteerJpaRepository (private)
  └─ Volunteer entity (private)
```

---

## Summary Table

| Aspect | VolunteerFacade | VolunteerFacadeImpl |
|--------|---|---|
| **Type** | Interface | @Component Class |
| **Location** | `modules/volunteer/` (PUBLIC) | `modules/volunteer/internal/` (PRIVATE) |
| **Visibility** | Other modules import | Other modules never see |
| **Purpose** | Defines contract | Implements contract |
| **Implementation** | No code | Delegates to use cases |
| **Injection** | `@Inject VolunteerFacade` | Java resolves automatically |
| **Modification** | Rarely change | Can refactor internally |

---

## Key Rules

### ✅ DO

```java
// DO: Inject facade interface
private final VolunteerFacade facade;

// DO: Use facade from other modules
VolunteerDTO vol = volunteerFacade.getProfile(id);

// DO: Import public classes
import volunteer.VolunteerFacade;
import volunteer.VolunteerDTO;

// DO: Facade delegates to use cases
@Override
public VolunteerDTO getProfile(UUID id) {
    return getProfileUseCase.execute(id);
}
```

### ❌ DON'T

```java
// DON'T: Inject implementation directly
private final VolunteerFacadeImpl facade;  // NO!

// DON'T: Use use cases from other modules
private final GetVolunteerProfileUseCase useCase;  // NO!

// DON'T: Import private classes
import volunteer.internal.VolunteerFacadeImpl;  // NO!

// DON'T: Complex logic in Facade
public VolunteerDTO getProfile(UUID id) {
    // Business logic here - NO!
    // Just delegate to use case
}
```

---

## Visual Summary

```
┌────────────────────────────────────────────────────┐
│           OTHER MODULE (e.g., Mission)             │
├────────────────────────────────────────────────────┤
│                                                    │
│  @RequiredArgsConstructor                         │
│  public class MissionService {                    │
│                                                    │
│    private final VolunteerFacade facade; ───┐    │
│                                             │    │
│    facade.getProfile(volunteerId); ────────┼──┐ │
│  }                                          │  │ │
│                                             │  │ │
└─────────────────────────────────────────────┼──┼─┘
                                              │  │
                                    Import    │  │
                                    from      │  │
                                    public    │  │
                                    API       │  │
                                              │  │
┌─────────────────────────────────────────────┼──┼─┐
│    VOLUNTEER MODULE (Root - PUBLIC)        │  │ │
├─────────────────────────────────────────────┼──┼─┐
│                                             │  │ │
│  VolunteerFacade (Interface) ◄──────────────┘  │ │
│  VolunteerDTO                              │  │ │
│  VolunteerStatusChangedEvent               │  │ │
│  VolunteerNotFoundException                │  │ │
│                                             │  │ │
└─────────────────────────────────────────────┼──┼─┐
                                              │  │ │
                                    Spring    │  │ │
                                    provides  │  │ │
                                              │  │ │
┌─────────────────────────────────────────────┼──┼─┐
│  VOLUNTEER MODULE (internal - PRIVATE)     │  │ │
├─────────────────────────────────────────────┼──┼─┐
│                                             │  │ │
│  VolunteerFacadeImpl (Implementation) ◄─────┘  │ │
│  ├─ GetVolunteerProfileUseCase              │ │
│  ├─ UpdateVolunteerProfileUseCase           │ │
│  ├─ ToggleVolunteerStatusUseCase            │ │
│  └─ UpdateVolunteerLocationUseCase          │ │
│                                             │ │
│  + Repository, Mapper, Entity, Listeners   │ │
│                                             │ │
└─────────────────────────────────────────────┼─┘

(Other modules only know public API above line)
(Implementation hidden below)
```
