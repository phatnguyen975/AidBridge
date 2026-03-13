# AidBridge — Database Design v2.0

> **Engine:** PostgreSQL 15+ (Supabase) · **Extensions:** PostGIS, uuid-ossp  
> **Schema file:** `database/02_schema_v2.sql`

---

## Table of Contents

1. [Overview](#1-overview)
2. [Table Count & Group Map](#2-table-count--group-map)
3. [Normalization Strategy](#3-normalization-strategy)
4. [Group-by-Group Breakdown](#4-group-by-group-breakdown)
   - [4.1 ENUMs](#41-enums)
   - [4.2 System Config](#42-system-config)
   - [4.3 User & Auth](#43-user--auth)
   - [4.4 Role Profiles](#44-role-profiles)
   - [4.5 Infrastructure](#45-infrastructure)
   - [4.6 Item Catalog](#46-item-catalog)
   - [4.7 Attachments](#47-attachments)
   - [4.8 Inventory Management](#48-inventory-management)
   - [4.9 Request System](#49-request-system)
   - [4.10 Donation System](#410-donation-system)
   - [4.11 Mission & Dispatch](#411-mission--dispatch)
   - [4.12 Communication & Social](#412-communication--social)
5. [Entity Relationship Summary](#5-entity-relationship-summary)
6. [Denormalization Decisions](#6-denormalization-decisions)
7. [Indexing Strategy](#7-indexing-strategy)
8. [Scalability Notes](#8-scalability-notes)
9. [Key Business Rules Enforced at DB Level](#9-key-business-rules-enforced-at-db-level)

---

## 1. Overview

AidBridge uses a **single PostgreSQL schema** with **26 tables**, **13 ENUM types**, **3 trigger functions**, and **40+ indexes**. The design:

- Achieves **3NF** for all transactional tables.
- Applies **selective denormalization** for read-heavy analytics and dispatch performance (documented explicitly in [Section 6](#6-denormalization-decisions)).
- Uses **PostGIS `GEOMETRY(Point, 4326)`** columns for all GPS coordinates, enabling `ST_DWithin` radius queries natively.
- Replaces magic-string `VARCHAR + CHECK` patterns with **ENUMs** for full type safety.
- Enforces **business rules at the DB level** via triggers and CHECK constraints — not just the application layer.

---

## 2. Table Count & Group Map

**Total: 26 tables** across 10 functional groups.

| # | Group | Tables | Purpose |
|---|-------|--------|---------|
| 1 | **System Config** | `system_config` | Runtime-tunable algorithm parameters |
| 2 | **User & Auth** | `users`, `otp_verifications`, `refresh_tokens` | Identity, verification, JWT session management |
| 3 | **Role Profiles** | `volunteer_profiles`, `volunteer_area_experiences`, `sponsor_profiles` | Extended role-specific attributes |
| 4 | **Infrastructure** | `hubs`, `hub_accepted_categories`, `hub_staff`, `shelters`, `safe_paths` | Physical locations and routing cache |
| 5 | **Item Catalog** | `item_categories` | Two-level category tree for aid items |
| 6 | **Attachments** | `attachments` | Normalized file storage (replaces all `TEXT[]` arrays) |
| 7 | **Inventory** | `hub_inventories`, `inventory_logs` | Stock levels + immutable audit trail |
| 8 | **Request System** | `sos_requests`, `aid_requests`, `aid_request_items` | Victim emergency and supply requests |
| 9 | **Donation System** | `donations`, `donation_items` | Sponsor pledge lifecycle + line items |
| 10 | **Mission & Dispatch** | `missions`, `dispatch_attempts` | Volunteer work units + algorithm audit log |
| 11 | **Communication** | `chat_messages`, `ratings`, `notifications` | In-mission chat, review, and inbox |

---

## 3. Normalization Strategy

### 3NF Compliance

Every transactional table satisfies **Third Normal Form (3NF)**:

| 1NF | Every column holds atomic values. No repeating groups. `TEXT[]` arrays from v1 have been replaced with the `attachments` table and `donation_items` line items. |
|-----|---|
| **2NF** | All non-key columns depend on the **full** primary key. Composite keys (e.g., `hub_accepted_categories`) have no partial dependencies. |
| **3NF** | No transitive dependencies. For example, `hub_name` is **not** stored in `hub_inventories`; it is always fetched via JOIN from `hubs`. |

### BCNF Notes

- `hub_inventories` has a UNIQUE constraint on `(hub_id, item_category_id)` — the natural candidate key. The surrogate `id` UUID is added only for FK convenience from `inventory_logs`. This is standard practice and does not violate BCNF.
- `hub_staff` uses a **partial UNIQUE index** on `(hub_id, user_id) WHERE unassigned_at IS NULL` rather than a regular UNIQUE constraint, because the same staff can be re-assigned after being unassigned.

### Intentional Denormalization

See [Section 6](#6-denormalization-decisions) for the three cases where 3NF is deliberately relaxed for performance.

---

## 4. Group-by-Group Breakdown

---

### 4.1 ENUMs

**13 ENUM types** replace all `VARCHAR + CHECK` patterns from v1. ENUMs are the right tool when a column's domain is small, stable, and application-controlled.

| ENUM | Values | Used By |
|------|--------|---------|
| `user_role` | VICTIM, VOLUNTEER, SPONSOR, STAFF, ADMIN | `users.role` |
| `hub_status` | ACTIVE, INACTIVE, EMERGENCY | `hubs.status` |
| `shelter_status` | AVAILABLE, FULL, UNAVAILABLE | `shelters.status` |
| `urgency_level` | CRITICAL, HIGH, MEDIUM, LOW | `sos_requests`, `aid_requests` |
| `sos_status` | PENDING → DISPATCHING → ASSIGNED → IN_PROGRESS → COMPLETED / CANCELLED | `sos_requests.status` |
| `aid_request_status` | PENDING → DISPATCHING → ASSIGNED → PICKED_UP → IN_TRANSIT → COMPLETED / CANCELLED | `aid_requests.status` |
| `mission_type` | RESCUE, DELIVERY | `missions.mission_type` |
| `mission_status` | PENDING → DISPATCHING → ASSIGNED → PICKING_UP → PICKED_UP → IN_TRANSIT → COMPLETED / CANCELLED | `missions.status` |
| `donation_status` | REGISTERED → QR_GENERATED → RECEIVED / REJECTED | `donations.status` |
| `inventory_change_type` | INBOUND, OUTBOUND | `inventory_logs.change_type` |
| `inventory_reference_type` | DONATION, MISSION | `inventory_logs.reference_type` |
| `dispatch_type` | BROADCAST, SEQUENTIAL | `dispatch_attempts.dispatch_type` |
| `dispatch_response` | PENDING, ACCEPTED, REJECTED, TIMEOUT | `dispatch_attempts.response` |
| `message_type` | TEXT, IMAGE | `chat_messages.message_type` |
| `entity_type` | SOS_REQUEST, AID_REQUEST, DONATION, MISSION, HUB, USER | `attachments.entity_type`, `notifications.related_entity_type` |
| `notification_type` | 19 values covering all 6 user roles | `notifications.notification_type` |
| `otp_type` | EMAIL_VERIFICATION, PHONE_VERIFICATION, PASSWORD_RESET | `otp_verifications.otp_type` |
| `badge_level` | BRONZE, SILVER, GOLD, PLATINUM | `sponsor_profiles.badge_level` |

---

### 4.2 System Config

#### `system_config`

```
key (PK) VARCHAR | value TEXT | description TEXT | updated_by UUID | updated_at TIMESTAMPTZ
```

**Purpose:** Runtime-tunable key-value store. Admin can adjust algorithm weights, dispatch radii, and OTP TTLs **without restarting the Spring Boot service**.

**Why a table (not `application.yaml`):**
- Changes take effect immediately and are logged (`updated_by`, `updated_at`).
- Admin dashboard can expose a UI to tweak weights and observe real-time dispatch behavior.
- Spring Boot reads these at startup and can refresh every N minutes.

**Pre-seeded keys:**

| Key | Default | Meaning |
|-----|---------|---------|
| `dispatch.weight.distance` | `0.40` | D-factor in Priority Score formula |
| `dispatch.weight.rating` | `0.20` | R-factor |
| `dispatch.weight.tasks` | `0.15` | T-factor |
| `dispatch.weight.response_time` | `0.15` | A-factor |
| `dispatch.weight.area_experience` | `0.10` | E-factor |
| `dispatch.radius.step1_km` … `step4_km` | `1/3/5/10` | Expanding search radii |
| `dispatch.sos.window_seconds` | `30` | Broadcast accept window |
| `dispatch.delivery.window_1` | `15` | Top-1 exclusive window |
| `dispatch.delivery.window_batch` | `20` | Batch broadcast window |
| `otp.ttl_minutes` | `10` | OTP expiry |

---

### 4.3 User & Auth

#### `users`

```
id UUID PK | full_name | email UNIQUE | phone_number UNIQUE | password_hash
role user_role | is_verified | is_active | fcm_token | avatar_url
created_at | updated_at
```

**Key rules:**
- `password_hash` is bcrypt-hashed. Never stored plain.
- `CONSTRAINT users_contact_method_check`: at least one of `email` or `phone_number` must be non-null.
- `fcm_token` is updated on every login to point to the current device.
- `is_active = FALSE` is used by Admin to ban users (soft-block, not hard delete).
- Anonymous SOS submitters are **not** stored here — they appear only in `sos_requests` with `requester_id = NULL`.

#### `otp_verifications`

```
id UUID PK | user_id FK(users) | otp_code | otp_type | expires_at | is_used | created_at
```

**Purpose:** Single-use short-lived codes for email/phone verification and password reset.  
**Pattern:** Redis caches the active OTP for O(1) lookup; this table is the durable record for audit and replay protection.  
**Expiry:** Controlled by `system_config['otp.ttl_minutes']`.

#### `refresh_tokens`

```
id UUID PK | user_id FK(users) | token_hash VARCHAR(64) UNIQUE | device_info
expires_at | is_revoked | revoked_at | created_at
```

**Purpose:** Durable JWT refresh token store.

**Why needed (not in v1):** v1 relied solely on Redis for JWT blacklisting. If Redis restarts, all revoked tokens become valid again — a critical security gap. This table is the authoritative source of truth. **Only the SHA-256 hash of the token is stored**, so even a full DB leak cannot be used to forge sessions.

---

### 4.4 Role Profiles

#### `volunteer_profiles`

```
id UUID PK | user_id FK(users) UNIQUE | is_online BOOLEAN
current_location GEOMETRY(Point,4326) | vehicle_type
total_tasks_completed INT | avg_rating DECIMAL(3,2) | avg_response_seconds INT
last_active_at | created_at | updated_at
```

**Purpose:** Holds the static factors of the Priority Score formula:

$$S = (D \times 40\%) + (R \times 20\%) + (T \times 15\%) + (A \times 15\%) + (E \times 10\%)$$

| Column | Factor | Notes |
|--------|--------|-------|
| `current_location` | **D** | Distance computed at query time via `ST_Distance` |
| `avg_rating` | **R** | Rolling average, updated after each `ratings` insert |
| `total_tasks_completed` | **T** | Incremented on each `COMPLETED` mission |
| `avg_response_seconds` | **A** | Rolling average of dispatch acceptance times |
| *(see below)* | **E** | Per-area score, stored in `volunteer_area_experiences` |

**`is_online`** is the gate for dispatch: the algorithm only scans rows where `is_online = TRUE`. A partial GIST index on `current_location WHERE is_online = TRUE` makes this extremely fast.

#### `volunteer_area_experiences`

```
id UUID PK | volunteer_id FK(volunteer_profiles)
area GEOMETRY(Polygon,4326) | area_label VARCHAR(255)
experience_score DECIMAL(5,2) | missions_in_area INT | last_mission_at
created_at | updated_at
```

**Purpose:** Per-polygon E-factor for the Priority Score.

**Why a separate table (not a scalar in v1):**  
A single `area_experience_score` column is meaningless — it implies the same experience level everywhere. This table stores one row per geographic zone. The dispatch query joins on `ST_Contains(area, victim_location)` to fetch the relevant score for the specific location of the mission. A volunteer who has completed 50 missions in District 1 scores higher for a mission there even if their global task count is lower than a newcomer.

#### `sponsor_profiles`

```
id UUID PK | user_id FK(users) UNIQUE
total_points INT | total_items_donated INT | donation_count INT | badge_level
created_at | updated_at
```

**Purpose:** Gamification stats for the Sponsor badge system. Updated each time a `donations` row reaches `RECEIVED` status.

---

### 4.5 Infrastructure

#### `hubs`

```
id UUID PK | name | address | location GEOMETRY(Point,4326)
status hub_status | contact_phone | notes | created_at | updated_at
```

Physical relief transfer stations. All donations flow **in** through hubs; all volunteer pickups flow **out**.

#### `hub_accepted_categories`

```
hub_id FK(hubs) + item_category_id FK(item_categories) → COMPOSITE PK
```

**Purpose:** Admin-configurable per-hub item allowlist.

**Why needed:** Requirements state *"Admin có thể thiết lập danh mục nhận hàng cho từng trạm"*. Without this table, Smart Hub Selection cannot know which hubs actually accept rice vs. medicine. The junction table makes the recommendation accurate.

**Smart Hub Selection query shape:**
```sql
SELECT h.id, ST_Distance(h.location, $sponsor_gps) AS dist
FROM hubs h
JOIN hub_accepted_categories hac ON hac.hub_id = h.id
JOIN hub_inventories hi ON hi.hub_id = h.id AND hi.item_category_id = hac.item_category_id
WHERE h.status = 'ACTIVE'
  AND hac.item_category_id = $requested_category
  AND hi.current_quantity <= hi.low_stock_threshold  -- hub actually needs this item
ORDER BY dist
LIMIT 3;
```

#### `hub_staff`

```
id UUID PK | hub_id FK(hubs) | user_id FK(users)
assigned_at | unassigned_at
```

**Partial UNIQUE index:** `(hub_id, user_id) WHERE unassigned_at IS NULL`  
→ Prevents duplicate active assignments but allows re-assignment after unassignment.

**CHECK constraint:** Only `ROLE = 'STAFF'` users may be inserted.

#### `shelters`

```
id UUID PK | name | address | location GEOMETRY(Point,4326)
current_capacity INT | max_capacity INT
has_electricity BOOLEAN | has_clean_water BOOLEAN
status shelter_status | notes | created_at | updated_at
```

**CHECK:** `0 ≤ current_capacity ≤ max_capacity`.

#### `safe_paths`

```
id UUID PK | origin GEOMETRY(Point,4326) | destination GEOMETRY(Point,4326)
path_line GEOMETRY(LineString,4326) | distance_m | duration_sec
is_active BOOLEAN | expires_at TIMESTAMPTZ | created_at
```

**Purpose:** Cache for routing polylines shown on the public map (Safe Path markers).

**Why a table:** Rather than calling Google Directions API on every map load, the backend computes the path once and caches it here with a 6-hour TTL. A background job marks rows `is_active = FALSE` when a hub goes `EMERGENCY` or the TTL expires. Clients read from this cache; only a miss triggers a fresh API call.

---

### 4.6 Item Catalog

#### `item_categories`

```
id UUID PK | parent_id FK(self) | name | name_vi | unit | is_leaf BOOLEAN
created_at | updated_at
```

**Two-level tree:**

```
Medicine (is_leaf=FALSE)
├── Fever/Cold      (is_leaf=TRUE)
├── Digestive       (is_leaf=TRUE)
└── Bandages        (is_leaf=TRUE)
Food (is_leaf=FALSE)
├── Rice            (is_leaf=TRUE)
├── Instant Noodles (is_leaf=TRUE)
└── Canned Food     (is_leaf=TRUE)
... (Clothing, Water, Other)
```

**Leaf enforcement:** A trigger `fn_mark_parent_non_leaf` automatically sets `parent.is_leaf = FALSE` when a child row is inserted. A second trigger `fn_assert_leaf_category` raises an exception if a non-leaf `item_category_id` is used in `hub_inventories`, `aid_request_items`, or `donation_items`. This guarantees stock is always tracked at the most granular level.

---

### 4.7 Attachments

#### `attachments`

```
id UUID PK | entity_type entity_type | entity_id UUID
url VARCHAR(512) | mime_type | file_size_bytes INT
uploaded_by FK(users) | created_at
```

**Replaces `TEXT[]` arrays from v1** (`image_urls TEXT[]` in `sos_requests`, `aid_requests`, `donations`).

**Polymorphic reference:** `(entity_type, entity_id)` points to any entity. No FK is enforced at DB level (polymorphic pattern is inherently non-FK), but a composite index on `(entity_type, entity_id)` makes lookups fast.

**Benefits over `TEXT[]`:**

| v1 `TEXT[]` | v2 `attachments` |
|-------------|-----------------|
| No metadata per file | `mime_type`, `file_size_bytes`, `uploaded_by` per file |
| Cannot delete individual files by query | `DELETE FROM attachments WHERE id = $x` |
| Array operators needed (`@>`, `ANY`) | Simple `WHERE entity_type = 'SOS_REQUEST' AND entity_id = $id` |
| No audit trail | `uploaded_by`, `created_at` per file |

---

### 4.8 Inventory Management

#### `hub_inventories`

```
id UUID PK | hub_id FK(hubs) | item_category_id FK(item_categories)
current_quantity INT | low_stock_threshold INT
UNIQUE(hub_id, item_category_id) | created_at | updated_at
```

**One row per (hub, item category) pair.** Updated atomically:
- `+quantity` when Staff scans a donation QR → `INBOUND`
- `-quantity` when Staff scans a volunteer pickup QR → `OUTBOUND`

**`low_stock_threshold`** (renamed from `min_threshold` in v1 for clarity): When `current_quantity ≤ low_stock_threshold`, a `STOCK_LOW` notification fires to Staff and a `STOCK_CRITICAL` fires to Admin.

**Trigger:** `fn_hub_inventories_leaf_check` prevents inserting a parent category as a stock slot.

#### `inventory_logs`

```
id UUID PK | hub_inventory_id FK | change_type inventory_change_type
quantity_delta INT | reference_type inventory_reference_type | reference_id UUID
performed_by FK(users) | quantity_after INT | notes | created_at
```

**Append-only audit trail.** Never UPDATE or DELETE rows.

**`quantity_after`** (new in v2): Snapshot of stock level after the transaction. This means any point-in-time stock level can be reconstructed from the log alone, without replaying all deltas — useful for Admin reports and fraud detection.

**`reference_type` ENUM** (was `VARCHAR + CHECK` in v1): `'DONATION'` or `'MISSION'` — tells the application which table `reference_id` points to.

---

### 4.9 Request System

#### `sos_requests`

```
id UUID PK | requester_id FK(users) nullable
requester_name | requester_phone
victim_name | victim_phone (when is_on_behalf=TRUE)
victim_location GEOMETRY(Point,4326) | victim_address
description | people_count | is_on_behalf BOOLEAN
urgency_level | ai_summary | status sos_status
cancellation_reason | resolved_at | created_at | updated_at
```

**Anonymous-friendly:** `requester_id` is nullable — Guest users do not need an account.

**`is_on_behalf`:** TRUE when a registered Victim submits on behalf of a different person (Requirement 2.3). `victim_name` and `victim_phone` hold the third party's details in that case.

**`ai_summary`:** Backend calls AI to classify the SOS text and generates a concise summary for Admin dashboard display.

#### `aid_requests` + `aid_request_items`

**Header (`aid_requests`):**
```
id UUID PK | requester_id FK(users) NOT NULL
location GEOMETRY(Point,4326) | address
adults_count | elderly_count | children_count
notes | urgency_level | ai_summary | status | resolved_at | created_at | updated_at
CHECK: (adults + elderly + children) > 0
```

**Line items (`aid_request_items`):**
```
id UUID PK | aid_request_id FK CASCADE | item_category_id FK | quantity
CHECK: quantity > 0
```

**Line-item model** allows one request to ask for multiple item types. Quantities are auto-calculated from person counts by the backend (e.g., 2 adults + 1 child = 3 packs of rice, 2 sets of clothing, etc.).

---

### 4.10 Donation System

#### `donations` + `donation_items`

**Header (`donations`):**
```
id UUID PK | sponsor_id FK(users) | hub_id FK(hubs)
description | estimated_delivery_at
qr_code_token VARCHAR(512) UNIQUE | status donation_status
received_by FK(users) | received_at | rejection_reason
created_at | updated_at
```

**Line items (`donation_items`):**
```
id UUID PK | donation_id FK CASCADE | item_category_id FK
quantity | expiry_date DATE | condition_notes TEXT
CHECK: quantity > 0
```

**Why header + items (not single row as in v1):**

> A sponsor donating Rice, Medicine, and Blankets in one trip gets **one QR code**, **one hub visit**, **one staff scan**. In v1 this required 3 separate `donations` rows and 3 QR codes — unrealistic and bad UX.

**Lifecycle:**  
`REGISTERED` → `QR_GENERATED` → `RECEIVED` (inventory updated) | `REJECTED` (reason logged)

---

### 4.11 Mission & Dispatch

#### `missions`

```
id UUID PK | mission_type mission_type
sos_request_id FK (RESCUE only) | aid_request_id FK (DELIVERY only)
volunteer_id FK(volunteer_profiles) | hub_id FK(hubs) (DELIVERY only)
status mission_status | qr_code_token UNIQUE
priority_score DECIMAL(8,4)
dispatched_at | accepted_at | picked_up_at | delivered_at | completed_at | cancelled_at
cancellation_reason | confirmation_image_url
created_at | updated_at
```

**Two CHECK constraints enforce business rules:**
1. `missions_type_request_integrity`: RESCUE must link SOS; DELIVERY must link AID request — never both.
2. `missions_hub_delivery_only`: DELIVERY must have a hub; RESCUE must not.

**`volunteer_id` → `volunteer_profiles(id)`** (changed from `users(id)` in v1): Guarantees the assigned user has a volunteer profile and makes Priority Score joins cheaper.

**`priority_score` snapshot:** The winning volunteer's score at acceptance time is stored for A/B testing and algorithm tuning by Admin. This is intentional denormalization (see Section 6).

**Status flow:**

```
RESCUE:    PENDING → DISPATCHING → ASSIGNED → IN_TRANSIT → COMPLETED
DELIVERY:  PENDING → DISPATCHING → ASSIGNED → PICKING_UP → PICKED_UP → IN_TRANSIT → COMPLETED
```

#### `dispatch_attempts`

```
id UUID PK | mission_id FK CASCADE | volunteer_id FK(volunteer_profiles)
dispatch_type | batch_number INT | radius_km DECIMAL(5,2)
priority_score DECIMAL(8,4) | sent_at | response dispatch_response | responded_at
```

**Full audit log** for every volunteer notification. Enables:
- Admin to see exactly who was notified, in what order, at what radius.
- Post-hoc analysis of dispatch efficiency.
- Algorithm tuning (why did we go to 10km radius before finding someone?).

**For BROADCAST:** one row per notified volunteer, `batch_number = 1`.  
**For SEQUENTIAL:** `batch_number = 1` (top volunteer), `2` (next trio), `3` (wider radius), etc.

---

### 4.12 Communication & Social

#### `chat_messages`

```
id UUID PK | mission_id FK CASCADE | sender_id FK(users)
message_type message_type | message_text TEXT | attachment_id FK(attachments)
is_read BOOLEAN | created_at
CHECK: TEXT has text+no attachment; IMAGE has attachment+no text
```

Mission-scoped. Full chat history is preserved with the mission record even after completion.

#### `ratings`

```
id UUID PK | mission_id FK UNIQUE | rater_id FK(users) | ratee_id FK(volunteer_profiles)
score INT (1–5) | comment TEXT | created_at
```

`UNIQUE(mission_id)` enforces one rating per completed mission. `ratee_id` references `volunteer_profiles` (not `users`) so the backend can efficiently update `avg_rating` without an extra lookup.

#### `notifications`

```
id UUID PK | user_id FK CASCADE | notification_type | title | body
related_entity_type entity_type | related_entity_id UUID
is_read BOOLEAN | created_at
```

Persistent inbox. FCM push fires in parallel — this table enables in-app history and unread badge counts. `related_entity_type` + `related_entity_id` is a **polymorphic reference** using the `entity_type` ENUM (type-safe, unlike v1's raw VARCHAR).

---

## 5. Entity Relationship Summary

```
users ──────────────────────────────────────────────────────────────────────
  │ 1:1  volunteer_profiles ──── 1:N  volunteer_area_experiences
  │ 1:1  sponsor_profiles
  │ 1:N  otp_verifications
  │ 1:N  refresh_tokens
  │ 1:N  sos_requests (requester, nullable)
  │ 1:N  aid_requests (requester)
  │ 1:N  donations    (sponsor)
  │ 1:N  notifications
  │ 1:N  hub_staff    (staff assignment)
  └──────────────────────────────────────────────────────────────────────────

hubs ───────────────────────────────────────────────────────────────────────
  │ 1:N  hub_staff
  │ 1:N  hub_accepted_categories ── M:N ── item_categories
  │ 1:N  hub_inventories ─── 1:N  inventory_logs
  └──────────────────────────────────────────────────────────────────────────

item_categories (self-referencing tree)
  └─ 1:N  aid_request_items
  └─ 1:N  donation_items
  └─ 1:N  hub_inventories

sos_requests ──── 1:1  missions (RESCUE)
aid_requests ──── 1:1  missions (DELIVERY)
              └── 1:N  aid_request_items

missions ───────────────────────────────────────────────────────────────────
  │ 1:N  dispatch_attempts
  │ 1:N  chat_messages
  │ 1:1  ratings
  └──────────────────────────────────────────────────────────────────────────

donations ──── 1:N  donation_items

attachments (polymorphic: entity_type + entity_id)
  ← sos_requests, aid_requests, donations, users, missions
```

---

## 6. Denormalization Decisions

The following columns violate strict 3NF **intentionally** for performance or business reasons:

| Column | Table | Why denormalized | Trade-off |
|--------|-------|-----------------|-----------|
| `priority_score` | `missions` | Snapshot of the winning volunteer's score **at acceptance time**. The actual score changes as the volunteer completes more missions. Storing it here preserves the historical decision for Admin analytics and algorithm A/B testing. | Data diverges from the current live score over time — this is desired behavior. |
| `priority_score` | `dispatch_attempts` | Same reasoning — records the score of each notified volunteer at the time of dispatch for algorithm audit. | Same as above. |
| `quantity_after` | `inventory_logs` | Stock level snapshot after each transaction. Strictly derivable by replaying `quantity_delta` from the beginning. Stored here so Admin can query any point-in-time stock level in O(1) instead of O(n) replay. | Adds one INT write per inventory transaction. Worth it. |
| `avg_rating`, `total_tasks_completed`, `avg_response_seconds` | `volunteer_profiles` | Aggregates derivable from `ratings` and `missions`. Stored here so the dispatch algorithm can compute Priority Score in a **single table scan** instead of expensive aggregation queries under real-time dispatch pressure. | Must be kept in sync via application-layer updates on every mission completion. |
| `total_points`, `total_items_donated`, `donation_count` | `sponsor_profiles` | Same reasoning — gamification dashboard reads require instant access without aggregating all `donations`. | Must be incremented atomically in the same transaction that marks a donation `RECEIVED`. |
| `requester_name`, `requester_phone` | `sos_requests` | Duplicates user data for authenticated submitters. Required because anonymous (Guest) submitters have no `users` row — so name/phone must always be stored in the SOS row to handle both cases uniformly. | Minor redundancy for authenticated users; unified access pattern for all SOS records. |

---

## 7. Indexing Strategy

### Spatial (GIST) — PostGIS radius queries

| Index | Column | Used By |
|-------|--------|---------|
| `idx_volunteer_location` | `volunteer_profiles.current_location` | Dispatch radius scan (`ST_DWithin`) — hottest index |
| `idx_volunteer_area_exp_area` | `volunteer_area_experiences.area` | E-factor join (`ST_Contains`) |
| `idx_hubs_location` | `hubs.location` | Smart Hub Selection, map markers |
| `idx_shelters_location` | `shelters.location` | Public map, nearest-shelter routing |
| `idx_sos_requests_victim_location` | `sos_requests.victim_location` | Dispatch + heatmap aggregation |
| `idx_aid_requests_location` | `aid_requests.location` | Dispatch + heatmap aggregation |
| `idx_safe_paths_origin/destination` | `safe_paths.origin/destination` | Nearest cached path lookup |

### Partial Indexes — filter to relevant rows only

| Index | Condition | Benefit |
|-------|-----------|---------|
| `idx_volunteers_online` | `WHERE is_online = TRUE` | Only online volunteers are dispatch candidates. Typical: 5% of volunteers are online. Index is tiny and fast. |
| `idx_dispatch_pending` | `WHERE response = 'PENDING'` | Timeout checker only cares about unresponded attempts. |
| `idx_hub_inv_low_stock` | `WHERE current_quantity <= low_stock_threshold` | Low-stock alerting scans only the minority of slots that are actually low. |
| `idx_otp_active` | `WHERE is_used = FALSE` | OTP validation only touches unused codes. |
| `idx_refresh_tokens_active` | `WHERE is_revoked = FALSE` | Token validation only touches active tokens. |
| `idx_chat_unread` | `WHERE is_read = FALSE` | Unread message badge counts. |
| `idx_notifications_unread` | `WHERE is_read = FALSE` | Notification badge counts. |
| `idx_safe_paths_active` | `WHERE is_active = TRUE` | Map rendering ignores expired/invalidated paths. |
| `idx_hub_staff_active_unique` | `WHERE unassigned_at IS NULL` | Prevents duplicate active assignments (unique partial). |

### Composite Indexes — optimized for exact query shapes

| Index | Columns | Query Pattern |
|-------|---------|---------------|
| `idx_notifications_user` | `(user_id, created_at DESC)` | "Fetch my notifications, newest first" |
| `idx_chat_mission` | `(mission_id, created_at DESC)` | "Fetch messages for mission X, oldest first" |
| `idx_inv_logs_hub_inv` | `(hub_inventory_id, created_at DESC)` | "Fetch audit log for this stock slot" |
| `idx_inv_logs_ref` | `(reference_id, reference_type)` | "Which inventory changes did this donation cause?" |
| `idx_attachments_entity` | `(entity_type, entity_id)` | "Fetch all attachments for SOS #X" |

---

## 8. Scalability Notes

### High-Growth Tables

These four tables will grow unbounded in production. Each has a note in the schema for partitioning:

| Table | Growth Driver | Recommended Strategy |
|-------|--------------|---------------------|
| `inventory_logs` | Every stock transaction | `PARTITION BY RANGE (created_at)` — monthly partitions |
| `dispatch_attempts` | Every volunteer notification | `PARTITION BY RANGE (sent_at)` — monthly partitions |
| `chat_messages` | Every in-mission message | `PARTITION BY RANGE (created_at)` + archive after 90 days |
| `notifications` | Every system event | `PARTITION BY RANGE (created_at)` + archive after 90 days |

### Redis Layer

The following state is **not in PostgreSQL** — it lives in Redis for sub-millisecond access:

| Data | Redis Structure | TTL |
|------|----------------|-----|
| Active OTP code | `String` key `otp:{user_id}:{type}` | From `system_config['otp.ttl_minutes']` |
| JWT blacklist | `Set` | Until token expiry |
| Dispatch state machine | `Hash` key `dispatch:{mission_id}` | Until mission resolves |
| Volunteer location (live tracking) | `Geo` structure | Until mission completes |
| WebSocket pub/sub | Redis Pub/Sub channels | Session lifetime |

PostgreSQL holds the **durable truth**; Redis holds the **hot path**.

### PostGIS Performance

- All `GEOMETRY` columns use **SRID 4326** (WGS84 — standard GPS coordinates).
- `ST_DWithin` is used in preference to `ST_Distance < X` because it takes advantage of the GIST index.
- Volunteer dispatch query uses `ST_DWithin(current_location, $center, $radius_meters)` with expanding radii read from `system_config`.

---

## 9. Key Business Rules Enforced at DB Level

These rules are enforced by constraints and triggers — **not only by the application layer**:

| Rule | Mechanism |
|------|-----------|
| At least email OR phone per user | `CHECK (email IS NOT NULL OR phone_number IS NOT NULL)` on `users` |
| OTP is single-use | `is_used BOOLEAN` + application sets TRUE on first use |
| Inventory never goes negative | `CHECK (current_quantity >= 0)` on `hub_inventories` |
| Only leaf categories in stock/requests/donations | `fn_assert_leaf_category` trigger on 3 tables |
| Parent category auto-marked non-leaf on child insert | `fn_mark_parent_non_leaf` trigger on `item_categories` |
| RESCUE mission cannot have a hub; DELIVERY must | `missions_hub_delivery_only` CHECK |
| Mission links exactly one request type | `missions_type_request_integrity` CHECK |
| One rating per mission | `UNIQUE(mission_id)` on `ratings` |
| One active staff assignment per hub per person | Partial UNIQUE index on `hub_staff` |
| Only STAFF role users assigned to hubs | `CHECK (EXISTS SELECT 1 FROM users WHERE role = 'STAFF')` on `hub_staff` |
| Inventory log delta always positive | `CHECK (quantity_delta > 0)` on `inventory_logs` |
| Rating score 1–5 only | `CHECK (score >= 1 AND score <= 5)` on `ratings` |
| Volunteer rating 1.00–5.00 | `CHECK (avg_rating >= 1.00 AND avg_rating <= 5.00)` on `volunteer_profiles` |
| Shelter never exceeds max capacity | `CHECK (current_capacity <= max_capacity)` on `shelters` |
| SOS people count > 0 | `CHECK (people_count > 0)` on `sos_requests` |
| Donation/AID item quantity > 0 | `CHECK (quantity > 0)` on `donation_items`, `aid_request_items` |
| Refresh token stored as hash only | Application-enforced: never write raw token to DB |
| `updated_at` always current | `fn_set_updated_at` trigger on all mutable tables |
