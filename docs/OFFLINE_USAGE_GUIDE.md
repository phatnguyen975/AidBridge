# AidBridge Offline Features Guide

This document defines how to implement two high-priority offline features for AidBridge:

1. Offline routing: user can find route from A to B without internet.
2. Offline SOS outbox: user can submit SOS offline, and app auto-sends when internet returns.

The design follows current frontend constraints:

1. Java 17.
2. XML + ViewBinding.
3. MVVM + Clean Architecture.
4. Room for local persistence.
5. Retrofit for online APIs.
6. WorkManager for deferred sync.

## 1. Offline-First Principles For Aid Scenarios

1. Never block critical actions because of connectivity.
2. Save first locally, sync second when possible.
3. Make sync idempotent to avoid duplicate SOS.
4. Show clear state to users: Offline saved, Waiting sync, Sent.
5. Keep core rescue workflows available with degraded mode.

## 2. Feature A: Offline Route A -> B

### 2.1 Recommended Free Stack

Use an on-device routing engine with OpenStreetMap data.

Options:

1. GraphHopper (recommended for Android offline navigation).
2. OSRM local bundle (heavier setup on mobile).

Recommendation: start with GraphHopper because integration and mobile packaging are more practical for MVP.

### 2.2 What Data Must Be Stored Offline

1. Region routing graph files (prebuilt or downloaded packs).
2. Optional map tiles for visualization.
3. Region metadata (version, size, updatedAt, checksum).

Room entity example fields:

1. regionCode
2. graphVersion
3. localPath
4. checksum
5. downloadedAt
6. isActive

### 2.3 Routing Workflow

1. User picks start and end point.
2. ViewModel triggers RouteUseCase.
3. RouteUseCase checks connectivity and local graph availability.
4. If online: call backend route API (current behavior).
5. If offline and graph exists: compute route locally.
6. If offline and graph missing: show message and allow download request when online.
7. Render route with different badge:
	1. Online Route
	2. Offline Route

### 2.4 Clean Architecture Placement

Domain:

1. RouteRepository interface exposes getRoute(params).
2. RouteUseCase decides source by network + local availability.

Data:

1. RemoteRouteDataSource uses Retrofit backend API.
2. LocalRouteDataSource wraps GraphHopper engine.
3. RegionPackRepository handles map/graph packs.

UI:

1. Fragment remains display-only.
2. ViewModel exposes RouteState LiveData.
3. UI shows route origin label: Online or Offline.

### 2.5 Practical Notes

1. Preload critical disaster-prone regions first.
2. Do not download huge nationwide packs by default.
3. Add pack versioning to support safe updates.
4. Validate coordinate bounds before local routing.

## 3. Feature B: Offline SOS + Auto Send On Reconnect

### 3.1 Core Pattern: Transactional Outbox On Mobile

When user taps Send SOS:

1. Always persist SOS locally first.
2. Mark status as PENDING.
3. Try immediate sync only if internet is available.
4. If offline, return success to UI as Saved Offline.

This guarantees no SOS is lost due to temporary network issues.

### 3.2 Room Outbox Schema

Create SOS outbox table with fields:

1. localId (UUID, primary key)
2. idempotencyKey (UUID, unique)
3. payloadJson
4. mediaLocalUris
5. latitude
6. longitude
7. createdAt
8. status (PENDING, SYNCING, SENT, FAILED_RETRY, FAILED_PERMANENT)
9. retryCount
10. nextRetryAt
11. lastError

### 3.3 Sync Workflow

1. ConnectivityMonitor detects network available.
2. WorkManager job starts SyncPendingSosWorker.
3. Worker pulls batch of PENDING and FAILED_RETRY rows.
4. For each row:
	1. Update status to SYNCING.
	2. Call backend SOS API with idempotencyKey.
	3. If success, mark as SENT.
	4. If transient error, set FAILED_RETRY with exponential backoff.
	5. If permanent validation error, set FAILED_PERMANENT.
5. Stop when queue is empty.

### 3.4 Backend Requirements For Safe Auto-Retry

Your backend should support idempotent SOS creation:

1. Accept idempotency key from client.
2. If same key is received again, return existing SOS result instead of creating duplicate.
3. Return stable response for retried requests.

Without this, auto-retry can create duplicate SOS tickets.

### 3.5 UX Rules For Trust In Emergency Context

1. After offline submit, show clear message: SOS saved offline, will auto-send when connected.
2. Show queue count in UI: for example Pending SOS: 2.
3. Show sent timestamp after successful sync.
4. Allow user to edit or cancel pending SOS before sync.

## 4. End-To-End State Model

### 4.1 Connectivity State

1. ONLINE
2. OFFLINE
3. DEGRADED (optional, low quality network)

### 4.2 Route State

1. ROUTE_ONLINE
2. ROUTE_OFFLINE
3. ROUTE_UNAVAILABLE

### 4.3 SOS State

1. DRAFT
2. PENDING
3. SYNCING
4. SENT
5. FAILED_RETRY
6. FAILED_PERMANENT

## 5. Implementation Plan (Suggested)

### Phase 1: SOS Offline Outbox First (Fastest Impact)

1. Add Room outbox table + DAO.
2. Add OutboxRepository and SyncPendingSosWorker.
3. Add idempotency key to SOS API contract.
4. Add UI indicators for pending/sent state.
5. Add retry policy with exponential backoff.

### Phase 2: Offline Routing Core

1. Integrate GraphHopper local routing module.
2. Add region pack manager and metadata storage.
3. Add RouteUseCase source switching logic.
4. Add route source badge in map UI.

### Phase 3: Hardening

1. Add queue telemetry and sync diagnostics.
2. Add corruption checks for local graph files.
3. Add integration tests for reconnect and retry flows.
4. Add emergency fallback texts for unavailable route.

## 6. Testing Checklist

### 6.1 Offline Route

1. Airplane mode + region pack exists -> route must be generated.
2. Airplane mode + no region pack -> clear guidance must be shown.
3. Online mode -> backend route still works.

### 6.2 Offline SOS

1. Airplane mode submit -> row created with PENDING.
2. Network reconnect -> worker sends and marks SENT.
3. Retry on timeout -> no duplicate SOS on server.
4. App restart with pending queue -> sync resumes correctly.

## 7. Risks And Mitigation

1. Large offline data size:
	1. Use per-region packs and user-controlled downloads.
2. Duplicate SOS from retries:
	1. Enforce idempotency key at backend.
3. Battery impact from aggressive sync:
	1. Use WorkManager constraints and batching.
4. Stale routing data:
	1. Add pack versioning and update prompts.

## 8. Definition Of Done

Both features are complete when:

1. User can compute route offline in preloaded regions.
2. User can submit SOS offline without losing data.
3. App auto-synces pending SOS after reconnect.
4. No duplicate SOS appears under retry conditions.
5. UI clearly reflects offline and sync states.

---

Summary:

1. Implement SOS offline outbox first for immediate safety value.
2. Implement offline routing with downloadable region packs.
3. Combine Room + WorkManager + idempotent backend API for reliable emergency behavior.
