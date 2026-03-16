-- ============================================================
-- AidBridge: Database Schema
-- Engine      : PostgreSQL 15+  |  Optimized for Supabase
-- Extensions  : uuid-ossp, postgis  (run 01_init_postgis.sql first)
-- Tables      : 23  |  ENUMs: 10  |  Indexes: ~38
-- ============================================================
-- SAFE TO RE-RUN: all DROP statements use IF EXISTS + CASCADE to handle dependencies
-- Execution order:
--   1. 01_init_postgis.sql   (extensions, run once)
--   2. schema.sql            (this file)
-- ============================================================


-- ============================================================
-- SECTION 0: RESET
-- ============================================================

-- Trigger functions
DROP FUNCTION IF EXISTS fn_set_updated_at()        CASCADE;
DROP FUNCTION IF EXISTS fn_mark_parent_non_leaf()  CASCADE;
DROP FUNCTION IF EXISTS fn_assert_leaf_category()  CASCADE;
DROP FUNCTION IF EXISTS fn_assert_staff_role()     CASCADE;

-- Tables (reverse dependency order; CASCADE handles remaining deps)
DROP TABLE IF EXISTS notifications           CASCADE;
DROP TABLE IF EXISTS ratings                 CASCADE;
DROP TABLE IF EXISTS chat_messages           CASCADE;
DROP TABLE IF EXISTS dispatch_attempts       CASCADE;
DROP TABLE IF EXISTS missions                CASCADE;
DROP TABLE IF EXISTS donation_items          CASCADE;
DROP TABLE IF EXISTS donations               CASCADE;
DROP TABLE IF EXISTS aid_request_items       CASCADE;
DROP TABLE IF EXISTS aid_requests            CASCADE;
DROP TABLE IF EXISTS sos_request_details     CASCADE;
DROP TABLE IF EXISTS sos_requests            CASCADE;
DROP TABLE IF EXISTS inventory_logs          CASCADE;
DROP TABLE IF EXISTS hub_inventories         CASCADE;
DROP TABLE IF EXISTS hub_accepted_categories CASCADE;
DROP TABLE IF EXISTS item_categories         CASCADE;
DROP TABLE IF EXISTS system_config           CASCADE;
DROP TABLE IF EXISTS shelters                CASCADE;
DROP TABLE IF EXISTS hub_staff               CASCADE;
DROP TABLE IF EXISTS hubs                    CASCADE;
DROP TABLE IF EXISTS sponsor_profiles        CASCADE;
DROP TABLE IF EXISTS volunteer_profiles      CASCADE;
DROP TABLE IF EXISTS refresh_tokens          CASCADE;
DROP TABLE IF EXISTS users                   CASCADE;

-- ENUM types
DROP TYPE IF EXISTS user_role         CASCADE;
DROP TYPE IF EXISTS hub_status        CASCADE;
DROP TYPE IF EXISTS urgency_level     CASCADE;
DROP TYPE IF EXISTS sos_status        CASCADE;
DROP TYPE IF EXISTS aid_status        CASCADE;
DROP TYPE IF EXISTS donation_status   CASCADE;
DROP TYPE IF EXISTS mission_type      CASCADE;
DROP TYPE IF EXISTS mission_status    CASCADE;
DROP TYPE IF EXISTS dispatch_response CASCADE;
DROP TYPE IF EXISTS badge_level       CASCADE;


-- ============================================================
-- SECTION 1: ENUM TYPES
-- ============================================================

CREATE TYPE user_role AS ENUM (
    'VICTIM',
    'VOLUNTEER',
    'SPONSOR',
    'STAFF',
    'ADMIN'
);

CREATE TYPE hub_status AS ENUM (
    'ACTIVE',
    'INACTIVE',
    'EMERGENCY'
);

CREATE TYPE urgency_level AS ENUM (
    'CRITICAL',
    'HIGH',
    'MEDIUM',
    'LOW'
);

CREATE TYPE sos_status AS ENUM (
    'PENDING',
    'DISPATCHING',
    'ASSIGNED',
    'IN_PROGRESS',
    'COMPLETED',
    'CANCELLED'
);

CREATE TYPE aid_status AS ENUM (
    'PENDING',
    'DISPATCHING',
    'ASSIGNED',
    'PICKED_UP',
    'IN_TRANSIT',
    'COMPLETED',
    'CANCELLED'
);

CREATE TYPE donation_status AS ENUM (
    'REGISTERED',
    'QR_GENERATED',
    'RECEIVED',
    'REJECTED'
);

CREATE TYPE mission_type AS ENUM (
    'RESCUE',
    'DELIVERY'
);

CREATE TYPE mission_status AS ENUM (
    'PENDING',
    'DISPATCHING',
    'ASSIGNED',
    'PICKING_UP',
    'PICKED_UP',
    'IN_TRANSIT',
    'COMPLETED',
    'CANCELLED'
);

CREATE TYPE dispatch_response AS ENUM (
    'PENDING',
    'ACCEPTED',
    'REJECTED',
    'TIMEOUT'
);

CREATE TYPE badge_level AS ENUM (
    'BRONZE',
    'SILVER',
    'GOLD',
    'PLATINUM'
);


-- ============================================================
-- SECTION 2: SHARED TRIGGER FUNCTIONS
-- ============================================================

-- Auto-maintain updated_at on every UPDATE
CREATE OR REPLACE FUNCTION fn_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Khi INSERT một leaf category → auto SET parent.is_leaf = FALSE
CREATE OR REPLACE FUNCTION fn_mark_parent_non_leaf()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.parent_id IS NOT NULL THEN
        UPDATE item_categories
        SET    is_leaf = FALSE
        WHERE  id = NEW.parent_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Đảm bảo chỉ leaf category được dùng trong inventory / request / donation
CREATE OR REPLACE FUNCTION fn_assert_leaf_category()
RETURNS TRIGGER AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM   item_categories
        WHERE  id = NEW.item_category_id
          AND  is_leaf = TRUE
    ) THEN
        RAISE EXCEPTION
            'Chỉ leaf category được phép. Category % không phải leaf.',
            NEW.item_category_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Đảm bảo chỉ user role=STAFF được thêm vào hub_staff
CREATE OR REPLACE FUNCTION fn_assert_staff_role()
RETURNS TRIGGER AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM   users
        WHERE  id   = NEW.user_id
          AND  role = 'STAFF'
    ) THEN
        RAISE EXCEPTION
            'Chỉ user có role=STAFF được phân công vào hub_staff. user_id: %',
            NEW.user_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- ============================================================
-- SECTION 3: CREATE ALL TABLES  (FKs added in Section 4)
-- ============================================================

-- ------------------------------------------------------------
-- GROUP: AUTH & SESSIONS
-- ------------------------------------------------------------

CREATE TABLE users (
    id              UUID          NOT NULL DEFAULT gen_random_uuid(),
    full_name       VARCHAR(100)  NOT NULL,
    email           VARCHAR(255),
    phone_number    VARCHAR(20),
    password_hash   VARCHAR(255)  NOT NULL,
    role            user_role     NOT NULL,
    is_verified     BOOLEAN       NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
    fcm_token       VARCHAR(500),
    avatar_url      VARCHAR(500),
    -- OTP (single-use, short-TTL, gộp vào users để tránh JOIN)
    otp_code        CHAR(6),
    otp_type        VARCHAR(20),
    otp_expires_at  TIMESTAMPTZ,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT users_pkey
        PRIMARY KEY (id),
    CONSTRAINT users_email_unique
        UNIQUE (email),
    CONSTRAINT users_phone_unique
        UNIQUE (phone_number),
    CONSTRAINT users_email_or_phone
        CHECK (email IS NOT NULL OR phone_number IS NOT NULL),
    CONSTRAINT users_otp_type_valid
        CHECK (otp_type IS NULL
            OR otp_type IN ('EMAIL_VERIFY', 'PHONE_VERIFY', 'PASSWORD_RESET'))
);

CREATE TABLE refresh_tokens (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL,          -- FK → users(id) CASCADE
    token_hash  VARCHAR(64) NOT NULL,
    device_info TEXT,
    expires_at  TIMESTAMPTZ NOT NULL,
    is_revoked  BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT refresh_tokens_pkey
        PRIMARY KEY (id),
    CONSTRAINT refresh_tokens_hash_unique
        UNIQUE (token_hash)
);

-- ------------------------------------------------------------
-- GROUP: PROFILES
-- ------------------------------------------------------------

CREATE TABLE volunteer_profiles (
    id                   UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_id              UUID         NOT NULL,  -- FK → users(id) CASCADE
    is_online            BOOLEAN      NOT NULL DEFAULT FALSE,
    current_lat          DECIMAL(9,6),           -- NULL khi offline
    current_lng          DECIMAL(9,6),
    vehicle_type         VARCHAR(50),
    total_tasks_completed INT         NOT NULL DEFAULT 0,
    avg_rating           DECIMAL(3,2) NOT NULL DEFAULT 0.00,
    avg_response_seconds INT          NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT volunteer_profiles_pkey
        PRIMARY KEY (id),
    CONSTRAINT volunteer_profiles_user_unique
        UNIQUE (user_id),
    CONSTRAINT volunteer_profiles_avg_rating_range
        CHECK (avg_rating BETWEEN 0.00 AND 5.00),
    CONSTRAINT volunteer_profiles_total_tasks_non_neg
        CHECK (total_tasks_completed >= 0),
    CONSTRAINT volunteer_profiles_avg_response_non_neg
        CHECK (avg_response_seconds >= 0)
);

CREATE TABLE sponsor_profiles (
    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    user_id             UUID        NOT NULL,  -- FK → users(id) CASCADE
    total_points        INT         NOT NULL DEFAULT 0,
    total_items_donated INT         NOT NULL DEFAULT 0,
    donation_count      INT         NOT NULL DEFAULT 0,
    badge_level         badge_level NOT NULL DEFAULT 'BRONZE',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT sponsor_profiles_pkey
        PRIMARY KEY (id),
    CONSTRAINT sponsor_profiles_user_unique
        UNIQUE (user_id),
    CONSTRAINT sponsor_profiles_points_non_neg
        CHECK (total_points >= 0),
    CONSTRAINT sponsor_profiles_items_non_neg
        CHECK (total_items_donated >= 0),
    CONSTRAINT sponsor_profiles_count_non_neg
        CHECK (donation_count >= 0)
);

-- ------------------------------------------------------------
-- GROUP: INFRASTRUCTURE
-- ------------------------------------------------------------

CREATE TABLE hubs (
    id            UUID        NOT NULL DEFAULT gen_random_uuid(),
    name          VARCHAR(100) NOT NULL,
    address       TEXT        NOT NULL,
    lat           DECIMAL(9,6) NOT NULL,
    lng           DECIMAL(9,6) NOT NULL,
    status        hub_status  NOT NULL DEFAULT 'ACTIVE',
    contact_phone VARCHAR(20),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT hubs_pkey
        PRIMARY KEY (id)
);

CREATE TABLE hub_staff (
    id            UUID        NOT NULL DEFAULT gen_random_uuid(),
    hub_id        UUID        NOT NULL,    -- FK → hubs(id) RESTRICT
    user_id       UUID        NOT NULL,    -- FK → users(id) RESTRICT
    is_available  BOOLEAN     NOT NULL DEFAULT TRUE,
    assigned_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    unassigned_at TIMESTAMPTZ,             -- NULL = đang trực
    created_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT hub_staff_pkey
        PRIMARY KEY (id)
);

-- Partial UNIQUE: mỗi người chỉ active ở 1 hub tại 1 thời điểm
CREATE UNIQUE INDEX idx_hub_staff_active_unique
    ON hub_staff (hub_id, user_id)
    WHERE unassigned_at IS NULL;

CREATE TABLE shelters (
    id               UUID        NOT NULL DEFAULT gen_random_uuid(),
    name             VARCHAR(100) NOT NULL,
    address          TEXT,
    lat              DECIMAL(9,6),
    lng              DECIMAL(9,6),
    current_capacity INT         NOT NULL DEFAULT 0,
    max_capacity     INT         NOT NULL,
    has_electricity  BOOLEAN     NOT NULL DEFAULT FALSE,
    has_clean_water  BOOLEAN     NOT NULL DEFAULT FALSE,
    status           VARCHAR(15) NOT NULL DEFAULT 'AVAILABLE',
    created_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT shelters_pkey
        PRIMARY KEY (id),
    CONSTRAINT shelters_capacity_valid
        CHECK (current_capacity >= 0 AND current_capacity <= max_capacity),
    CONSTRAINT shelters_max_capacity_positive
        CHECK (max_capacity > 0),
    CONSTRAINT shelters_status_valid
        CHECK (status IN ('AVAILABLE', 'FULL', 'UNAVAILABLE'))
);

CREATE TABLE system_config (
    key         VARCHAR(100) NOT NULL,
    value       TEXT         NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT system_config_pkey
        PRIMARY KEY (key)
);

-- ------------------------------------------------------------
-- GROUP: CATALOG & INVENTORY
-- ------------------------------------------------------------

CREATE TABLE item_categories (
    id         UUID         NOT NULL DEFAULT gen_random_uuid(),
    parent_id  UUID,                       -- FK → item_categories(id) self-ref, SET NULL
    name       VARCHAR(100) NOT NULL,
    name_vi    VARCHAR(100) NOT NULL,
    unit       VARCHAR(20)  NOT NULL,
    is_leaf    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT item_categories_pkey
        PRIMARY KEY (id)
);

-- Junction table: Hub ↔ ItemCategory (M:N)
CREATE TABLE hub_accepted_categories (
    hub_id           UUID        NOT NULL,  -- FK → hubs(id) CASCADE
    item_category_id UUID        NOT NULL,  -- FK → item_categories(id) CASCADE
    created_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT hub_accepted_categories_pkey
        PRIMARY KEY (hub_id, item_category_id)
);

CREATE TABLE hub_inventories (
    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    hub_id              UUID        NOT NULL,  -- FK → hubs(id) RESTRICT
    item_category_id    UUID        NOT NULL,  -- FK → item_categories(id) RESTRICT
    current_quantity    INT         NOT NULL DEFAULT 0,
    low_stock_threshold INT         NOT NULL DEFAULT 0,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT hub_inventories_pkey
        PRIMARY KEY (id),
    CONSTRAINT hub_inventories_unique_slot
        UNIQUE (hub_id, item_category_id),
    CONSTRAINT hub_inventories_qty_non_negative
        CHECK (current_quantity >= 0),
    CONSTRAINT hub_inventories_threshold_non_negative
        CHECK (low_stock_threshold >= 0)
);

CREATE TABLE inventory_logs (
    id               UUID        NOT NULL DEFAULT gen_random_uuid(),
    hub_inventory_id UUID        NOT NULL,  -- FK → hub_inventories(id) RESTRICT
    change_type      VARCHAR(10) NOT NULL,
    quantity_delta   INT         NOT NULL,
    reference_type   VARCHAR(10),
    reference_id     UUID,                  -- Polymorphic: donations.id | missions.id
    performed_by     UUID,                  -- FK → users(id) SET NULL
    quantity_after   INT         NOT NULL,
    notes            TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT inventory_logs_pkey
        PRIMARY KEY (id),
    CONSTRAINT inventory_logs_change_type_valid
        CHECK (change_type IN ('INBOUND', 'OUTBOUND')),
    CONSTRAINT inventory_logs_delta_positive
        CHECK (quantity_delta > 0),
    CONSTRAINT inventory_logs_ref_type_valid
        CHECK (reference_type IS NULL
            OR reference_type IN ('DONATION', 'MISSION'))
);

-- ------------------------------------------------------------
-- GROUP: REQUESTS
-- [VERTICAL SPLIT] sos_requests (HOT) + sos_request_details (COLD)
-- ------------------------------------------------------------

-- HOT TABLE: chỉ cột dispatch/heatmap-critical
CREATE TABLE sos_requests (
    id            UUID          NOT NULL DEFAULT gen_random_uuid(),
    requester_id  UUID,                          -- FK nullable → users(id) SET NULL
    victim_lat    DECIMAL(9,6)  NOT NULL,
    victim_lng    DECIMAL(9,6)  NOT NULL,
    people_count  INT           NOT NULL,
    is_on_behalf  BOOLEAN       NOT NULL DEFAULT FALSE,
    urgency_level urgency_level NOT NULL DEFAULT 'MEDIUM',
    status        sos_status    NOT NULL DEFAULT 'PENDING',
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT sos_requests_pkey
        PRIMARY KEY (id),
    CONSTRAINT sos_requests_people_count_positive
        CHECK (people_count > 0)
);

-- COLD TABLE: TEXT nặng, chỉ JOIN khi cần hiển thị chi tiết
CREATE TABLE sos_request_details (
    sos_request_id  UUID         NOT NULL,  -- PK + FK → sos_requests(id) CASCADE
    requester_name  VARCHAR(100) NOT NULL,
    requester_phone VARCHAR(20)  NOT NULL,
    victim_name     VARCHAR(100),
    victim_phone    VARCHAR(20),
    victim_address  TEXT,
    description     TEXT         NOT NULL,
    ai_summary      TEXT,
    image_url       VARCHAR(500),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT sos_request_details_pkey
        PRIMARY KEY (sos_request_id)
);

CREATE TABLE aid_requests (
    id             UUID          NOT NULL DEFAULT gen_random_uuid(),
    requester_id   UUID          NOT NULL,  -- FK → users(id) RESTRICT
    lat            DECIMAL(9,6)  NOT NULL,
    lng            DECIMAL(9,6)  NOT NULL,
    address        TEXT,
    adults_count   INT           NOT NULL DEFAULT 0,
    elderly_count  INT           NOT NULL DEFAULT 0,
    children_count INT           NOT NULL DEFAULT 0,
    notes          TEXT,
    urgency_level  urgency_level NOT NULL DEFAULT 'MEDIUM',
    status         aid_status    NOT NULL DEFAULT 'PENDING',
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT aid_requests_pkey
        PRIMARY KEY (id),
    CONSTRAINT aid_requests_people_positive
        CHECK (adults_count + elderly_count + children_count > 0),
    CONSTRAINT aid_requests_counts_non_negative
        CHECK (adults_count >= 0 AND elderly_count >= 0 AND children_count >= 0)
);

CREATE TABLE aid_request_items (
    id               UUID        NOT NULL DEFAULT gen_random_uuid(),
    aid_request_id   UUID        NOT NULL,  -- FK → aid_requests(id) CASCADE
    item_category_id UUID        NOT NULL,  -- FK → item_categories(id) RESTRICT
    quantity         INT         NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT aid_request_items_pkey
        PRIMARY KEY (id),
    CONSTRAINT aid_request_items_qty_positive
        CHECK (quantity > 0)
);

-- ------------------------------------------------------------
-- GROUP: DONATIONS
-- ------------------------------------------------------------

CREATE TABLE donations (
    id                    UUID            NOT NULL DEFAULT gen_random_uuid(),
    sponsor_id            UUID            NOT NULL,  -- FK → users(id) RESTRICT
    hub_id                UUID,                      -- FK → hubs(id) SET NULL
    hub_name              VARCHAR(100),              -- DENORM snapshot khi QR_GENERATED
    estimated_delivery_at DATE,
    qr_code_token         VARCHAR(255),
    status                donation_status NOT NULL DEFAULT 'REGISTERED',
    received_by           UUID,                      -- FK → users(id) SET NULL
    received_at           TIMESTAMPTZ,
    rejection_reason      TEXT,
    created_at            TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT donations_pkey
        PRIMARY KEY (id),
    CONSTRAINT donations_qr_code_unique
        UNIQUE (qr_code_token)
);

CREATE TABLE donation_items (
    id               UUID        NOT NULL DEFAULT gen_random_uuid(),
    donation_id      UUID        NOT NULL,  -- FK → donations(id) CASCADE
    item_category_id UUID        NOT NULL,  -- FK → item_categories(id) RESTRICT
    quantity         INT         NOT NULL,
    expiry_date      DATE,
    condition_notes  TEXT,
    image_url        VARCHAR(500),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT donation_items_pkey
        PRIMARY KEY (id),
    CONSTRAINT donation_items_qty_positive
        CHECK (quantity > 0)
);

-- ------------------------------------------------------------
-- GROUP: MISSIONS
-- [DENORM] snapshot_* columns — vị trí & liên lạc nạn nhân, tại accepted_at
-- ------------------------------------------------------------

CREATE TABLE missions (
    id                       UUID           NOT NULL DEFAULT gen_random_uuid(),
    mission_type             mission_type   NOT NULL,
    sos_request_id           UUID,           -- FK → sos_requests(id) SET NULL (RESCUE only)
    aid_request_id           UUID,           -- FK → aid_requests(id) SET NULL (DELIVERY only)
    volunteer_id             UUID,           -- FK → volunteer_profiles(id) SET NULL
    hub_id                   UUID,           -- FK → hubs(id) SET NULL (DELIVERY only)
    status                   mission_status NOT NULL DEFAULT 'PENDING',
    qr_code_token            VARCHAR(255),
    priority_score           DECIMAL(8,4),
    -- DENORM: snapshot tại thời điểm Volunteer accept
    snapshot_lat             DECIMAL(9,6),
    snapshot_lng             DECIMAL(9,6),
    snapshot_address         TEXT,
    snapshot_requester_name  VARCHAR(100),
    snapshot_requester_phone VARCHAR(20),
    -- Lifecycle timestamps
    accepted_at              TIMESTAMPTZ,
    picked_up_at             TIMESTAMPTZ,
    completed_at             TIMESTAMPTZ,
    cancelled_at             TIMESTAMPTZ,
    cancellation_reason      TEXT,
    confirmation_image_url   VARCHAR(500),
    created_at               TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT missions_pkey
        PRIMARY KEY (id),
    CONSTRAINT missions_qr_code_unique
        UNIQUE (qr_code_token),
    -- RESCUE: sos_request_id bắt buộc; aid_request_id và hub_id phải NULL
    CONSTRAINT missions_rescue_integrity CHECK (
        mission_type <> 'RESCUE'
        OR (    sos_request_id IS NOT NULL
            AND aid_request_id IS NULL
            AND hub_id IS NULL)
    ),
    -- DELIVERY: aid_request_id và hub_id bắt buộc; sos_request_id phải NULL
    CONSTRAINT missions_delivery_integrity CHECK (
        mission_type <> 'DELIVERY'
        OR (    aid_request_id IS NOT NULL
            AND hub_id IS NOT NULL
            AND sos_request_id IS NULL)
    )
);

CREATE TABLE dispatch_attempts (
    id             UUID              NOT NULL DEFAULT gen_random_uuid(),
    mission_id     UUID              NOT NULL,  -- FK → missions(id) CASCADE
    volunteer_id   UUID              NOT NULL,  -- FK → volunteer_profiles(id) RESTRICT
    dispatch_type  VARCHAR(15)       NOT NULL,
    batch_number   INT               NOT NULL DEFAULT 1,
    radius_km      DECIMAL(5,2),
    priority_score DECIMAL(8,4),
    sent_at        TIMESTAMPTZ       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    response       dispatch_response NOT NULL DEFAULT 'PENDING',
    responded_at   TIMESTAMPTZ,
    created_at     TIMESTAMPTZ       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT dispatch_attempts_pkey
        PRIMARY KEY (id),
    CONSTRAINT dispatch_attempts_type_valid
        CHECK (dispatch_type IN ('BROADCAST', 'SEQUENTIAL')),
    CONSTRAINT dispatch_attempts_batch_positive
        CHECK (batch_number >= 1)
);

-- ------------------------------------------------------------
-- GROUP: COMMUNICATION
-- ------------------------------------------------------------

CREATE TABLE chat_messages (
    id           UUID        NOT NULL DEFAULT gen_random_uuid(),
    mission_id   UUID        NOT NULL,  -- FK → missions(id) CASCADE
    sender_id    UUID        NOT NULL,  -- FK → users(id) SET NULL
    message_type VARCHAR(5)  NOT NULL,
    message_text TEXT,
    image_url    VARCHAR(500),
    is_read      BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chat_messages_pkey
        PRIMARY KEY (id),
    CONSTRAINT chat_messages_type_valid
        CHECK (message_type IN ('TEXT', 'IMAGE')),
    -- TEXT XOR IMAGE: chỉ một trong hai được phép
    CONSTRAINT chat_messages_text_xor_image CHECK (
        (message_type = 'TEXT'  AND message_text IS NOT NULL AND image_url    IS NULL)
        OR
        (message_type = 'IMAGE' AND image_url    IS NOT NULL AND message_text IS NULL)
    )
);

CREATE TABLE ratings (
    id         UUID        NOT NULL DEFAULT gen_random_uuid(),
    mission_id UUID        NOT NULL,  -- FK → missions(id) CASCADE  (UNIQUE: 1 rating/mission)
    rater_id   UUID        NOT NULL,  -- FK → users(id) SET NULL    (người đánh giá, thường là Victim)
    ratee_id   UUID        NOT NULL,  -- FK → users(id) SET NULL    (người được đánh giá, thường là Volunteer)
    score      SMALLINT    NOT NULL,
    comment    TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ratings_pkey
        PRIMARY KEY (id),
    CONSTRAINT ratings_mission_unique
        UNIQUE (mission_id),
    CONSTRAINT ratings_score_range
        CHECK (score BETWEEN 1 AND 5)
);

CREATE TABLE notifications (
    id           UUID        NOT NULL DEFAULT gen_random_uuid(),
    user_id      UUID        NOT NULL,  -- FK → users(id) CASCADE
    title        VARCHAR(200) NOT NULL,
    body         TEXT,
    related_type VARCHAR(20),
    related_id   UUID,                  -- Polymorphic FK (no DB constraint)
    is_read      BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT notifications_pkey
        PRIMARY KEY (id),
    CONSTRAINT notifications_related_type_valid
        CHECK (related_type IS NULL
            OR related_type IN ('MISSION', 'SOS', 'DONATION', 'AID_REQUEST'))
);


-- ============================================================
-- SECTION 4: FOREIGN KEY CONSTRAINTS  (ALTER TABLE)
-- Tách riêng tránh circular dependency, dễ đọc/audit
-- ============================================================

-- ---------------- AUTH ----------------------------------------

ALTER TABLE refresh_tokens
    ADD CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE;               -- Token theo User; User xóa → Token xóa

-- ---------------- PROFILES ------------------------------------

ALTER TABLE volunteer_profiles
    ADD CONSTRAINT fk_volunteer_profiles_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE;               -- Profile theo User

ALTER TABLE sponsor_profiles
    ADD CONSTRAINT fk_sponsor_profiles_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE;

-- ---------------- INFRASTRUCTURE ------------------------------

ALTER TABLE hub_staff
    ADD CONSTRAINT fk_hub_staff_hub
        FOREIGN KEY (hub_id)
        REFERENCES hubs(id)
        ON DELETE RESTRICT;              -- Không xóa Hub còn nhân viên active

ALTER TABLE hub_staff
    ADD CONSTRAINT fk_hub_staff_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT;              -- Không xóa User đang là Staff

-- ---------------- CATALOG (self-ref) --------------------------

ALTER TABLE item_categories
    ADD CONSTRAINT fk_item_categories_parent
        FOREIGN KEY (parent_id)
        REFERENCES item_categories(id)
        ON DELETE SET NULL;              -- Xóa parent → children thành root

-- ---------------- INVENTORY -----------------------------------

ALTER TABLE hub_accepted_categories
    ADD CONSTRAINT fk_hub_accepted_cat_hub
        FOREIGN KEY (hub_id)
        REFERENCES hubs(id)
        ON DELETE CASCADE;               -- Hub xóa → xóa toàn bộ danh sách categories chấp nhận

ALTER TABLE hub_accepted_categories
    ADD CONSTRAINT fk_hub_accepted_cat_category
        FOREIGN KEY (item_category_id)
        REFERENCES item_categories(id)
        ON DELETE CASCADE;               -- Category xóa → xóa khỏi danh sách Hub chấp nhận

ALTER TABLE hub_inventories
    ADD CONSTRAINT fk_hub_inventories_hub
        FOREIGN KEY (hub_id)
        REFERENCES hubs(id)
        ON DELETE RESTRICT;              -- Không xóa Hub còn tồn kho

ALTER TABLE hub_inventories
    ADD CONSTRAINT fk_hub_inventories_category
        FOREIGN KEY (item_category_id)
        REFERENCES item_categories(id)
        ON DELETE RESTRICT;              -- Không xóa Category đang được theo dõi tồn kho

ALTER TABLE inventory_logs
    ADD CONSTRAINT fk_inventory_logs_hub_inventory
        FOREIGN KEY (hub_inventory_id)
        REFERENCES hub_inventories(id)
        ON DELETE RESTRICT;              -- Audit log phải bất biến (append-only)

ALTER TABLE inventory_logs
    ADD CONSTRAINT fk_inventory_logs_performed_by
        FOREIGN KEY (performed_by)
        REFERENCES users(id)
        ON DELETE SET NULL;              -- User xóa → giữ log, NULL performer

-- ---------------- REQUESTS ------------------------------------

ALTER TABLE sos_requests
    ADD CONSTRAINT fk_sos_requests_requester
        FOREIGN KEY (requester_id)
        REFERENCES users(id)
        ON DELETE SET NULL;              -- User xóa → giữ SOS record (nullable cho Guest)

ALTER TABLE sos_request_details
    ADD CONSTRAINT fk_sos_request_details_sos
        FOREIGN KEY (sos_request_id)
        REFERENCES sos_requests(id)
        ON DELETE CASCADE;               -- SOS xóa → xóa details (1:1 bắt buộc)

ALTER TABLE aid_requests
    ADD CONSTRAINT fk_aid_requests_requester
        FOREIGN KEY (requester_id)
        REFERENCES users(id)
        ON DELETE RESTRICT;              -- Không xóa User có Aid Request active

ALTER TABLE aid_request_items
    ADD CONSTRAINT fk_aid_request_items_request
        FOREIGN KEY (aid_request_id)
        REFERENCES aid_requests(id)
        ON DELETE CASCADE;               -- Aid Request xóa → xóa tất cả items

ALTER TABLE aid_request_items
    ADD CONSTRAINT fk_aid_request_items_category
        FOREIGN KEY (item_category_id)
        REFERENCES item_categories(id)
        ON DELETE RESTRICT;              -- Không xóa Category đang được yêu cầu

-- ---------------- DONATIONS -----------------------------------

ALTER TABLE donations
    ADD CONSTRAINT fk_donations_sponsor
        FOREIGN KEY (sponsor_id)
        REFERENCES users(id)
        ON DELETE RESTRICT;              -- Không xóa Sponsor còn donation

ALTER TABLE donations
    ADD CONSTRAINT fk_donations_hub
        FOREIGN KEY (hub_id)
        REFERENCES hubs(id)
        ON DELETE SET NULL;              -- Hub xóa → giữ donation record, hub_id = NULL

ALTER TABLE donations
    ADD CONSTRAINT fk_donations_received_by
        FOREIGN KEY (received_by)
        REFERENCES users(id)
        ON DELETE SET NULL;              -- Staff xóa → giữ donation record

ALTER TABLE donation_items
    ADD CONSTRAINT fk_donation_items_donation
        FOREIGN KEY (donation_id)
        REFERENCES donations(id)
        ON DELETE CASCADE;               -- Donation xóa → xóa tất cả items

ALTER TABLE donation_items
    ADD CONSTRAINT fk_donation_items_category
        FOREIGN KEY (item_category_id)
        REFERENCES item_categories(id)
        ON DELETE RESTRICT;

-- ---------------- MISSIONS ------------------------------------

ALTER TABLE missions
    ADD CONSTRAINT fk_missions_sos_request
        FOREIGN KEY (sos_request_id)
        REFERENCES sos_requests(id)
        ON DELETE SET NULL;              -- Giữ mission record dù SOS bị xóa

ALTER TABLE missions
    ADD CONSTRAINT fk_missions_aid_request
        FOREIGN KEY (aid_request_id)
        REFERENCES aid_requests(id)
        ON DELETE SET NULL;

ALTER TABLE missions
    ADD CONSTRAINT fk_missions_volunteer
        FOREIGN KEY (volunteer_id)
        REFERENCES volunteer_profiles(id)
        ON DELETE SET NULL;              -- Giữ mission history dù volunteer xóa profile

ALTER TABLE missions
    ADD CONSTRAINT fk_missions_hub
        FOREIGN KEY (hub_id)
        REFERENCES hubs(id)
        ON DELETE SET NULL;              -- Giữ mission record dù Hub bị xóa

ALTER TABLE dispatch_attempts
    ADD CONSTRAINT fk_dispatch_attempts_mission
        FOREIGN KEY (mission_id)
        REFERENCES missions(id)
        ON DELETE CASCADE;               -- Mission xóa → xóa toàn bộ dispatch audit

ALTER TABLE dispatch_attempts
    ADD CONSTRAINT fk_dispatch_attempts_volunteer
        FOREIGN KEY (volunteer_id)
        REFERENCES volunteer_profiles(id)
        ON DELETE RESTRICT;              -- Không xóa volunteer còn dispatch attempts

-- ---------------- COMMUNICATION -------------------------------

ALTER TABLE chat_messages
    ADD CONSTRAINT fk_chat_messages_mission
        FOREIGN KEY (mission_id)
        REFERENCES missions(id)
        ON DELETE CASCADE;               -- Mission xóa → xóa chat history

ALTER TABLE chat_messages
    ADD CONSTRAINT fk_chat_messages_sender
        FOREIGN KEY (sender_id)
        REFERENCES users(id)
        ON DELETE SET NULL;              -- User xóa → giữ tin nhắn, author = NULL

ALTER TABLE ratings
    ADD CONSTRAINT fk_ratings_mission
        FOREIGN KEY (mission_id)
        REFERENCES missions(id)
        ON DELETE CASCADE;

ALTER TABLE ratings
    ADD CONSTRAINT fk_ratings_rater
        FOREIGN KEY (rater_id)
        REFERENCES users(id)
        ON DELETE SET NULL;              -- Giữ rating khi user bị xóa

ALTER TABLE ratings
    ADD CONSTRAINT fk_ratings_ratee
        FOREIGN KEY (ratee_id)
        REFERENCES users(id)
        ON DELETE SET NULL;

ALTER TABLE notifications
    ADD CONSTRAINT fk_notifications_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE;               -- User xóa → xóa toàn bộ notifications


-- ============================================================
-- SECTION 5: TRIGGERS
-- ============================================================

-- -------- updated_at triggers (tất cả bảng mutable) ---------

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_volunteer_profiles_updated_at
    BEFORE UPDATE ON volunteer_profiles
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_sponsor_profiles_updated_at
    BEFORE UPDATE ON sponsor_profiles
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_hubs_updated_at
    BEFORE UPDATE ON hubs
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_hub_staff_updated_at
    BEFORE UPDATE ON hub_staff
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_shelters_updated_at
    BEFORE UPDATE ON shelters
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_system_config_updated_at
    BEFORE UPDATE ON system_config
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_hub_inventories_updated_at
    BEFORE UPDATE ON hub_inventories
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_sos_requests_updated_at
    BEFORE UPDATE ON sos_requests
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_aid_requests_updated_at
    BEFORE UPDATE ON aid_requests
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_donations_updated_at
    BEFORE UPDATE ON donations
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_missions_updated_at
    BEFORE UPDATE ON missions
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- -------- Business rule triggers ----------------------------

-- item_categories: set parent.is_leaf = FALSE khi thêm child
CREATE TRIGGER trg_item_categories_mark_parent
    AFTER INSERT ON item_categories
    FOR EACH ROW EXECUTE FUNCTION fn_mark_parent_non_leaf();

-- hub_inventories: chỉ leaf categories được dùng
CREATE TRIGGER trg_hub_inventories_assert_leaf
    BEFORE INSERT OR UPDATE ON hub_inventories
    FOR EACH ROW EXECUTE FUNCTION fn_assert_leaf_category();

-- aid_request_items: chỉ leaf categories
CREATE TRIGGER trg_aid_request_items_assert_leaf
    BEFORE INSERT OR UPDATE ON aid_request_items
    FOR EACH ROW EXECUTE FUNCTION fn_assert_leaf_category();

-- donation_items: chỉ leaf categories
CREATE TRIGGER trg_donation_items_assert_leaf
    BEFORE INSERT OR UPDATE ON donation_items
    FOR EACH ROW EXECUTE FUNCTION fn_assert_leaf_category();

-- hub_staff: chỉ role=STAFF được phân công
CREATE TRIGGER trg_hub_staff_assert_role
    BEFORE INSERT OR UPDATE ON hub_staff
    FOR EACH ROW EXECUTE FUNCTION fn_assert_staff_role();


-- ============================================================
-- SECTION 6: INDEXES
-- ============================================================

-- -------- DISPATCH CRITICAL PATH (highest priority) ----------

-- Online Volunteer location — Dispatch Algorithm §7.2
-- Partial index: chỉ quét rows is_online=TRUE (~nhỏ hơn nhiều)
CREATE INDEX idx_vol_online_location
    ON volunteer_profiles (current_lat, current_lng)
    WHERE is_online = TRUE;

-- SOS location — Dispatch origin + Public Map Heatmap §3.2, §7.2
CREATE INDEX idx_sos_location_status
    ON sos_requests (victim_lat, victim_lng, status)
    WHERE status IN ('PENDING', 'DISPATCHING');

-- AID request location — Dispatch + Heatmap
CREATE INDEX idx_aid_location_status
    ON aid_requests (lat, lng, status)
    WHERE status IN ('PENDING', 'DISPATCHING');

-- Hubs active — Smart Hub Selection §6.2
CREATE INDEX idx_hubs_active_latng
    ON hubs (lat, lng)
    WHERE status = 'ACTIVE';

-- Smart Hub Selection: category filter JOIN
CREATE INDEX idx_hub_accepted_cat_category
    ON hub_accepted_categories (item_category_id);

-- -------- FOREIGN KEY INDEXES (prevent FK scan) --------------

CREATE INDEX idx_refresh_tokens_user_id          ON refresh_tokens          (user_id);
CREATE INDEX idx_refresh_tokens_expires_at        ON refresh_tokens          (expires_at);
CREATE INDEX idx_volunteer_profiles_user_id       ON volunteer_profiles       (user_id);
CREATE INDEX idx_sponsor_profiles_user_id         ON sponsor_profiles         (user_id);
CREATE INDEX idx_hub_staff_hub_id                 ON hub_staff               (hub_id);
CREATE INDEX idx_hub_staff_user_id                ON hub_staff               (user_id);
CREATE INDEX idx_item_categories_parent_id        ON item_categories         (parent_id);
CREATE INDEX idx_hub_accepted_cat_hub_id          ON hub_accepted_categories (hub_id);
CREATE INDEX idx_hub_inventories_hub_id           ON hub_inventories         (hub_id);
CREATE INDEX idx_hub_inventories_category_id      ON hub_inventories         (item_category_id);
CREATE INDEX idx_inventory_logs_hub_inv_id        ON inventory_logs          (hub_inventory_id);
CREATE INDEX idx_inventory_logs_performed_by      ON inventory_logs          (performed_by);
CREATE INDEX idx_sos_requests_requester_id        ON sos_requests            (requester_id);
CREATE INDEX idx_aid_requests_requester_id        ON aid_requests            (requester_id);
CREATE INDEX idx_aid_request_items_request_id     ON aid_request_items       (aid_request_id);
CREATE INDEX idx_aid_request_items_category_id    ON aid_request_items       (item_category_id);
CREATE INDEX idx_donations_sponsor_id             ON donations               (sponsor_id);
CREATE INDEX idx_donations_hub_id                 ON donations               (hub_id);
CREATE INDEX idx_donations_received_by            ON donations               (received_by);
CREATE INDEX idx_donation_items_donation_id       ON donation_items          (donation_id);
CREATE INDEX idx_donation_items_category_id       ON donation_items          (item_category_id);
CREATE INDEX idx_missions_volunteer_id            ON missions                (volunteer_id);
CREATE INDEX idx_missions_hub_id                  ON missions                (hub_id);
CREATE INDEX idx_dispatch_attempts_mission_id     ON dispatch_attempts       (mission_id);
CREATE INDEX idx_dispatch_attempts_volunteer_id   ON dispatch_attempts       (volunteer_id);
CREATE INDEX idx_chat_messages_mission_id         ON chat_messages           (mission_id);
CREATE INDEX idx_chat_messages_sender_id          ON chat_messages           (sender_id);
CREATE INDEX idx_notifications_user_id            ON notifications           (user_id);

-- -------- BUSINESS LOGIC INDEXES -----------------------------

-- Missions: trạng thái (Dashboard, History screens)
CREATE INDEX idx_missions_status
    ON missions (status);

-- Missions: tìm theo volunteer + status (Volunteer app §5.3)
CREATE INDEX idx_missions_volunteer_status
    ON missions (volunteer_id, status);

-- Missions: partial index cho RESCUE / DELIVERY lookup
CREATE INDEX idx_missions_sos_id_notnull
    ON missions (sos_request_id)
    WHERE sos_request_id IS NOT NULL;

CREATE INDEX idx_missions_aid_id_notnull
    ON missions (aid_request_id)
    WHERE aid_request_id IS NOT NULL;

-- Missions: sort by created (History screen §5.6)
CREATE INDEX idx_missions_created_at
    ON missions (created_at DESC);

-- Dispatch attempts đang chờ phản hồi (Redis lock cleanup)
CREATE INDEX idx_dispatch_pending
    ON dispatch_attempts (mission_id)
    WHERE response = 'PENDING';

-- Inventory: low stock alert — Staff screen §8.3
CREATE INDEX idx_hub_inv_low_stock
    ON hub_inventories (hub_id, current_quantity)
    WHERE current_quantity <= low_stock_threshold;

-- Polymorphic reference lookup — inventory_logs audit
CREATE INDEX idx_inventory_logs_ref
    ON inventory_logs (reference_type, reference_id);

-- Donations: Sponsor history + filter §6.5
CREATE INDEX idx_donations_sponsor_created
    ON donations (sponsor_id, created_at DESC);

CREATE INDEX idx_donations_hub_status
    ON donations (hub_id, status);

-- Chat: conversation ordered chronologically
CREATE INDEX idx_chat_messages_mission_time
    ON chat_messages (mission_id, created_at ASC);

-- Notifications: unread badge count §10
CREATE INDEX idx_notifications_user_unread
    ON notifications (user_id, created_at DESC)
    WHERE is_read = FALSE;

-- Users: role lookup (Admin user management §9.1)
CREATE INDEX idx_users_role
    ON users (role);

-- Users: active + verified (auth guard, frequent check)
CREATE INDEX idx_users_active_verified
    ON users (is_active, is_verified)
    WHERE is_active = TRUE AND is_verified = TRUE;

-- SOS history — Victim §4.6
CREATE INDEX idx_sos_requests_requester_created
    ON sos_requests (requester_id, created_at DESC);

-- AID history — Victim §4.6
CREATE INDEX idx_aid_requests_requester_created
    ON aid_requests (requester_id, created_at DESC);

-- Item categories: leaf-only lookup (form dropdowns)
CREATE INDEX idx_item_categories_is_leaf
    ON item_categories (is_leaf)
    WHERE is_leaf = TRUE;


-- ============================================================
-- SECTION 7: TABLE & COLUMN COMMENTS
-- ============================================================

-- ---------------------- users --------------------------------
COMMENT ON TABLE users IS
    'Tất cả actor: VICTIM, VOLUNTEER, SPONSOR, STAFF, ADMIN. '
    'OTP fields gộp vào đây (single-use, TTL từ system_config[otp.ttl_minutes]). '
    'STAFF và ADMIN không tự đăng ký — chỉ Admin tạo (§3.3 requirements).';

COMMENT ON COLUMN users.role           IS 'Vai trò: VICTIM|VOLUNTEER|SPONSOR|STAFF|ADMIN.';
COMMENT ON COLUMN users.is_verified    IS 'FALSE sau khi đăng ký; TRUE sau khi xác thực OTP thành công.';
COMMENT ON COLUMN users.is_active      IS 'FALSE = bị Admin soft-block (§9.1). Login → thông báo "Tài khoản đã bị tạm khóa."';
COMMENT ON COLUMN users.fcm_token      IS 'Firebase Cloud Messaging token — cập nhật mỗi lần login.';
COMMENT ON COLUMN users.otp_code       IS 'OTP 6 chữ số đang active. NULL khi không có OTP pending.';
COMMENT ON COLUMN users.otp_type       IS 'EMAIL_VERIFY | PHONE_VERIFY | PASSWORD_RESET.';
COMMENT ON COLUMN users.otp_expires_at IS 'TTL OTP từ system_config[otp.ttl_minutes], mặc định 10 phút.';

-- ---------------------- refresh_tokens -----------------------
COMMENT ON TABLE refresh_tokens IS
    'JWT Refresh Token store — chỉ lưu SHA-256 hash (không bao giờ raw token). '
    'Redis blacklist cho revocation realtime; bảng này là durable backup khi Redis restart.';

COMMENT ON COLUMN refresh_tokens.token_hash  IS 'SHA-256 hex của raw token — không log/lưu plain text.';
COMMENT ON COLUMN refresh_tokens.device_info IS 'User-Agent/OS để hiển thị active sessions.';
COMMENT ON COLUMN refresh_tokens.is_revoked  IS 'TRUE = đã thu hồi. Cần kiểm tra cả Redis blacklist.';

-- ---------------------- volunteer_profiles -------------------
COMMENT ON TABLE volunteer_profiles IS
    'Mở rộng users cho VOLUNTEER. Lưu aggregate stats cho Priority Score §7.1. '
    'Partial index WHERE is_online=TRUE tối ưu dispatch query.';

COMMENT ON COLUMN volunteer_profiles.is_online             IS 'Toggle online/offline §5.1. Không được tắt khi đang có mission active.';
COMMENT ON COLUMN volunteer_profiles.current_lat           IS 'GPS mới nhất — cập nhật 3-5s/lần qua Redis Geo; cột này là fallback khi Redis restart.';
COMMENT ON COLUMN volunteer_profiles.total_tasks_completed IS 'DENORM T-factor: tăng atomic khi mission → COMPLETED.';
COMMENT ON COLUMN volunteer_profiles.avg_rating            IS 'DENORM R-factor (0-5): cập nhật atomic khi INSERT ratings.';
COMMENT ON COLUMN volunteer_profiles.avg_response_seconds  IS 'DENORM A-factor (inverse): thấp hơn = điểm cao hơn trong Priority Score.';

-- ---------------------- sponsor_profiles ---------------------
COMMENT ON TABLE sponsor_profiles IS
    'Mở rộng users cho SPONSOR. Gamification aggregate stats §6.5. '
    'Cập nhật atomic cùng transaction khi donation → RECEIVED.';

COMMENT ON COLUMN sponsor_profiles.total_points        IS 'DENORM: điểm tích lũy (1 item = 1 điểm).';
COMMENT ON COLUMN sponsor_profiles.total_items_donated IS 'DENORM: tổng vật phẩm đã đóng góp.';
COMMENT ON COLUMN sponsor_profiles.donation_count      IS 'DENORM: số lần donate. Badge: BRONZE≥1, SILVER≥10, GOLD≥25, PLATINUM≥50.';
COMMENT ON COLUMN sponsor_profiles.badge_level         IS 'Cấp độ badge hiện tại — tự động nâng khi donation_count đạt ngưỡng.';

-- ---------------------- hubs ---------------------------------
COMMENT ON TABLE hubs IS
    'Trạm trung chuyển vật lý — trung tâm chuỗi cung ứng. '
    'EMERGENCY status → hệ thống tự điều phối lại missions tại trạm này (§8.3).';

COMMENT ON COLUMN hubs.status IS 'ACTIVE: hoạt động | INACTIVE: tạm ngừng | EMERGENCY: sự cố, auto re-dispatch.';
COMMENT ON COLUMN hubs.lat    IS 'Vĩ độ — dùng cho Haversine distance (Smart Hub Selection §6.2, Public Map §3.2).';

-- ---------------------- hub_staff ----------------------------
COMMENT ON TABLE hub_staff IS
    'Phân công Staff vào Hub. Partial UNIQUE idx (hub_id, user_id) WHERE unassigned_at IS NULL '
    'ngăn duplicate active assignment. Trigger fn_assert_staff_role kiểm tra role=STAFF.';

COMMENT ON COLUMN hub_staff.assigned_at   IS 'Thời điểm bắt đầu phân công.';
COMMENT ON COLUMN hub_staff.unassigned_at IS 'NULL = đang trực. NOT NULL = kết thúc phân công (soft pattern).';
COMMENT ON COLUMN hub_staff.is_available  IS 'Toggle sẵn sàng — hệ thống dùng để quyết định gửi FCM thông báo.';

-- ---------------------- shelters -----------------------------
COMMENT ON TABLE shelters IS
    'Nơi trú ẩn khẩn cấp, hiển thị trên Public Map §3.2. '
    'Không liên kết inventory — chỉ cung cấp thông tin chỗ ở.';

COMMENT ON COLUMN shelters.status IS 'AVAILABLE: còn chỗ | FULL: hết chỗ | UNAVAILABLE: tạm đóng.';

-- ---------------------- system_config ------------------------
COMMENT ON TABLE system_config IS
    'Runtime config — Admin chỉnh qua §9.6 Settings, áp dụng ngay không restart service. '
    'Backend Spring Boot đọc định kỳ (Spring @RefreshScope).';

COMMENT ON COLUMN system_config.key   IS 'Ví dụ: dispatch.weight.distance, otp.ttl_minutes.';
COMMENT ON COLUMN system_config.value IS 'TEXT — backend parse sang đúng kiểu (Float/Int) khi dùng.';

-- ---------------------- item_categories ----------------------
COMMENT ON TABLE item_categories IS
    'Cây danh mục 2 cấp: Root (is_leaf=FALSE) → Leaf (is_leaf=TRUE). '
    'Chỉ LEAF được dùng trong inventory/request/donation. '
    'Trigger fn_mark_parent_non_leaf auto-set parent.is_leaf=FALSE khi thêm child.';

COMMENT ON COLUMN item_categories.parent_id IS 'NULL = root category. Self-ref FK.';
COMMENT ON COLUMN item_categories.is_leaf   IS 'TRUE = có thể dùng trong stock/request/donation. FALSE = chỉ là nhóm.';
COMMENT ON COLUMN item_categories.unit      IS 'Đơn vị: Kg, Thùng, Hộp, Bộ, Chai... Auto-fill trên form.';

-- ---------------------- hub_accepted_categories --------------
COMMENT ON TABLE hub_accepted_categories IS
    'Junction table M:N (Hub ↔ ItemCategory): Hub nào nhận loại hàng nào. '
    'Admin cấu hình per-hub. JOIN trong Smart Hub Selection query §6.2.';

-- ---------------------- hub_inventories ----------------------
COMMENT ON TABLE hub_inventories IS
    'Tồn kho hiện tại per (Hub, Category). '
    'UNIQUE(hub_id, item_category_id). Số lượng chỉ thay đổi qua luồng nhập/xuất kho §8.1 §8.2.';

COMMENT ON COLUMN hub_inventories.current_quantity    IS 'CHECK >= 0 ngăn tồn kho âm.';
COMMENT ON COLUMN hub_inventories.low_stock_threshold IS 'current_quantity <= threshold → badge CAM (Staff §8.3) + notify Admin.';

-- ---------------------- inventory_logs -----------------------
COMMENT ON TABLE inventory_logs IS
    'Append-only audit trail. quantity_after là DENORM snapshot O(1) time-travel query. '
    'reference_id là polymorphic FK (DONATION id | MISSION id) — application validate.';

COMMENT ON COLUMN inventory_logs.change_type    IS 'INBOUND: hàng vào từ Sponsor | OUTBOUND: hàng ra cho Volunteer.';
COMMENT ON COLUMN inventory_logs.quantity_delta IS 'Luôn dương (>0). Chiều xác định bởi change_type.';
COMMENT ON COLUMN inventory_logs.reference_type IS 'DONATION = nhập kho từ Sponsor | MISSION = xuất kho cho Volunteer.';
COMMENT ON COLUMN inventory_logs.reference_id   IS 'Polymorphic: donations.id hoặc missions.id. Không có DB-level FK.';
COMMENT ON COLUMN inventory_logs.quantity_after IS 'DENORM snapshot — tồn kho SAU transaction. Query tại thời điểm T = O(1).';

-- ---------------------- sos_requests (HOT) -------------------
COMMENT ON TABLE sos_requests IS
    'HOT TABLE [Vertical Split v3.1]: chỉ cột dispatch/heatmap-critical. '
    'TEXT fields nằm trong sos_request_details. requester_id=NULL cho Guest SOS §3.1.';

COMMENT ON COLUMN sos_requests.requester_id  IS 'NULL = Guest anonymous. FK → users ON DELETE SET NULL.';
COMMENT ON COLUMN sos_requests.victim_lat    IS 'Tọa độ nạn nhân — index heatmap §3.2 và dispatch origin §7.2.';
COMMENT ON COLUMN sos_requests.urgency_level IS 'AI-classified từ description. CRITICAL/HIGH/MEDIUM/LOW.';
COMMENT ON COLUMN sos_requests.is_on_behalf  IS 'TRUE = người gửi đang báo hộ người khác §4.3. Dispatch tại victim_location.';

-- ---------------------- sos_request_details (COLD) -----------
COMMENT ON TABLE sos_request_details IS
    'COLD TABLE [Vertical Split v3.1]: TEXT lớn ít dùng trong dispatch. '
    'JOIN chỉ khi hiển thị chi tiết (Volunteer mission screen, Admin dashboard). '
    'PK = FK → sos_requests.id (1:1 bắt buộc, ON DELETE CASCADE).';

COMMENT ON COLUMN sos_request_details.description IS 'Mô tả tình trạng: đặc điểm nhà, sức khỏe... AI dùng để classify urgency.';
COMMENT ON COLUMN sos_request_details.ai_summary  IS 'Tóm tắt do AI sinh — hiển thị trên Admin dashboard.';

-- ---------------------- aid_requests -------------------------
COMMENT ON TABLE aid_requests IS
    'Yêu cầu tiếp tế nhu yếu phẩm §4.2. Header record; line items trong aid_request_items. '
    'Submit → Dispatch DELIVERY Algorithm §7.2.';

COMMENT ON COLUMN aid_requests.adults_count  IS 'Số người lớn. Dùng tính quantity per item.';
COMMENT ON COLUMN aid_requests.elderly_count IS 'Số người già.';
COMMENT ON COLUMN aid_requests.children_count IS 'Số trẻ em.';

-- ---------------------- aid_request_items --------------------
COMMENT ON TABLE aid_request_items IS
    'Line items của Aid Request. Chỉ leaf categories. '
    'quantity = adults + elderly + children per item (tính bởi backend §4.2).';

-- ---------------------- donations ----------------------------
COMMENT ON TABLE donations IS
    'Header lô hàng ký gửi §6.1. Lifecycle: REGISTERED → QR_GENERATED → RECEIVED | REJECTED. '
    'hub_name là DENORM snapshot khi Sponsor chọn Hub — tránh JOIN cho QR Screen §6.3.';

COMMENT ON COLUMN donations.qr_code_token IS 'Single-use QR. NULL → QR_GENERATED → RECEIVED (hết hiệu lực). §6.3.';
COMMENT ON COLUMN donations.hub_name      IS 'DENORM snapshot tên Hub tại QR_GENERATED. Dùng QR Screen + History §6.5.';
COMMENT ON COLUMN donations.received_by   IS 'Staff user đã confirm nhập kho §8.1.';

-- ---------------------- donation_items -----------------------
COMMENT ON TABLE donation_items IS
    'Line items của Donation §6.1. Staff xem khi nhập kho §8.1. '
    'condition_notes và expiry_date hiển thị để Staff kiểm tra thực tế.';

-- ---------------------- missions -----------------------------
COMMENT ON TABLE missions IS
    'Đơn vị công việc của Volunteer. RESCUE: từ sos_request, không qua Hub. '
    'DELIVERY: từ aid_request, bắt buộc qua Hub. '
    'snapshot_* columns là DENORM tại accepted_at — tránh JOIN trong Live Tracking §4.4 §5.4.';

COMMENT ON COLUMN missions.mission_type              IS 'RESCUE: cứu hộ khẩn cấp | DELIVERY: giao tiếp tế.';
COMMENT ON COLUMN missions.qr_code_token             IS 'Volunteer đưa Staff quét tại Hub §5.3 (DELIVERY Step 1-2).';
COMMENT ON COLUMN missions.priority_score            IS 'Snapshot điểm Priority Score tại assign — dùng cho Admin analytics §9.5.';
COMMENT ON COLUMN missions.snapshot_lat              IS 'DENORM: tọa độ đích tại accepted_at — Live Tracking không cần JOIN.';
COMMENT ON COLUMN missions.snapshot_lng              IS 'DENORM: tọa độ đích.';
COMMENT ON COLUMN missions.snapshot_address          IS 'DENORM: địa chỉ đích — Navigation screen.';
COMMENT ON COLUMN missions.snapshot_requester_name   IS 'DENORM: tên nạn nhân — Mission Screen §5.3.';
COMMENT ON COLUMN missions.snapshot_requester_phone  IS 'DENORM: SĐT nạn nhân — tap to call.';
COMMENT ON COLUMN missions.confirmation_image_url    IS 'Ảnh xác nhận hoàn thành — Volunteer chụp khi COMPLETED §5.3.';

-- ---------------------- dispatch_attempts --------------------
COMMENT ON TABLE dispatch_attempts IS
    'Audit log từng lần dispatch gửi đến Volunteer §7.2. '
    'Admin phân tích hiệu quả algorithm §9.5. Ghi cả ACCEPTED, REJECTED, TIMEOUT.';

COMMENT ON COLUMN dispatch_attempts.dispatch_type  IS 'BROADCAST: Top-N đồng thời (RESCUE) | SEQUENTIAL: tuần tự Round 1-2+ (DELIVERY).';
COMMENT ON COLUMN dispatch_attempts.batch_number   IS 'Round dispatch. RESCUE=1. DELIVERY tăng dần (Round 1, 2, 3...).';
COMMENT ON COLUMN dispatch_attempts.radius_km      IS 'Bán kính tại thời điểm dispatch (1/3/5/10 km từ system_config).';
COMMENT ON COLUMN dispatch_attempts.priority_score IS 'Snapshot điểm volunteer này tại thời điểm gửi.';

-- ---------------------- chat_messages ------------------------
COMMENT ON TABLE chat_messages IS
    'Chat scoped theo mission_id §4.5 §5.5. TEXT XOR IMAGE constraint. '
    'Lịch sử chat giữ nguyên sau khi mission hoàn thành.';

COMMENT ON COLUMN chat_messages.message_type IS 'TEXT: văn bản | IMAGE: ảnh upload.';

-- ---------------------- ratings ------------------------------
COMMENT ON TABLE ratings IS
    'Đánh giá sau mission COMPLETED §4.4. '
    'UNIQUE(mission_id) = mỗi mission chỉ 1 lần rating. '
    'INSERT → UPDATE volunteer_profiles.avg_rating atomic.';

COMMENT ON COLUMN ratings.rater_id IS 'Người đánh giá — thường là Victim.';
COMMENT ON COLUMN ratings.ratee_id IS 'Người được đánh giá — thường là Volunteer user.';
COMMENT ON COLUMN ratings.score    IS '1-5 sao. Cập nhật volunteer_profiles.avg_rating sau INSERT.';

-- ---------------------- notifications ------------------------
COMMENT ON TABLE notifications IS
    'In-app notification inbox §10. Mỗi FCM push có 1 bản ghi tương ứng. '
    'related_id là polymorphic FK — application validate.';

COMMENT ON COLUMN notifications.related_type IS 'MISSION | SOS | DONATION | AID_REQUEST.';
COMMENT ON COLUMN notifications.related_id   IS 'Polymorphic FK — không có DB-level constraint.';


-- ============================================================
-- SECTION 8: DEFAULT SEED DATA — system_config
-- ============================================================

INSERT INTO system_config (key, value, description) VALUES

-- OTP
('otp.ttl_minutes',                '10',
    'Thời gian sống OTP (phút). Sau hết hạn, phải request OTP mới. Dùng cho đăng ký và quên mật khẩu.'),

-- ---- Priority Score Weights (tổng phải = 1.00) §7.1 --------
('dispatch.weight.distance',        '0.40',
    'D-factor: khoảng cách Volunteer → nạn nhân. Gần hơn = điểm cao hơn.'),

('dispatch.weight.rating',          '0.20',
    'R-factor: avg_rating Volunteer (0–5). Lấy trực tiếp từ volunteer_profiles.avg_rating.'),

('dispatch.weight.tasks',           '0.15',
    'T-factor: total_tasks_completed. Normalize trong batch hiện tại.'),

('dispatch.weight.response_time',   '0.15',
    'A-factor: avg_response_seconds. Thấp hơn = điểm cao hơn (inverse).'),

('dispatch.weight.area_experience', '0.10',
    'E-factor: kinh nghiệm khu vực. Tạm = 0 trong MVP (volunteer_area_experiences chưa triển khai).'),

-- ---- Dispatch radius steps §7.2 Bước 2 ---------------------
('dispatch.radius.step1_km',        '1',
    'Vòng tìm TNV bán kính 1 km đầu tiên.'),

('dispatch.radius.step2_km',        '3',
    'Mở rộng 3 km nếu bước 1 không đủ TNV.'),

('dispatch.radius.step3_km',        '5',
    'Mở rộng 5 km nếu bước 2 thất bại.'),

('dispatch.radius.step4_km',        '10',
    'Mở rộng 10 km — bước cuối trước khi báo lỗi không tìm được TNV.'),

-- ---- Dispatch time windows §7.2 Bước 4 ---------------------
('dispatch.sos.window_seconds',     '30',
    'RESCUE BROADCAST: TNV có 30 giây để accept. Hết hạn → TIMEOUT, tiếp tục broadcast vòng mới.'),

('dispatch.delivery.window_1',      '15',
    'DELIVERY Round 1: TNV #1 có 15 giây exclusive. Hết hạn → broadcast Round 2.'),

('dispatch.delivery.window_batch',  '20',
    'DELIVERY Round 2+: Broadcast batch #2-#4 có 20 giây. Hết hạn → mở rộng bán kính.')

ON CONFLICT (key) DO NOTHING;


-- ============================================================
-- END OF SCHEMA
-- ============================================================
-- Verification query (uncomment to check after execution):
-- SELECT table_name, (SELECT COUNT(*) FROM information_schema.columns
--     WHERE table_name = t.table_name AND table_schema = 'public') AS col_count
-- FROM information_schema.tables t
-- WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
-- ORDER BY table_name;
-- ============================================================
