-- ============================================================================
-- AidBridge Database Schema v3.0
-- PostgreSQL 15+ · 22 tables · 16 ENUMs · ~70 indexes
--
-- Usage: psql -d aidbridge -f schema.sql
-- ============================================================================
-- ============================================================================
-- PART 1: EXTENSIONS
-- ============================================================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "citext";
-- ============================================================================
-- PART 2: ENUM TYPES (16 types)
-- ============================================================================
-- User roles
CREATE TYPE user_role AS ENUM (
    'VICTIM',
    'VOLUNTEER',
    'SPONSOR',
    'STAFF',
    'ADMIN'
);
-- Hub operational status
CREATE TYPE hub_status AS ENUM ('ACTIVE', 'INACTIVE', 'EMERGENCY');
-- Shelter operational status
CREATE TYPE shelter_status AS ENUM ('ACTIVE', 'INACTIVE', 'FULL');
-- Request urgency levels
CREATE TYPE urgency_level AS ENUM ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW');
-- SOS request status
CREATE TYPE sos_status AS ENUM (
    'PENDING',
    'DISPATCHING',
    'ASSIGNED',
    'IN_PROGRESS',
    'COMPLETED',
    'CANCELLED'
);
-- Aid request status
CREATE TYPE aid_status AS ENUM (
    'PENDING',
    'DISPATCHING',
    'ASSIGNED',
    'PICKED_UP',
    'IN_TRANSIT',
    'COMPLETED',
    'CANCELLED'
);
-- Donation status
CREATE TYPE donation_status AS ENUM (
    'REGISTERED',
    'QR_GENERATED',
    'RECEIVED',
    'REJECTED'
);
-- Mission types
CREATE TYPE mission_type AS ENUM ('RESCUE', 'DELIVERY');
-- Mission status
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
-- Dispatch response status
CREATE TYPE dispatch_response AS ENUM (
    'PENDING',
    'ACCEPTED',
    'REJECTED',
    'TIMEOUT'
);
-- Sponsor badge levels
CREATE TYPE badge_level AS ENUM ('BRONZE', 'SILVER', 'GOLD', 'PLATINUM');
-- Vehicle types for volunteers
CREATE TYPE vehicle_type AS ENUM ('MOTORBIKE', 'CAR', 'BICYCLE', 'WALKING');
-- OTP types
CREATE TYPE otp_type AS ENUM (
    'REGISTER',
    'FORGOT_PASSWORD',
    'VERIFY_PHONE'
);
-- Inventory change types
CREATE TYPE inventory_change_type AS ENUM (
    'DONATION_IN',
    'MISSION_OUT',
    'ADJUSTMENT',
    'INITIAL',
    'EXPIRED'
);
-- Reference types for inventory logs
CREATE TYPE inventory_reference_type AS ENUM ('DONATION', 'MISSION', 'MANUAL');
-- Chat message types
CREATE TYPE message_type AS ENUM ('TEXT', 'IMAGE');
-- Notification related types
CREATE TYPE notification_related_type AS ENUM (
    'MISSION',
    'DONATION',
    'SOS_REQUEST',
    'AID_REQUEST',
    'SYSTEM'
);
-- ============================================================================
-- PART 3: TABLES (22 tables)
-- ============================================================================
-- ----------------------------------------------------------------------------
-- GROUP: AUTH
-- ----------------------------------------------------------------------------
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    full_name VARCHAR(100) NOT NULL,
    email CITEXT UNIQUE,
    phone_number VARCHAR(15) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role user_role NOT NULL DEFAULT 'VICTIM',
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    fcm_token VARCHAR(255),
    avatar_url VARCHAR(500),
    otp_code VARCHAR(6),
    otp_type otp_type,
    otp_expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_users_contact CHECK (
        email IS NOT NULL
        OR phone_number IS NOT NULL
    )
);
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    device_info VARCHAR(255),
    expires_at TIMESTAMPTZ NOT NULL,
    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
-- ----------------------------------------------------------------------------
-- GROUP: PROFILES
-- ----------------------------------------------------------------------------
CREATE TABLE volunteer_profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    is_online BOOLEAN NOT NULL DEFAULT FALSE,
    current_lat DECIMAL(9, 6),
    current_lng DECIMAL(9, 6),
    vehicle_type vehicle_type,
    total_tasks_completed INTEGER NOT NULL DEFAULT 0,
    avg_rating DECIMAL(3, 2) DEFAULT 0.00,
    avg_response_seconds INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_volunteer_rating CHECK (
        avg_rating >= 0
        AND avg_rating <= 5
    ),
    CONSTRAINT chk_volunteer_tasks CHECK (total_tasks_completed >= 0)
);
CREATE TABLE sponsor_profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    total_points INTEGER NOT NULL DEFAULT 0,
    total_items_donated INTEGER NOT NULL DEFAULT 0,
    donation_count INTEGER NOT NULL DEFAULT 0,
    badge_level badge_level NOT NULL DEFAULT 'BRONZE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_sponsor_points CHECK (total_points >= 0),
    CONSTRAINT chk_sponsor_items CHECK (total_items_donated >= 0),
    CONSTRAINT chk_sponsor_count CHECK (donation_count >= 0)
);
-- ----------------------------------------------------------------------------
-- GROUP: INFRASTRUCTURE
-- ----------------------------------------------------------------------------
CREATE TABLE hubs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    address VARCHAR(255) NOT NULL,
    lat DECIMAL(9, 6) NOT NULL,
    lng DECIMAL(9, 6) NOT NULL,
    status hub_status NOT NULL DEFAULT 'ACTIVE',
    contact_phone VARCHAR(15),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TABLE hub_staff (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    hub_id UUID NOT NULL REFERENCES hubs(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    unassigned_at TIMESTAMPTZ
);
CREATE TABLE shelters (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    address VARCHAR(255) NOT NULL,
    lat DECIMAL(9, 6) NOT NULL,
    lng DECIMAL(9, 6) NOT NULL,
    current_capacity INTEGER NOT NULL DEFAULT 0,
    max_capacity INTEGER NOT NULL,
    has_electricity BOOLEAN NOT NULL DEFAULT FALSE,
    has_clean_water BOOLEAN NOT NULL DEFAULT FALSE,
    status shelter_status NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_shelters_capacity CHECK (
        current_capacity >= 0
        AND current_capacity <= max_capacity
    )
);
CREATE TABLE system_config (
    key VARCHAR(100) PRIMARY KEY,
    value TEXT NOT NULL,
    description VARCHAR(255),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
-- ----------------------------------------------------------------------------
-- GROUP: CATALOG & INVENTORY
-- ----------------------------------------------------------------------------
CREATE TABLE item_categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    parent_id UUID REFERENCES item_categories(id) ON DELETE
    SET NULL,
        name VARCHAR(100) NOT NULL,
        name_vi VARCHAR(100) NOT NULL,
        unit VARCHAR(50) NOT NULL,
        is_leaf BOOLEAN NOT NULL DEFAULT TRUE,
        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TABLE hub_accepted_categories (
    hub_id UUID NOT NULL REFERENCES hubs(id) ON DELETE CASCADE,
    item_category_id UUID NOT NULL REFERENCES item_categories(id) ON DELETE CASCADE,
    PRIMARY KEY (hub_id, item_category_id)
);
CREATE TABLE hub_inventories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    hub_id UUID NOT NULL REFERENCES hubs(id) ON DELETE CASCADE,
    item_category_id UUID NOT NULL REFERENCES item_categories(id) ON DELETE CASCADE,
    current_quantity INTEGER NOT NULL DEFAULT 0,
    low_stock_threshold INTEGER NOT NULL DEFAULT 10,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_inventory_quantity CHECK (current_quantity >= 0),
    CONSTRAINT uq_hub_item UNIQUE (hub_id, item_category_id)
);
CREATE TABLE inventory_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    hub_inventory_id UUID NOT NULL REFERENCES hub_inventories(id) ON DELETE CASCADE,
    change_type inventory_change_type NOT NULL,
    quantity_delta INTEGER NOT NULL,
    reference_type inventory_reference_type,
    reference_id UUID,
    performed_by UUID REFERENCES users(id) ON DELETE
    SET NULL,
        quantity_after INTEGER NOT NULL,
        notes TEXT,
        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
        CONSTRAINT chk_inventory_delta CHECK (quantity_delta != 0)
);
-- ----------------------------------------------------------------------------
-- GROUP: REQUESTS
-- ----------------------------------------------------------------------------
CREATE TABLE sos_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    requester_id UUID REFERENCES users(id) ON DELETE SET NULL,
    lat DECIMAL(9, 6) NOT NULL,
    lng DECIMAL(9, 6) NOT NULL,
    address VARCHAR(500),
    description TEXT,
    people_count INTEGER NOT NULL DEFAULT 1,
    urgency_level urgency_level NOT NULL DEFAULT 'MEDIUM',
    status sos_status NOT NULL DEFAULT 'PENDING',
    image_url VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_sos_people CHECK (people_count > 0)
);
CREATE TABLE aid_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    requester_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    lat DECIMAL(9, 6) NOT NULL,
    lng DECIMAL(9, 6) NOT NULL,
    address VARCHAR(255) NOT NULL,
    adults_count INTEGER NOT NULL DEFAULT 0,
    elderly_count INTEGER NOT NULL DEFAULT 0,
    children_count INTEGER NOT NULL DEFAULT 0,
    notes TEXT,
    urgency_level urgency_level NOT NULL DEFAULT 'MEDIUM',
    status aid_status NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_aid_people CHECK (
        adults_count + elderly_count + children_count > 0
    )
);
CREATE TABLE aid_request_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    aid_request_id UUID NOT NULL REFERENCES aid_requests(id) ON DELETE CASCADE,
    item_category_id UUID NOT NULL REFERENCES item_categories(id) ON DELETE CASCADE,
    quantity INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_aid_item_qty CHECK (quantity > 0)
);
-- ----------------------------------------------------------------------------
-- GROUP: DONATIONS
-- ----------------------------------------------------------------------------
CREATE TABLE donations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    sponsor_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    hub_id UUID NOT NULL REFERENCES hubs(id) ON DELETE CASCADE,
    estimated_delivery_at DATE,
    qr_code_token VARCHAR(100) UNIQUE,
    status donation_status NOT NULL DEFAULT 'REGISTERED',
    received_by UUID REFERENCES users(id) ON DELETE
    SET NULL,
        received_at TIMESTAMPTZ,
        rejection_reason TEXT,
        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
        updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TABLE donation_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    donation_id UUID NOT NULL REFERENCES donations(id) ON DELETE CASCADE,
    item_category_id UUID NOT NULL REFERENCES item_categories(id) ON DELETE CASCADE,
    quantity INTEGER NOT NULL,
    expiry_date DATE,
    condition_notes TEXT,
    image_url VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_donation_item_qty CHECK (quantity > 0)
);
-- ----------------------------------------------------------------------------
-- GROUP: MISSIONS
-- ----------------------------------------------------------------------------
CREATE TABLE missions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    mission_type mission_type NOT NULL,
    sos_request_id UUID REFERENCES sos_requests(id) ON DELETE SET NULL,
    aid_request_id UUID REFERENCES aid_requests(id) ON DELETE SET NULL,
    help_request_id UUID REFERENCES help_requests(id) ON DELETE SET NULL,
    volunteer_id UUID REFERENCES users(id) ON DELETE SET NULL,
    hub_id UUID REFERENCES hubs(id) ON DELETE SET NULL,
    status mission_status NOT NULL DEFAULT 'PENDING',
    qr_code_token VARCHAR(100) UNIQUE,
    priority_score DECIMAL(5, 2) DEFAULT 0.00,
    victim_lat DECIMAL(9, 6),
    victim_lng DECIMAL(9, 6),
    cancellation_reason TEXT,
    image_url VARCHAR(500),
    comment TEXT,
    started_at TIMESTAMPTZ,
    accepted_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    picked_up_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    confirmation_image_url VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_missions_type_rescue CHECK (
        mission_type != 'RESCUE'
        OR (
            sos_request_id IS NOT NULL
            AND aid_request_id IS NULL
        )
    ),
    CONSTRAINT chk_missions_type_delivery CHECK (
        mission_type != 'DELIVERY'
        OR (
            aid_request_id IS NOT NULL
            AND sos_request_id IS NULL
            AND hub_id IS NOT NULL
        )
    )
);
CREATE TABLE dispatch_attempts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    mission_id UUID NOT NULL REFERENCES missions(id) ON DELETE CASCADE,
    volunteer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    dispatch_type VARCHAR(20) NOT NULL DEFAULT 'BROADCAST',
    batch_number INTEGER NOT NULL DEFAULT 1,
    radius_km DECIMAL(5, 2),
    priority_score DECIMAL(5, 2),
    sent_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    response dispatch_response NOT NULL DEFAULT 'PENDING',
    responded_at TIMESTAMPTZ
);
-- ----------------------------------------------------------------------------
-- GROUP: COMMUNICATION
-- ----------------------------------------------------------------------------
CREATE TABLE chat_messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    mission_id UUID NOT NULL REFERENCES missions(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message_type message_type NOT NULL,
    message_text TEXT,
    image_url VARCHAR(500),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_chat_content CHECK (
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
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    mission_id UUID NOT NULL UNIQUE REFERENCES missions(id) ON DELETE CASCADE,
    rater_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    ratee_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    score INTEGER NOT NULL,
    comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_rating_score CHECK (
        score >= 1
        AND score <= 5
    ),
    CONSTRAINT chk_rating_self CHECK (rater_id != ratee_id)
);
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(100) NOT NULL,
    body TEXT NOT NULL,
    related_type notification_related_type,
    related_id UUID,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
-- ============================================================================
-- PART 4: INDEXES (~70 indexes)
-- ============================================================================
-- AUTH
CREATE INDEX idx_users_email ON users(email)
WHERE email IS NOT NULL;
CREATE INDEX idx_users_phone ON users(phone_number)
WHERE phone_number IS NOT NULL;
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_active ON users(is_active)
WHERE is_active = TRUE;
CREATE INDEX idx_users_otp_expires ON users(otp_expires_at)
WHERE otp_code IS NOT NULL;
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens(expires_at)
WHERE is_revoked = FALSE;
-- PROFILES
CREATE INDEX idx_volunteer_user ON volunteer_profiles(user_id);
CREATE INDEX idx_volunteer_online ON volunteer_profiles(is_online)
WHERE is_online = TRUE;
CREATE INDEX idx_volunteer_location ON volunteer_profiles(current_lat, current_lng)
WHERE is_online = TRUE;
CREATE INDEX idx_volunteer_rating ON volunteer_profiles(avg_rating DESC);
CREATE INDEX idx_sponsor_user ON sponsor_profiles(user_id);
CREATE INDEX idx_sponsor_points ON sponsor_profiles(total_points DESC);
CREATE INDEX idx_sponsor_badge ON sponsor_profiles(badge_level);
-- INFRASTRUCTURE
CREATE INDEX idx_hubs_status ON hubs(status)
WHERE status = 'ACTIVE';
CREATE INDEX idx_hubs_location ON hubs(lat, lng);
CREATE INDEX idx_hub_staff_hub ON hub_staff(hub_id);
CREATE INDEX idx_hub_staff_user ON hub_staff(user_id);
CREATE UNIQUE INDEX idx_hub_staff_active ON hub_staff(hub_id, user_id)
WHERE unassigned_at IS NULL;
CREATE INDEX idx_shelters_status ON shelters(status);
CREATE INDEX idx_shelters_location ON shelters(lat, lng);
CREATE INDEX idx_shelters_capacity ON shelters(current_capacity, max_capacity)
WHERE status = 'ACTIVE';
-- CATALOG & INVENTORY
CREATE INDEX idx_item_categories_parent ON item_categories(parent_id);
CREATE INDEX idx_item_categories_leaf ON item_categories(is_leaf)
WHERE is_leaf = TRUE;
CREATE INDEX idx_hub_inventories_hub ON hub_inventories(hub_id);
CREATE INDEX idx_hub_inventories_category ON hub_inventories(item_category_id);
CREATE INDEX idx_hub_inventories_low_stock ON hub_inventories(hub_id, current_quantity)
WHERE current_quantity <= low_stock_threshold;
CREATE INDEX idx_inventory_logs_hub_inv ON inventory_logs(hub_inventory_id);
CREATE INDEX idx_inventory_logs_created ON inventory_logs(created_at DESC);
CREATE INDEX idx_inventory_logs_reference ON inventory_logs(reference_type, reference_id);
-- REQUESTS
CREATE INDEX idx_sos_requester ON sos_requests(requester_id);
CREATE INDEX idx_sos_status ON sos_requests(status);
CREATE INDEX idx_sos_location ON sos_requests(lat, lng);
CREATE INDEX idx_sos_created ON sos_requests(created_at DESC);
CREATE INDEX idx_sos_pending ON sos_requests(urgency_level, created_at)
WHERE status = 'PENDING';
CREATE INDEX idx_aid_requester ON aid_requests(requester_id);
CREATE INDEX idx_aid_status ON aid_requests(status);
CREATE INDEX idx_aid_location ON aid_requests(lat, lng);
CREATE INDEX idx_aid_created ON aid_requests(created_at DESC);
CREATE INDEX idx_aid_pending ON aid_requests(urgency_level, created_at)
WHERE status = 'PENDING';
CREATE INDEX idx_aid_items_request ON aid_request_items(aid_request_id);
CREATE INDEX idx_aid_items_category ON aid_request_items(item_category_id);
-- DONATIONS
CREATE INDEX idx_donations_sponsor ON donations(sponsor_id);
CREATE INDEX idx_donations_hub ON donations(hub_id);
CREATE INDEX idx_donations_status ON donations(status);
CREATE INDEX idx_donations_qr ON donations(qr_code_token)
WHERE qr_code_token IS NOT NULL;
CREATE INDEX idx_donations_created ON donations(created_at DESC);
CREATE INDEX idx_donation_items_donation ON donation_items(donation_id);
CREATE INDEX idx_donation_items_category ON donation_items(item_category_id);
CREATE INDEX idx_donation_items_expiry ON donation_items(expiry_date)
WHERE expiry_date IS NOT NULL;
-- MISSIONS
CREATE INDEX idx_missions_type ON missions(mission_type);
CREATE INDEX idx_missions_sos ON missions(sos_request_id)
WHERE sos_request_id IS NOT NULL;
CREATE INDEX idx_missions_aid ON missions(aid_request_id)
WHERE aid_request_id IS NOT NULL;
CREATE INDEX idx_missions_help ON missions(help_request_id)
WHERE help_request_id IS NOT NULL;
CREATE INDEX idx_missions_volunteer ON missions(volunteer_id)
WHERE volunteer_id IS NOT NULL;
CREATE INDEX idx_missions_hub ON missions(hub_id)
WHERE hub_id IS NOT NULL;
CREATE INDEX idx_missions_status ON missions(status);
CREATE INDEX idx_missions_qr ON missions(qr_code_token)
WHERE qr_code_token IS NOT NULL;
CREATE INDEX idx_missions_priority ON missions(priority_score DESC)
WHERE status = 'PENDING';
CREATE INDEX idx_missions_created ON missions(created_at DESC);
CREATE INDEX idx_dispatch_mission ON dispatch_attempts(mission_id);
CREATE INDEX idx_dispatch_volunteer ON dispatch_attempts(volunteer_id);
CREATE INDEX idx_dispatch_pending ON dispatch_attempts(response, sent_at)
WHERE response = 'PENDING';
-- COMMUNICATION
CREATE INDEX idx_chat_mission ON chat_messages(mission_id);
CREATE INDEX idx_chat_sender ON chat_messages(sender_id);
CREATE INDEX idx_chat_created ON chat_messages(mission_id, created_at DESC);
CREATE INDEX idx_chat_unread ON chat_messages(mission_id, is_read)
WHERE is_read = FALSE;
CREATE INDEX idx_ratings_rater ON ratings(rater_id);
CREATE INDEX idx_ratings_ratee ON ratings(ratee_id);
CREATE INDEX idx_ratings_score ON ratings(ratee_id, score);
CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_unread ON notifications(user_id, created_at DESC)
WHERE is_read = FALSE;
CREATE INDEX idx_notifications_related ON notifications(related_type, related_id);
-- ============================================================================
-- PART 5: TRIGGER FUNCTIONS
-- ============================================================================
-- Auto-update updated_at timestamp
CREATE OR REPLACE FUNCTION trigger_set_updated_at() RETURNS TRIGGER AS $$ BEGIN NEW.updated_at = NOW();
RETURN NEW;
END;
$$ LANGUAGE plpgsql;
-- Update volunteer avg_rating after new rating
CREATE OR REPLACE FUNCTION trigger_update_volunteer_rating() RETURNS TRIGGER AS $$ BEGIN
UPDATE volunteer_profiles
SET avg_rating = (
        SELECT COALESCE(AVG(r.score), 0)
        FROM ratings r
            JOIN missions m ON r.mission_id = m.id
        WHERE m.volunteer_id = (
                SELECT user_id
                FROM volunteer_profiles
                WHERE user_id = NEW.ratee_id
            )
    ),
    updated_at = NOW()
WHERE user_id = NEW.ratee_id;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;
-- Update sponsor stats after donation received
CREATE OR REPLACE FUNCTION trigger_update_sponsor_stats() RETURNS TRIGGER AS $$
DECLARE v_total_items INTEGER;
BEGIN IF NEW.status = 'RECEIVED'
AND (
    OLD.status IS NULL
    OR OLD.status != 'RECEIVED'
) THEN
SELECT COALESCE(SUM(quantity), 0) INTO v_total_items
FROM donation_items
WHERE donation_id = NEW.id;
UPDATE sponsor_profiles
SET donation_count = donation_count + 1,
    total_items_donated = total_items_donated + v_total_items,
    total_points = total_points + (v_total_items * 10),
    badge_level = CASE
        WHEN total_points + (v_total_items * 10) >= 10000 THEN 'PLATINUM'
        WHEN total_points + (v_total_items * 10) >= 5000 THEN 'GOLD'
        WHEN total_points + (v_total_items * 10) >= 1000 THEN 'SILVER'
        ELSE 'BRONZE'
    END,
    updated_at = NOW()
WHERE user_id = NEW.sponsor_id;
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;
-- Update volunteer stats after mission completed
CREATE OR REPLACE FUNCTION trigger_update_volunteer_stats() RETURNS TRIGGER AS $$ BEGIN IF NEW.status = 'COMPLETED'
    AND (
        OLD.status IS NULL
        OR OLD.status != 'COMPLETED'
    ) THEN
UPDATE volunteer_profiles
SET total_tasks_completed = total_tasks_completed + 1,
    updated_at = NOW()
WHERE user_id = NEW.volunteer_id;
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;
-- Update hub inventory after donation received
CREATE OR REPLACE FUNCTION trigger_update_inventory_on_donation() RETURNS TRIGGER AS $$ BEGIN IF NEW.status = 'RECEIVED'
    AND (
        OLD.status IS NULL
        OR OLD.status != 'RECEIVED'
    ) THEN
INSERT INTO hub_inventories (hub_id, item_category_id, current_quantity)
SELECT NEW.hub_id,
    di.item_category_id,
    di.quantity
FROM donation_items di
WHERE di.donation_id = NEW.id ON CONFLICT (hub_id, item_category_id) DO
UPDATE
SET current_quantity = hub_inventories.current_quantity + EXCLUDED.current_quantity,
    updated_at = NOW();
INSERT INTO inventory_logs (
        hub_inventory_id,
        change_type,
        quantity_delta,
        reference_type,
        reference_id,
        performed_by,
        quantity_after
    )
SELECT hi.id,
    'DONATION_IN',
    di.quantity,
    'DONATION',
    NEW.id,
    NEW.received_by,
    hi.current_quantity
FROM donation_items di
    JOIN hub_inventories hi ON hi.hub_id = NEW.hub_id
    AND hi.item_category_id = di.item_category_id
WHERE di.donation_id = NEW.id;
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;
-- ============================================================================
-- PART 6: TRIGGERS
-- ============================================================================
-- Auto updated_at triggers
CREATE TRIGGER set_updated_at_users BEFORE
UPDATE ON users FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();
CREATE TRIGGER set_updated_at_volunteer_profiles BEFORE
UPDATE ON volunteer_profiles FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();
CREATE TRIGGER set_updated_at_sponsor_profiles BEFORE
UPDATE ON sponsor_profiles FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();
CREATE TRIGGER set_updated_at_hubs BEFORE
UPDATE ON hubs FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();
CREATE TRIGGER set_updated_at_shelters BEFORE
UPDATE ON shelters FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();
CREATE TRIGGER set_updated_at_hub_inventories BEFORE
UPDATE ON hub_inventories FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();
CREATE TRIGGER set_updated_at_sos_requests BEFORE
UPDATE ON sos_requests FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();
CREATE TRIGGER set_updated_at_aid_requests BEFORE
UPDATE ON aid_requests FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();
CREATE TRIGGER set_updated_at_donations BEFORE
UPDATE ON donations FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();
CREATE TRIGGER set_updated_at_missions BEFORE
UPDATE ON missions FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();
-- Business logic triggers
CREATE TRIGGER update_volunteer_rating
AFTER
INSERT ON ratings FOR EACH ROW EXECUTE FUNCTION trigger_update_volunteer_rating();
CREATE TRIGGER update_sponsor_stats
AFTER
UPDATE ON donations FOR EACH ROW EXECUTE FUNCTION trigger_update_sponsor_stats();
CREATE TRIGGER update_volunteer_stats
AFTER
UPDATE ON missions FOR EACH ROW EXECUTE FUNCTION trigger_update_volunteer_stats();
CREATE TRIGGER update_inventory_on_donation
AFTER
UPDATE ON donations FOR EACH ROW EXECUTE FUNCTION trigger_update_inventory_on_donation();
-- ============================================================================
-- PART 7: SEED DATA
-- ============================================================================
-- System configuration
INSERT INTO system_config (key, value, description)
VALUES (
        'dispatch_radius_initial_km',
        '5',
        'Initial radius (km) for volunteer dispatch'
    ),
    (
        'dispatch_radius_max_km',
        '20',
        'Maximum radius (km) for volunteer dispatch'
    ),
    (
        'dispatch_timeout_seconds',
        '120',
        'Timeout (seconds) for volunteer to respond'
    ),
    (
        'dispatch_batch_size',
        '5',
        'Number of volunteers per dispatch batch'
    ),
    (
        'mission_auto_cancel_hours',
        '24',
        'Hours before unassigned mission auto-cancels'
    ),
    (
        'otp_expiry_minutes',
        '5',
        'OTP expiration time in minutes'
    ),
    (
        'jwt_access_token_hours',
        '1',
        'JWT access token expiration in hours'
    ),
    (
        'jwt_refresh_token_days',
        '30',
        'JWT refresh token expiration in days'
    ),
    (
        'donation_points_per_item',
        '10',
        'Points awarded per donated item'
    ),
    (
        'badge_silver_threshold',
        '1000',
        'Points required for Silver badge'
    ),
    (
        'badge_gold_threshold',
        '5000',
        'Points required for Gold badge'
    ),
    (
        'badge_platinum_threshold',
        '10000',
        'Points required for Platinum badge'
    );
-- Item categories (root)
INSERT INTO item_categories (id, parent_id, name, name_vi, unit, is_leaf)
VALUES (
        '00000000-0000-0000-0000-000000000001',
        NULL,
        'Food & Water',
        'Thực phẩm & Nước',
        'unit',
        FALSE
    ),
    (
        '00000000-0000-0000-0000-000000000002',
        NULL,
        'Medical Supplies',
        'Vật tư y tế',
        'unit',
        FALSE
    ),
    (
        '00000000-0000-0000-0000-000000000003',
        NULL,
        'Clothing',
        'Quần áo',
        'piece',
        FALSE
    ),
    (
        '00000000-0000-0000-0000-000000000004',
        NULL,
        'Shelter Supplies',
        'Vật dụng trú ẩn',
        'unit',
        FALSE
    ),
    (
        '00000000-0000-0000-0000-000000000005',
        NULL,
        'Hygiene',
        'Vệ sinh',
        'unit',
        FALSE
    );
-- Food & Water
INSERT INTO item_categories (id, parent_id, name, name_vi, unit, is_leaf)
VALUES (
        '00000000-0000-0000-0001-000000000001',
        '00000000-0000-0000-0000-000000000001',
        'Drinking Water',
        'Nước uống',
        'liter',
        TRUE
    ),
    (
        '00000000-0000-0000-0001-000000000002',
        '00000000-0000-0000-0000-000000000001',
        'Rice',
        'Gạo',
        'kg',
        TRUE
    ),
    (
        '00000000-0000-0000-0001-000000000003',
        '00000000-0000-0000-0000-000000000001',
        'Instant Noodles',
        'Mì gói',
        'pack',
        TRUE
    ),
    (
        '00000000-0000-0000-0001-000000000004',
        '00000000-0000-0000-0000-000000000001',
        'Canned Food',
        'Đồ hộp',
        'can',
        TRUE
    ),
    (
        '00000000-0000-0000-0001-000000000005',
        '00000000-0000-0000-0000-000000000001',
        'Baby Formula',
        'Sữa bột trẻ em',
        'box',
        TRUE
    ),
    (
        '00000000-0000-0000-0001-000000000006',
        '00000000-0000-0000-0000-000000000001',
        'Cooking Oil',
        'Dầu ăn',
        'liter',
        TRUE
    ),
    (
        '00000000-0000-0000-0001-000000000007',
        '00000000-0000-0000-0000-000000000001',
        'Salt/Sugar',
        'Muối/Đường',
        'kg',
        TRUE
    ),
    (
        '00000000-0000-0000-0001-000000000008',
        '00000000-0000-0000-0000-000000000001',
        'Biscuits',
        'Bánh quy',
        'pack',
        TRUE
    );
-- Medical Supplies
INSERT INTO item_categories (id, parent_id, name, name_vi, unit, is_leaf)
VALUES (
        '00000000-0000-0000-0002-000000000001',
        '00000000-0000-0000-0000-000000000002',
        'First Aid Kit',
        'Bộ sơ cứu',
        'kit',
        TRUE
    ),
    (
        '00000000-0000-0000-0002-000000000002',
        '00000000-0000-0000-0000-000000000002',
        'Pain Relievers',
        'Thuốc giảm đau',
        'box',
        TRUE
    ),
    (
        '00000000-0000-0000-0002-000000000003',
        '00000000-0000-0000-0000-000000000002',
        'Bandages',
        'Băng gạc',
        'pack',
        TRUE
    ),
    (
        '00000000-0000-0000-0002-000000000004',
        '00000000-0000-0000-0000-000000000002',
        'Antiseptic',
        'Thuốc sát trùng',
        'bottle',
        TRUE
    ),
    (
        '00000000-0000-0000-0002-000000000005',
        '00000000-0000-0000-0000-000000000002',
        'Oral Rehydration',
        'Oresol',
        'pack',
        TRUE
    ),
    (
        '00000000-0000-0000-0002-000000000006',
        '00000000-0000-0000-0000-000000000002',
        'Face Masks',
        'Khẩu trang',
        'pack',
        TRUE
    );
-- Clothing
INSERT INTO item_categories (id, parent_id, name, name_vi, unit, is_leaf)
VALUES (
        '00000000-0000-0000-0003-000000000001',
        '00000000-0000-0000-0000-000000000003',
        'T-Shirts',
        'Áo thun',
        'piece',
        TRUE
    ),
    (
        '00000000-0000-0000-0003-000000000002',
        '00000000-0000-0000-0000-000000000003',
        'Pants',
        'Quần',
        'piece',
        TRUE
    ),
    (
        '00000000-0000-0000-0003-000000000003',
        '00000000-0000-0000-0000-000000000003',
        'Underwear',
        'Đồ lót',
        'piece',
        TRUE
    ),
    (
        '00000000-0000-0000-0003-000000000004',
        '00000000-0000-0000-0000-000000000003',
        'Raincoats',
        'Áo mưa',
        'piece',
        TRUE
    ),
    (
        '00000000-0000-0000-0003-000000000005',
        '00000000-0000-0000-0000-000000000003',
        'Blankets',
        'Chăn',
        'piece',
        TRUE
    ),
    (
        '00000000-0000-0000-0003-000000000006',
        '00000000-0000-0000-0000-000000000003',
        'Baby Clothes',
        'Quần áo trẻ em',
        'piece',
        TRUE
    );
-- Shelter Supplies
INSERT INTO item_categories (id, parent_id, name, name_vi, unit, is_leaf)
VALUES (
        '00000000-0000-0000-0004-000000000001',
        '00000000-0000-0000-0000-000000000004',
        'Tarpaulin',
        'Bạt che',
        'piece',
        TRUE
    ),
    (
        '00000000-0000-0000-0004-000000000002',
        '00000000-0000-0000-0000-000000000004',
        'Sleeping Mats',
        'Chiếu',
        'piece',
        TRUE
    ),
    (
        '00000000-0000-0000-0004-000000000003',
        '00000000-0000-0000-0000-000000000004',
        'Flashlights',
        'Đèn pin',
        'piece',
        TRUE
    ),
    (
        '00000000-0000-0000-0004-000000000004',
        '00000000-0000-0000-0000-000000000004',
        'Batteries',
        'Pin',
        'pack',
        TRUE
    ),
    (
        '00000000-0000-0000-0004-000000000005',
        '00000000-0000-0000-0000-000000000004',
        'Ropes',
        'Dây thừng',
        'meter',
        TRUE
    ),
    (
        '00000000-0000-0000-0004-000000000006',
        '00000000-0000-0000-0000-000000000004',
        'Candles',
        'Nến',
        'pack',
        TRUE
    );
-- Hygiene
INSERT INTO item_categories (id, parent_id, name, name_vi, unit, is_leaf)
VALUES (
        '00000000-0000-0000-0005-000000000001',
        '00000000-0000-0000-0000-000000000005',
        'Soap',
        'Xà phòng',
        'bar',
        TRUE
    ),
    (
        '00000000-0000-0000-0005-000000000002',
        '00000000-0000-0000-0000-000000000005',
        'Toothbrush/Toothpaste',
        'Bàn chải/Kem đánh răng',
        'set',
        TRUE
    ),
    (
        '00000000-0000-0000-0005-000000000003',
        '00000000-0000-0000-0000-000000000005',
        'Sanitary Pads',
        'Băng vệ sinh',
        'pack',
        TRUE
    ),
    (
        '00000000-0000-0000-0005-000000000004',
        '00000000-0000-0000-0000-000000000005',
        'Diapers',
        'Tã/Bỉm',
        'pack',
        TRUE
    ),
    (
        '00000000-0000-0000-0005-000000000005',
        '00000000-0000-0000-0000-000000000005',
        'Toilet Paper',
        'Giấy vệ sinh',
        'roll',
        TRUE
    ),
    (
        '00000000-0000-0000-0005-000000000006',
        '00000000-0000-0000-0000-000000000005',
        'Hand Sanitizer',
        'Nước rửa tay',
        'bottle',
        TRUE
    );
-- Default admin (password: Admin@123 - CHANGE IMMEDIATELY)
INSERT INTO users (
        id,
        full_name,
        email,
        phone_number,
        password_hash,
        role,
        is_verified,
        is_active
    )
VALUES (
        '00000000-0000-0000-0000-000000000000',
        'System Administrator',
        'admin@aidbridge.vn',
        '0900000000',
        '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4lOQQZ9ZP.EXAMPLE',
        'ADMIN',
        TRUE,
        TRUE
    );
-- ============================================================================
-- END OF MIGRATION
-- ============================================================================