# Tracing Event Flow Simply

**Project**: AidBridge | **Date**: April 3, 2026

---

## TL;DR

Bạn muốn biết **event flow chính xác** mà không phức tạp?

**Cách đơn giản nhất: Dùng logging**

---

## Simple Event Flow (With Logging)

### 1️⃣ Event Class (định nghĩa)

```java
// modules/volunteer/VolunteerStatusChangedEvent.java
public class VolunteerStatusChangedEvent extends ApplicationEvent {
    private final UUID volunteerId;
    private final boolean isOnline;
    private final Instant timestamp;
    private final String reason;
    // ...
}
```

**Khi nào hiện?** Không cần log ở đây (chỉ là định nghĩa)

---

### 2️⃣ UseCase (publish event)

```java
@Component
public class ToggleVolunteerStatusUseCase {
    
    @Autowired private EventPublisher eventPublisher;
    
    @Transactional
    public void execute(UUID userId, ToggleStatusRequest request) {
        
        // 1. Business logic
        log.info("🟦 [STEP 1] Fetching volunteer: userId={}", userId);
        Volunteer volunteer = repository.findByUserId(userId).orElseThrow();
        
        log.info("🟦 [STEP 2] Updating volunteer status: volunteerI={}, isOnline={}", 
            userId, request.isOnline());
        volunteer.setOnline(request.isOnline());
        
        log.info("🟦 [STEP 3] Saving to database");
        volunteer = repository.save(volunteer);
        
        // 2. PUBLISH EVENT ← Key step
        log.info("🟨 [PUBLISH] Publishing VolunteerStatusChangedEvent: volunteerId={}, isOnline={}", 
            volunteer.getId(), request.isOnline());
        
        eventPublisher.publish(
            new VolunteerStatusChangedEvent(
                source = this,
                volunteerId = volunteer.getId(),
                isOnline = request.isOnline(),
                timestamp = Instant.now(),
                reason = "toggle"
            )
        );
        
        log.info("🟦 [STEP 4] UseCase completed. Event published to listeners");
        return mapper.toDTO(volunteer);
    }
}
```

**Output log sẽ là:**
```
🟦 [STEP 1] Fetching volunteer: userId=abc-123
🟦 [STEP 2] Updating volunteer status: volunteerI=abc-123, isOnline=true
🟦 [STEP 3] Saving to database
🟨 [PUBLISH] Publishing VolunteerStatusChangedEvent: volunteerId=abc-123, isOnline=true
🟦 [STEP 4] UseCase completed. Event published to listeners
```

**⏱️ Timing**: Tất cả xong trong ~5ms

---

### 3️⃣ Listener (react to event)

```java
// modules/mission/internal/listener/VolunteerStatusListener.java
@Component
public class VolunteerStatusListener {
    
    @EventListener
    @Transactional
    public void onVolunteerStatusChanged(VolunteerStatusChangedEvent event) {
        
        // Listener runs in BACKGROUND THREAD (async)
        log.info("🟩 [LISTENER] Received VolunteerStatusChangedEvent: volunteerId={}, isOnline={}", 
            event.getVolunteerId(), event.isOnline());
        
        if (event.isOnline()) {
            log.info("🟩 [LISTENER] Volunteer is ONLINE, finding pending missions...");
            
            List<Mission> missions = missionRepository
                .findByVolunteerId(event.getVolunteerId(), PENDING);
            
            log.info("🟩 [LISTENER] Found {} pending missions", missions.size());
            
            for (Mission mission : missions) {
                log.info("🟩 [LISTENER] Activating mission: id={}", mission.getId());
                mission.setStatus(ACTIVE);
                missionRepository.save(mission);
            }
            
            log.info("🟩 [LISTENER] Completed: activated {} missions", missions.size());
        } else {
            log.info("🟩 [LISTENER] Volunteer is OFFLINE, skipping");
        }
    }
}
```

**Output log sẽ là (chạy trong background):**
```
🟩 [LISTENER] Received VolunteerStatusChangedEvent: volunteerId=abc-123, isOnline=true
🟩 [LISTENER] Volunteer is ONLINE, finding pending missions...
🟩 [LISTENER] Found 3 pending missions
🟩 [LISTENER] Activating mission: id=m1
🟩 [LISTENER] Activating mission: id=m2
🟩 [LISTENER] Activating mission: id=m3
🟩 [LISTENER] Completed: activated 3 missions
```

**⏱️ Timing**: Chạy trong background, ~50-100ms sau khi publish

---

## 📊 Full Timeline (Bạn thấy gì trong logs)

```
T=0ms   🟦 [STEP 1] Fetching volunteer: userId=abc-123
        🟦 [STEP 2] Updating volunteer status: volunteerI=abc-123, isOnline=true
        🟦 [STEP 3] Saving to database
        🟨 [PUBLISH] Publishing VolunteerStatusChangedEvent: volunteerId=abc-123, isOnline=true
        🟦 [STEP 4] UseCase completed. Event published to listeners
        ↓
T=5ms   ✅ HTTP Response sent to client (request finished!)
        ↓
T=50ms  (In background thread, listeners processing...)
        🟩 [LISTENER] Received VolunteerStatusChangedEvent: volunteerId=abc-123, isOnline=true
        🟩 [LISTENER] Volunteer is ONLINE, finding pending missions...
        🟩 [LISTENER] Found 3 pending missions
        🟩 [LISTENER] Activating mission: id=m1
        🟩 [LISTENER] Activating mission: id=m2
        🟩 [LISTENER] Activating mission: id=m3
        🟩 [LISTENER] Completed: activated 3 missions
```

**Key insight**: 
- Rows 🟦 & 🟨 xảy ra trong main request (5ms)
- Row ✅ client nhận response (5ms)
- Rows 🟩 xảy ra sau đó, background (50ms, không block client)

---

## 🎯 Cách Trace Event Flow Đơn Giản

### Color Codes (dễ dàng track)

```
🟦 = UseCase steps (blue)
🟨 = Event published (yellow)
🟩 = Listener processing (green)
🔴 = Error (red)
```

### Log Format (consistent)

```
[TAG] [MESSAGE]: param1=value1, param2=value2

Example:
🟦 [STEP 1] Fetching volunteer: userId=abc-123
🟨 [PUBLISH] Publishing event: eventType=VolunteerStatusChangedEvent, volunteerId=abc-123
🟩 [LISTENER] Received event: eventType=VolunteerStatusChangedEvent, isOnline=true
🟩 [LISTENER] Found missions: count=3
```

### Grep to Find Flow

```bash
# Find all logs related to one event
grep "abc-123" logs.txt

# Should show:
🟦 [STEP 1] Fetching volunteer: userId=abc-123
🟦 [STEP 2] Updating volunteer status: ...
🟦 [STEP 3] Saving...
🟨 [PUBLISH] Publishing event: volunteerId=abc-123
🟩 [LISTENER] Received event: volunteerId=abc-123
🟩 [LISTENER] Found missions: ...
```

---

## 📝 Summary: Track Event Flow

### UseCase (3-4 log statements)

```java
log.info("🟦 [STEP 1] Fetching: {}", identifier);
log.info("🟦 [STEP 2] Updating: {}", values);
log.info("🟨 [PUBLISH] Publishing event: {}", eventDetails);
log.info("🟦 [DONE] Completed");
```

### Listener (3-4 log statements)

```java
log.info("🟩 [LISTENER] Received event: {}", eventType);
log.info("🟩 [LISTENER] Processing: {}", action);
log.info("🟩 [LISTENER] Completed: {}", result);
```

### Read Logs

```
grep "volunteerId-xyz" logs.txt
# Shows entire flow for that volunteer
```

---

## ✅ Verify Event Flow Works

### Quick Test (5 minutes)

1. **Start app**
   ```bash
   ./gradlew bootRun
   ```

2. **Call API** (toggle volunteer status)
   ```bash
   curl -X POST http://localhost:8080/api/volunteers/{id}/toggle \
     -H "Content-Type: application/json" \
     -d '{"isOnline": true}'
   ```

3. **Check logs**
   ```bash
   # Terminal: should see logs like
   🟦 [STEP 1] Fetching volunteer: userId=...
   🟦 [STEP 2] Updating...
   🟦 [STEP 3] Saving...
   🟨 [PUBLISH] Publishing...
   ```

4. **After 50ms, see listener logs**
   ```bash
   🟩 [LISTENER] Received event: volunteerId=...
   🟩 [LISTENER] Found missions: count=...
   🟩 [LISTENER] Activated: ...
   ```

**If you see all 4 groups of logs → Event flow works! ✅**

---

## 🐛 Debugging Event Flow Issues

### Problem 1: Listener not called

**Checklist**:
- [ ] Listener has `@Component` decorator
- [ ] Listener has `@EventListener` on method
- [ ] Event type matches (`VolunteerStatusChangedEvent`)
- [ ] Check logs for `🟩` (should appear 50ms after publish)

**Debug**:
```java
@EventListener
public void onVolunteerStatusChanged(VolunteerStatusChangedEvent event) {
    log.info("🟩 [LISTENER] RECEIVED EVENT"); // ← Add this first
    // ...
}
```

If this log appears → listener is being called ✓

### Problem 2: Listener called but not reacting

**Checklist**:
- [ ] Check `if (event.isOnline())` condition
- [ ] Data in event is correct
- [ ] Database query works
- [ ] Database save works

**Debug**:
```java
log.info("🟩 [LISTENER] Event received: isOnline={}", event.isOnline());
log.info("🟩 [LISTENER] Finding missions...");
List<Mission> missions = missionRepository.find(...);
log.info("🟩 [LISTENER] Found {} missions", missions.size()); // ← Does this appear?
```

### Problem 3: Event not published

**Checklist**:
- [ ] UseCase calls `eventPublisher.publish(event)`
- [ ] UseCase has `@Transactional`
- [ ] No exception before publish
- [ ] Check for `🟨 [PUBLISH]` log

**Debug**:
```java
log.info("🟦 [BEFORE PUBLISH]");
eventPublisher.publish(event);
log.info("🟨 [AFTER PUBLISH]"); // ← Did this appear?
```

---

## 🎯 Best Practice: Simple Logging Pattern

```java
// ========== USE CASE ==========
@Component
public class ToggleVolunteerStatusUseCase {
    
    @Transactional
    public void execute(UUID userId, ToggleStatusRequest request) {
        log.info("🟦 [USECASE] START: userId={}", userId);
        
        // Business logic
        Volunteer volunteer = repository.findByUserId(userId).orElseThrow();
        volunteer.setOnline(request.isOnline());
        repository.save(volunteer);
        
        // Publish event
        log.info("🟨 [PUBLISH] event: volunteerId={}, isOnline={}", 
            volunteer.getId(), request.isOnline());
        eventPublisher.publish(new VolunteerStatusChangedEvent(...));
        
        log.info("🟦 [USECASE] DONE");
        return mapper.toDTO(volunteer);
    }
}

// ========== LISTENER ==========
@Component
public class VolunteerStatusListener {
    
    @EventListener
    public void onVolunteerStatusChanged(VolunteerStatusChangedEvent event) {
        log.info("🟩 [LISTENER] START: volunteerId={}", event.getVolunteerId());
        
        // React
        List<Mission> missions = missionRepository
            .findByVolunteerId(event.getVolunteerId());
        log.info("🟩 [LISTENER] Found {} missions", missions.size());
        
        // Update
        for (Mission mission : missions) {
            mission.setStatus(ACTIVE);
            missionRepository.save(mission);
        }
        
        log.info("🟩 [LISTENER] DONE: activated {} missions", missions.size());
    }
}
```

---

## 📚 Reading Logs in IDE

### Application.properties (enable debug logs)

```properties
logging.level.root=INFO
logging.level.com.drc.aidbridge=DEBUG
logging.pattern.console=%d{HH:mm:ss.SSS} %msg%n
```

### In IDE (Run tab)

```
12:34:56.001 🟦 [STEP 1] Fetching volunteer: userId=abc
12:34:56.003 🟦 [STEP 2] Updating...
12:34:56.004 🟦 [STEP 3] Saving...
12:34:56.005 🟨 [PUBLISH] Publishing event
12:34:56.006 🟦 [DONE] UseCase completed

(background thread)

12:34:56.045 🟩 [LISTENER] Received event
12:34:56.046 🟩 [LISTENER] Processing...
12:34:56.047 🟩 [LISTENER] DONE
```

Click on any log → jump to that code! ✨

---

## ✅ Checklist: Verify Event Flow

- [ ] **Event class defined** at module root (public)
- [ ] **UseCase publishes** with color-coded log `🟨 [PUBLISH]`
- [ ] **Listener receives** with color-coded log `🟩 [LISTENER]`
- [ ] **Logs show in IDE** when you run API call
- [ ] **Listener runs ~50ms after** publish (shows in timestamps)
- [ ] **No circular dependencies** (Mission doesn't import Volunteer module, only event)

---

## TL;DR: Simple Event Flow Tracing

1. **Add logs with colors** 🟦🟨🟩
2. **Each log includes**: WHO (id), WHAT (action), WHEN (timestamp auto)
3. **Run API call**
4. **Check IDE logs** → see flow
5. **If log appears** → that step worked ✓
6. **If log missing** → debug that step ❌

**That's it!** Simple, effective, no magic. 🚀
