# AidBridge — Database Design v3.1

> **Engine:** PostgreSQL 15+  ·  **Extension:** `uuid-ossp`
> **Design principle:** 3NF cho tất cả bảng transactional; denorm có chủ ý cho read-heavy aggregates; query đơn giản và nhanh là ưu tiên hàng đầu.
> **Logical ERD:** [erd_v2.md](./erd_v2.md) · **Physical Schema Diagram:** [physical_database.md](./physical_database.md)

---

## 1. Schema Overview

**23 bảng · 10 kiểu ENUM · ~38 index**

| Group | Bảng |
|-------|------|
| **Auth** | `users`, `refresh_tokens` |
| **Profiles** | `volunteer_profiles`, `sponsor_profiles` |
| **Infrastructure** | `hubs`, `hub_staff`, `shelters`, `system_config` |
| **Catalog & Inventory** | `item_categories`, `hub_accepted_categories`, `hub_inventories`, `inventory_logs` |
| **Requests** | `sos_requests`, `sos_request_details`\*, `aid_requests`, `aid_request_items` |
| **Donations** | `donations`, `donation_items` |
| **Missions** | `missions`, `dispatch_attempts` |
| **Communication** | `chat_messages`, `ratings`, `notifications` |

> \* `sos_request_details` — bảng mới (v3.1): vertical split từ `sos_requests`, chứa các cột TEXT ít truy vấn.

**Thay đổi so với v3.0:**

| Thay đổi | Lý do |
|----------|-------|
| Tách `sos_requests` → `sos_requests` + `sos_request_details` | Vertical split: hot columns (dispatch, heatmap) tách khỏi cold TEXT columns (description, ai_summary) |
| Thêm `snapshot_*` columns vào `missions` | Denorm: Live Tracking & Mission Screen cần destination info liên tục, tránh JOIN sang sos/aid tables |
| Thêm `hub_name` vào `donations` | Denorm: QR Screen cần hub name mà không JOIN |

**Thay đổi so với v2 (26 bảng):**

| Bỏ | Lý do |
|----|-------|
| `otp_verifications` | OTP fields gộp thẳng vào `users` — loại bỏ JOIN, đủ dùng cho MVP |
| `volunteer_area_experiences` | Quá phức tạp cho MVP; E-factor = 0 trong Priority Score lúc đầu |
| `attachments` (polymorphic) | Thay bằng `image_url VARCHAR(500)` trực tiếp trên từng bảng cần |
| `safe_paths` | Deferred to v3; hiện dùng Google Directions API trực tiếp |

**Thay đổi so với v3.0 (22 bảng):**

| Thay đổi | Loại | Lý do |
|----------|------|-------|
| Tách `sos_requests` → `sos_requests` + `sos_request_details` | Table Split | Dispatch & heatmap chỉ cần hot columns; TEXT columns kéo dài row size gây buffer waste |
| `missions` + `snapshot_lat/lng/address/name/phone` | Denorm | Live Tracking poll 3-5s/lần; tránh double JOIN sang sos/aid tables |
| `donations` + `hub_name` | Denorm | QR screen hiển thị hub name mà không JOIN |

---

## 2. ENUM Types

| ENUM | Giá trị |
|------|---------|
| `user_role` | `VICTIM`, `VOLUNTEER`, `SPONSOR`, `STAFF`, `ADMIN` |
| `hub_status` | `ACTIVE`, `INACTIVE`, `EMERGENCY` |
| `urgency_level` | `CRITICAL`, `HIGH`, `MEDIUM`, `LOW` |
| `sos_status` | `PENDING`, `DISPATCHING`, `ASSIGNED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED` |
| `aid_status` | `PENDING`, `DISPATCHING`, `ASSIGNED`, `PICKED_UP`, `IN_TRANSIT`, `COMPLETED`, `CANCELLED` |
| `donation_status` | `REGISTERED`, `QR_GENERATED`, `RECEIVED`, `REJECTED` |
| `mission_type` | `RESCUE`, `DELIVERY` |
| `mission_status` | `PENDING`, `DISPATCHING`, `ASSIGNED`, `PICKING_UP`, `PICKED_UP`, `IN_TRANSIT`, `COMPLETED`, `CANCELLED` |
| `dispatch_response` | `PENDING`, `ACCEPTED`, `REJECTED`, `TIMEOUT` |
| `badge_level` | `BRONZE`, `SILVER`, `GOLD`, `PLATINUM` |

---

## 3. Table Definitions

---

### GROUP 1 — Auth & Users

#### `users`

| Column | Type | Constraint | Ghi chú |
|--------|------|-----------|---------|
| id | UUID | PK | DEFAULT gen_random_uuid() |
| full_name | VARCHAR(100) | NOT NULL | |
| email | VARCHAR(255) | UNIQUE | |
| phone_number | VARCHAR(20) | UNIQUE | |
| password_hash | VARCHAR(255) | NOT NULL | bcrypt — không log plain text |
| role | user_role | NOT NULL | |
| is_verified | BOOLEAN | DEFAULT FALSE | |
| is_active | BOOLEAN | DEFAULT TRUE | FALSE = bị Admin khóa |
| fcm_token | VARCHAR(500) | | cập nhật mỗi lần login |
| avatar_url | VARCHAR(500) | | |
| otp_code | VARCHAR(6) | | mã OTP đang active |
| otp_type | VARCHAR(20) | | `EMAIL_VERIFY` / `PHONE_VERIFY` / `PASSWORD_RESET` |
| otp_expires_at | TIMESTAMP | | |
| created_at | TIMESTAMP | DEFAULT NOW() | |
| updated_at | TIMESTAMP | DEFAULT NOW() | |

**Constraints:**
```sql
CHECK (email IS NOT NULL OR phone_number IS NOT NULL)
```

> **Tại sao gộp OTP vào `users`:** OTP là single-use, short-TTL (10 phút), chỉ 1 OTP active per user tại một thời điểm. Gộp vào loại bỏ JOIN khi verify. Redis mirror OTP cho O(1) lookup; cột này là durable fallback.

---

#### `refresh_tokens`

| Column | Type | Constraint |
|--------|------|-----------|
| id | UUID | PK |
| user_id | UUID | FK(users) ON DELETE CASCADE |
| token_hash | VARCHAR(64) | UNIQUE NOT NULL |
| device_info | TEXT | |
| expires_at | TIMESTAMP | NOT NULL |
| is_revoked | BOOLEAN | DEFAULT FALSE |
| created_at | TIMESTAMP | DEFAULT NOW() |

> Chỉ lưu SHA-256 hash — không bao giờ lưu raw token. Redis blacklist cho revocation tức thì; bảng này là durable backup khi Redis restart.

---

### GROUP 2 — Role Profiles

#### `volunteer_profiles`

| Column | Type | Constraint | Ghi chú |
|--------|------|-----------|---------|
| id | UUID | PK | |
| user_id | UUID | FK(users) UNIQUE | 1:1 với users |
| is_online | BOOLEAN | DEFAULT FALSE | **dispatch gate** — chỉ query khi TRUE |
| current_lat | DECIMAL(9,6) | | GPS mới nhất |
| current_lng | DECIMAL(9,6) | | GPS mới nhất |
| vehicle_type | VARCHAR(50) | | |
| total_tasks_completed | INT | DEFAULT 0 | T-factor trong Priority Score |
| avg_rating | DECIMAL(3,2) | DEFAULT 0.00 | R-factor (0–5) |
| avg_response_seconds | INT | DEFAULT 0 | A-factor |
| created_at | TIMESTAMP | DEFAULT NOW() | |
| updated_at | TIMESTAMP | DEFAULT NOW() | |

**Dispatch Priority Score:**
```
S = (D × 40%) + (R × 20%) + (T × 15%) + (A × 15%) + (E × 10%)
```
D = khoảng cách Haversine từ lat/lng | R = avg_rating | T = total_tasks_completed (normalized) | A = avg_response_seconds (inverse) | E = 0 (MVP đơn giản hóa)

---

#### `sponsor_profiles`

| Column | Type | Constraint |
|--------|------|-----------|
| id | UUID | PK |
| user_id | UUID | FK(users) UNIQUE |
| total_points | INT | DEFAULT 0 |
| total_items_donated | INT | DEFAULT 0 |
| donation_count | INT | DEFAULT 0 |
| badge_level | badge_level | DEFAULT 'BRONZE' |
| created_at | TIMESTAMP | DEFAULT NOW() |
| updated_at | TIMESTAMP | DEFAULT NOW() |

> Các cột aggregate cập nhật atomic cùng transaction khi donation → `RECEIVED`.
> **Badge thresholds:** BRONZE ≥ 1, SILVER ≥ 10, GOLD ≥ 25, PLATINUM ≥ 50 (`donation_count`).

---

### GROUP 3 — Infrastructure

#### `hubs`

| Column | Type | Constraint |
|--------|------|-----------|
| id | UUID | PK |
| name | VARCHAR(100) | NOT NULL |
| address | TEXT | NOT NULL |
| lat | DECIMAL(9,6) | NOT NULL |
| lng | DECIMAL(9,6) | NOT NULL |
| status | hub_status | DEFAULT 'ACTIVE' |
| contact_phone | VARCHAR(20) | |
| created_at | TIMESTAMP | DEFAULT NOW() |
| updated_at | TIMESTAMP | DEFAULT NOW() |

---

#### `hub_staff`

| Column | Type | Constraint |
|--------|------|-----------|
| id | UUID | PK |
| hub_id | UUID | FK(hubs) |
| user_id | UUID | FK(users) |
| is_available | BOOLEAN | DEFAULT TRUE |
| assigned_at | TIMESTAMP | DEFAULT NOW() |
| unassigned_at | TIMESTAMP | NULL = đang trực |

**Constraints:**
```sql
-- Partial UNIQUE: mỗi người chỉ active ở 1 hub tại 1 thời điểm
CREATE UNIQUE INDEX ON hub_staff(hub_id, user_id) WHERE unassigned_at IS NULL;

-- Chỉ STAFF được assign
CHECK (EXISTS(SELECT 1 FROM users WHERE id = user_id AND role = 'STAFF'))
```

---

#### `shelters`

| Column | Type | Constraint |
|--------|------|-----------|
| id | UUID | PK |
| name | VARCHAR(100) | NOT NULL |
| address | TEXT | |
| lat | DECIMAL(9,6) | |
| lng | DECIMAL(9,6) | |
| current_capacity | INT | DEFAULT 0 |
| max_capacity | INT | NOT NULL |
| has_electricity | BOOLEAN | DEFAULT FALSE |
| has_clean_water | BOOLEAN | DEFAULT FALSE |
| status | VARCHAR(15) | `AVAILABLE` / `FULL` / `UNAVAILABLE` |
| created_at | TIMESTAMP | DEFAULT NOW() |
| updated_at | TIMESTAMP | DEFAULT NOW() |

**Constraint:** `CHECK (current_capacity <= max_capacity)`

---

#### `system_config`

| Column | Type | Constraint |
|--------|------|-----------|
| key | VARCHAR(100) | PK |
| value | TEXT | NOT NULL |
| description | TEXT | |
| updated_at | TIMESTAMP | DEFAULT NOW() |

**Pre-seeded keys:**

| Key | Default |
|-----|---------|
| `dispatch.weight.distance` | `0.40` |
| `dispatch.weight.rating` | `0.20` |
| `dispatch.weight.tasks` | `0.15` |
| `dispatch.weight.response_time` | `0.15` |
| `dispatch.radius.step1_km` … `step4_km` | `1/3/5/10` |
| `dispatch.sos.window_seconds` | `30` |
| `dispatch.delivery.window_1` | `15` |
| `dispatch.delivery.window_batch` | `20` |
| `otp.ttl_minutes` | `10` |

---

### GROUP 4 — Catalog & Inventory

#### `item_categories`

| Column | Type | Constraint |
|--------|------|-----------|
| id | UUID | PK |
| parent_id | UUID | FK(self) nullable |
| name | VARCHAR(100) | NOT NULL |
| name_vi | VARCHAR(100) | NOT NULL |
| unit | VARCHAR(20) | NOT NULL |
| is_leaf | BOOLEAN | DEFAULT TRUE |
| created_at | TIMESTAMP | DEFAULT NOW() |

**Cây 2 cấp:**
```
Thức ăn (is_leaf=FALSE)
├── Gạo       (is_leaf=TRUE, unit='Kg')
├── Mì tôm    (is_leaf=TRUE, unit='Thùng')
└── Đồ hộp    (is_leaf=TRUE, unit='Hộp')
```
Trigger `fn_mark_parent_non_leaf`: khi INSERT child → tự SET `parent.is_leaf = FALSE`.
Chỉ leaf categories được dùng trong `hub_inventories`, `aid_request_items`, `donation_items`.

---

#### `hub_accepted_categories`

| Column | Type | Constraint |
|--------|------|-----------|
| hub_id | UUID | FK(hubs) |
| item_category_id | UUID | FK(item_categories) |

**PK:** `(hub_id, item_category_id)` — composite.

> Admin cấu hình per-hub: trạm nào nhận loại hàng nào. Bắt buộc cho Smart Hub Selection query.

---

#### `hub_inventories`

| Column | Type | Constraint |
|--------|------|-----------|
| id | UUID | PK |
| hub_id | UUID | FK(hubs) |
| item_category_id | UUID | FK(item_categories) |
| current_quantity | INT | DEFAULT 0 |
| low_stock_threshold | INT | DEFAULT 0 |
| updated_at | TIMESTAMP | DEFAULT NOW() |

**Constraints:**
```sql
UNIQUE (hub_id, item_category_id)
CHECK (current_quantity >= 0)
```

---

#### `inventory_logs`

| Column | Type | Constraint | Ghi chú |
|--------|------|-----------|---------|
| id | UUID | PK | |
| hub_inventory_id | UUID | FK(hub_inventories) | |
| change_type | VARCHAR(10) | NOT NULL | `INBOUND` / `OUTBOUND` |
| quantity_delta | INT | CHECK > 0 | luôn dương; hướng xác định bởi change_type |
| reference_type | VARCHAR(10) | | `DONATION` / `MISSION` |
| reference_id | UUID | | FK tới donation hoặc mission |
| performed_by | UUID | FK(users) | Staff thực hiện |
| quantity_after | INT | NOT NULL | snapshot sau transaction |
| notes | TEXT | | |
| created_at | TIMESTAMP | DEFAULT NOW() | **Append-only** |

> `quantity_after` là denorm có chủ ý: query tồn kho tại bất kỳ thời điểm nào = O(1) thay vì O(n) replay.

---

### GROUP 5 — Requests

#### `sos_requests` — **HOT TABLE** (v3.1: Vertical Split)

> **v3.1 thay đổi:** Các cột TEXT ít dùng đã được tách sang `sos_request_details`. Bảng này chỉ giữ các cột truy vấn nóng phục vụ dispatch algorithm và heatmap.

| Column | Type | Constraint | Ghi chú |
|--------|------|-----------|---------|
| id | UUID | PK | DEFAULT gen_random_uuid() |
| requester_id | UUID | FK(users) nullable | NULL = Guest |
| victim_lat | DECIMAL(9,6) | NOT NULL | hot: dispatch origin |
| victim_lng | DECIMAL(9,6) | NOT NULL | hot: dispatch origin |
| people_count | INT | CHECK > 0 | hot: urgency weighting |
| is_on_behalf | BOOLEAN | DEFAULT FALSE | |
| urgency_level | urgency_level | | AI-classified; hot: Priority Score |
| status | sos_status | DEFAULT 'PENDING' | hot: dispatch & heatmap filter |
| created_at | TIMESTAMP | DEFAULT NOW() | |
| updated_at | TIMESTAMP | DEFAULT NOW() | |

**Constraint:** `CHECK (people_count > 0)`

---

#### `sos_request_details` — **COLD TABLE** (v3.1: mới)

> **Mục đích:** Lưu các cột TEXT lớn, ít dùng — chỉ JOIN khi cần hiển thị chi tiết (Volunteer mission screen, Admin dashboard). Quan hệ 1:1 bắt buộc với `sos_requests`.

| Column | Type | Constraint | Ghi chú |
|--------|------|-----------|---------|
| sos_request_id | UUID | PK · FK(sos_requests) | 1:1 mandatory |
| requester_name | VARCHAR(100) | NOT NULL | |
| requester_phone | VARCHAR(20) | NOT NULL | |
| victim_name | VARCHAR(100) | | khi `is_on_behalf=TRUE` |
| victim_phone | VARCHAR(20) | | |
| victim_address | TEXT | | |
| description | TEXT | NOT NULL | raw text từ Victim |
| ai_summary | TEXT | | AI-generated tóm tắt |
| image_url | VARCHAR(500) | | ảnh đính kèm (optional) |

---

#### `aid_requests`

| Column | Type | Constraint |
|--------|------|-----------|
| id | UUID | PK |
| requester_id | UUID | FK(users) NOT NULL |
| lat | DECIMAL(9,6) | |
| lng | DECIMAL(9,6) | |
| address | TEXT | |
| adults_count | INT | DEFAULT 0 |
| elderly_count | INT | DEFAULT 0 |
| children_count | INT | DEFAULT 0 |
| notes | TEXT | |
| urgency_level | urgency_level | |
| status | aid_status | DEFAULT 'PENDING' |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

**Constraint:** `CHECK (adults_count + elderly_count + children_count > 0)`

---

#### `aid_request_items`

| Column | Type | Constraint |
|--------|------|-----------|
| id | UUID | PK |
| aid_request_id | UUID | FK(aid_requests) ON DELETE CASCADE |
| item_category_id | UUID | FK(item_categories) |
| quantity | INT | CHECK > 0 |
| created_at | TIMESTAMP | |

---

### GROUP 6 — Donations

#### `donations`

| Column | Type | Constraint | Ghi chú |
|--------|------|-----------|---------|
| id | UUID | PK | |
| sponsor_id | UUID | FK(users) | |
| hub_id | UUID | FK(hubs) | |
| hub_name | VARCHAR(100) | | **DENORM** — snapshot khi QR_GENERATED |
| estimated_delivery_at | DATE | | |
| qr_code_token | VARCHAR(255) | UNIQUE | |
| status | donation_status | DEFAULT 'REGISTERED' | |
| received_by | UUID | FK(users) nullable | |
| received_at | TIMESTAMP | | |
| rejection_reason | TEXT | | |
| created_at | TIMESTAMP | | |
| updated_at | TIMESTAMP | | |

> `hub_name`: Snapshot tên Hub tại thời điểm Sponsor chọn Hub (QR_GENERATED). QR Screen (§6.3) dùng trực tiếp mà không JOIN `hubs`.
> **Lifecycle:** `REGISTERED → QR_GENERATED → RECEIVED` (kho cộng) `| REJECTED` (lý do ghi log)

---

#### `donation_items`

| Column | Type | Constraint |
|--------|------|-----------|
| id | UUID | PK |
| donation_id | UUID | FK(donations) ON DELETE CASCADE |
| item_category_id | UUID | FK(item_categories) |
| quantity | INT | CHECK > 0 |
| expiry_date | DATE | |
| condition_notes | TEXT | |
| image_url | VARCHAR(500) | |
| created_at | TIMESTAMP | |

---

### GROUP 7 — Missions

#### `missions`

| Column | Type | Constraint | Ghi chú |
|--------|------|-----------|---------|
| id | UUID | PK | |
| mission_type | mission_type | NOT NULL | `RESCUE` / `DELIVERY` |
| sos_request_id | UUID | FK(sos_requests) nullable | RESCUE only |
| aid_request_id | UUID | FK(aid_requests) nullable | DELIVERY only |
| volunteer_id | UUID | FK(volunteer_profiles) | |
| hub_id | UUID | FK(hubs) nullable | DELIVERY only |
| status | mission_status | DEFAULT 'PENDING' | |
| qr_code_token | VARCHAR(255) | UNIQUE | single-use |
| priority_score | DECIMAL(8,4) | | snapshot tại thời điểm assign |
| snapshot_lat | DECIMAL(9,6) | | **DENORM** — tọa độ đích tại accepted_at |
| snapshot_lng | DECIMAL(9,6) | | **DENORM** |
| snapshot_address | TEXT | | **DENORM** — địa chỉ đích để hiển thị |
| snapshot_requester_name | VARCHAR(100) | | **DENORM** — tên nạn nhân/người gửi |
| snapshot_requester_phone | VARCHAR(20) | | **DENORM** — SĐT liên lạc |
| accepted_at | TIMESTAMP | | |
| picked_up_at | TIMESTAMP | | DELIVERY only |
| completed_at | TIMESTAMP | | |
| cancelled_at | TIMESTAMP | | |
| cancellation_reason | TEXT | | |
| confirmation_image_url | VARCHAR(500) | | ảnh xác nhận giao hàng |
| created_at | TIMESTAMP | | |
| updated_at | TIMESTAMP | | |

> **Snapshot columns (DENORM):** Được populate **tại thời điểm** Volunteer accept (`accepted_at`). Phục vụ Live Tracking (§4.4) và Mission Screen (§5.3) poll liên tục mà không JOIN sang `sos_requests`/`aid_requests`.

**CHECK constraints:**
```sql
-- RESCUE: không có hub, không có AID request
-- DELIVERY: bắt buộc có hub và AID request
CHECK (
  (mission_type = 'RESCUE'   AND sos_request_id IS NOT NULL AND aid_request_id IS NULL AND hub_id IS NULL)
  OR
  (mission_type = 'DELIVERY' AND aid_request_id IS NOT NULL AND sos_request_id IS NULL AND hub_id IS NOT NULL)
)
```

---

#### `dispatch_attempts`

| Column | Type | Constraint |
|--------|------|-----------|
| id | UUID | PK |
| mission_id | UUID | FK(missions) ON DELETE CASCADE |
| volunteer_id | UUID | FK(volunteer_profiles) |
| dispatch_type | VARCHAR(15) | `BROADCAST` / `SEQUENTIAL` |
| batch_number | INT | DEFAULT 1 |
| radius_km | DECIMAL(5,2) | |
| priority_score | DECIMAL(8,4) | snapshot |
| sent_at | TIMESTAMP | DEFAULT NOW() |
| response | dispatch_response | DEFAULT 'PENDING' |
| responded_at | TIMESTAMP | |

> Full audit log cho mỗi volunteer được notify. Admin dùng để phân tích hiệu quả dispatch.

---

### GROUP 8 — Communication

#### `chat_messages`

| Column | Type | Constraint |
|--------|------|-----------|
| id | UUID | PK |
| mission_id | UUID | FK(missions) ON DELETE CASCADE |
| sender_id | UUID | FK(users) |
| message_type | VARCHAR(5) | `TEXT` / `IMAGE` |
| message_text | TEXT | |
| image_url | VARCHAR(500) | |
| is_read | BOOLEAN | DEFAULT FALSE |
| created_at | TIMESTAMP | |

**CHECK:**
```sql
CHECK (
  (message_type = 'TEXT'  AND message_text IS NOT NULL AND image_url IS NULL)
  OR
  (message_type = 'IMAGE' AND image_url IS NOT NULL AND message_text IS NULL)
)
```

---

#### `ratings`

| Column | Type | Constraint |
|--------|------|-----------|
| id | UUID | PK |
| mission_id | UUID | FK(missions) UNIQUE |
| rater_id | UUID | FK(users) |
| ratee_id | UUID | FK(volunteer_profiles) |
| score | INT | CHECK (score BETWEEN 1 AND 5) |
| comment | TEXT | |
| created_at | TIMESTAMP | |

> `UNIQUE(mission_id)` đảm bảo mỗi mission chỉ được đánh giá 1 lần.

---

#### `notifications`

| Column | Type | Constraint |
|--------|------|-----------|
| id | UUID | PK |
| user_id | UUID | FK(users) ON DELETE CASCADE |
| title | VARCHAR(200) | NOT NULL |
| body | TEXT | |
| related_type | VARCHAR(20) | `MISSION` / `SOS` / `DONATION` / `AID_REQUEST` |
| related_id | UUID | |
| is_read | BOOLEAN | DEFAULT FALSE |
| created_at | TIMESTAMP | |

---

## 4. Indexing Strategy

### Dispatch (query nóng nhất)

| Index | Column | Loại | Mục đích |
|-------|--------|------|---------|
| `idx_vol_online` | `volunteer_profiles(current_lat, current_lng)` WHERE `is_online=TRUE` | PARTIAL BTREE | `is_online` filter + bounding box distance pre-filter |
| `idx_sos_location` | `sos_requests(victim_lat, victim_lng, status)` WHERE `status IN ('PENDING','DISPATCHING')` | PARTIAL BTREE | Heatmap + dispatch origin (hot table sau split) |
| `idx_aid_location` | `aid_requests(lat, lng, status)` WHERE `status IN ('PENDING','DISPATCHING')` | PARTIAL BTREE | Heatmap + dispatch origin |
| `idx_hubs_location` | `hubs(lat, lng)` WHERE `status='ACTIVE'` | PARTIAL BTREE | Smart Hub Selection |

### Read (frequent lookups)

| Index | Column |
|-------|--------|
| `idx_missions_volunteer` | `missions(volunteer_id, status)` |
| `idx_missions_sos` | `missions(sos_request_id)` WHERE NOT NULL |
| `idx_missions_aid` | `missions(aid_request_id)` WHERE NOT NULL |
| `idx_dispatch_pending` | `dispatch_attempts(mission_id)` WHERE `response='PENDING'` |
| `idx_donations_sponsor` | `donations(sponsor_id, created_at DESC)` |
| `idx_donations_hub_status` | `donations(hub_id, status)` |
| `idx_inventory_hub` | `hub_inventories(hub_id)` |
| `idx_inv_low_stock` | `hub_inventories(hub_id, current_quantity)` WHERE `current_quantity <= low_stock_threshold` | PARTIAL |
| `idx_inv_logs_ref` | `inventory_logs(reference_type, reference_id)` |
| `idx_chat_mission` | `chat_messages(mission_id, created_at ASC)` |
| `idx_notifications_user` | `notifications(user_id, created_at DESC)` WHERE `is_read=FALSE` |

---

## 5. Business Rules — DB Level

| Rule | Cơ chế |
|------|--------|
| Ít nhất email hoặc phone | `CHECK (email IS NOT NULL OR phone_number IS NOT NULL)` |
| Tồn kho không âm | `CHECK (current_quantity >= 0)` trên `hub_inventories` |
| RESCUE không có hub; DELIVERY bắt buộc có hub | `CHECK` 2 nhánh trên `missions` |
| Mission link đúng loại request | Cùng CHECK trên `missions` |
| Mỗi mission chỉ 1 rating | `UNIQUE(mission_id)` trên `ratings` |
| Score 1–5 | `CHECK (score BETWEEN 1 AND 5)` |
| Tồn kho log delta > 0 | `CHECK (quantity_delta > 0)` |
| Item qty > 0 | `CHECK (quantity > 0)` trên `donation_items`, `aid_request_items` |
| SOS people_count > 0 | `CHECK (people_count > 0)` |
| Shelter capacity | `CHECK (current_capacity <= max_capacity)` |
| 1 active staff per hub per person | Partial UNIQUE index `(hub_id, user_id) WHERE unassigned_at IS NULL` |
| Chỉ STAFF vào hub_staff | `CHECK role = 'STAFF'` |
| Refresh token = hash only | Application-enforced |
| `sos_request_details` bắt buộc với mỗi SOS | Application-enforced: INSERT 2 bảng trong cùng transaction |
| `missions.snapshot_*` populated khi accept | Application-enforced: populate khi Volunteer accept, trước khi UPDATE status → ASSIGNED |
| updated_at auto-maintain | Trigger `fn_set_updated_at` trên mọi bảng mutable |

---

## 6. Redis Layer (không trong PostgreSQL)

| Dữ liệu | Cấu trúc Redis | TTL |
|---------|----------------|-----|
| JWT blacklist | `SET revoked:{jti}` | Đến khi token hết hạn |
| Volunteer live location | `GEOADD volunteer_locations {lat} {lng} {vol_id}` | Cho đến khi `is_online=FALSE` |
| Dispatch state | `HASH dispatch:{mission_id}` | Cho đến khi mission kết thúc |
| WebSocket pub/sub | Pub/Sub channels | Session lifetime |
| Dispatch Redis Lock | `SET lock:mission:{id} NX EX 5` | 5 giây (anti race condition) |
