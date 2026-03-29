# Event-Driven Listener Pattern - Complete Guide

**Project**: AidBridge | **Date**: March 29, 2026

---

## Table of Contents
1. [What is a Listener?](#what-is-a-listener)
2. [Without Listener (Traditional)](#without-listener-traditional)
3. [With Listener (Event-Driven)](#with-listener-event-driven)
4. [Code Examples](#code-examples)
5. [Data Transfer Flow](#data-transfer-flow)
6. [Why It's Better](#why-its-better)
7. [Real-World Scenarios](#real-world-scenarios)

---

## What is a Listener?

### Simple Definition

A **Listener** is a class that **watches for events** and **automatically reacts** when they occur.

```
Think of it like a doorbell system:

WITHOUT Listener:
  Visitor: "Ring doorbell"
  → Must manually go tell everyone in house
  → Tell Mom: "visitor here"
  → Tell Dog: "visitor here"
  → Tell Cat: "visitor here"
  → Everyone does their thing based on who told them

WITH Listener:
  Visitor: "Ring doorbell"
  → Doorbell rings (EVENT published)
  → Mom listens & opens door
  → Dog listens & barks
  → Cat listens & hides
  → All react automatically without anyone telling them!
```

---

## Without Listener (Traditional)

### Problem: Tight Coupling & Manual Calls

```java
// modules/volunteer/internal/usecase/ToggleVolunteerStatusUseCase.java
@Component
@RequiredArgsConstructor
public class ToggleVolunteerStatusUseCase {

    private final VolunteerJpaRepository repository;
    private final VolunteerMapper mapper;

    // PROBLEM: Direct dependency on other modules
    private final MissionService missionService;        // ❌ Import from mission module
    private final NotificationService notificationService; // ❌ Import from notification module
    private final AnalyticsService analyticsService;    // ❌ Import from analytics module

    @Transactional
    public VolunteerDTO execute(UUID userId, ToggleStatusRequest request) {
        // Get volunteer
        Volunteer volunteer = repository.findByUserId(userId)
            .orElseThrow(() -> new VolunteerNotFoundException("Not found"));

        // Update status
        volunteer.setOnline(request.isOnline());
        volunteer = repository.save(volunteer);

        // PROBLEM: Manually call all dependent services
        if (request.isOnline()) {
            missionService.assignPendingMissions(userId);              // ❌ Hard couple with Mission
            notificationService.sendOnlineAlert(userId);              // ❌ Hard couple with Notification
            analyticsService.trackVolunteerOnline(userId);            // ❌ Hard couple with Analytics
        }

        return mapper.toDTO(volunteer);
    }
}
```

### Problems
```
❌ TIGHT COUPLING:
   - Volunteer knows about Mission, Notification, Analytics
   - Can't test without mocking all 3 services
   - If Mission changes, Volunteer breaks

❌ SCALABILITY:
   - Add new requirement? Add new import & call
   - Soon: 10+ services imported in one file

❌ CIRCULAR DEPENDENCIES:
   - Mission might also need Volunteer
   - Leads to circular imports (Java hell)

❌ HARD TO TEST:
   @Test
   void testToggleStatus() {
       when(missionService.assignPendingMissions(any())).thenReturn(...);
       when(notificationService.sendOnlineAlert(any())).thenReturn(...);
       when(analyticsService.trackVolunteerOnline(any())).thenReturn(...);
       // ... 10+ mocks just to test volunteer toggle!
   }
```

---

## With Listener (Event-Driven)

### Solution: Publish Event + Let Others Listen

```java
// modules/volunteer/internal/usecase/ToggleVolunteerStatusUseCase.java
@Component
@RequiredArgsConstructor
public class ToggleVolunteerStatusUseCase {

    private final VolunteerJpaRepository repository;
    private final VolunteerMapper mapper;

    // GOOD: Only dependency on event publisher (infrastructure)
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public VolunteerDTO execute(UUID userId, ToggleStatusRequest request) {
        // Get volunteer
        Volunteer volunteer = repository.findByUserId(userId)
            .orElseThrow(() -> new VolunteerNotFoundException("Not found"));

        // Update status
        volunteer.setOnline(request.isOnline());
        volunteer = repository.save(volunteer);

        // GOOD: Just publish event
        // Other modules listen & react automatically
        eventPublisher.publishEvent(
            new VolunteerStatusChangedEvent(
                volunteerId = userId,
                isOnline = request.isOnline(),
                timestamp = Instant.now()
            )
        );

        return mapper.toDTO(volunteer);
    }
}
```

### Benefits
```
✅ LOOSE COUPLING:
   - Volunteer doesn't know about Mission, Notification, Analytics
   - Each module independent
   - Can add new listeners without touching volunteer code

✅ SCALABILITY:
   - Add new requirement? Add new Listener in new module
   - Original code unchanged

✅ NO CIRCULAR DEPENDENCIES:
   - Mission doesn't import Volunteer
   - Volunteer doesn't import Mission
   - Only share: Event classes

✅ EASY TO TEST:
   @Test
   void testToggleStatus() {
       // NO mocks needed for other services!
       // Just verify event published
       VolunteerDTO result = useCase.execute(userId, request);

       verify(eventPublisher).publishEvent(
           any(VolunteerStatusChangedEvent.class)
       );
   }
```

---

## Code Examples

### 1. Event Definition (PUBLIC - Root of Module)

```java
// modules/volunteer/VolunteerStatusChangedEvent.java
@Getter
@Builder
public class VolunteerStatusChangedEvent extends ApplicationEvent {

    // Event metadata
    private final UUID volunteerId;
    private final boolean isOnline;
    private final Instant timestamp;
    private final String reason;  // "toggle", "timeout", "offline_at_distance"

    // Constructor (required by Spring ApplicationEvent)
    public VolunteerStatusChangedEvent(Object source, UUID volunteerId,
                                       boolean isOnline, Instant timestamp, String reason) {
        super(source);
        this.volunteerId = volunteerId;
        this.isOnline = isOnline;
        this.timestamp = timestamp;
        this.reason = reason;
    }
}
```

**Key Point**: Event is PUBLIC so other modules can import it

---

### 2. Publishing Event (In UseCase)

```java
// modules/volunteer/internal/usecase/ToggleVolunteerStatusUseCase.java
@Component
@RequiredArgsConstructor
public class ToggleVolunteerStatusUseCase {

    private final VolunteerJpaRepository repository;
    private final ApplicationEventPublisher eventPublisher;  // Spring's event bus

    @Transactional
    public VolunteerDTO execute(UUID userId, ToggleStatusRequest request) {
        Volunteer volunteer = repository.findByUserId(userId)
            .orElseThrow(() -> new VolunteerNotFoundException("Not found"));

        boolean wasOnline = volunteer.isOnline();
        volunteer.setOnline(request.isOnline());
        volunteer = repository.save(volunteer);

        // Publish event AFTER saving (ensure DB consistency)
        eventPublisher.publishEvent(
            new VolunteerStatusChangedEvent(
                source = this,
                volunteerId = userId,
                isOnline = request.isOnline(),
                timestamp = Instant.now(),
                reason = "toggle"  // User manually toggled
            )
        );

        return VolunteerDTO.from(volunteer);
    }
}
```

---

### 3. Listening to Event (In Other Modules)

#### Listener 1: Mission Module

```java
// modules/mission/internal/listener/VolunteerStatusListener.java
@Component
@RequiredArgsConstructor
public class MissionVolunteerStatusListener {

    private final MissionJpaRepository missionRepository;
    private final MissionMapper mapper;

    /**
     * When volunteer comes online, activate pending missions
     */
    @EventListener
    @Transactional
    public void onVolunteerOnline(VolunteerStatusChangedEvent event) {

        // Only react to "online" events
        if (!event.isOnline()) {
            return;
        }

        // Find missions assigned to this volunteer
        List<Mission> assignedMissions = missionRepository
            .findAssignedToVolunteer(event.getVolunteerId());

        for (Mission mission : assignedMissions) {
            // If mission was pending, activate it
            if (mission.getStatus() == MissionStatus.PENDING) {
                mission.setStatus(MissionStatus.ACTIVE);
                mission.setActivatedAt(event.getTimestamp());
                missionRepository.save(mission);

                // Could also publish another event for notification
                // eventPublisher.publishEvent(new MissionActivatedEvent(...));
            }
        }

        log.info("Activated {} missions for volunteer {}",
            assignedMissions.size(), event.getVolunteerId());
    }

    /**
     * When volunteer goes offline, pause active missions
     */
    @EventListener
    @Transactional
    public void onVolunteerOffline(VolunteerStatusChangedEvent event) {

        if (event.isOnline()) {
            return;
        }

        List<Mission> activeMissions = missionRepository
            .findActiveByVolunteer(event.getVolunteerId());

        for (Mission mission : activeMissions) {
            mission.setStatus(MissionStatus.PAUSED);
            mission.setPausedAt(event.getTimestamp());
            missionRepository.save(mission);
        }

        log.info("Paused {} missions for volunteer {}",
            activeMissions.size(), event.getVolunteerId());
    }
}
```

#### Listener 2: Notification Module

```java
// modules/notification/internal/listener/VolunteerStatusListener.java
@Component
@RequiredArgsConstructor
public class NotificationVolunteerStatusListener {

    private final NotificationService notificationService;
    private final UserJpaRepository userRepository;

    @EventListener
    public void onVolunteerStatusChanged(VolunteerStatusChangedEvent event) {

        // Get volunteer user details
        User user = userRepository.findUserByVolunteerId(event.getVolunteerId())
            .orElse(null);

        if (user == null) {
            return;
        }

        // Send notification based on status
        if (event.isOnline()) {
            notificationService.sendPushNotification(
                userId = user.getId(),
                title = "You're Online",
                message = "Your status updated. Ready to help!",
                type = NotificationType.VOLUNTEER_ONLINE
            );

            // Also send to hub staff
            notificationService.broadcastToHubs(
                title = "Volunteer Online",
                message = user.getFullName() + " is now available",
                type = NotificationType.VOLUNTEER_AVAILABLE
            );
        } else {
            notificationService.sendPushNotification(
                userId = user.getId(),
                title = "You're Offline",
                message = "Status changed to offline",
                type = NotificationType.VOLUNTEER_OFFLINE
            );
        }
    }
}
```

#### Listener 3: Analytics Module

```java
// modules/analytics/internal/listener/VolunteerAnalyticsListener.java
@Component
@RequiredArgsConstructor
public class VolunteerAnalyticsListener {

    private final AnalyticsService analyticsService;

    @EventListener
    public void trackVolunteerStatusChange(VolunteerStatusChangedEvent event) {

        analyticsService.recordEvent(
            eventName = "volunteer_status_changed",
            volunteerId = event.getVolunteerId(),
            data = Map.of(
                "is_online", event.isOnline(),
                "reason", event.getReason(),
                "timestamp", event.getTimestamp()
            )
        );

        // Update aggregate statistics
        if (event.isOnline()) {
            analyticsService.incrementMetric("volunteers_online_count");
        } else {
            analyticsService.decrementMetric("volunteers_online_count");
        }
    }
}
```

---

## Data Transfer Flow

### Without Listener (Synchronous - Direct Calls)

```
┌─────────────────────────────────────────┐
│ ToggleVolunteerStatusUseCase            │
│ (volunteer module)                      │
└──────────────┬──────────────────────────┘
               │
               ├─→ Step 1: Save volunteer to DB
               │        repository.save(volunteer)
               │
               │
               ├─→ Step 2: Call MissionService
               │        missionService.assignPendingMissions()
               │        ↓
               │        (waits for response: 100ms)
               │
               │
               ├─→ Step 3: Call NotificationService
               │        notificationService.sendAlert()
               │        ↓
               │        (waits for response: 50ms)
               │
               │
               ├─→ Step 4: Call AnalyticsService
               │        analyticsService.trackEvent()
               │        ↓
               │        (waits for response: 30ms)
               │
               │
               └─→ Step 5: Return to controller
                  (Total time: DB save + 100ms + 50ms + 30ms = ~180ms)

PROBLEMS:
❌ Slow: Must wait for all services to complete
❌ If MissionService is down: Entire operation fails
❌ All must succeed or all fail (no partial success)
❌ Tight coupling: Volunteer knows about all services
```

### With Listener (Asynchronous - Event-Driven)

```
┌──────────────────────────────────────────────────────────────────┐
│ ToggleVolunteerStatusUseCase (volunteer module)                  │
└────────────┬─────────────────────────────────────────────────────┘
             │
             ├─→ Step 1: Save volunteer to DB [SYNC: 5ms]
             │        repository.save(volunteer)
             │
             │
             ├─→ Step 2: Publish Event [SYNC: 1ms]
             │        eventPublisher.publishEvent(VolunteerStatusChangedEvent)
             │
             │        ✓ Return immediately (fire-and-forget)
             │        ✓ Total time: 6ms
             │
             └─→ Return to controller [FAST: 6ms total!]


┌──────────────────────────────────────────────────────────────────┐
│ Spring Event Bus (Infrastructure)                                │
│                                                                   │
│ Distributes event to all registered listeners in parallel:       │
└────────────┬─────────────────────────────────────────────────────┘
             │
             ├─→ Listener 1: MissionVolunteerStatusListener [ASYNC: ~50ms]
             │   - Fires immediately after event published
             │   - Runs in background thread
             │   - If fails: Doesn't affect volunteer toggle
             │
             │
             ├─→ Listener 2: NotificationVolunteerStatusListener [ASYNC: ~30ms]
             │   - Fires immediately after event published
             │   - Runs in background thread
             │   - If fails: Volunteer already toggled (no rollback)
             │
             │
             └─→ Listener 3: AnalyticsListener [ASYNC: ~20ms]
                 - Fires immediately after event published
                 - Runs in background thread
                 - If fails: Doesn't matter (analytics is not critical)

BENEFITS:
✅ Fast: Returns in 6ms (vs 180ms synchronous)
✅ Fault tolerant: If listener fails, original operation still succeeded
✅ Scalable: Add more listeners without slowing down
✅ Loose coupling: Each listener independent
✅ Better UX: User sees instant response
```

---

## Detailed Data Flow Diagram

### Scenario: Volunteer Toggles Online Status

```
TIME    VOLUNTEER MODULE          EVENT BUS           MISSION MODULE    NOTIFICATION MODULE   ANALYTICS MODULE
│
0ms     ┌────────────────┐
        │ HTTP Request   │
        └────────┬───────┘
                 │
                 ▼
5ms     ┌────────────────┐
        │ Save to DB     │
        │ volunteer      │
        │ .setOnline(t)  │
        └────────┬───────┘
                 │
                 ▼ (Data: Volunteer entity)
10ms    ┌────────────────────────┐
        │ Publish Event          │
        │ VolunteerStatusChanged │
        │ Event(id, true, ...)   │
        └────────┬───────────────┘
                 │
                 │ (Event object in memory)
                 │
                 ▼
15ms    ┌─────────────────────────────────────────────────────┐
        │ Spring ApplicationEventPublisher.publishEvent()      │
        │ (triggers all listeners)                            │
        └─────────────────────────────────────────────────────┘
                 │
                 ├──────────────┬──────────────┬───────────────┐
                 │              │              │               │
20ms    ┌────────┘      ┌───────┘      ┌──────┘        ┌──────┘
        │               │              │               │
        ▼               ▼              ▼               ▼
  MissionListener Receives   Notification     Analytics
  (Background Thread 1)      (Background T2)  (Background T3)
        │                    │                │
        ▼                    ▼                ▼
  - Query: Find          - Get user info   - Record event
    missions for           from DB          in analytics
    volunteer           - Send push          DB
  - Update mission         notification   - Update metrics
    status              - Send to hubs      counts
  - Save changes          broadcast

70ms    ┌────────────────────────────────────────────────────────────┐
        │ All async operations complete                              │
        │ (But frontend already received response at 15ms)           │
        └────────────────────────────────────────────────────────────┘

USER SEES: Response at 15ms (listener operations happen in background)
BACKEND WORKS ON: Missions, Notifications, Analytics for 50-70ms
```

---

## Why It's Better

### Comparison Table

| Aspect | Without Listener | With Listener |
|--------|------------------|---------------|
| **Response Time** | 180ms (wait for all) | 15ms (immediate) |
| **Scalability** | O(n) - each service slows response | O(1) - constant time |
| **Coupling** | TicketService knows 5+ services | Only knows event publisher |
| **Testing** | Needs 5+ mocks | Needs 0 mocks |
| **Failure Isolation** | One fails = all fails | Listeners fail independently |
| **Adding Feature** | Modify existing service | Add new listener (isolated) |
| **Code Changes** | Touch original file | Create new file |
| **Circular Dependencies** | Likely | Impossible |
| **UX Perception** | Feels slow | Feels instant |
| **Database Consistency** | All succeed or rollback | Original succeeds, listeners async |

---

## Real-World Scenarios

### Scenario 1: Volunteer Goes Online

```
Event: VolunteerStatusChangedEvent(volunteerId, isOnline=true)

Listener Reactions:
  Mission Module:
    → Activate pending missions
    → Calculate ETA to mission locations
    → Auto-assign if match score high

  Notification Module:
    → Send "ready to help" notification to volunteer
    → Alert hub staff: "Volunteer available in [district]"

  Analytics Module:
    → Track "volunteers_online_count" ↑
    → Record "volunteer_online" event
    → Update "avg_response_time" metric

  SOS Request Module:
    → Check pending SOS requests in volunteer's area
    → Re-rank SOS by distance to online volunteers
    → Auto-dispatch if rules allow

TIME TO VOLUNTEER:
  Without listeners: 500ms+ (wait for all modules)
  With listeners: 15ms (instant!)
```

### Scenario 2: Inventory Low Stock

```
Event: InventoryLowStockEvent(itemId, remainingQuantity=5)

Listener Reactions:
  Notification Module:
    → Alert hub staff: "Item low: only 5 units left"

  Mission Module:
    → Don't assign missions requesting this item
    → Update mission priorities

  Analytics Module:
    → Track "low_stock_alerts"
    → Alert supply chain manager

BENEFIT:
  All modules react without inventory knowing about them!
  Add new module? Just add new listener!
```

### Scenario 3: Mission Completed

```
Event: MissionCompletedEvent(missionId, volunteerId, completionTime)

Listener Reactions:
  Volunteer Module:
    → Update volunteer.totalTasksCompleted++
    → Update volunteer.avgResponseTime
    → Recalculate volunteer.avgRating

  Notification Module:
    → Send "mission completed" confirmation
    → Notify hub staff

  Analytics Module:
    → Track "missions_completed"
    → Record completion time for ML model
    → Update leaderboards

  Inventory Module:
    → Update item usage logs
    → Track "items_used_in_mission"

LOOSE COUPLING:
  Mission doesn't know about Volunteer, Notification, Analytics, Inventory
  All react independently!
```

---

## Implementation Rules

### ✅ DO

```java
// DO: Listen to external module's events
@EventListener
public void onVolunteerStatusChanged(VolunteerStatusChangedEvent event) {
    // React based on public event
}

// DO: Publish events from use cases
@Transactional
public void execute(...) {
    // Business logic
    eventPublisher.publishEvent(new YourEvent(...));
}

// DO: Only import public classes
import volunteer.VolunteerStatusChangedEvent;  // ✅

// DO: Make events immutable
@Getter
@Builder
public class YourEvent extends ApplicationEvent {
    private final UUID id;  // final
    private final String name;  // final
}
```

### ❌ DON'T

```java
// DON'T: Access other module's listeners
import volunteer.internal.listener.*;  // ❌

// DON'T: Import other module's use cases
import mission.internal.usecase.*;  // ❌

// DON'T: Make events mutable
public class YourEvent {
    private UUID id;  // ❌ Can be changed
    public void setId(UUID id) { ... }  // ❌
}

// DON'T: Block on event
eventPublisher.publishEvent(...);  // Wait for all listeners? NO!
// Fire and forget, return immediately
```

---

## Summary

### Without Listener
```
Tight Coupling + Slow + Hard to Test + Not Scalable
```

### With Listener
```
Loose Coupling + Fast + Easy to Test + Highly Scalable
```

**Use listeners for loosely-coupled inter-module communication!** 🚀
