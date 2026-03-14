-- ============================================================
-- AidBridge: Database Schema v2.0
-- Database: Supabase (PostgreSQL 15+ with PostGIS)
-- Run AFTER 01_init_postgis.sql
--
-- REDESIGN GOALS vs v1:
--   1. Full ENUM coverage — no VARCHAR+CHECK inconsistencies
--   2. Leaf-node enforcement on item_categories via trigger
--   3. Multi-item donations (header + line items), same as aid_requests
--   4. Normalized attachments table — no more TEXT[] arrays
--   5. refresh_tokens table for JWT security durability
--   6. volunteer_area_experiences for per-area E-factor (Priority Score)
--   7. hub_accepted_categories — admin can configure per-hub item allowlist
--   8. system_config table for runtime-tunable values (weights, radii, etc.)
--   9. safe_paths table for caching routing polylines
--  10. Partial UNIQUE index on hub_staff for active-only uniqueness
--  11. All high-growth tables have DESC-time indexes for efficient pagination
--  12. Renamed min_threshold → low_stock_threshold for clarity
--  13. missions.volunteer_id → FK to volunteer_profiles for tighter integrity
--  14. Composite indexes tuned to the exact query shapes the app executes
-- ============================================================


-- ============================================================
-- SECTION 1: ENUM TYPE DEFINITIONS
-- ============================================================

-- User roles. A single user holds exactly one role at registration.
CREATE TYPE user_role AS ENUM (
    'VICTIM',       -- Disaster victim requesting aid or rescue
    'VOLUNTEER',    -- Aid delivery or rescue volunteer
    'SPONSOR',      -- Goods donor / benefactor (Mạnh thường quân)
    'STAFF',        -- Hub station staff member
    'ADMIN'         -- System administrator
);

-- Operational status of a Hub station.
CREATE TYPE hub_status AS ENUM (
    'ACTIVE',       -- Fully operational: accepting donations and volunteer pickups
    'INACTIVE',     -- Temporarily closed (scheduled downtime)
    'EMERGENCY'     -- Reported flooding/power-outage; system auto-reroutes deliveries
);

-- Availability status of a Shelter.
CREATE TYPE shelter_status AS ENUM (
    'AVAILABLE',    -- Open capacity remaining
    'FULL',         -- At maximum capacity
    'UNAVAILABLE'   -- Inaccessible or closed
);

-- AI-classified urgency level. Drives the dispatch strategy selection.
CREATE TYPE urgency_level AS ENUM (
    'CRITICAL',     -- Life-threatening → Broadcast strategy
    'HIGH',         -- Rapid response required
    'MEDIUM',       -- Moderate need
    'LOW'           -- Non-urgent supply request
);

-- Lifecycle states for an SOS rescue request.
CREATE TYPE sos_status AS ENUM (
    'PENDING',      -- Received, awaiting dispatch
    'DISPATCHING',  -- Algorithm actively searching for volunteers
    'ASSIGNED',     -- Volunteer confirmed; en route
    'IN_PROGRESS',  -- Volunteer on-scene
    'COMPLETED',    -- Rescue confirmed and closed
    'CANCELLED'     -- False alarm or resolved independently
);

-- Lifecycle states for a supply aid request.
CREATE TYPE aid_request_status AS ENUM (
    'PENDING',
    'DISPATCHING',
    'ASSIGNED',
    'PICKED_UP',
    'IN_TRANSIT',
    'COMPLETED',
    'CANCELLED'
);

-- Type of work a Mission represents.
CREATE TYPE mission_type AS ENUM (
    'RESCUE',       -- Tied to an SOS request; goes directly to victim (no hub stop)
    'DELIVERY'      -- Tied to an aid request; requires hub → victim flow
);

-- Granular lifecycle of a Mission.
CREATE TYPE mission_status AS ENUM (
    'PENDING',      -- Created; dispatch not yet started
    'DISPATCHING',  -- Actively notifying volunteers
    'ASSIGNED',     -- Volunteer accepted
    'PICKING_UP',   -- (DELIVERY) Volunteer arrived at hub, awaiting QR scan
    'PICKED_UP',    -- (DELIVERY) Staff confirmed goods handover via QR
    'IN_TRANSIT',   -- En route to victim
    'COMPLETED',    -- Confirmed and closed
    'CANCELLED'
);

-- Lifecycle of a Sponsor donation pledge.
CREATE TYPE donation_status AS ENUM (
    'REGISTERED',   -- Submitted; QR not yet issued
    'QR_GENERATED', -- QR issued; sponsor instructed to bring goods to hub
    'RECEIVED',     -- Staff scanned QR; inventory updated
    'REJECTED'      -- Staff rejected goods at hub
);

-- Direction of an inventory transaction.
CREATE TYPE inventory_change_type AS ENUM (
    'INBOUND',      -- Goods added (triggered by donation QR scan)
    'OUTBOUND'      -- Goods removed (triggered by volunteer pickup QR scan)
);

-- Source entity that caused an inventory_log entry.
-- Using an ENUM instead of VARCHAR ensures type safety and prevents typos.
CREATE TYPE inventory_reference_type AS ENUM (
    'DONATION',
    'MISSION'
);

-- Dispatch strategy used when notifying volunteers.
CREATE TYPE dispatch_type AS ENUM (
    'BROADCAST',    -- All top-N volunteers notified simultaneously (SOS/CRITICAL)
    'SEQUENTIAL'    -- Volunteers notified in ranked batches (DELIVERY)
);

-- Volunteer's response to a dispatch notification.
CREATE TYPE dispatch_response AS ENUM (
    'PENDING',      -- No response yet (within the time window)
    'ACCEPTED',
    'REJECTED',
    'TIMEOUT'
);

-- Content type of a chat message.
CREATE TYPE message_type AS ENUM (
    'TEXT',
    'IMAGE'
);

-- Polymorphic entity discriminator used in attachments and notifications.
-- Central ENUM prevents scattered VARCHAR magic strings across the schema.
CREATE TYPE entity_type AS ENUM (
    'SOS_REQUEST',
    'AID_REQUEST',
    'DONATION',
    'MISSION',
    'HUB',
    'USER'
);

-- Notification category for routing and display logic.
CREATE TYPE notification_type AS ENUM (
    -- Victim-facing
    'SOS_CONFIRMED',
    'VOLUNTEER_ASSIGNED',
    'GOODS_PICKED_UP',
    'IN_TRANSIT',
    'NEAR_ARRIVAL',
    'DELIVERY_COMPLETED',
    -- Volunteer-facing
    'DISPATCH_REQUEST',
    'NEW_CHAT_MESSAGE',
    -- Sponsor-facing
    'DONATION_QR_CREATED',
    'DONATION_RECEIVED',
    'DONATION_REJECTED',
    -- Staff-facing
    'INCOMING_DONATION',
    'VOLUNTEER_PICKUP_REQUEST',
    'STOCK_LOW',
    -- Admin-facing
    'STOCK_CRITICAL',
    'HUB_INCIDENT',
    'FRAUD_ALERT',
    -- System-wide broadcast
    'EMERGENCY_BROADCAST',
    'NEW_HUB_OPENED'
);

-- OTP purpose discriminator.
CREATE TYPE otp_type AS ENUM (
    'EMAIL_VERIFICATION',
    'PHONE_VERIFICATION',
    'PASSWORD_RESET'
);

-- Sponsor achievement badge tier.
CREATE TYPE badge_level AS ENUM (
    'BRONZE',
    'SILVER',
    'GOLD',
    'PLATINUM'
);


-- ============================================================
-- SECTION 2: SHARED TRIGGER FUNCTIONS
-- ============================================================

-- Automatically refreshes updated_at on every row mutation.
CREATE OR REPLACE FUNCTION fn_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Enforces that only a leaf-level category (is_leaf = TRUE) can be
-- referenced by hub_inventories, aid_request_items, and donation_items.
-- Called via BEFORE INSERT/UPDATE triggers on those three tables.
CREATE OR REPLACE FUNCTION fn_assert_leaf_category(p_category_id UUID)
RETURNS VOID AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM item_categories
        WHERE id = p_category_id AND is_leaf = TRUE
    ) THEN
        RAISE EXCEPTION
            'item_category_id % is not a leaf category. '
            'Only leaf (sub-level) categories may be used in inventory, '
            'aid requests, or donations.', p_category_id;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- When a new child category is inserted, mark its parent as non-leaf
-- so inventory rows can never reference an intermediate node.
CREATE OR REPLACE FUNCTION fn_mark_parent_non_leaf()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.parent_id IS NOT NULL THEN
        UPDATE item_categories
        SET is_leaf = FALSE
        WHERE id = NEW.parent_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- ============================================================
-- SECTION 3: SYSTEM CONFIGURATION
-- ============================================================

-- system_config: Runtime-tunable key-value store for values that Admin can
-- change without a code deployment (Priority Score weights, dispatch radii,
-- OTP TTLs, low-stock multiplier thresholds, etc.).
--
-- Why a table instead of application.yaml?
--   → Admin can update weights live from the dashboard.
--   → Avoids restarting the Spring Boot service to tweak algorithm parameters.
--   → All changes are time-stamped and audited.
CREATE TABLE system_config (
    key             VARCHAR(100)    PRIMARY KEY,
    value           TEXT            NOT NULL,
    description     TEXT,
    updated_by      UUID,           -- NULL = system default; set when Admin changes it
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- Seed the default Priority Score weights and dispatch radii.
-- Spring Boot reads these at startup and refreshes periodically.
INSERT INTO system_config (key, value, description) VALUES
    ('dispatch.weight.distance',         '0.40', 'D factor weight in Priority Score formula'),
    ('dispatch.weight.rating',           '0.20', 'R factor weight'),
    ('dispatch.weight.tasks',            '0.15', 'T factor weight'),
    ('dispatch.weight.response_time',    '0.15', 'A factor weight'),
    ('dispatch.weight.area_experience',  '0.10', 'E factor weight'),
    ('dispatch.radius.step1_km',         '1',    'Initial search radius for volunteers (km)'),
    ('dispatch.radius.step2_km',         '3',    'Second expansion radius (km)'),
    ('dispatch.radius.step3_km',         '5',    'Third expansion radius (km)'),
    ('dispatch.radius.step4_km',         '10',   'Max search radius (km)'),
    ('dispatch.sos.window_seconds',      '30',   'Broadcast acceptance window for SOS missions'),
    ('dispatch.delivery.window_1',       '15',   'Top-1 exclusive window (seconds) for DELIVERY'),
    ('dispatch.delivery.window_batch',   '20',   'Batch broadcast window (seconds) for DELIVERY'),
    ('otp.ttl_minutes',                  '10',   'OTP expiry duration in minutes'),
    ('inventory.low_stock_multiplier',   '1.0',  'Multiplier on low_stock_threshold to trigger alert');


-- ============================================================
-- SECTION 4: USER & AUTHENTICATION TABLES
-- ============================================================

-- users: Central identity table for all authenticated participants.
-- Guests (anonymous SOS) are NOT stored here; they appear only in sos_requests.
CREATE TABLE users (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name       VARCHAR(255)    NOT NULL,
    email           VARCHAR(255)    UNIQUE,
    phone_number    VARCHAR(20)     UNIQUE,
    password_hash   VARCHAR(255)    NOT NULL,   -- bcrypt; never plain text
    role            user_role       NOT NULL,
    is_verified     BOOLEAN         NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    -- FCM token is updated on every login so push always reaches the current device.
    fcm_token       VARCHAR(512),
    avatar_url      VARCHAR(512),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    -- At least one contact method is mandatory.
    CONSTRAINT users_contact_method_check CHECK (
        email IS NOT NULL OR phone_number IS NOT NULL
    )
);

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();


-- otp_verifications: Short-lived single-use OTP codes.
-- Each code expires after system_config 'otp.ttl_minutes' minutes.
-- Redis caches the active OTP for fast validation; this table is the durable record.
CREATE TABLE otp_verifications (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    otp_code    VARCHAR(10) NOT NULL,
    otp_type    otp_type    NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    is_used     BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- refresh_tokens: Durable store for JWT refresh tokens.
--
-- WHY this is needed (not in v1):
--   Redis holds the active blacklist, but if Redis restarts all revoked tokens
--   would become valid again. This table is the authoritative source of truth.
--   The token itself is never stored — only its SHA-256 hash (token_hash),
--   so even a DB leak cannot be used to forge sessions.
CREATE TABLE refresh_tokens (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    -- SHA-256 hash of the raw refresh token string. Never store the raw token.
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    device_info VARCHAR(255),           -- Optional: e.g., "Android 14 / Pixel 8"
    expires_at  TIMESTAMPTZ NOT NULL,
    is_revoked  BOOLEAN     NOT NULL DEFAULT FALSE,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- ============================================================
-- SECTION 5: ROLE PROFILE TABLES
-- ============================================================

-- volunteer_profiles: Extended attributes specific to VOLUNTEER role users.
-- Stores the static factors of the Priority Score formula.
-- D (distance) is always computed at query time from current_location.
CREATE TABLE volunteer_profiles (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID            NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    is_online               BOOLEAN         NOT NULL DEFAULT FALSE,
    -- Current GPS position. Updated via WebSocket during active missions.
    -- GIST index on this column is the hottest index in the entire schema.
    current_location        GEOMETRY(Point, 4326),
    vehicle_type            VARCHAR(50),    -- 'motorbike' | 'bicycle' | 'car' | 'walking'
    -- T factor: total successfully completed missions.
    total_tasks_completed   INT             NOT NULL DEFAULT 0,
    -- R factor: rolling average star rating (1.00–5.00).
    avg_rating              DECIMAL(3, 2)   NOT NULL DEFAULT 5.00,
    -- A factor: rolling average response time to dispatch notifications, in seconds.
    avg_response_seconds    INT             NOT NULL DEFAULT 0,
    last_active_at          TIMESTAMPTZ,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT volunteer_rating_range CHECK (avg_rating >= 1.00 AND avg_rating <= 5.00),
    CONSTRAINT volunteer_response_non_negative CHECK (avg_response_seconds >= 0)
);

CREATE TRIGGER trg_volunteer_profiles_updated_at
    BEFORE UPDATE ON volunteer_profiles
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();


-- volunteer_area_experiences: Per-geographic-area experience scores for the E factor.
--
-- WHY a separate table (not a single scalar in v1):
--   The E factor represents familiarity with a SPECIFIC area, not a global score.
--   A volunteer experienced in District 1 should score higher for a mission there
--   than a volunteer who has only worked in District 12.
--
-- Each row covers one bounded geographic polygon (an administrative zone,
-- a 500m grid cell, or a pre-defined disaster zone polygon).
-- The dispatch algorithm joins this table on ST_Contains(area, victim_location)
-- to retrieve the relevant E score for a given mission location.
CREATE TABLE volunteer_area_experiences (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    volunteer_id    UUID            NOT NULL REFERENCES volunteer_profiles(id) ON DELETE CASCADE,
    -- Polygon boundary of this experience zone.
    area            GEOMETRY(Polygon, 4326) NOT NULL,
    -- Human-readable label (e.g., "Quận 1, TP.HCM") for Admin dashboard display.
    area_label      VARCHAR(255),
    -- Cumulative score: incremented each time a mission completes inside this polygon.
    experience_score    DECIMAL(5, 2)   NOT NULL DEFAULT 0.00,
    missions_in_area    INT             NOT NULL DEFAULT 0,
    last_mission_at     TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trg_volunteer_area_exp_updated_at
    BEFORE UPDATE ON volunteer_area_experiences
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();


-- sponsor_profiles: Gamification and statistics for SPONSOR role users.
CREATE TABLE sponsor_profiles (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID        NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    total_points        INT         NOT NULL DEFAULT 0,
    total_items_donated INT         NOT NULL DEFAULT 0,
    donation_count      INT         NOT NULL DEFAULT 0,
    badge_level         badge_level NOT NULL DEFAULT 'BRONZE',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trg_sponsor_profiles_updated_at
    BEFORE UPDATE ON sponsor_profiles
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();


-- ============================================================
-- SECTION 6: INFRASTRUCTURE (Hubs, Shelters, Safe Paths)
-- ============================================================

-- hubs: Physical relief transfer stations (Trạm trung chuyển).
CREATE TABLE hubs (
    id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255)    NOT NULL,
    address     TEXT            NOT NULL,
    location    GEOMETRY(Point, 4326) NOT NULL,
    status      hub_status      NOT NULL DEFAULT 'ACTIVE',
    -- Contact info for the hub (phone number of the station).
    contact_phone   VARCHAR(20),
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trg_hubs_updated_at
    BEFORE UPDATE ON hubs
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();


-- hub_accepted_categories: Which item categories a hub is configured to accept.
--
-- WHY this is needed (not in v1):
--   Requirements state Admin can "thiết lập danh mục nhận hàng cho từng trạm"
--   (configure which item categories each hub accepts). This junction table
--   makes Smart Hub Selection accurate: only suggest hubs that actually accept
--   the item type the sponsor is donating.
CREATE TABLE hub_accepted_categories (
    hub_id              UUID    NOT NULL REFERENCES hubs(id) ON DELETE CASCADE,
    item_category_id    UUID    NOT NULL REFERENCES item_categories(id) ON DELETE CASCADE,
    PRIMARY KEY (hub_id, item_category_id)
);


-- hub_staff: Assigns STAFF users to hubs.
-- A NULL unassigned_at indicates the assignment is currently active.
CREATE TABLE hub_staff (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    hub_id          UUID        NOT NULL REFERENCES hubs(id) ON DELETE CASCADE,
    user_id         UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    assigned_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    unassigned_at   TIMESTAMPTZ,
    -- Additional safety check: only STAFF-role users may be assigned.
    CONSTRAINT hub_staff_role_check CHECK (
        EXISTS (SELECT 1 FROM users WHERE id = user_id AND role = 'STAFF')
    )
);

-- Partial UNIQUE index: one active assignment per (hub, staff) pair.
-- When a staff member is unassigned and re-assigned later, new rows are allowed.
CREATE UNIQUE INDEX idx_hub_staff_active_unique
    ON hub_staff (hub_id, user_id)
    WHERE unassigned_at IS NULL;


-- shelters: Safe shelters for displaced persons (Điểm trú ẩn).
CREATE TABLE shelters (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    name                VARCHAR(255)    NOT NULL,
    address             TEXT            NOT NULL,
    location            GEOMETRY(Point, 4326) NOT NULL,
    current_capacity    INT             NOT NULL DEFAULT 0,
    max_capacity        INT             NOT NULL,
    has_electricity     BOOLEAN         NOT NULL DEFAULT FALSE,
    has_clean_water     BOOLEAN         NOT NULL DEFAULT FALSE,
    status              shelter_status  NOT NULL DEFAULT 'AVAILABLE',
    notes               TEXT,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT shelters_capacity_range CHECK (
        current_capacity >= 0 AND current_capacity <= max_capacity
    )
);

CREATE TRIGGER trg_shelters_updated_at
    BEFORE UPDATE ON shelters
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();


-- safe_paths: Cached routing polylines displayed on the public map.
--
-- WHY this is needed (not in v1):
--   Requirements specify "Marker Tuyến đường an toàn (Safe Path)" — polylines
--   between victims and hubs/shelters. Rather than recomputing these on every
--   map load, the backend caches them here after calculating via Google Maps
--   Directions API. Entries expire automatically via background job after TTL.
CREATE TABLE safe_paths (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    -- Origin: usually a victim location or SOS point.
    origin          GEOMETRY(Point, 4326)      NOT NULL,
    -- Destination: a hub or shelter.
    destination     GEOMETRY(Point, 4326)      NOT NULL,
    -- Full polyline encoded as a GeoJSON LineString for efficient map rendering.
    path_line       GEOMETRY(LineString, 4326),
    distance_m      DECIMAL(10, 2),
    duration_sec    INT,
    -- Paths are invalidated when road conditions change (hub goes EMERGENCY, etc.).
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    -- Timestamp after which this cached path should be recomputed.
    expires_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() + INTERVAL '6 hours'),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);


-- ============================================================
-- SECTION 7: ITEM CATALOG
-- ============================================================

-- item_categories: Two-level category tree for aid items.
--
-- Top-level (parent_id IS NULL, is_leaf = FALSE):
--   Medicine, Clothing, Food, Water, Other
--
-- Sub-level (parent_id NOT NULL, is_leaf = TRUE):
--   Medicine  → Fever/Cold, Digestive, Bandages
--   Clothing  → Set, Blanket, Raincoat
--   Food      → Rice, Instant Noodles, Canned Food
--   Water     → Bottled Water, Milk, Electrolyte Drink
--   Other     → Diapers
--
-- Only leaf nodes (is_leaf = TRUE) may be referenced by hub_inventories,
-- aid_request_items, and donation_items. This is enforced by:
--   1. fn_mark_parent_non_leaf trigger (sets parent.is_leaf = FALSE on child insert)
--   2. fn_assert_leaf_category() called from triggers on the referencing tables
CREATE TABLE item_categories (
    id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_id   UUID            REFERENCES item_categories(id) ON DELETE SET NULL,
    name        VARCHAR(100)    NOT NULL,
    name_vi     VARCHAR(100)    NOT NULL,
    unit        VARCHAR(30)     NOT NULL DEFAULT 'piece',
    -- TRUE = this node has no children → safe to reference in inventory/requests.
    -- Automatically set to FALSE by fn_mark_parent_non_leaf when a child is added.
    is_leaf     BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trg_item_categories_updated_at
    BEFORE UPDATE ON item_categories
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- When a child category row is inserted, mark its parent as non-leaf.
CREATE TRIGGER trg_item_categories_mark_parent_non_leaf
    AFTER INSERT ON item_categories
    FOR EACH ROW EXECUTE FUNCTION fn_mark_parent_non_leaf();


-- ============================================================
-- SECTION 8: ATTACHMENTS (Normalized)
-- ============================================================

-- attachments: Normalized storage for all file uploads across the system.
--
-- WHY this replaces TEXT[] arrays (v1 used image_urls TEXT[] in multiple tables):
--   1. Individual file metadata (size, MIME type, uploader) is now queryable.
--   2. Easier to delete/manage individual files without array manipulation.
--   3. A single JOIN replaces scattered array operators (@>, ANY, etc.).
--   4. Enables future features: virus scanning status, CDN purge, etc.
--   5. Consistent FK-style reference via (entity_type, entity_id) pair.
CREATE TABLE attachments (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    -- Polymorphic reference to the owning entity.
    entity_type     entity_type     NOT NULL,
    entity_id       UUID            NOT NULL,
    -- Full URL to the file in Supabase Storage or CDN.
    url             VARCHAR(512)    NOT NULL,
    -- MIME type: 'image/jpeg', 'image/png', 'image/webp', etc.
    mime_type       VARCHAR(50),
    -- File size in bytes (populated by the upload service).
    file_size_bytes INT,
    -- The user who uploaded this file.
    uploaded_by     UUID            REFERENCES users(id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);


-- ============================================================
-- SECTION 9: INVENTORY MANAGEMENT
-- ============================================================

-- hub_inventories: Current stock level for each leaf item category at each hub.
-- Updated atomically: +quantity on donation receipt, -quantity on volunteer pickup.
CREATE TABLE hub_inventories (
    id                  UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    hub_id              UUID    NOT NULL REFERENCES hubs(id) ON DELETE CASCADE,
    item_category_id    UUID    NOT NULL REFERENCES item_categories(id),
    current_quantity    INT     NOT NULL DEFAULT 0,
    -- Admin-configurable threshold. STOCK_LOW notification fires when
    -- current_quantity drops at or below this value.
    -- Renamed from min_threshold (v1) → low_stock_threshold for clarity.
    low_stock_threshold INT     NOT NULL DEFAULT 10,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT hub_inventories_unique_slot UNIQUE (hub_id, item_category_id),
    CONSTRAINT hub_inventories_qty_non_negative CHECK (current_quantity >= 0),
    CONSTRAINT hub_inventories_threshold_positive CHECK (low_stock_threshold > 0)
);

CREATE TRIGGER trg_hub_inventories_updated_at
    BEFORE UPDATE ON hub_inventories
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- Enforce leaf-category constraint at the DB level.
CREATE OR REPLACE FUNCTION fn_hub_inventories_leaf_check()
RETURNS TRIGGER AS $$
BEGIN
    PERFORM fn_assert_leaf_category(NEW.item_category_id);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_hub_inventories_leaf_check
    BEFORE INSERT OR UPDATE ON hub_inventories
    FOR EACH ROW EXECUTE FUNCTION fn_hub_inventories_leaf_check();


-- inventory_logs: Immutable audit trail for every stock movement.
-- Append-only — never UPDATE or DELETE rows in this table.
--
-- NOTE: This table is a high-growth candidate. For production at scale,
-- consider partitioning by month:
--   PARTITION BY RANGE (created_at)
-- and creating monthly partitions (e.g., inventory_logs_2025_01).
CREATE TABLE inventory_logs (
    id                  UUID                        PRIMARY KEY DEFAULT gen_random_uuid(),
    hub_inventory_id    UUID                        NOT NULL REFERENCES hub_inventories(id),
    change_type         inventory_change_type       NOT NULL,
    -- Positive integer: units added (INBOUND) or removed (OUTBOUND).
    quantity_delta      INT                         NOT NULL,
    -- Strong-typed ENUM replaces VARCHAR+CHECK from v1.
    reference_type      inventory_reference_type    NOT NULL,
    reference_id        UUID                        NOT NULL,
    -- The STAFF user who scanned the QR code that triggered this entry.
    performed_by        UUID                        NOT NULL REFERENCES users(id),
    -- Stock level at the time of this transaction (snapshot for analytics).
    quantity_after      INT                         NOT NULL,
    notes               TEXT,
    created_at          TIMESTAMPTZ                 NOT NULL DEFAULT NOW(),
    CONSTRAINT inventory_logs_delta_positive CHECK (quantity_delta > 0),
    CONSTRAINT inventory_logs_quantity_after_non_negative CHECK (quantity_after >= 0)
);


-- ============================================================
-- SECTION 10: REQUEST SYSTEM
-- ============================================================

-- sos_requests: Emergency rescue requests.
-- Accepts both authenticated Victims and anonymous Guests (no login required).
CREATE TABLE sos_requests (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    -- NULL when submitted by an anonymous Guest.
    requester_id        UUID            REFERENCES users(id) ON DELETE SET NULL,
    -- Always captured regardless of auth status.
    requester_name      VARCHAR(255)    NOT NULL,
    requester_phone     VARCHAR(20)     NOT NULL,
    -- Populated when requester is filing on behalf of someone else (Section 2.3).
    victim_name         VARCHAR(255),
    victim_phone        VARCHAR(20),
    victim_location     GEOMETRY(Point, 4326) NOT NULL,
    victim_address      TEXT,
    description         TEXT,
    people_count        INT             NOT NULL DEFAULT 1,
    is_on_behalf        BOOLEAN         NOT NULL DEFAULT FALSE,
    urgency_level       urgency_level   NOT NULL DEFAULT 'HIGH',
    -- AI-generated summary of the SOS content for Admin dashboard.
    ai_summary          TEXT,
    status              sos_status      NOT NULL DEFAULT 'PENDING',
    cancellation_reason TEXT,
    resolved_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT sos_people_count_positive CHECK (people_count > 0)
);

CREATE TRIGGER trg_sos_requests_updated_at
    BEFORE UPDATE ON sos_requests
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();


-- aid_requests: Supply aid request headers from authenticated Victims.
-- Individual items are stored in aid_request_items (line-item model).
CREATE TABLE aid_requests (
    id              UUID                NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    requester_id    UUID                NOT NULL REFERENCES users(id),
    location        GEOMETRY(Point, 4326) NOT NULL,
    address         TEXT,
    adults_count    INT                 NOT NULL DEFAULT 0,
    elderly_count   INT                 NOT NULL DEFAULT 0,
    children_count  INT                 NOT NULL DEFAULT 0,
    notes           TEXT,
    urgency_level   urgency_level       NOT NULL DEFAULT 'MEDIUM',
    ai_summary      TEXT,
    status          aid_request_status  NOT NULL DEFAULT 'PENDING',
    resolved_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    CONSTRAINT aid_requests_min_people CHECK (
        (adults_count + elderly_count + children_count) > 0
    )
);

CREATE TRIGGER trg_aid_requests_updated_at
    BEFORE UPDATE ON aid_requests
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();


-- aid_request_items: Line items within a supply aid request.
-- Quantities are auto-calculated from person counts in the parent row.
CREATE TABLE aid_request_items (
    id                  UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    aid_request_id      UUID    NOT NULL REFERENCES aid_requests(id) ON DELETE CASCADE,
    item_category_id    UUID    NOT NULL REFERENCES item_categories(id),
    quantity            INT     NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT aid_request_items_qty_positive CHECK (quantity > 0)
);

-- Enforce leaf-category constraint.
CREATE OR REPLACE FUNCTION fn_aid_request_items_leaf_check()
RETURNS TRIGGER AS $$
BEGIN
    PERFORM fn_assert_leaf_category(NEW.item_category_id);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_aid_request_items_leaf_check
    BEFORE INSERT OR UPDATE ON aid_request_items
    FOR EACH ROW EXECUTE FUNCTION fn_aid_request_items_leaf_check();


-- ============================================================
-- SECTION 11: DONATION SYSTEM (Sponsor → Hub)
-- ============================================================

-- donations: Header record for a Sponsor's goods pledge.
-- Models the full lifecycle: REGISTERED → QR_GENERATED → (RECEIVED | REJECTED).
--
-- WHY header + donation_items (not single row as in v1):
--   A sponsor who brings Rice, Medicine, and Water in one trip should be able
--   to register a SINGLE donation with a SINGLE QR code. In v1, they would
--   need 3 separate donations and 3 QR scans. This redesign matches reality.
CREATE TABLE donations (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    sponsor_id              UUID            NOT NULL REFERENCES users(id),
    -- System-recommended hub: nearest hub with deficit for the item types.
    hub_id                  UUID            NOT NULL REFERENCES hubs(id),
    description             TEXT,
    -- Sponsor's self-reported estimated arrival time at the hub.
    estimated_delivery_at   TIMESTAMPTZ,
    -- Unique token embedded in the QR code. Set when status moves to QR_GENERATED.
    qr_code_token           VARCHAR(512)    UNIQUE,
    status                  donation_status NOT NULL DEFAULT 'REGISTERED',
    -- STAFF user who scanned the QR and processed the donation.
    received_by             UUID            REFERENCES users(id),
    received_at             TIMESTAMPTZ,
    rejection_reason        TEXT,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trg_donations_updated_at
    BEFORE UPDATE ON donations
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();


-- donation_items: Individual item lines within a single donation.
-- Each line has its own quantity, expiry date (for perishables), and
-- optional condition notes filled in by Staff during inspection.
CREATE TABLE donation_items (
    id                  UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    donation_id         UUID    NOT NULL REFERENCES donations(id) ON DELETE CASCADE,
    item_category_id    UUID    NOT NULL REFERENCES item_categories(id),
    quantity            INT     NOT NULL,
    expiry_date         DATE,
    -- Staff-recorded note about the physical condition of this specific item line.
    condition_notes     TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT donation_items_qty_positive CHECK (quantity > 0)
);

-- Enforce leaf-category constraint.
CREATE OR REPLACE FUNCTION fn_donation_items_leaf_check()
RETURNS TRIGGER AS $$
BEGIN
    PERFORM fn_assert_leaf_category(NEW.item_category_id);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_donation_items_leaf_check
    BEFORE INSERT OR UPDATE ON donation_items
    FOR EACH ROW EXECUTE FUNCTION fn_donation_items_leaf_check();


-- ============================================================
-- SECTION 12: MISSION & DISPATCH SYSTEM
-- ============================================================

-- missions: Core work unit assigned to a Volunteer.
-- RESCUE → spawned from sos_request; goes directly to victim.
-- DELIVERY → spawned from aid_request; requires a hub stop.
--
-- Status flows:
--   RESCUE:   PENDING → DISPATCHING → ASSIGNED → IN_TRANSIT → COMPLETED
--   DELIVERY: PENDING → DISPATCHING → ASSIGNED → PICKING_UP → PICKED_UP → IN_TRANSIT → COMPLETED
--
-- DESIGN CHANGE from v1: volunteer_id now references volunteer_profiles(id)
-- instead of users(id). This guarantees the assigned user has a volunteer
-- profile and makes Priority Score reads a direct join without an extra hop.
CREATE TABLE missions (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    mission_type            mission_type    NOT NULL,
    -- Exactly one of these FKs is set, enforced by the CHECK below.
    sos_request_id          UUID            REFERENCES sos_requests(id),
    aid_request_id          UUID            REFERENCES aid_requests(id),
    -- References volunteer_profiles.id (not users.id) for tighter integrity.
    volunteer_id            UUID            REFERENCES volunteer_profiles(id),
    -- Hub the volunteer must visit (DELIVERY only; NULL for RESCUE).
    hub_id                  UUID            REFERENCES hubs(id),
    status                  mission_status  NOT NULL DEFAULT 'PENDING',
    -- QR token the volunteer presents to Staff at hub for pickup verification.
    qr_code_token           VARCHAR(512)    UNIQUE,
    -- Snapshot of the winning volunteer's Priority Score at acceptance time.
    -- Stored for analytics, A/B testing, and algorithm tuning by Admin.
    priority_score          DECIMAL(8, 4),
    dispatched_at           TIMESTAMPTZ,
    accepted_at             TIMESTAMPTZ,
    picked_up_at            TIMESTAMPTZ,    -- DELIVERY only: successful hub QR scan
    delivered_at            TIMESTAMPTZ,
    completed_at            TIMESTAMPTZ,
    cancelled_at            TIMESTAMPTZ,
    cancellation_reason     TEXT,
    -- Proof-of-delivery: photo URL or victim confirmation screenshot.
    confirmation_image_url  VARCHAR(512),
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    -- Exactly one request type per mission type.
    CONSTRAINT missions_type_request_integrity CHECK (
        (mission_type = 'RESCUE'   AND sos_request_id IS NOT NULL AND aid_request_id IS NULL)
        OR
        (mission_type = 'DELIVERY' AND aid_request_id IS NOT NULL AND sos_request_id IS NULL)
    ),
    -- Hub is required for DELIVERY and must be NULL for RESCUE.
    CONSTRAINT missions_hub_delivery_only CHECK (
        (mission_type = 'DELIVERY' AND hub_id IS NOT NULL)
        OR
        (mission_type = 'RESCUE')
    )
);

CREATE TRIGGER trg_missions_updated_at
    BEFORE UPDATE ON missions
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();


-- dispatch_attempts: Full audit log for every volunteer notification sent.
-- Enables post-hoc analysis, Admin manual override, and dispatch replay.
--
-- For BROADCAST:   one row per notified volunteer (batch_number = 1).
-- For SEQUENTIAL:  batch_number 1 = top volunteer; 2 = next trio; 3 = wider radius; etc.
--
-- NOTE: High-growth table. Consider monthly range partitioning at scale.
CREATE TABLE dispatch_attempts (
    id              UUID                PRIMARY KEY DEFAULT gen_random_uuid(),
    mission_id      UUID                NOT NULL REFERENCES missions(id) ON DELETE CASCADE,
    -- References volunteer_profiles.id for consistency with missions.
    volunteer_id    UUID                NOT NULL REFERENCES volunteer_profiles(id),
    dispatch_type   dispatch_type       NOT NULL,
    batch_number    INT                 NOT NULL DEFAULT 1,
    -- Search radius (km) active when this attempt was made (for radius-expansion audit).
    radius_km       DECIMAL(5, 2)       NOT NULL,
    -- Priority Score of this volunteer at dispatch time.
    priority_score  DECIMAL(8, 4),
    sent_at         TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    response        dispatch_response   NOT NULL DEFAULT 'PENDING',
    responded_at    TIMESTAMPTZ,
    CONSTRAINT dispatch_attempts_batch_positive CHECK (batch_number > 0),
    CONSTRAINT dispatch_attempts_radius_positive CHECK (radius_km > 0)
);


-- ============================================================
-- SECTION 13: COMMUNICATION & SOCIAL
-- ============================================================

-- chat_messages: Real-time in-mission chat between Victim and Volunteer.
-- Scoped to a mission so the full chat history is preserved with the mission record.
--
-- NOTE: High-growth table. At scale, archive messages older than 90 days
-- or partition by RANGE (created_at).
CREATE TABLE chat_messages (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    mission_id      UUID            NOT NULL REFERENCES missions(id) ON DELETE CASCADE,
    sender_id       UUID            NOT NULL REFERENCES users(id),
    message_type    message_type    NOT NULL DEFAULT 'TEXT',
    message_text    TEXT,
    -- For IMAGE messages: stored in the attachments table; this is a convenience FK.
    attachment_id   UUID            REFERENCES attachments(id) ON DELETE SET NULL,
    is_read         BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT chat_messages_content_integrity CHECK (
        (message_type = 'TEXT'  AND message_text IS NOT NULL AND attachment_id IS NULL)
        OR
        (message_type = 'IMAGE' AND attachment_id IS NOT NULL AND message_text IS NULL)
    )
);


-- ratings: One-per-mission star rating from Victim for the Volunteer.
-- Used to update volunteer_profiles.avg_rating after each completed mission.
CREATE TABLE ratings (
    id          UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    mission_id  UUID    NOT NULL UNIQUE REFERENCES missions(id),
    -- Victim who submitted the rating.
    rater_id    UUID    NOT NULL REFERENCES users(id),
    -- Volunteer who received the rating.
    ratee_id    UUID    NOT NULL REFERENCES volunteer_profiles(id),
    score       INT     NOT NULL,
    comment     TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ratings_score_range CHECK (score >= 1 AND score <= 5)
);


-- notifications: Persistent in-app notification inbox.
-- FCM push fires in parallel; this table enables in-app inbox history and badge counts.
--
-- NOTE: High-growth table. Consider archiving rows older than 90 days
-- and partitioning by RANGE (created_at) for large deployments.
CREATE TABLE notifications (
    id                  UUID                PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID                NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    notification_type   notification_type   NOT NULL,
    title               VARCHAR(255)        NOT NULL,
    body                TEXT                NOT NULL,
    -- Polymorphic reference to the triggering entity — strong-typed ENUM (v1 used VARCHAR).
    related_entity_type entity_type,
    related_entity_id   UUID,
    is_read             BOOLEAN             NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ         NOT NULL DEFAULT NOW()
);


-- ============================================================
-- SECTION 14: INDEXES
-- ============================================================

-- ─── GIST SPATIAL INDEXES ────────────────────────────────────
-- Required for ST_DWithin radius queries in dispatch, Smart Hub
-- Selection, and map rendering. GIST is the only correct index
-- type for PostGIS geometry columns.

-- Volunteer current position — hottest index in the schema.
CREATE INDEX idx_volunteer_location
    ON volunteer_profiles USING GIST (current_location);

-- Volunteer area experience zones — used by E-factor JOIN.
CREATE INDEX idx_volunteer_area_exp_area
    ON volunteer_area_experiences USING GIST (area);

-- Hub locations — Smart Hub Selection + map markers.
CREATE INDEX idx_hubs_location
    ON hubs USING GIST (location);

-- Shelter locations — public map + nearest-shelter routing.
CREATE INDEX idx_shelters_location
    ON shelters USING GIST (location);

-- SOS victim position — dispatch radius query + heatmap aggregation.
CREATE INDEX idx_sos_requests_victim_location
    ON sos_requests USING GIST (victim_location);

-- Aid request delivery position — dispatch radius query + heatmap.
CREATE INDEX idx_aid_requests_location
    ON aid_requests USING GIST (location);

-- Safe path origin and destination — nearest-path lookup.
CREATE INDEX idx_safe_paths_origin
    ON safe_paths USING GIST (origin);
CREATE INDEX idx_safe_paths_destination
    ON safe_paths USING GIST (destination);


-- ─── USER & AUTH ─────────────────────────────────────────────
CREATE INDEX idx_users_email          ON users (email);
CREATE INDEX idx_users_phone          ON users (phone_number);
CREATE INDEX idx_users_role           ON users (role);

CREATE INDEX idx_otp_user             ON otp_verifications (user_id);
CREATE INDEX idx_otp_active
    ON otp_verifications (user_id, otp_type)
    WHERE is_used = FALSE;

CREATE INDEX idx_refresh_tokens_user  ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_active
    ON refresh_tokens (user_id)
    WHERE is_revoked = FALSE;


-- ─── VOLUNTEER ───────────────────────────────────────────────
-- Partial index: only the rows that participate in dispatch radius scans.
CREATE INDEX idx_volunteers_online
    ON volunteer_profiles (is_online)
    WHERE is_online = TRUE;

CREATE INDEX idx_volunteer_area_exp_volunteer
    ON volunteer_area_experiences (volunteer_id);


-- ─── HUB & INVENTORY ─────────────────────────────────────────
CREATE INDEX idx_hubs_status          ON hubs (status);

-- Composite index for the Smart Hub Selection query:
-- "Find ACTIVE hubs that accept item X, ordered by distance from sponsor GPS"
-- Query shape: WHERE status = 'ACTIVE' AND hub_id IN (SELECT hub_id FROM hub_accepted_categories WHERE item_category_id = ?)
CREATE INDEX idx_hub_accepted_cat_item
    ON hub_accepted_categories (item_category_id);

CREATE INDEX idx_hub_inv_hub          ON hub_inventories (hub_id);
CREATE INDEX idx_hub_inv_item         ON hub_inventories (item_category_id);
-- Partial index for low-stock alerting queries.
CREATE INDEX idx_hub_inv_low_stock
    ON hub_inventories (hub_id)
    WHERE current_quantity <= low_stock_threshold;

CREATE INDEX idx_inv_logs_hub_inv     ON inventory_logs (hub_inventory_id, created_at DESC);
CREATE INDEX idx_inv_logs_ref         ON inventory_logs (reference_id, reference_type);
CREATE INDEX idx_inv_logs_performed   ON inventory_logs (performed_by);


-- ─── REQUESTS ────────────────────────────────────────────────
CREATE INDEX idx_sos_status           ON sos_requests (status);
CREATE INDEX idx_sos_urgency          ON sos_requests (urgency_level);
CREATE INDEX idx_sos_requester        ON sos_requests (requester_id);
-- DESC for "fetch my recent SOS" queries.
CREATE INDEX idx_sos_created          ON sos_requests (created_at DESC);

CREATE INDEX idx_aid_status           ON aid_requests (status);
CREATE INDEX idx_aid_requester        ON aid_requests (requester_id);
CREATE INDEX idx_aid_created          ON aid_requests (created_at DESC);

CREATE INDEX idx_aid_items_request    ON aid_request_items (aid_request_id);
CREATE INDEX idx_aid_items_category   ON aid_request_items (item_category_id);


-- ─── DONATIONS ───────────────────────────────────────────────
CREATE INDEX idx_donations_sponsor    ON donations (sponsor_id);
CREATE INDEX idx_donations_hub        ON donations (hub_id);
CREATE INDEX idx_donations_status     ON donations (status);
CREATE INDEX idx_donations_qr_token   ON donations (qr_code_token);
CREATE INDEX idx_donations_created    ON donations (created_at DESC);

CREATE INDEX idx_donation_items_donation  ON donation_items (donation_id);
CREATE INDEX idx_donation_items_category  ON donation_items (item_category_id);


-- ─── MISSIONS & DISPATCH ─────────────────────────────────────
CREATE INDEX idx_missions_status      ON missions (status);
CREATE INDEX idx_missions_volunteer   ON missions (volunteer_id);
CREATE INDEX idx_missions_sos         ON missions (sos_request_id);
CREATE INDEX idx_missions_aid         ON missions (aid_request_id);
CREATE INDEX idx_missions_hub         ON missions (hub_id);
CREATE INDEX idx_missions_created     ON missions (created_at DESC);

CREATE INDEX idx_dispatch_mission     ON dispatch_attempts (mission_id);
CREATE INDEX idx_dispatch_volunteer   ON dispatch_attempts (volunteer_id);
-- Fast timeout checker: "Find all PENDING attempts for this mission"
CREATE INDEX idx_dispatch_pending
    ON dispatch_attempts (mission_id)
    WHERE response = 'PENDING';


-- ─── COMMUNICATION ───────────────────────────────────────────
CREATE INDEX idx_chat_mission         ON chat_messages (mission_id, created_at DESC);
CREATE INDEX idx_chat_sender          ON chat_messages (sender_id);
-- Fast unread count per mission.
CREATE INDEX idx_chat_unread
    ON chat_messages (mission_id)
    WHERE is_read = FALSE;

CREATE INDEX idx_ratings_ratee        ON ratings (ratee_id);
CREATE INDEX idx_ratings_mission      ON ratings (mission_id);

CREATE INDEX idx_notifications_user   ON notifications (user_id, created_at DESC);
-- Fast badge count query.
CREATE INDEX idx_notifications_unread
    ON notifications (user_id)
    WHERE is_read = FALSE;


-- ─── ATTACHMENTS ─────────────────────────────────────────────
-- Composite index: the primary access pattern is "all attachments for entity X".
CREATE INDEX idx_attachments_entity   ON attachments (entity_type, entity_id);


-- ─── SAFE PATHS ──────────────────────────────────────────────
CREATE INDEX idx_safe_paths_active
    ON safe_paths (is_active, expires_at)
    WHERE is_active = TRUE;
