# README - Pure Pseudocode Call Flow (AID + SOS)

## 0) Thread Model

```text
REQUEST_THREAD = HTTP thread handling API request
ASYNC_THREAD   = thread from missionTaskExecutor (prefix: mission-async-*)
```

## 1) AID Flow (Create Aid Request -> Create Mission -> Dispatch)

### 1.1 Call Graph

```text
REQUEST_THREAD
POST /api/aid-requests
  -> AidController.createAidRequest
     -> CreateAidRequestUseCase.execute [TX]
        -> AidRequestJpaRepository.save
        -> AidRequestItemJpaRepository.saveAll
        -> ApplicationEventPublisher.publishEvent(AidRequestCreatedEvent)
     -> return ApiResponse.success

REQUEST_THREAD (after transaction commit)
  -> AidRequestCreatedEventListener.handleAidRequestCreated [AFTER_COMMIT]
     -> AidRequestMissionDispatchAsyncProcessor.process (async invocation)

ASYNC_THREAD
  -> AidRequestMissionDispatchAsyncProcessor.process [TX REQUIRES_NEW]
     -> MissionFacade.createDeliveryMission
     -> DispatchMissionUseCase.execute
     -> done
```

### 1.2 Function-Level Pseudocode

```text
function AidController.createAidRequest(request, jwt):
    userId = UUID.fromString(jwt.subject)
    response = CreateAidRequestUseCase.execute(userId, request)
    return ApiResponse.success("Aid request created", response)

function CreateAidRequestUseCase.execute(userId, request) [transactional]:
    validate(request)
    savedAid = aidRequestRepository.save(buildAidEntity(userId, request))
    savedItems = aidRequestItemRepository.saveAll(mapItems(request.items, savedAid))

    lat = savedAid.location?.y
    lng = savedAid.location?.x
    publishEvent(AidRequestCreatedEvent(savedAid.id, lat, lng))

    return mapAidResponse(savedAid, savedItems, mission=null)

function AidRequestCreatedEventListener.handleAidRequestCreated(event) [after_commit]:
    aidRequestMissionDispatchAsyncProcessor.process(event)

function AidRequestMissionDispatchAsyncProcessor.process(event) [async, requires_new_tx]:
    require event.lat != null and event.lng != null
    mission = missionFacade.createDeliveryMission(event.aidRequestId, event.lat, event.lng)
    dispatchMissionUseCase.execute(mission.id, preferredVolunteerIds=null)
```

## 2) SOS Flow (Create SOS Request -> Create Mission -> Dispatch)

### 2.1 Call Graph

```text
REQUEST_THREAD
POST /api/sos-requests (or equivalent SOS create endpoint)
  -> SosController / CreateSosRequestUseCase.execute [TX]
     -> SosRequestJpaRepository.save
     -> ApplicationEventPublisher.publishEvent(SosRequestCreatedEvent)
  -> return SosRequestResponse

REQUEST_THREAD (after transaction commit)
  -> SosRequestCreatedEventListener.handleSosRequestCreated [AFTER_COMMIT]
     -> SosRequestMissionDispatchAsyncProcessor.process (async invocation)

ASYNC_THREAD
  -> SosRequestMissionDispatchAsyncProcessor.process [TX REQUIRES_NEW]
     -> MissionFacade.createRescueMission
     -> DispatchMissionUseCase.execute
     -> done
```

### 2.2 Function-Level Pseudocode

```text
function CreateSosRequestUseCase.execute(userId, request) [transactional]:
    validate(request)
    savedSos = sosRepository.save(buildSosEntity(userId, request))
    publishEvent(SosRequestCreatedEvent(savedSos.id, savedSos.lat, savedSos.lng))
    return mapSosResponse(savedSos)

function SosRequestCreatedEventListener.handleSosRequestCreated(event) [after_commit]:
    sosRequestMissionDispatchAsyncProcessor.process(event)

function SosRequestMissionDispatchAsyncProcessor.process(event) [async, requires_new_tx]:
    require event.lat != null and event.lng != null
    mission = missionFacade.createRescueMission(event.sosRequestId, event.lat, event.lng)
    dispatchMissionUseCase.execute(mission.id, preferredVolunteerIds=null)
```

## 3) Async Executor Pseudocode

```text
bean missionTaskExecutor:
    corePoolSize = 2
    maxPoolSize = 8
    queueCapacity = 200
    threadNamePrefix = "mission-async-"
```

## 4) Fast Verification Flow

```text
step 1: call create aid/sos API
step 2: verify API response returns quickly
step 3: verify logs continue on thread mission-async-*
step 4: verify mission created + dispatch executed
```
