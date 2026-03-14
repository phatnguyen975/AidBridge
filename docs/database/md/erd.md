# AidBridge — ERD: Detailed Logical ERD

> **Mục đích:** Bản đồ dữ liệu nghiệp vụ thuần túy — dành cho Business Analyst & Developer hiểu luồng dữ liệu.
> **Nguyên tắc:** KHÔNG có FK columns, KHÔNG có junction table thuần túy, KHÔNG có kiểu dữ liệu vật lý. Cardinality phản ánh đúng thực tế nghiệp vụ.
> **Physical Schema:** Xem [physical_database.md](./physical_database.md)

---

## Sơ đồ tổng thể (Full Logical ERD)

```mermaid
%%{init: {"er": {"layoutDirection": "TB", "diagramPadding": 20}}}%%
erDiagram
    %% ================================================================
    %% NHÓM: AUTH & SESSIONS
    %% ================================================================
    users {}
    refresh_tokens {}

    %% ================================================================
    %% NHÓM: PROFILES (mở rộng theo vai trò)
    %% ================================================================
    volunteer_profiles {}
    sponsor_profiles {}

    %% ================================================================
    %% NHÓM: HẠ TẦNG
    %% ================================================================
    hubs {}
    hub_staff {}
    shelters {}
    system_config {}

    %% ================================================================
    %% NHÓM: DANH MỤC & KHO HÀNG
    %% ================================================================
    item_categories {}
    hub_inventories {}
    inventory_logs {}

    %% ================================================================
    %% NHÓM: YÊU CẦU CỨU TRỢ
    %% ================================================================
    sos_requests {}
    aid_requests {}
    aid_request_items {}

    %% ================================================================
    %% NHÓM: QUYÊN GÓP
    %% ================================================================
    donations {}
    donation_items {}

    %% ================================================================
    %% NHÓM: NHIỆM VỤ
    %% ================================================================
    missions {}
    dispatch_attempts {}

    %% ================================================================
    %% NHÓM: GIAO TIẾP
    %% ================================================================
    chat_messages {}
    ratings {}
    notifications {}

    %% ================================================================
    %% QUAN HỆ: AUTH & PROFILES
    %% ================================================================
    users ||--o{ refresh_tokens          : "authenticates via"
    users ||--o| volunteer_profiles      : "has volunteer profile"
    users ||--o| sponsor_profiles        : "has sponsor profile"

    %% ================================================================
    %% QUAN HỆ: HẠ TẦNG & KHO
    %% ================================================================
    users           ||--o{ hub_staff       : "is assigned as staff"
    hubs            ||--o{ hub_staff       : "employs"
    hubs            }o--o{ item_categories : "accepts category (M:N)"
    hubs            ||--o{ hub_inventories : "tracks stock of"
    item_categories  o|--o{ item_categories : "is parent of"
    item_categories ||--o{ hub_inventories : "stocked as"
    hub_inventories ||--o{ inventory_logs  : "change audited via"

    %% ================================================================
    %% QUAN HỆ: YÊU CẦU CỨU TRỢ
    %% ================================================================
    users           o|--o{ sos_requests      : "submits (nullable for Guest)"
    users           ||--o{ aid_requests      : "submits"
    aid_requests    ||--|{ aid_request_items : "details"
    item_categories ||--o{ aid_request_items : "categorises"

    %% ================================================================
    %% QUAN HỆ: QUYÊN GÓP
    %% ================================================================
    users           ||--o{ donations      : "sponsors"
    hubs            ||--o{ donations      : "receives at"
    users           ||--o{ inventory_logs : "performs action"
    donations       ||--|{ donation_items : "details"
    item_categories ||--o{ donation_items : "categorises"

    %% Inventory log nguồn (polymorphic reference)
    donations ||--o{ inventory_logs : "source of INBOUND log"
    missions  ||--o{ inventory_logs : "source of OUTBOUND log"

    %% ================================================================
    %% QUAN HỆ: NHIỆM VỤ & PHÂN CÔNG
    %% ================================================================
    sos_requests       ||--o| missions          : "triggers RESCUE mission"
    aid_requests       ||--o| missions          : "triggers DELIVERY mission"
    volunteer_profiles ||--o{ missions          : "executes"
    hubs               ||--o{ missions          : "is dispatch point for (DELIVERY)"
    volunteer_profiles ||--o{ dispatch_attempts : "notified via"
    missions           ||--o{ dispatch_attempts : "dispatched through"

    %% ================================================================
    %% QUAN HỆ: GIAO TIẾP
    %% ================================================================
    missions ||--o{ chat_messages : "has conversation"
    users    ||--o{ chat_messages : "sends message"
    missions ||--o|  ratings      : "rated via"
    users    ||--o{ ratings       : "gives rating (rater)"
    users    ||--o{ ratings       : "receives rating (ratee)"
    users    ||--o{ notifications : "receives"
```

---

## Sub-Diagrams theo Domain

### A — Auth & Profiles

```mermaid
%%{init: {"er": {"layoutDirection": "TB", "diagramPadding": 20}}}%%
erDiagram
    users {
        string id PK
        string full_name
        string email
        string phone_number
        user_role role
        boolean is_verified
        boolean is_active
        string fcm_token
        string otp_code
        string otp_type
        datetime otp_expires_at
    }

    refresh_tokens {
        string id PK
        string token_hash
        string device_info
        datetime expires_at
        boolean is_revoked
    }

    volunteer_profiles {
        string id PK
        boolean is_online
        decimal current_lat
        decimal current_lng
        string vehicle_type
        integer total_tasks_completed
        decimal avg_rating
        integer avg_response_seconds
    }

    sponsor_profiles {
        string id PK
        integer total_points
        integer total_items_donated
        integer donation_count
        badge_level badge_level
    }

    users ||--o{ refresh_tokens     : "authenticates via"
    users ||--o| volunteer_profiles : "has volunteer profile"
    users ||--o| sponsor_profiles   : "has sponsor profile"
```

---

### B — Infrastructure & Inventory

```mermaid
%%{init: {"er": {"layoutDirection": "TB", "diagramPadding": 20}}}%%
erDiagram
    hubs {
        string id PK
        string name
        string address
        decimal lat
        decimal lng
        hub_status status
        string contact_phone
    }

    hub_staff {
        string id PK
        boolean is_available
        datetime assigned_at
        datetime unassigned_at
    }

    item_categories {
        string id PK
        string name
        string name_vi
        string unit
        boolean is_leaf
    }

    hub_inventories {
        string id PK
        integer current_quantity
        integer low_stock_threshold
        datetime updated_at
    }

    inventory_logs {
        string id PK
        string change_type
        integer quantity_delta
        string reference_type
        string reference_id
        integer quantity_after
        string notes
        datetime created_at
    }

    users           ||--o{ hub_staff      : "is assigned as staff"
    hubs            ||--o{ hub_staff      : "employs"
    hubs            }o--o{ item_categories : "accepts category (M:N)"
    hubs            ||--o{ hub_inventories : "tracks stock of"
    item_categories o|--o{ item_categories : "is parent of"
    item_categories ||--o{  hub_inventories : "stocked as"
    hub_inventories ||--o{  inventory_logs  : "change audited via"
```

---

### C — Requests & Donations

```mermaid
%%{init: {"er": {"layoutDirection": "TB", "diagramPadding": 20}}}%%
erDiagram
    sos_requests {
        string id PK
        string requester_name
        string requester_phone
        string victim_name
        string victim_phone
        decimal victim_lat
        decimal victim_lng
        string victim_address
        string description
        integer people_count
        boolean is_on_behalf
        urgency_level urgency_level
        string ai_summary
        sos_status status
        string image_url
    }

    aid_requests {
        string id PK
        decimal lat
        decimal lng
        string address
        integer adults_count
        integer elderly_count
        integer children_count
        string notes
        urgency_level urgency_level
        aid_status status
    }

    aid_request_items {
        string id PK
        integer quantity
    }

    donations {
        string id PK
        date estimated_delivery_at
        string qr_code_token
        donation_status status
        datetime received_at
        string rejection_reason
    }

    donation_items {
        string id PK
        integer quantity
        date expiry_date
        string condition_notes
        string image_url
    }

    item_categories {
        string id PK
        string name
        string unit
        boolean is_leaf
    }

    users           o|--o{ sos_requests      : "submits (nullable=Guest)"
    users           ||--o{ aid_requests       : "submits"
    aid_requests    ||--|{ aid_request_items  : "details"
    item_categories ||--o{ aid_request_items  : "categorises"
    users           ||--o{ donations          : "sponsors"
    hubs            ||--o{ donations          : "receives at"
    donations       ||--|{ donation_items     : "details"
    item_categories ||--o{ donation_items     : "categorises"
```

---

### D — Missions & Dispatch

```mermaid
%%{init: {"er": {"layoutDirection": "TB", "diagramPadding": 20}}}%%
erDiagram
    sos_requests {
        string id PK
        decimal victim_lat
        decimal victim_lng
        urgency_level urgency_level
        sos_status status
        integer people_count
    }

    aid_requests {
        string id PK
        decimal lat
        decimal lng
        urgency_level urgency_level
        aid_status status
    }

    volunteer_profiles {
        string id PK
        boolean is_online
        decimal current_lat
        decimal current_lng
        decimal avg_rating
        integer total_tasks_completed
        integer avg_response_seconds
    }

    missions {
        string id PK
        mission_type mission_type
        mission_status status
        string qr_code_token
        decimal priority_score
        datetime accepted_at
        datetime picked_up_at
        datetime completed_at
        datetime cancelled_at
    }

    dispatch_attempts {
        string id PK
        string dispatch_type
        integer batch_number
        decimal radius_km
        decimal priority_score
        datetime sent_at
        dispatch_response response
        datetime responded_at
    }

    sos_requests       ||--o| missions          : "triggers RESCUE"
    aid_requests       ||--o| missions          : "triggers DELIVERY"
    volunteer_profiles ||--o{ missions          : "executes"
    hubs               ||--o{ missions          : "is dispatch point for"
    volunteer_profiles ||--o{ dispatch_attempts : "notified via"
    missions           ||--o{ dispatch_attempts : "dispatched through"
```

---

### E — Communication

```mermaid
%%{init: {"er": {"layoutDirection": "TB", "diagramPadding": 20}}}%%
erDiagram
    missions {
        string id PK
        mission_type mission_type
        mission_status status
    }

    chat_messages {
        string id PK
        string message_type
        string message_text
        string image_url
        boolean is_read
        datetime created_at
    }

    ratings {
        string id PK
        integer score
        string comment
        datetime created_at
    }

    notifications {
        string id PK
        string title
        string body
        string related_type
        string related_id
        boolean is_read
        datetime created_at
    }

    users {
        string id PK
        string full_name
        user_role role
        string avatar_url
    }

    missions ||--o{ chat_messages : "has conversation"
    users    ||--o{ chat_messages : "sends message"
    missions ||--o|  ratings      : "rated via"
    users    ||--o{ ratings       : "gives rating (rater)"
    users    ||--o{ ratings       : "receives rating (ratee)"
    users    ||--o{ notifications : "receives"
```

---

## Ghi chú về Cardinality

| Ký hiệu Mermaid | Nghĩa |
|-----------------|-------|
| `\|\|--\|\|` | Exactly one — Exactly one (1:1 bắt buộc hai phía) |
| `\|\|--o\|` | Exactly one — Zero or one (bắt buộc một phía, tuỳ chọn phía kia) |
| `\|\|--|{` | Exactly one — One or many (bắt buộc có ít nhất 1) |
| `\|\|--o{` | Exactly one — Zero or many (1:N) |
| `o\|--o{` | Zero or one — Zero or many |
| `}o--o{` | Zero or many — Zero or many **(M:N thuần khái niệm)** |

> **Chú ý M:N:** Quan hệ `hubs }o--o{ item_categories` biểu diễn việc Hub chấp nhận nhiều loại hàng và mỗi loại hàng được nhiều Hub chấp nhận. Trong Physical Schema, quan hệ này được giải quyết bằng Junction Table `hub_accepted_categories`.

---

## ENUM Reference

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

## Những điểm thiết kế Logic quan trọng

| Thực thể | Ghi chú |
|----------|---------|
| `hub_staff` | **Associative entity** (quan hệ thực thể kết hợp) — giao điểm User–Hub có thuộc tính nghiệp vụ riêng (`is_available`, `assigned_at`, `unassigned_at`) |
| `hub_inventories` | **Associative entity** — giao điểm Hub–ItemCategory có thuộc tính (`current_quantity`, `low_stock_threshold`) |
| `aid_request_items` / `donation_items` | **Associative entities (Line Item pattern)** — giao điểm Request/Donation–ItemCategory có `quantity` |
| `hub_accepted_categories` | **Pure junction (không có thuộc tính riêng)** — biểu diễn là M:N trực tiếp (`}o--o{`) trong Logical ERD |
| `sos_requests.requester_id` | **Nullable** — Guest (không đăng nhập) vẫn được tạo SOS; cardinality phía `users` là `o\|` |
| `ratings` | Có **2 FK ngược chiều** đến `users`: `rater_id` (người đánh giá) và `ratee_id` (người được đánh giá) → Mermaid hiển thị 2 đường nối |
| `inventory_logs.reference_id` | **Polymorphic FK** — trỏ tới `donations` (INBOUND) hoặc `missions` (OUTBOUND); `reference_type` làm discriminator |
| `missions` | **XOR constraint**: loại RESCUE chỉ link `sos_requests`; loại DELIVERY chỉ link `aid_requests` + `hubs` |
