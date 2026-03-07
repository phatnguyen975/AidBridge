-- ============================================================
-- AidBridge: Full Database Schema (DDL)
-- Database: Supabase (PostgreSQL 15+ with PostGIS)
-- Run AFTER 01_init_postgis.sql
-- ============================================================


-- ============================================================
-- SECTION 1: ENUM TYPE DEFINITIONS
-- ============================================================

-- Roles available in the system.
-- A single user holds exactly one role at registration.
CREATE TYPE user_role AS ENUM (
    'VICTIM',       -- Disaster victim requesting aid or rescue
    'VOLUNTEER',    -- Aid delivery or rescue volunteer
    'SPONSOR',      -- Goods donor / benefactor (Mạnh thường quân)
    'STAFF',        -- Hub station staff member (Nhân viên trực trạm)
    'ADMIN'         -- System administrator
);

-- Operational status of a Hub (Trạm trung chuyển).
CREATE TYPE hub_status AS ENUM (
    'ACTIVE',       -- Hub is fully operational and accepting goods/pickups
    'INACTIVE',     -- Hub is temporarily closed (scheduled downtime)
    'EMERGENCY'     -- Hub reported an emergency (flooding/power outage); system auto-reroutes deliveries
);

-- Availability status of an emergency Shelter (Điểm trú ẩn).
CREATE TYPE shelter_status AS ENUM (
    'AVAILABLE',    -- Shelter has open capacity
    'FULL',         -- Shelter is at maximum capacity
    'UNAVAILABLE'   -- Shelter is inaccessible or closed
);

-- AI-classified urgency level for both SOS and aid requests.
-- Used by the dispatch algorithm to prioritise missions (CRITICAL uses Broadcast strategy).
CREATE TYPE urgency_level AS ENUM (
    'CRITICAL',     -- Life-threatening; triggers Broadcast dispatch strategy
    'HIGH',         -- Serious situation requiring rapid response
    'MEDIUM',       -- Moderate need
    'LOW'           -- Non-urgent supply top-up
);

-- Lifecycle states for an SOS emergency rescue request.
CREATE TYPE sos_status AS ENUM (
    'PENDING',      -- SOS received, awaiting dispatch
    'DISPATCHING',  -- Algorithm actively searching for volunteers
    'ASSIGNED',     -- A volunteer confirmed; en route to victim
    'IN_PROGRESS',  -- Volunteer is on-scene actively rescuing
    'COMPLETED',    -- Rescue confirmed and closed
    'CANCELLED'     -- SOS cancelled (false alarm or resolved independently)
);

-- Lifecycle states for a supply aid request (Yêu cầu tiếp tế).
CREATE TYPE aid_request_status AS ENUM (
    'PENDING',      -- Request received, awaiting dispatch
    'DISPATCHING',  -- Algorithm searching for a volunteer
    'ASSIGNED',     -- Volunteer accepted; heading to hub
    'PICKED_UP',    -- Volunteer confirmed goods pickup at hub
    'IN_TRANSIT',   -- Volunteer en route to victim
    'COMPLETED',    -- Delivery confirmed; goods received by victim
    'CANCELLED'     -- Request cancelled
);

-- Distinguishes the type of work a Mission represents.
CREATE TYPE mission_type AS ENUM (
    'RESCUE',       -- Emergency rescue tied to an SOS request (direct to victim, no hub)
    'DELIVERY'      -- Supply delivery tied to an aid request (hub → victim)
);

-- Granular lifecycle states for a Mission (shared across both types).
CREATE TYPE mission_status AS ENUM (
    'PENDING',      -- Mission created, dispatch not yet started
    'DISPATCHING',  -- System is actively notifying volunteers
    'ASSIGNED',     -- Volunteer accepted; progressing to next step
    'PICKING_UP',   -- (DELIVERY only) Volunteer arrived at hub, awaiting QR scan
    'PICKED_UP',    -- (DELIVERY only) Staff confirmed goods handover via QR scan
    'IN_TRANSIT',   -- Volunteer is en route to the victim
    'COMPLETED',    -- Mission fully completed and confirmed
    'CANCELLED'     -- Mission cancelled for any reason
);

-- Lifecycle states for a Sponsor donation pledge.
CREATE TYPE donation_status AS ENUM (
    'REGISTERED',   -- Sponsor submitted donation, QR not yet issued
    'QR_GENERATED', -- System generated a QR code; sponsor instructed to bring goods to hub
    'RECEIVED',     -- Staff scanned QR and confirmed receipt; inventory updated
    'REJECTED'      -- Staff rejected donation (failed quality/physical inspection)
);

-- Direction of an inventory transaction.
CREATE TYPE inventory_change_type AS ENUM (
    'INBOUND',      -- Goods added to hub stock (triggered by donation QR scan)
    'OUTBOUND'      -- Goods removed from hub stock (triggered by volunteer pickup QR scan)
);

-- Dispatch strategy used when notifying a volunteer about a mission.
CREATE TYPE dispatch_type AS ENUM (
    'BROADCAST',    -- All top-N volunteers notified simultaneously (used for CRITICAL/SOS)
    'SEQUENTIAL'    -- Volunteers notified one-by-one or in ranked batches (used for DELIVERY)
);

-- Volunteer's response to a dispatch notification within the time window.
CREATE TYPE dispatch_response AS ENUM (
    'PENDING',      -- No response yet (within the time window)
    'ACCEPTED',     -- Volunteer accepted the mission
    'REJECTED',     -- Volunteer explicitly declined
    'TIMEOUT'       -- Time window expired with no response
);

-- Content type of a chat message.
CREATE TYPE message_type AS ENUM (
    'TEXT',
    'IMAGE'
);

-- Categorises notifications for routing and display logic.
CREATE TYPE notification_type AS ENUM (
    -- Victim-facing notifications
    'SOS_CONFIRMED',            -- SOS logged and volunteer search started
    'VOLUNTEER_ASSIGNED',       -- A volunteer has accepted and is en route
    'GOODS_PICKED_UP',          -- Volunteer collected goods from hub
    'IN_TRANSIT',               -- Volunteer is on the way to victim
    'NEAR_ARRIVAL',             -- Volunteer is within 500 m
    'DELIVERY_COMPLETED',       -- Delivery/rescue confirmed, rating prompt issued
    -- Volunteer-facing notifications
    'DISPATCH_REQUEST',         -- New mission offer (30-second acceptance window)
    'NEW_CHAT_MESSAGE',         -- New message from victim in active mission chat
    -- Sponsor-facing notifications
    'DONATION_QR_CREATED',      -- QR code issued; bring goods to hub
    'DONATION_RECEIVED',        -- Staff confirmed hub receipt of goods
    'DONATION_REJECTED',        -- Staff rejected goods at hub
    -- Staff-facing notifications
    'INCOMING_DONATION',        -- Sponsor is en route to hub with goods
    'VOLUNTEER_PICKUP_REQUEST', -- Volunteer is coming to hub to pick up goods
    'STOCK_LOW',                -- Item at hub has dropped below min_threshold
    -- Admin-facing notifications
    'STOCK_CRITICAL',           -- System-wide critical shortage across multiple hubs
    'HUB_INCIDENT',             -- Hub reported an emergency and suspended operations
    'FRAUD_ALERT',              -- Suspicious duplicate SOS reports detected at same coordinates
    -- System-wide broadcast notifications
    'EMERGENCY_BROADCAST',      -- Admin-issued emergency alert to all users
    'NEW_HUB_OPENED'            -- New hub or shelter established; community announcement
);

-- OTP purpose discriminator for the otp_verifications table.
CREATE TYPE otp_type AS ENUM (
    'EMAIL_VERIFICATION',
    'PHONE_VERIFICATION',
    'PASSWORD_RESET'
);

-- Tiered achievement badge for Sponsors based on cumulative contributions.
CREATE TYPE badge_level AS ENUM (
    'BRONZE',
    'SILVER',
    'GOLD',
    'PLATINUM'
);


-- ============================================================
-- SECTION 2: SHARED TRIGGER FUNCTION
-- Automatically updates the `updated_at` column on every row update.
-- ============================================================

CREATE OR REPLACE FUNCTION fn_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- ============================================================
-- SECTION 3: CORE USER TABLES
-- ============================================================

-- users: Central table for all authenticated participants.
-- Guests (anonymous SOS) are NOT stored here; they appear only in sos_requests.
CREATE TABLE users (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name       VARCHAR(255)    NOT NULL,
    email           VARCHAR(255)    UNIQUE,
    phone_number    VARCHAR(20)     UNIQUE,
    -- bcrypt-hashed password. Never stored in plain text.
    password_hash   VARCHAR(255)    NOT NULL,
    role            user_role       NOT NULL,
    -- FALSE until the user verifies email or phone via OTP.
    is_verified     BOOLEAN         NOT NULL DEFAULT FALSE,
    -- Admin can set to FALSE to block a spammer or bad actor.
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    -- Firebase Cloud Messaging token for push notifications. Updated on each login.
    fcm_token       VARCHAR(512),
    avatar_url      VARCHAR(512),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    -- At least one contact method must be present.
    CONSTRAINT users_contact_method_check CHECK (
        email IS NOT NULL OR phone_number IS NOT NULL
    )
);

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();


-- otp_verifications: Stores short-lived OTP codes for email/phone verification
-- and password resets. Each code is single-use and expires after a set TTL.
CREATE TABLE otp_verifications (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    otp_code    VARCHAR(10) NOT NULL,
    otp_type    otp_type    NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    is_used     BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- volunteer_profiles: Extended attributes specific to VOLUNTEER role users.
-- Stores the five factors used by the Priority Score formula:
--   S = (D×40%) + (R×20%) + (T×15%) + (A×15%) + (E×10%)
-- where D is computed at query time from current_location.
CREATE TABLE volunteer_profiles (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID            NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    -- Whether the volunteer is accepting new missions right now.
    is_online               BOOLEAN         NOT NULL DEFAULT FALSE,
    -- Last reported GPS position. Updated via WebSocket during active missions
    -- and periodically when online. Used for radius-based dispatch queries.
    current_location        GEOMETRY(Point, 4326),
    -- e.g., 'motorbike', 'bicycle', 'car', 'walking'
    vehicle_type            VARCHAR(50),
    -- T factor: cumulative count of successfully completed missions.
    total_tasks_completed   INT             NOT NULL DEFAULT 0,
    -- R factor: rolling average star rating (1.00 – 5.00).
    avg_rating              DECIMAL(3, 2)   NOT NULL DEFAULT 5.00,
    -- A factor: rolling average response time to dispatch notifications, in seconds.
    avg_response_seconds    INT             NOT NULL DEFAULT 0,
    -- E factor: computed score reflecting familiarity with a geographic area.
    area_experience_score   DECIMAL(5, 2)   NOT NULL DEFAULT 0.00,
    last_active_at          TIMESTAMPTZ,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trg_volunteer_profiles_updated_at
    BEFORE UPDATE ON volunteer_profiles
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();


-- sponsor_profiles: Extended attributes specific to SPONSOR role users.
-- Tracks contribution statistics for the badge and recognition system.
CREATE TABLE sponsor_profiles (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID        NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    -- Accumulated gamification points; incremented on every RECEIVED donation.
    total_points        INT         NOT NULL DEFAULT 0,
    -- Total number of individual items donated (across all accepted donations).
    total_items_donated INT         NOT NULL DEFAULT 0,
    -- Count of donations with status = 'RECEIVED'.
    donation_count      INT         NOT NULL DEFAULT 0,
    badge_level         badge_level NOT NULL DEFAULT 'BRONZE',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trg_sponsor_profiles_updated_at
    BEFORE UPDATE ON sponsor_profiles
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();


-- ============================================================
-- SECTION 4: INFRASTRUCTURE TABLES (Hubs & Shelters)
-- ============================================================

-- hubs: Physical relief transfer stations (Trạm trung chuyển).
-- All incoming donations and outbound volunteer pickups pass through hubs.
CREATE TABLE hubs (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    address     TEXT        NOT NULL,
    -- GPS coordinates used for Smart Hub Selection and map rendering.
    location    GEOMETRY(Point, 4326) NOT NULL,
    status      hub_status  NOT NULL DEFAULT 'ACTIVE',
    notes       TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trg_hubs_updated_at
    BEFORE UPDATE ON hubs
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();


-- hub_staff: Assigns STAFF users to hubs (many staff can work one hub;
-- one staff can be transferred to another hub over time).
-- A NULL unassigned_at means the assignment is currently active.
CREATE TABLE hub_staff (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    hub_id          UUID        NOT NULL REFERENCES hubs(id) ON DELETE CASCADE,
    user_id         UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    assigned_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    -- Populated by Admin when a staff member is reassigned or removed.
    unassigned_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trg_hub_staff_updated_at
    BEFORE UPDATE ON hub_staff
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();


-- shelters: Safe shelters for displaced persons (Điểm trú ẩn).
-- Displayed on the public map with capacity and amenity info.
CREATE TABLE shelters (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    name                VARCHAR(255)    NOT NULL,
    address             TEXT            NOT NULL,
    -- GPS coordinates used for map marker and nearest-shelter routing.
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


-- ============================================================
-- SECTION 5: ITEM CATALOG
-- ============================================================

-- item_categories: Two-level category tree for aid items.
--
-- Top-level categories (parent_id IS NULL):
--   Medicine, Clothing, Food, Water, Other
--
-- Sub-level categories (parent_id NOT NULL):
--   Medicine  → Fever/Cold, Digestive, Bandages
--   Clothing  → Set, Blanket, Raincoat
--   Food      → Rice, Instant Noodles, Canned Food
--   Water     → Bottled Water, Milk, Electrolyte Drink
--   Other     → Diapers
--
-- Inventory, aid requests, and donations all reference leaf (sub-level) categories.
CREATE TABLE item_categories (
    id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    -- NULL indicates a top-level category; otherwise points to the parent category.
    parent_id   UUID            REFERENCES item_categories(id) ON DELETE SET NULL,
    name        VARCHAR(100)    NOT NULL,
    -- Vietnamese display name used in the mobile app UI.
    name_vi     VARCHAR(100)    NOT NULL,
    -- Base unit of measurement for stock counting (e.g., 'kg', 'liter', 'piece', 'pack').
    unit        VARCHAR(30)     NOT NULL DEFAULT 'piece',
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trg_item_categories_updated_at
    BEFORE UPDATE ON item_categories
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();


-- ============================================================
-- SECTION 6: INVENTORY MANAGEMENT
-- ============================================================

-- hub_inventories: Current stock level for each item subcategory at each hub.
-- Updated atomically: +quantity on donation receipt, -quantity on volunteer pickup.
CREATE TABLE hub_inventories (
    id                  UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    hub_id              UUID    NOT NULL REFERENCES hubs(id) ON DELETE CASCADE,
    -- Must reference a leaf (subcategory) node in item_categories.
    item_category_id    UUID    NOT NULL REFERENCES item_categories(id),
    current_quantity    INT     NOT NULL DEFAULT 0,
    -- Admin-configurable threshold. STOCK_LOW notification fires when
    -- current_quantity drops at or below this value.
    min_threshold       INT     NOT NULL DEFAULT 10,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    -- Enforce one row per item per hub.
    CONSTRAINT hub_inventories_unique_slot UNIQUE (hub_id, item_category_id),
    CONSTRAINT hub_inventories_qty_non_negative CHECK (current_quantity >= 0)
);

CREATE TRIGGER trg_hub_inventories_updated_at
    BEFORE UPDATE ON hub_inventories
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();


-- inventory_logs: Immutable audit trail for every stock movement.
-- Written by the Atomic Inventory Update transaction; never mutated after insert.
CREATE TABLE inventory_logs (
    id                  UUID                    PRIMARY KEY DEFAULT gen_random_uuid(),
    hub_inventory_id    UUID                    NOT NULL REFERENCES hub_inventories(id),
    change_type         inventory_change_type   NOT NULL,
    -- Positive integer: represents units added (INBOUND) or removed (OUTBOUND).
    quantity_delta      INT                     NOT NULL,
    -- 'DONATION' | 'MISSION' — identifies which table reference_id points to.
    reference_type      VARCHAR(20)             NOT NULL,
    -- ID of the donation or mission that caused this transaction.
    reference_id        UUID                    NOT NULL,
    -- The STAFF user who scanned the QR code triggering this log entry.
    performed_by        UUID                    NOT NULL REFERENCES users(id),
    notes               TEXT,
    created_at          TIMESTAMPTZ             NOT NULL DEFAULT NOW(),
    CONSTRAINT inventory_logs_delta_positive CHECK (quantity_delta > 0),
    CONSTRAINT inventory_logs_ref_type_check CHECK (
        reference_type IN ('DONATION', 'MISSION')
    )
);


-- ============================================================
-- SECTION 7: REQUEST SYSTEM
-- ============================================================

-- sos_requests: Emergency rescue requests.
-- Can be submitted by authenticated Victims OR anonymous Guests (no login required
-- to ensure maximum speed during life-threatening situations).
CREATE TABLE sos_requests (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    -- NULL when submitted anonymously (Guest flow: Section 1.1 in requirements).
    requester_id        UUID            REFERENCES users(id) ON DELETE SET NULL,
    -- Always captured regardless of authentication status.
    requester_name      VARCHAR(255)    NOT NULL,
    requester_phone     VARCHAR(20)     NOT NULL,
    -- Populated when the requester is submitting on behalf of someone else (Section 2.3).
    victim_name         VARCHAR(255),
    victim_phone        VARCHAR(20),
    -- Precise GPS position of the person in distress.
    victim_location     GEOMETRY(Point, 4326) NOT NULL,
    -- Human-readable address or landmark description provided by the requester.
    victim_address      TEXT,
    -- Free-text field: health status, number of people, house description, etc.
    description         TEXT,
    people_count        INT             NOT NULL DEFAULT 1,
    -- TRUE when the requester is filing on behalf of a different person (Section 2.3).
    is_on_behalf        BOOLEAN         NOT NULL DEFAULT FALSE,
    -- AI-classified urgency level used to determine dispatch strategy.
    urgency_level       urgency_level   NOT NULL DEFAULT 'HIGH',
    ai_summary          TEXT,
    status              sos_status      NOT NULL DEFAULT 'PENDING',
    cancellation_reason TEXT,
    -- Array of Supabase Storage or CDN URLs for attached photos.
    image_urls          TEXT[]          NOT NULL DEFAULT '{}',
    resolved_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT sos_people_count_positive CHECK (people_count > 0)
);

CREATE TRIGGER trg_sos_requests_updated_at
    BEFORE UPDATE ON sos_requests
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();


-- aid_requests: Supply aid requests from authenticated Victims (Yêu cầu tiếp tế).
-- Holds the head record; individual items are stored in aid_request_items.
CREATE TABLE aid_requests (
    id              UUID                NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    -- Must be an authenticated Victim; anonymous users cannot place supply requests.
    requester_id    UUID                NOT NULL REFERENCES users(id),
    -- Delivery destination GPS coordinates.
    location        GEOMETRY(Point, 4326) NOT NULL,
    address         TEXT,
    -- Breakdown used to auto-calculate item quantities per person.
    adults_count    INT                 NOT NULL DEFAULT 0,
    elderly_count   INT                 NOT NULL DEFAULT 0,
    children_count  INT                 NOT NULL DEFAULT 0,
    -- Additional context transcribed from AI Voice Support or typed by the victim.
    notes           TEXT,
    urgency_level   urgency_level       NOT NULL DEFAULT 'MEDIUM',
    ai_summary      TEXT,
    status          aid_request_status  NOT NULL DEFAULT 'PENDING',
    image_urls      TEXT[]              NOT NULL DEFAULT '{}',
    resolved_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    -- At least one person must be listed to calculate quantities.
    CONSTRAINT aid_requests_min_people CHECK (
        (adults_count + elderly_count + children_count) > 0
    )
);

CREATE TRIGGER trg_aid_requests_updated_at
    BEFORE UPDATE ON aid_requests
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();


-- aid_request_items: Individual line items within a supply aid request.
-- References leaf-level item_categories whose quantities are auto-calculated
-- based on the person counts in the parent aid_request.
CREATE TABLE aid_request_items (
    id                  UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    aid_request_id      UUID    NOT NULL REFERENCES aid_requests(id) ON DELETE CASCADE,
    item_category_id    UUID    NOT NULL REFERENCES item_categories(id),
    quantity            INT     NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT aid_request_items_qty_positive CHECK (quantity > 0)
);


-- ============================================================
-- SECTION 8: DONATION SYSTEM (Sponsor → Hub)
-- ============================================================

-- donations: Full lifecycle record for a Sponsor's goods pledge.
-- Flow: REGISTERED → QR_GENERATED → (RECEIVED | REJECTED)
-- On RECEIVED: hub_inventories is incremented atomically and an inventory_log is inserted.
CREATE TABLE donations (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    sponsor_id              UUID            NOT NULL REFERENCES users(id),
    -- System-recommended hub (nearest hub with deficit for the item type).
    hub_id                  UUID            NOT NULL REFERENCES hubs(id),
    -- Must be a leaf-level item category.
    item_category_id        UUID            NOT NULL REFERENCES item_categories(id),
    quantity                INT             NOT NULL,
    description             TEXT,
    -- Array of Supabase Storage URLs for photos of the actual goods.
    image_urls              TEXT[]          NOT NULL DEFAULT '{}',
    expiry_date             DATE,
    -- Sponsor's self-reported estimated arrival time at the hub.
    estimated_delivery_at   TIMESTAMPTZ,
    -- Unique token embedded in the QR code. Generated after REGISTERED → QR_GENERATED.
    qr_code_token           VARCHAR(512)    UNIQUE,
    status                  donation_status NOT NULL DEFAULT 'REGISTERED',
    -- STAFF user who scanned the QR and processed the donation.
    received_by             UUID            REFERENCES users(id),
    received_at             TIMESTAMPTZ,
    rejection_reason        TEXT,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT donations_qty_positive CHECK (quantity > 0)
);

CREATE TRIGGER trg_donations_updated_at
    BEFORE UPDATE ON donations
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();


-- ============================================================
-- SECTION 9: MISSION & DISPATCH SYSTEM
-- ============================================================

-- missions: The core work unit for volunteers.
-- A RESCUE mission is spawned from an sos_request.
-- A DELIVERY mission is spawned from an aid_request and requires a hub stop.
--
-- Status flow for RESCUE:
--   PENDING → DISPATCHING → ASSIGNED → IN_TRANSIT → COMPLETED
--
-- Status flow for DELIVERY:
--   PENDING → DISPATCHING → ASSIGNED → PICKING_UP → PICKED_UP → IN_TRANSIT → COMPLETED
CREATE TABLE missions (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    mission_type            mission_type    NOT NULL,
    -- Exactly one of these two FK columns must be set (enforced via CHECK constraint below).
    sos_request_id          UUID            REFERENCES sos_requests(id),
    aid_request_id          UUID            REFERENCES aid_requests(id),
    -- NULL until a volunteer accepts the dispatch notification.
    volunteer_id            UUID            REFERENCES users(id),
    -- The hub volunteer must visit to pick up goods (DELIVERY only; NULL for RESCUE).
    hub_id                  UUID            REFERENCES hubs(id),
    status                  mission_status  NOT NULL DEFAULT 'PENDING',
    -- QR code token the volunteer presents to staff at the hub for pickup verification.
    qr_code_token           VARCHAR(512)    UNIQUE,
    -- The computed Priority Score of the winning volunteer at the time of acceptance.
    -- Stored for analytics and algorithm tuning.
    priority_score          DECIMAL(8, 4),
    dispatched_at           TIMESTAMPTZ,
    accepted_at             TIMESTAMPTZ,
    -- Timestamp of successful hub QR scan (DELIVERY only).
    picked_up_at            TIMESTAMPTZ,
    delivered_at            TIMESTAMPTZ,
    completed_at            TIMESTAMPTZ,
    cancelled_at            TIMESTAMPTZ,
    cancellation_reason     TEXT,
    -- Proof-of-delivery photo URL or confirmation screenshot.
    confirmation_image_url  VARCHAR(512),
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    -- Enforce that exactly one request type is linked per mission_type.
    CONSTRAINT missions_type_request_integrity CHECK (
        (mission_type = 'RESCUE'   AND sos_request_id IS NOT NULL AND aid_request_id IS NULL)
        OR
        (mission_type = 'DELIVERY' AND aid_request_id IS NOT NULL AND sos_request_id IS NULL)
    )
);

CREATE TRIGGER trg_missions_updated_at
    BEFORE UPDATE ON missions
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();


-- dispatch_attempts: Audit log for every volunteer notification sent by the
-- coordination algorithm. Enables post-hoc analysis and manual override by Admin.
--
-- For BROADCAST: one row per volunteer notified simultaneously (batch_number = 1).
-- For SEQUENTIAL: batch_number 1 = top volunteer; 2 = next trio; 3 = expanded radius, etc.
CREATE TABLE dispatch_attempts (
    id              UUID                PRIMARY KEY DEFAULT gen_random_uuid(),
    mission_id      UUID                NOT NULL REFERENCES missions(id) ON DELETE CASCADE,
    volunteer_id    UUID                NOT NULL REFERENCES users(id),
    dispatch_type   dispatch_type       NOT NULL,
    -- Sequential batch number (1 = first attempt). Always 1 for BROADCAST.
    batch_number    INT                 NOT NULL DEFAULT 1,
    -- The search radius (in km) that was active when this attempt was made.
    radius_km       DECIMAL(5, 2)       NOT NULL,
    sent_at         TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    response        dispatch_response   NOT NULL DEFAULT 'PENDING',
    responded_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    CONSTRAINT dispatch_attempts_batch_positive CHECK (batch_number > 0),
    CONSTRAINT dispatch_attempts_radius_positive CHECK (radius_km > 0)
);


-- ============================================================
-- SECTION 10: COMMUNICATION SYSTEM
-- ============================================================

-- chat_messages: Real-time in-mission chat between a Victim and the assigned Volunteer.
-- Messages are mission-scoped so the chat history is preserved with the mission record.
CREATE TABLE chat_messages (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    mission_id      UUID            NOT NULL REFERENCES missions(id) ON DELETE CASCADE,
    sender_id       UUID            NOT NULL REFERENCES users(id),
    message_type    message_type    NOT NULL DEFAULT 'TEXT',
    -- Populated for TEXT messages; NULL for IMAGE messages.
    message_text    TEXT,
    -- Populated for IMAGE messages (Supabase Storage URL); NULL for TEXT messages.
    image_url       VARCHAR(512),
    is_read         BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    -- Enforce content is present and correct for the declared message type.
    CONSTRAINT chat_messages_content_integrity CHECK (
        (message_type = 'TEXT'  AND message_text IS NOT NULL AND image_url IS NULL)
        OR
        (message_type = 'IMAGE' AND image_url    IS NOT NULL AND message_text IS NULL)
    )
);


-- ratings: One-per-mission star rating submitted by the Victim for the Volunteer.
-- Used to update volunteer_profiles.avg_rating after each completed mission.
CREATE TABLE ratings (
    id          UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    -- UNIQUE ensures only one rating can exist per completed mission.
    mission_id  UUID    NOT NULL UNIQUE REFERENCES missions(id),
    rater_id    UUID    NOT NULL REFERENCES users(id),   -- The Victim
    ratee_id    UUID    NOT NULL REFERENCES users(id),   -- The Volunteer
    -- Stars: 1 (worst) to 5 (best).
    score       INT     NOT NULL,
    comment     TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ratings_score_range CHECK (score >= 1 AND score <= 5)
);


-- notifications: Persistent in-app notification inbox.
-- Created server-side after each significant state transition.
-- FCM push is fired in parallel; this record enables the in-app inbox history.
CREATE TABLE notifications (
    id                  UUID                PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID                NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    notification_type   notification_type   NOT NULL,
    title               VARCHAR(255)        NOT NULL,
    body                TEXT                NOT NULL,
    -- Polymorphic soft-reference to the triggering entity (e.g., 'MISSION', 'DONATION').
    related_entity_type VARCHAR(50),
    -- ID of the triggering entity in its respective table.
    related_entity_id   UUID,
    is_read             BOOLEAN             NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ         NOT NULL DEFAULT NOW()
);


-- ============================================================
-- SECTION 11: INDEXES
-- ============================================================

-- --- GIST SPATIAL INDEXES ---
-- Required for ST_DWithin radius queries used in volunteer dispatch,
-- Smart Hub Selection, and map rendering.

-- Volunteer current position (primary dispatch lookup — most frequently queried spatial index).
CREATE INDEX idx_volunteer_location
    ON volunteer_profiles USING GIST (current_location);

-- Hub location (Smart Hub Selection, map markers).
CREATE INDEX idx_hubs_location
    ON hubs USING GIST (location);

-- Shelter location (public map + nearest-shelter routing).
CREATE INDEX idx_shelters_location
    ON shelters USING GIST (location);

-- SOS victim position (dispatch radius query + heatmap aggregation).
CREATE INDEX idx_sos_requests_victim_location
    ON sos_requests USING GIST (victim_location);

-- Aid request delivery position (dispatch radius query + heatmap aggregation).
CREATE INDEX idx_aid_requests_location
    ON aid_requests USING GIST (location);


-- --- B-TREE INDEXES FOR FREQUENT LOOKUPS ---

CREATE INDEX idx_users_email          ON users (email);
CREATE INDEX idx_users_phone          ON users (phone_number);
CREATE INDEX idx_users_role           ON users (role);

-- Partial index: only the rows that matter for dispatch radius queries.
CREATE INDEX idx_volunteers_online
    ON volunteer_profiles (is_online)
    WHERE is_online = TRUE;

CREATE INDEX idx_sos_requests_status   ON sos_requests (status);
CREATE INDEX idx_sos_requests_urgency  ON sos_requests (urgency_level);
CREATE INDEX idx_sos_requests_created  ON sos_requests (created_at DESC);

CREATE INDEX idx_aid_requests_status   ON aid_requests (status);
CREATE INDEX idx_aid_requests_requester ON aid_requests (requester_id);

CREATE INDEX idx_missions_status        ON missions (status);
CREATE INDEX idx_missions_volunteer     ON missions (volunteer_id);
CREATE INDEX idx_missions_sos_request   ON missions (sos_request_id);
CREATE INDEX idx_missions_aid_request   ON missions (aid_request_id);
CREATE INDEX idx_missions_hub           ON missions (hub_id);

CREATE INDEX idx_dispatch_mission       ON dispatch_attempts (mission_id);
CREATE INDEX idx_dispatch_volunteer     ON dispatch_attempts (volunteer_id);
-- Quickly find all PENDING attempts for a given mission (timeout checking).
CREATE INDEX idx_dispatch_pending
    ON dispatch_attempts (mission_id)
    WHERE response = 'PENDING';

CREATE INDEX idx_donations_sponsor     ON donations (sponsor_id);
CREATE INDEX idx_donations_hub         ON donations (hub_id);
CREATE INDEX idx_donations_status      ON donations (status);
CREATE INDEX idx_donations_qr_token    ON donations (qr_code_token);

-- Quickly find all stock entries for a hub below threshold (STOCK_LOW alerting).
CREATE INDEX idx_hub_inv_hub           ON hub_inventories (hub_id);
CREATE INDEX idx_hub_inv_low_stock
    ON hub_inventories (hub_id)
    WHERE current_quantity <= min_threshold;

CREATE INDEX idx_inv_logs_ref          ON inventory_logs (reference_id, reference_type);

CREATE INDEX idx_chat_mission          ON chat_messages (mission_id);
-- Quickly fetch unread messages per mission.
CREATE INDEX idx_chat_unread
    ON chat_messages (mission_id)
    WHERE is_read = FALSE;

CREATE INDEX idx_notifications_user    ON notifications (user_id, created_at DESC);
-- Fast "badge count" query for unread notifications.
CREATE INDEX idx_notifications_unread
    ON notifications (user_id)
    WHERE is_read = FALSE;

CREATE INDEX idx_otp_user              ON otp_verifications (user_id);
-- Quickly find valid (unused, non-expired) OTPs.
CREATE INDEX idx_otp_active
    ON otp_verifications (user_id, otp_type)
    WHERE is_used = FALSE;
