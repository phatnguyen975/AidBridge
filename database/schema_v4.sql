-- ============================================================
-- AidBridge: Database Schema v4.0
-- Engine      : PostgreSQL 15+  |  Optimized for Supabase
-- Extensions  : uuid-ossp (run CREATE EXTENSION IF NOT EXISTS "uuid-ossp")
-- Tables      : 24  |  ENUMs: 18  |  Indexes: ~46
-- ============================================================
-- CHANGES from v3.1:
--   1. Tất cả CHECK string → ENUM (type-safe, maintainable)
--   2. Thêm mission_items để track items mỗi mission
--   3. Thêm OTP rate limiting (otp_attempts, otp_locked_until)
--   4. Thêm version column cho optimistic locking
--   5. Cải thiện QR security (hash token)
--   6. Thêm triggers tự động cập nhật stats
--   7. Email/phone normalization
--   8. aid_requests.sos_request_id để link SOS → Aid Request
-- ============================================================
-- SAFE TO RE-RUN: DROP IF EXISTS trước khi tạo lại.
-- ============================================================
-- ============================================================
-- SECTION 0: RESET
-- ============================================================
-- Trigger functions
DROP FUNCTION IF EXISTS fn_set_updated_at() CASCADE;
DROP FUNCTION IF EXISTS fn_mark_parent_non_leaf() CASCADE;
DROP FUNCTION IF EXISTS fn_assert_leaf_category() CASCADE;
DROP FUNCTION IF EXISTS fn_assert_staff_role() CASCADE;
DROP FUNCTION IF EXISTS fn_update_volunteer_stats() CASCADE;
DROP FUNCTION IF EXISTS fn_update_volunteer_rating() CASCADE;
DROP FUNCTION IF EXISTS fn_update_sponsor_badge() CASCADE;
DROP FUNCTION IF EXISTS fn_normalize_email() CASCADE;
DROP FUNCTION IF EXISTS fn_normalize_phone() CASCADE;
-- Tables (reverse dependency order)
DROP TABLE IF EXISTS notifications CASCADE;
DROP TABLE IF EXISTS ratings CASCADE;
DROP TABLE IF EXISTS chat_messages CASCADE;
DROP TABLE IF EXISTS dispatch_attempts CASCADE;
DROP TABLE IF EXISTS mission_items CASCADE;
DROP TABLE IF EXISTS missions CASCADE;
DROP TABLE IF EXISTS donation_items CASCADE;
DROP TABLE IF EXISTS donations CASCADE;
DROP TABLE IF EXISTS aid_request_items CASCADE;
DROP TABLE IF EXISTS aid_requests CASCADE;
DROP TABLE IF EXISTS sos_requests CASCADE;
DROP TABLE IF EXISTS inventory_logs CASCADE;
DROP TABLE IF EXISTS hub_inventories CASCADE;
DROP TABLE IF EXISTS hub_accepted_categories CASCADE;
DROP TABLE IF EXISTS item_categories CASCADE;
DROP TABLE IF EXISTS system_config CASCADE;
DROP TABLE IF EXISTS shelters CASCADE;
DROP TABLE IF EXISTS hub_staff CASCADE;
DROP TABLE IF EXISTS hubs CASCADE;
DROP TABLE IF EXISTS sponsor_profiles CASCADE;
DROP TABLE IF EXISTS volunteer_profiles CASCADE;
DROP TABLE IF EXISTS refresh_tokens CASCADE;
DROP TABLE IF EXISTS users CASCADE;
-- ENUM types (all)
DROP TYPE IF EXISTS user_role CASCADE;
DROP TYPE IF EXISTS hub_status CASCADE;
DROP TYPE IF EXISTS urgency_level CASCADE;
DROP TYPE IF EXISTS sos_status CASCADE;
DROP TYPE IF EXISTS aid_status CASCADE;
DROP TYPE IF EXISTS donation_status CASCADE;
DROP TYPE IF EXISTS mission_type CASCADE;
DROP TYPE IF EXISTS mission_status CASCADE;
DROP TYPE IF EXISTS dispatch_response CASCADE;
DROP TYPE IF EXISTS badge_level CASCADE;
DROP TYPE IF EXISTS otp_type CASCADE;
DROP TYPE IF EXISTS shelter_status CASCADE;
DROP TYPE IF EXISTS inventory_change CASCADE;
DROP TYPE IF EXISTS inventory_ref_type CASCADE;
DROP TYPE IF EXISTS dispatch_type CASCADE;
DROP TYPE IF EXISTS message_type CASCADE;
DROP TYPE IF EXISTS notification_type CASCADE;
DROP TYPE IF EXISTS vehicle_type CASCADE;
-- ============================================================
-- SECTION 1: ENUM TYPES (18 ENUMs)
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
CREATE TYPE mission_type AS ENUM ('RESCUE', 'DELIVERY');
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
-- NEW ENUMs (converted from CHECK constraints)
CREATE TYPE otp_type AS ENUM (
    'EMAIL_VERIFY',
    'PHONE_VERIFY',
    'PASSWORD_RESET'
);
CREATE TYPE shelter_status AS ENUM (
    'AVAILABLE',
    'FULL',
    'UNAVAILABLE'
);
CREATE TYPE inventory_change AS ENUM ('INBOUND', 'OUTBOUND');
CREATE TYPE inventory_ref_type AS ENUM ('DONATION', 'MISSION');
CREATE TYPE dispatch_type AS ENUM ('BROADCAST', 'SEQUENTIAL');
CREATE TYPE message_type AS ENUM ('TEXT', 'IMAGE');
CREATE TYPE notification_type AS ENUM (
    'MISSION',
    'SOS',
    'DONATION',
    'AID_REQUEST',
    'SYSTEM'
);
CREATE TYPE vehicle_type AS ENUM (
    'MOTORBIKE',
    'CAR',
    'TRUCK',
    'BICYCLE',
    'WALK'
);
-- ============================================================
-- SECTION 2: SHARED TRIGGER FUNCTIONS
-- ============================================================
-- Auto-maintain updated_at on every UPDATE
CREATE OR REPLACE FUNCTION fn_set_updated_at() RETURNS TRIGGER AS $$ BEGIN NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;
-- Khi INSERT một leaf category → auto SET parent.is_leaf = FALSE
CREATE OR REPLACE FUNCTION fn_mark_parent_non_leaf() RETURNS TRIGGER AS $$ BEGIN IF NEW.parent_id IS NOT NULL THEN
UPDATE item_categories
SET is_leaf = FALSE
WHERE id = NEW.parent_id;
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;
-- Đảm bảo chỉ leaf category được dùng trong inventory / request / donation
CREATE OR REPLACE FUNCTION fn_assert_leaf_category() RETURNS TRIGGER AS $$ BEGIN IF NOT EXISTS (
        SELECT 1
        FROM item_categories
        WHERE id = NEW.item_category_id
            AND is_leaf = TRUE
    ) THEN RAISE EXCEPTION 'Chỉ leaf category được phép. Category % không phải leaf.',
    NEW.item_category_id;
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;
-- Đảm bảo chỉ user role=STAFF được thêm vào hub_staff
CREATE OR REPLACE FUNCTION fn_assert_staff_role() RETURNS TRIGGER AS $$ BEGIN IF NOT EXISTS (
        SELECT 1
        FROM users
        WHERE id = NEW.user_id
            AND role = 'STAFF'
    ) THEN RAISE EXCEPTION 'Chỉ user có role=STAFF được phân công vào hub_staff. user_id: %',
    NEW.user_id;
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;
-- Auto-update volunteer stats khi mission COMPLETED
CREATE OR REPLACE FUNCTION fn_update_volunteer_stats() RETURNS TRIGGER AS $$ BEGIN -- Chỉ trigger khi status chuyển sang COMPLETED
    IF NEW.status = 'COMPLETED'
    AND OLD.status <> 'COMPLETED'
    AND NEW.volunteer_id IS NOT NULL THEN
UPDATE volunteer_profiles
SET total_tasks_completed = total_tasks_completed + 1,
    updated_at = CURRENT_TIMESTAMP
WHERE id = NEW.volunteer_id;
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;
-- Auto-update volunteer avg_rating khi có rating mới
CREATE OR REPLACE FUNCTION fn_update_volunteer_rating() RETURNS TRIGGER AS $$
DECLARE v_volunteer_id UUID;
v_new_avg DECIMAL(3, 2);
BEGIN -- Lấy volunteer_id từ mission
SELECT volunteer_id INTO v_volunteer_id
FROM missions
WHERE id = NEW.mission_id;
IF v_volunteer_id IS NOT NULL THEN -- Tính trung bình mới từ tất cả ratings của volunteer này
SELECT COALESCE(AVG(r.score), 0)::DECIMAL(3, 2) INTO v_new_avg
FROM ratings r
    JOIN missions m ON r.mission_id = m.id
WHERE m.volunteer_id = v_volunteer_id;
UPDATE volunteer_profiles
SET avg_rating = v_new_avg,
    updated_at = CURRENT_TIMESTAMP
WHERE id = v_volunteer_id;
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;
-- Auto-update sponsor badge khi donation được RECEIVED
CREATE OR REPLACE FUNCTION fn_update_sponsor_badge() RETURNS TRIGGER AS $$
DECLARE v_donation_count INT;
v_new_badge badge_level;
BEGIN -- Chỉ trigger khi status chuyển sang RECEIVED
IF NEW.status = 'RECEIVED'
AND OLD.status <> 'RECEIVED' THEN -- Cập nhật stats
UPDATE sponsor_profiles
SET donation_count = donation_count + 1,
    total_items_donated = total_items_donated + (
        SELECT COALESCE(SUM(quantity), 0)
        FROM donation_items
        WHERE donation_id = NEW.id
    ),
    total_points = total_points + (
        SELECT COALESCE(SUM(quantity), 0)
        FROM donation_items
        WHERE donation_id = NEW.id
    ),
    updated_at = CURRENT_TIMESTAMP
WHERE user_id = NEW.sponsor_id
RETURNING donation_count INTO v_donation_count;
-- Tính badge mới
v_new_badge := CASE
    WHEN v_donation_count >= 50 THEN 'PLATINUM'
    WHEN v_donation_count >= 25 THEN 'GOLD'
    WHEN v_donation_count >= 10 THEN 'SILVER'
    ELSE 'BRONZE'
END;
-- Update badge nếu khác
UPDATE sponsor_profiles
SET badge_level = v_new_badge,
    updated_at = CURRENT_TIMESTAMP
WHERE user_id = NEW.sponsor_id
    AND badge_level <> v_new_badge;
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;
-- Normalize email to lowercase
CREATE OR REPLACE FUNCTION fn_normalize_email() RETURNS TRIGGER AS $$ BEGIN IF NEW.email IS NOT NULL THEN NEW.email = LOWER(TRIM(NEW.email));
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;
-- Normalize phone (chỉ giữ số)
CREATE OR REPLACE FUNCTION fn_normalize_phone() RETURNS TRIGGER AS $$ BEGIN IF NEW.phone_number IS NOT NULL THEN NEW.phone_number = REGEXP_REPLACE(NEW.phone_number, '[^0-9+]', '', 'g');
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;
-- ============================================================
-- SECTION 3: CREATE ALL TABLES
-- ============================================================
-- ------------------------------------------------------------
-- GROUP: AUTH & SESSIONS
-- ------------------------------------------------------------
CREATE TABLE users (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(255),
    phone_number VARCHAR(20),
    password_hash VARCHAR(255) NOT NULL,
    role user_role NOT NULL,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    fcm_token VARCHAR(500),
    avatar_url VARCHAR(500),
    -- OTP (single-use, short-TTL)
    otp_code CHAR(6),
    otp_type otp_type,
    -- ENUM thay vì CHECK
    otp_expires_at TIMESTAMPTZ,
    otp_attempts SMALLINT NOT NULL DEFAULT 0,
    -- Rate limiting
    otp_locked_until TIMESTAMPTZ,
    -- Lockout time
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT users_pkey PRIMARY KEY (id),
    CONSTRAINT users_email_unique UNIQUE (email),
    CONSTRAINT users_phone_unique UNIQUE (phone_number),
    CONSTRAINT users_email_or_phone CHECK (
        email IS NOT NULL
        OR phone_number IS NOT NULL
    ),
    CONSTRAINT users_otp_attempts_valid CHECK (
        otp_attempts >= 0
        AND otp_attempts <= 5
    )
);
CREATE TABLE refresh_tokens (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    -- SHA-256 hash
    device_info TEXT,
    expires_at TIMESTAMPTZ NOT NULL,
    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id),
    CONSTRAINT refresh_tokens_hash_unique UNIQUE (token_hash)
);
-- ------------------------------------------------------------
-- GROUP: PROFILES
-- ------------------------------------------------------------
CREATE TABLE volunteer_profiles (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    is_online BOOLEAN NOT NULL DEFAULT FALSE,
    current_lat DECIMAL(9, 6),
    current_lng DECIMAL(9, 6),
    vehicle_type vehicle_type,
    -- ENUM thay vì VARCHAR
    total_tasks_completed INT NOT NULL DEFAULT 0,
    avg_rating DECIMAL(3, 2) NOT NULL DEFAULT 0.00,
    avg_response_seconds INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT volunteer_profiles_pkey PRIMARY KEY (id),
    CONSTRAINT volunteer_profiles_user_unique UNIQUE (user_id),
    CONSTRAINT volunteer_profiles_avg_rating_range CHECK (
        avg_rating BETWEEN 0.00 AND 5.00
    ),
    CONSTRAINT volunteer_profiles_total_tasks_non_neg CHECK (total_tasks_completed >= 0),
    CONSTRAINT volunteer_profiles_avg_response_non_neg CHECK (avg_response_seconds >= 0)
);
CREATE TABLE sponsor_profiles (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    total_points INT NOT NULL DEFAULT 0,
    total_items_donated INT NOT NULL DEFAULT 0,
    donation_count INT NOT NULL DEFAULT 0,
    badge_level badge_level NOT NULL DEFAULT 'BRONZE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT sponsor_profiles_pkey PRIMARY KEY (id),
    CONSTRAINT sponsor_profiles_user_unique UNIQUE (user_id),
    CONSTRAINT sponsor_profiles_points_non_neg CHECK (total_points >= 0),
    CONSTRAINT sponsor_profiles_items_non_neg CHECK (total_items_donated >= 0),
    CONSTRAINT sponsor_profiles_count_non_neg CHECK (donation_count >= 0)
);
-- ------------------------------------------------------------
-- GROUP: INFRASTRUCTURE
-- ------------------------------------------------------------
CREATE TABLE hubs (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    address TEXT NOT NULL,
    lat DECIMAL(9, 6) NOT NULL,
    lng DECIMAL(9, 6) NOT NULL,
    status hub_status NOT NULL DEFAULT 'ACTIVE',
    contact_phone VARCHAR(20),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT hubs_pkey PRIMARY KEY (id)
);
CREATE TABLE hub_staff (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    hub_id UUID NOT NULL,
    user_id UUID NOT NULL,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    unassigned_at TIMESTAMPTZ,
    CONSTRAINT hub_staff_pkey PRIMARY KEY (id)
);
-- Partial UNIQUE: mỗi người chỉ active ở 1 hub tại 1 thời điểm
CREATE UNIQUE INDEX idx_hub_staff_active_unique ON hub_staff (hub_id, user_id)
WHERE unassigned_at IS NULL;
CREATE TABLE shelters (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    address TEXT,
    lat DECIMAL(9, 6),
    lng DECIMAL(9, 6),
    current_capacity INT NOT NULL DEFAULT 0,
    max_capacity INT NOT NULL,
    has_electricity BOOLEAN NOT NULL DEFAULT FALSE,
    has_clean_water BOOLEAN NOT NULL DEFAULT FALSE,
    status shelter_status NOT NULL DEFAULT 'AVAILABLE',
    -- ENUM
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT shelters_pkey PRIMARY KEY (id),
    CONSTRAINT shelters_capacity_valid CHECK (
        current_capacity >= 0
        AND current_capacity <= max_capacity
    ),
    CONSTRAINT shelters_max_capacity_positive CHECK (max_capacity > 0)
);
CREATE TABLE system_config (
    key VARCHAR(100) NOT NULL,
    value TEXT NOT NULL,
    description TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT system_config_pkey PRIMARY KEY (key)
);
-- ------------------------------------------------------------
-- GROUP: CATALOG & INVENTORY
-- ------------------------------------------------------------
CREATE TABLE item_categories (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    parent_id UUID,
    name VARCHAR(100) NOT NULL,
    name_vi VARCHAR(100) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    is_leaf BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT item_categories_pkey PRIMARY KEY (id)
);
CREATE TABLE hub_accepted_categories (
    hub_id UUID NOT NULL,
    item_category_id UUID NOT NULL,
    CONSTRAINT hub_accepted_categories_pkey PRIMARY KEY (hub_id, item_category_id)
);
CREATE TABLE hub_inventories (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    hub_id UUID NOT NULL,
    item_category_id UUID NOT NULL,
    current_quantity INT NOT NULL DEFAULT 0,
    low_stock_threshold INT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 1,
    -- Optimistic locking
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT hub_inventories_pkey PRIMARY KEY (id),
    CONSTRAINT hub_inventories_unique_slot UNIQUE (hub_id, item_category_id),
    CONSTRAINT hub_inventories_qty_non_negative CHECK (current_quantity >= 0),
    CONSTRAINT hub_inventories_threshold_non_negative CHECK (low_stock_threshold >= 0),
    CONSTRAINT hub_inventories_version_positive CHECK (version >= 1)
);
CREATE TABLE inventory_logs (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    hub_inventory_id UUID NOT NULL,
    change_type inventory_change NOT NULL,
    -- ENUM: INBOUND/OUTBOUND
    quantity_delta INT NOT NULL,
    reference_type inventory_ref_type,
    -- ENUM: DONATION/MISSION
    reference_id UUID,
    performed_by UUID,
    quantity_after INT NOT NULL,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT inventory_logs_pkey PRIMARY KEY (id),
    CONSTRAINT inventory_logs_delta_positive CHECK (quantity_delta > 0)
);
-- ------------------------------------------------------------
-- GROUP: REQUESTS
-- ------------------------------------------------------------
-- SOS Requests - đầy đủ theo ERD.md v3.0
CREATE TABLE sos_requests (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    requester_id UUID,
    -- NULL cho Guest
    requester_name VARCHAR(100) NOT NULL,
    requester_phone VARCHAR(20) NOT NULL,
    victim_name VARCHAR(100),
    victim_phone VARCHAR(20),
    victim_lat DECIMAL(9, 6) NOT NULL,
    victim_lng DECIMAL(9, 6) NOT NULL,
    victim_address TEXT,
    description TEXT,
    people_count INT NOT NULL,
    is_on_behalf BOOLEAN NOT NULL DEFAULT FALSE,
    urgency_level urgency_level NOT NULL DEFAULT 'MEDIUM',
    ai_summary TEXT,
    status sos_status NOT NULL DEFAULT 'PENDING',
    image_url VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT sos_requests_pkey PRIMARY KEY (id),
    CONSTRAINT sos_requests_people_count_positive CHECK (people_count > 0)
);
CREATE TABLE aid_requests (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    requester_id UUID NOT NULL,
    sos_request_id UUID,
    -- Link to SOS (SOS có thể kèm Aid Request)
    lat DECIMAL(9, 6) NOT NULL,
    lng DECIMAL(9, 6) NOT NULL,
    address TEXT,
    adults_count INT NOT NULL DEFAULT 0,
    elderly_count INT NOT NULL DEFAULT 0,
    children_count INT NOT NULL DEFAULT 0,
    notes TEXT,
    urgency_level urgency_level NOT NULL DEFAULT 'MEDIUM',
    status aid_status NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT aid_requests_pkey PRIMARY KEY (id),
    CONSTRAINT aid_requests_people_positive CHECK (
        adults_count + elderly_count + children_count > 0
    ),
    CONSTRAINT aid_requests_counts_non_negative CHECK (
        adults_count >= 0
        AND elderly_count >= 0
        AND children_count >= 0
    )
);
CREATE TABLE aid_request_items (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    aid_request_id UUID NOT NULL,
    item_category_id UUID NOT NULL,
    quantity INT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT aid_request_items_pkey PRIMARY KEY (id),
    CONSTRAINT aid_request_items_qty_positive CHECK (quantity > 0)
);
-- ------------------------------------------------------------
-- GROUP: DONATIONS
-- ------------------------------------------------------------
CREATE TABLE donations (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    sponsor_id UUID NOT NULL,
    hub_id UUID,
    estimated_delivery_at DATE,
    qr_code_token VARCHAR(64),
    -- SHA-256 hash (64 hex chars)
    status donation_status NOT NULL DEFAULT 'REGISTERED',
    received_by UUID,
    received_at TIMESTAMPTZ,
    rejection_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT donations_pkey PRIMARY KEY (id),
    CONSTRAINT donations_qr_code_unique UNIQUE (qr_code_token)
);
CREATE TABLE donation_items (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    donation_id UUID NOT NULL,
    item_category_id UUID NOT NULL,
    quantity INT NOT NULL,
    expiry_date DATE,
    condition_notes TEXT,
    image_url VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT donation_items_pkey PRIMARY KEY (id),
    CONSTRAINT donation_items_qty_positive CHECK (quantity > 0)
);
-- ------------------------------------------------------------
-- GROUP: MISSIONS
-- ------------------------------------------------------------
CREATE TABLE missions (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    mission_type mission_type NOT NULL,
    sos_request_id UUID,
    -- RESCUE only
    aid_request_id UUID,
    -- DELIVERY only
    volunteer_id UUID,
    hub_id UUID,
    -- DELIVERY only
    status mission_status NOT NULL DEFAULT 'PENDING',
    qr_code_token VARCHAR(64),
    -- SHA-256 hash
    priority_score DECIMAL(8, 4),
    version INT NOT NULL DEFAULT 1,
    -- Optimistic locking
    -- Lifecycle timestamps
    accepted_at TIMESTAMPTZ,
    picked_up_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    cancellation_reason TEXT,
    confirmation_image_url VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT missions_pkey PRIMARY KEY (id),
    CONSTRAINT missions_qr_code_unique UNIQUE (qr_code_token),
    CONSTRAINT missions_version_positive CHECK (version >= 1),
    -- RESCUE: sos_request_id bắt buộc; aid_request_id và hub_id phải NULL
    CONSTRAINT missions_rescue_integrity CHECK (
        mission_type <> 'RESCUE'
        OR (
            sos_request_id IS NOT NULL
            AND aid_request_id IS NULL
            AND hub_id IS NULL
        )
    ),
    -- DELIVERY: aid_request_id và hub_id bắt buộc; sos_request_id phải NULL
    CONSTRAINT missions_delivery_integrity CHECK (
        mission_type <> 'DELIVERY'
        OR (
            aid_request_id IS NOT NULL
            AND hub_id IS NOT NULL
            AND sos_request_id IS NULL
        )
    )
);
-- NEW: Track items cho mỗi mission (DELIVERY)
CREATE TABLE mission_items (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    mission_id UUID NOT NULL,
    item_category_id UUID NOT NULL,
    quantity INT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT mission_items_pkey PRIMARY KEY (id),
    CONSTRAINT mission_items_qty_positive CHECK (quantity > 0)
);
CREATE TABLE dispatch_attempts (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    mission_id UUID NOT NULL,
    volunteer_id UUID NOT NULL,
    dispatch_type dispatch_type NOT NULL,
    -- ENUM: BROADCAST/SEQUENTIAL
    batch_number INT NOT NULL DEFAULT 1,
    radius_km DECIMAL(5, 2),
    priority_score DECIMAL(8, 4),
    sent_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    response dispatch_response NOT NULL DEFAULT 'PENDING',
    responded_at TIMESTAMPTZ,
    CONSTRAINT dispatch_attempts_pkey PRIMARY KEY (id),
    CONSTRAINT dispatch_attempts_batch_positive CHECK (batch_number >= 1)
);
-- ------------------------------------------------------------
-- GROUP: COMMUNICATION
-- ------------------------------------------------------------
CREATE TABLE chat_messages (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    mission_id UUID NOT NULL,
    sender_id UUID NOT NULL,
    message_type message_type NOT NULL,
    -- ENUM: TEXT/IMAGE
    message_text TEXT,
    image_url VARCHAR(500),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chat_messages_pkey PRIMARY KEY (id),
    -- TEXT XOR IMAGE: chỉ một trong hai được phép
    CONSTRAINT chat_messages_text_xor_image CHECK (
        (
            message_type = 'TEXT'
            AND message_text IS NOT NULL
            AND image_url IS NULL
        )
        OR (
            message_type = 'IMAGE'
            AND image_url IS NOT NULL
            AND message_text IS NULL
        )
    )
);
CREATE TABLE ratings (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    mission_id UUID NOT NULL,
    rater_id UUID NOT NULL,
    ratee_id UUID NOT NULL,
    score SMALLINT NOT NULL,
    comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ratings_pkey PRIMARY KEY (id),
    CONSTRAINT ratings_mission_unique UNIQUE (mission_id),
    CONSTRAINT ratings_score_range CHECK (
        score BETWEEN 1 AND 5
    )
);
CREATE TABLE notifications (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    body TEXT,
    related_type notification_type,
    -- ENUM
    related_id UUID,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT notifications_pkey PRIMARY KEY (id)
);
-- ============================================================
-- SECTION 4: FOREIGN KEY CONSTRAINTS
-- ============================================================
-- ---------------- AUTH ----------------------------------------
ALTER TABLE refresh_tokens
ADD CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
-- ---------------- PROFILES ------------------------------------
ALTER TABLE volunteer_profiles
ADD CONSTRAINT fk_volunteer_profiles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE sponsor_profiles
ADD CONSTRAINT fk_sponsor_profiles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
-- ---------------- INFRASTRUCTURE ------------------------------
ALTER TABLE hub_staff
ADD CONSTRAINT fk_hub_staff_hub FOREIGN KEY (hub_id) REFERENCES hubs(id) ON DELETE RESTRICT;
ALTER TABLE hub_staff
ADD CONSTRAINT fk_hub_staff_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT;
-- ---------------- CATALOG (self-ref) --------------------------
ALTER TABLE item_categories
ADD CONSTRAINT fk_item_categories_parent FOREIGN KEY (parent_id) REFERENCES item_categories(id) ON DELETE
SET NULL;
-- ---------------- INVENTORY -----------------------------------
ALTER TABLE hub_accepted_categories
ADD CONSTRAINT fk_hub_accepted_cat_hub FOREIGN KEY (hub_id) REFERENCES hubs(id) ON DELETE CASCADE;
ALTER TABLE hub_accepted_categories
ADD CONSTRAINT fk_hub_accepted_cat_category FOREIGN KEY (item_category_id) REFERENCES item_categories(id) ON DELETE CASCADE;
ALTER TABLE hub_inventories
ADD CONSTRAINT fk_hub_inventories_hub FOREIGN KEY (hub_id) REFERENCES hubs(id) ON DELETE RESTRICT;
ALTER TABLE hub_inventories
ADD CONSTRAINT fk_hub_inventories_category FOREIGN KEY (item_category_id) REFERENCES item_categories(id) ON DELETE RESTRICT;
ALTER TABLE inventory_logs
ADD CONSTRAINT fk_inventory_logs_hub_inventory FOREIGN KEY (hub_inventory_id) REFERENCES hub_inventories(id) ON DELETE RESTRICT;
ALTER TABLE inventory_logs
ADD CONSTRAINT fk_inventory_logs_performed_by FOREIGN KEY (performed_by) REFERENCES users(id) ON DELETE
SET NULL;
-- ---------------- REQUESTS ------------------------------------
ALTER TABLE sos_requests
ADD CONSTRAINT fk_sos_requests_requester FOREIGN KEY (requester_id) REFERENCES users(id) ON DELETE
SET NULL;
ALTER TABLE aid_requests
ADD CONSTRAINT fk_aid_requests_requester FOREIGN KEY (requester_id) REFERENCES users(id) ON DELETE RESTRICT;
ALTER TABLE aid_requests
ADD CONSTRAINT fk_aid_requests_sos FOREIGN KEY (sos_request_id) REFERENCES sos_requests(id) ON DELETE
SET NULL;
-- SOS xóa → giữ Aid Request, sos_request_id = NULL
ALTER TABLE aid_request_items
ADD CONSTRAINT fk_aid_request_items_request FOREIGN KEY (aid_request_id) REFERENCES aid_requests(id) ON DELETE CASCADE;
ALTER TABLE aid_request_items
ADD CONSTRAINT fk_aid_request_items_category FOREIGN KEY (item_category_id) REFERENCES item_categories(id) ON DELETE RESTRICT;
-- ---------------- DONATIONS -----------------------------------
ALTER TABLE donations
ADD CONSTRAINT fk_donations_sponsor FOREIGN KEY (sponsor_id) REFERENCES users(id) ON DELETE RESTRICT;
ALTER TABLE donations
ADD CONSTRAINT fk_donations_hub FOREIGN KEY (hub_id) REFERENCES hubs(id) ON DELETE
SET NULL;
ALTER TABLE donations
ADD CONSTRAINT fk_donations_received_by FOREIGN KEY (received_by) REFERENCES users(id) ON DELETE
SET NULL;
ALTER TABLE donation_items
ADD CONSTRAINT fk_donation_items_donation FOREIGN KEY (donation_id) REFERENCES donations(id) ON DELETE CASCADE;
ALTER TABLE donation_items
ADD CONSTRAINT fk_donation_items_category FOREIGN KEY (item_category_id) REFERENCES item_categories(id) ON DELETE RESTRICT;
-- ---------------- MISSIONS ------------------------------------
ALTER TABLE missions
ADD CONSTRAINT fk_missions_sos_request FOREIGN KEY (sos_request_id) REFERENCES sos_requests(id) ON DELETE
SET NULL;
ALTER TABLE missions
ADD CONSTRAINT fk_missions_aid_request FOREIGN KEY (aid_request_id) REFERENCES aid_requests(id) ON DELETE
SET NULL;
ALTER TABLE missions
ADD CONSTRAINT fk_missions_volunteer FOREIGN KEY (volunteer_id) REFERENCES volunteer_profiles(id) ON DELETE
SET NULL;
ALTER TABLE missions
ADD CONSTRAINT fk_missions_hub FOREIGN KEY (hub_id) REFERENCES hubs(id) ON DELETE
SET NULL;
ALTER TABLE mission_items
ADD CONSTRAINT fk_mission_items_mission FOREIGN KEY (mission_id) REFERENCES missions(id) ON DELETE CASCADE;
ALTER TABLE mission_items
ADD CONSTRAINT fk_mission_items_category FOREIGN KEY (item_category_id) REFERENCES item_categories(id) ON DELETE RESTRICT;
ALTER TABLE dispatch_attempts
ADD CONSTRAINT fk_dispatch_attempts_mission FOREIGN KEY (mission_id) REFERENCES missions(id) ON DELETE CASCADE;
ALTER TABLE dispatch_attempts
ADD CONSTRAINT fk_dispatch_attempts_volunteer FOREIGN KEY (volunteer_id) REFERENCES volunteer_profiles(id) ON DELETE RESTRICT;
-- ---------------- COMMUNICATION -------------------------------
ALTER TABLE chat_messages
ADD CONSTRAINT fk_chat_messages_mission FOREIGN KEY (mission_id) REFERENCES missions(id) ON DELETE CASCADE;
ALTER TABLE chat_messages
ADD CONSTRAINT fk_chat_messages_sender FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE
SET NULL;
ALTER TABLE ratings
ADD CONSTRAINT fk_ratings_mission FOREIGN KEY (mission_id) REFERENCES missions(id) ON DELETE CASCADE;
ALTER TABLE ratings
ADD CONSTRAINT fk_ratings_rater FOREIGN KEY (rater_id) REFERENCES users(id) ON DELETE
SET NULL;
ALTER TABLE ratings
ADD CONSTRAINT fk_ratings_ratee FOREIGN KEY (ratee_id) REFERENCES users(id) ON DELETE
SET NULL;
ALTER TABLE notifications
ADD CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
-- ============================================================
-- SECTION 5: TRIGGERS
-- ============================================================
-- -------- updated_at triggers --------------------------------
CREATE TRIGGER trg_users_updated_at BEFORE
UPDATE ON users FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_volunteer_profiles_updated_at BEFORE
UPDATE ON volunteer_profiles FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_sponsor_profiles_updated_at BEFORE
UPDATE ON sponsor_profiles FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_hubs_updated_at BEFORE
UPDATE ON hubs FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_shelters_updated_at BEFORE
UPDATE ON shelters FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_system_config_updated_at BEFORE
UPDATE ON system_config FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_hub_inventories_updated_at BEFORE
UPDATE ON hub_inventories FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_sos_requests_updated_at BEFORE
UPDATE ON sos_requests FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_aid_requests_updated_at BEFORE
UPDATE ON aid_requests FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_donations_updated_at BEFORE
UPDATE ON donations FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_missions_updated_at BEFORE
UPDATE ON missions FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
-- -------- Business rule triggers ----------------------------
-- item_categories: set parent.is_leaf = FALSE khi thêm child
CREATE TRIGGER trg_item_categories_mark_parent
AFTER
INSERT ON item_categories FOR EACH ROW EXECUTE FUNCTION fn_mark_parent_non_leaf();
-- hub_inventories: chỉ leaf categories được dùng
CREATE TRIGGER trg_hub_inventories_assert_leaf BEFORE
INSERT
    OR
UPDATE ON hub_inventories FOR EACH ROW EXECUTE FUNCTION fn_assert_leaf_category();
-- aid_request_items: chỉ leaf categories
CREATE TRIGGER trg_aid_request_items_assert_leaf BEFORE
INSERT
    OR
UPDATE ON aid_request_items FOR EACH ROW EXECUTE FUNCTION fn_assert_leaf_category();
-- donation_items: chỉ leaf categories
CREATE TRIGGER trg_donation_items_assert_leaf BEFORE
INSERT
    OR
UPDATE ON donation_items FOR EACH ROW EXECUTE FUNCTION fn_assert_leaf_category();
-- mission_items: chỉ leaf categories
CREATE TRIGGER trg_mission_items_assert_leaf BEFORE
INSERT
    OR
UPDATE ON mission_items FOR EACH ROW EXECUTE FUNCTION fn_assert_leaf_category();
-- hub_staff: chỉ role=STAFF được phân công
CREATE TRIGGER trg_hub_staff_assert_role BEFORE
INSERT
    OR
UPDATE ON hub_staff FOR EACH ROW EXECUTE FUNCTION fn_assert_staff_role();
-- missions: auto-update volunteer stats khi COMPLETED
CREATE TRIGGER trg_missions_update_volunteer_stats
AFTER
UPDATE ON missions FOR EACH ROW EXECUTE FUNCTION fn_update_volunteer_stats();
-- ratings: auto-update volunteer avg_rating
CREATE TRIGGER trg_ratings_update_volunteer
AFTER
INSERT ON ratings FOR EACH ROW EXECUTE FUNCTION fn_update_volunteer_rating();
-- donations: auto-update sponsor stats và badge
CREATE TRIGGER trg_donations_update_sponsor
AFTER
UPDATE ON donations FOR EACH ROW EXECUTE FUNCTION fn_update_sponsor_badge();
-- users: normalize email
CREATE TRIGGER trg_users_normalize_email BEFORE
INSERT
    OR
UPDATE ON users FOR EACH ROW EXECUTE FUNCTION fn_normalize_email();
-- users: normalize phone
CREATE TRIGGER trg_users_normalize_phone BEFORE
INSERT
    OR
UPDATE ON users FOR EACH ROW EXECUTE FUNCTION fn_normalize_phone();
-- ============================================================
-- SECTION 6: INDEXES
-- ============================================================
-- -------- DISPATCH CRITICAL PATH -----------------------------
CREATE INDEX idx_vol_online_location ON volunteer_profiles (current_lat, current_lng)
WHERE is_online = TRUE;
CREATE INDEX idx_sos_location_status ON sos_requests (victim_lat, victim_lng, status)
WHERE status IN ('PENDING', 'DISPATCHING');
CREATE INDEX idx_aid_location_status ON aid_requests (lat, lng, status)
WHERE status IN ('PENDING', 'DISPATCHING');
CREATE INDEX idx_hubs_active_latng ON hubs (lat, lng)
WHERE status = 'ACTIVE';
CREATE INDEX idx_hub_accepted_cat_category ON hub_accepted_categories (item_category_id);
-- -------- FOREIGN KEY INDEXES --------------------------------
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);
CREATE INDEX idx_volunteer_profiles_user_id ON volunteer_profiles (user_id);
CREATE INDEX idx_sponsor_profiles_user_id ON sponsor_profiles (user_id);
CREATE INDEX idx_hub_staff_hub_id ON hub_staff (hub_id);
CREATE INDEX idx_hub_staff_user_id ON hub_staff (user_id);
CREATE INDEX idx_item_categories_parent_id ON item_categories (parent_id);
CREATE INDEX idx_hub_accepted_cat_hub_id ON hub_accepted_categories (hub_id);
CREATE INDEX idx_hub_inventories_hub_id ON hub_inventories (hub_id);
CREATE INDEX idx_hub_inventories_category_id ON hub_inventories (item_category_id);
CREATE INDEX idx_inventory_logs_hub_inv_id ON inventory_logs (hub_inventory_id);
CREATE INDEX idx_inventory_logs_performed_by ON inventory_logs (performed_by);
CREATE INDEX idx_sos_requests_requester_id ON sos_requests (requester_id);
CREATE INDEX idx_aid_requests_requester_id ON aid_requests (requester_id);
CREATE INDEX idx_aid_requests_sos_id ON aid_requests (sos_request_id)
WHERE sos_request_id IS NOT NULL;
CREATE INDEX idx_aid_request_items_request_id ON aid_request_items (aid_request_id);
CREATE INDEX idx_aid_request_items_category_id ON aid_request_items (item_category_id);
CREATE INDEX idx_donations_sponsor_id ON donations (sponsor_id);
CREATE INDEX idx_donations_hub_id ON donations (hub_id);
CREATE INDEX idx_donations_received_by ON donations (received_by);
CREATE INDEX idx_donation_items_donation_id ON donation_items (donation_id);
CREATE INDEX idx_donation_items_category_id ON donation_items (item_category_id);
CREATE INDEX idx_missions_volunteer_id ON missions (volunteer_id);
CREATE INDEX idx_missions_hub_id ON missions (hub_id);
CREATE INDEX idx_mission_items_mission_id ON mission_items (mission_id);
CREATE INDEX idx_mission_items_category_id ON mission_items (item_category_id);
CREATE INDEX idx_dispatch_attempts_mission_id ON dispatch_attempts (mission_id);
CREATE INDEX idx_dispatch_attempts_volunteer_id ON dispatch_attempts (volunteer_id);
CREATE INDEX idx_chat_messages_mission_id ON chat_messages (mission_id);
CREATE INDEX idx_chat_messages_sender_id ON chat_messages (sender_id);
CREATE INDEX idx_notifications_user_id ON notifications (user_id);
-- -------- BUSINESS LOGIC INDEXES -----------------------------
CREATE INDEX idx_missions_status ON missions (status);
CREATE INDEX idx_missions_volunteer_status ON missions (volunteer_id, status);
CREATE INDEX idx_missions_sos_id_notnull ON missions (sos_request_id)
WHERE sos_request_id IS NOT NULL;
CREATE INDEX idx_missions_aid_id_notnull ON missions (aid_request_id)
WHERE aid_request_id IS NOT NULL;
CREATE INDEX idx_missions_created_at ON missions (created_at DESC);
CREATE INDEX idx_dispatch_pending ON dispatch_attempts (mission_id)
WHERE response = 'PENDING';
CREATE INDEX idx_hub_inv_low_stock ON hub_inventories (hub_id, current_quantity)
WHERE current_quantity <= low_stock_threshold;
CREATE INDEX idx_inventory_logs_ref ON inventory_logs (reference_type, reference_id);
CREATE INDEX idx_donations_sponsor_created ON donations (sponsor_id, created_at DESC);
CREATE INDEX idx_donations_hub_status ON donations (hub_id, status);
CREATE INDEX idx_chat_messages_mission_time ON chat_messages (mission_id, created_at ASC);
CREATE INDEX idx_notifications_user_unread ON notifications (user_id, created_at DESC)
WHERE is_read = FALSE;
CREATE INDEX idx_users_role ON users (role);
CREATE INDEX idx_users_active_verified ON users (is_active, is_verified)
WHERE is_active = TRUE
    AND is_verified = TRUE;
CREATE INDEX idx_sos_requests_requester_created ON sos_requests (requester_id, created_at DESC);
CREATE INDEX idx_aid_requests_requester_created ON aid_requests (requester_id, created_at DESC);
CREATE INDEX idx_item_categories_is_leaf ON item_categories (is_leaf)
WHERE is_leaf = TRUE;
-- NEW: Index cho OTP rate limiting
CREATE INDEX idx_users_otp_locked ON users (otp_locked_until)
WHERE otp_locked_until IS NOT NULL;
-- ============================================================
-- SECTION 7: TABLE COMMENTS
-- ============================================================
COMMENT ON TABLE users IS 'Tất cả actor: VICTIM, VOLUNTEER, SPONSOR, STAFF, ADMIN. ' 'OTP fields gộp vào đây với rate limiting (otp_attempts, otp_locked_until).';
COMMENT ON TABLE refresh_tokens IS 'JWT Refresh Token store — chỉ lưu SHA-256 hash.';
COMMENT ON TABLE volunteer_profiles IS 'Mở rộng users cho VOLUNTEER. Aggregate stats cho Priority Score.';
COMMENT ON TABLE sponsor_profiles IS 'Mở rộng users cho SPONSOR. Gamification với auto-update badge.';
COMMENT ON TABLE hubs IS 'Trạm trung chuyển — trung tâm chuỗi cung ứng.';
COMMENT ON TABLE hub_staff IS 'Phân công Staff vào Hub. Partial UNIQUE ngăn duplicate active assignment.';
COMMENT ON TABLE shelters IS 'Nơi trú ẩn khẩn cấp cho Public Map.';
COMMENT ON TABLE system_config IS 'Runtime config — Admin chỉnh, áp dụng ngay không restart.';
COMMENT ON TABLE item_categories IS 'Cây danh mục 2 cấp. Chỉ LEAF được dùng trong inventory/request/donation.';
COMMENT ON TABLE hub_accepted_categories IS 'Junction M:N: Hub nào nhận loại hàng nào.';
COMMENT ON TABLE hub_inventories IS 'Tồn kho per (Hub, Category). Có optimistic locking via version column.';
COMMENT ON TABLE inventory_logs IS 'Append-only audit trail. quantity_after là snapshot O(1) time-travel.';
COMMENT ON TABLE sos_requests IS 'Yêu cầu cứu hộ khẩn cấp. requester_id=NULL cho Guest.';
COMMENT ON TABLE aid_requests IS 'Yêu cầu tiếp tế nhu yếu phẩm. Có thể liên kết với SOS qua sos_request_id.';
COMMENT ON TABLE aid_request_items IS 'Line items của Aid Request.';
COMMENT ON TABLE donations IS 'Header lô hàng ký gửi. QR token được hash bảo mật.';
COMMENT ON TABLE donation_items IS 'Line items của Donation.';
COMMENT ON TABLE missions IS 'Đơn vị công việc của Volunteer. RESCUE từ SOS, DELIVERY từ Aid Request.';
COMMENT ON TABLE mission_items IS 'Track cụ thể items được giao trong mỗi DELIVERY mission.';
COMMENT ON TABLE dispatch_attempts IS 'Audit log từng lần dispatch gửi đến Volunteer.';
COMMENT ON TABLE chat_messages IS 'Chat scoped theo mission_id. TEXT XOR IMAGE.';
COMMENT ON TABLE ratings IS 'Đánh giá sau mission COMPLETED. UNIQUE per mission.';
COMMENT ON TABLE notifications IS 'In-app notification inbox.';
-- ============================================================
-- SECTION 8: SEED DATA — system_config
-- ============================================================
INSERT INTO system_config (key, value, description)
VALUES -- OTP
    (
        'otp.ttl_minutes',
        '10',
        'Thời gian sống OTP (phút).'
    ),
    (
        'otp.max_attempts',
        '5',
        'Số lần thử OTP sai tối đa trước khi khóa.'
    ),
    (
        'otp.lockout_minutes',
        '15',
        'Thời gian khóa sau khi vượt quá số lần thử.'
    ),
    -- Priority Score Weights
    (
        'dispatch.weight.distance',
        '0.40',
        'D-factor: khoảng cách Volunteer → nạn nhân.'
    ),
    (
        'dispatch.weight.rating',
        '0.20',
        'R-factor: avg_rating Volunteer (0–5).'
    ),
    (
        'dispatch.weight.tasks',
        '0.15',
        'T-factor: total_tasks_completed.'
    ),
    (
        'dispatch.weight.response_time',
        '0.15',
        'A-factor: avg_response_seconds (inverse).'
    ),
    (
        'dispatch.weight.area_experience',
        '0.10',
        'E-factor: kinh nghiệm khu vực.'
    ),
    -- Dispatch radius steps
    (
        'dispatch.radius.step1_km',
        '1',
        'Vòng tìm TNV bán kính 1 km đầu tiên.'
    ),
    (
        'dispatch.radius.step2_km',
        '3',
        'Mở rộng 3 km nếu bước 1 không đủ TNV.'
    ),
    (
        'dispatch.radius.step3_km',
        '5',
        'Mở rộng 5 km nếu bước 2 thất bại.'
    ),
    (
        'dispatch.radius.step4_km',
        '10',
        'Mở rộng 10 km — bước cuối.'
    ),
    -- Dispatch time windows
    (
        'dispatch.sos.window_seconds',
        '30',
        'RESCUE BROADCAST: TNV có 30 giây để accept.'
    ),
    (
        'dispatch.delivery.window_1',
        '15',
        'DELIVERY Round 1: TNV #1 có 15 giây exclusive.'
    ),
    (
        'dispatch.delivery.window_batch',
        '20',
        'DELIVERY Round 2+: Broadcast batch có 20 giây.'
    ) ON CONFLICT (key) DO NOTHING;
-- ============================================================
-- END OF SCHEMA v4.0
-- ============================================================