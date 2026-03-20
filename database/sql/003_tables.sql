-- ============================================================================
-- AidBridge Database Schema v3.0
-- File: 003_tables.sql
-- Description: Table definitions (22 tables)
-- ============================================================================
-- ============================================================================
-- GROUP: AUTH
-- ============================================================================
-- Users table - central authentication
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
    -- OTP fields (merged from otp_verifications)
    otp_code VARCHAR(6),
    otp_type otp_type,
    otp_expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    -- At least email or phone must be provided
    CONSTRAINT chk_users_contact CHECK (
        email IS NOT NULL
        OR phone_number IS NOT NULL
    )
);
-- Refresh tokens for JWT authentication
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    device_info VARCHAR(255),
    expires_at TIMESTAMPTZ NOT NULL,
    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
-- ============================================================================
-- GROUP: PROFILES
-- ============================================================================
-- Volunteer profiles - extends users with volunteer-specific data
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
-- Sponsor profiles - extends users with sponsor-specific data
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
-- ============================================================================
-- GROUP: INFRASTRUCTURE
-- ============================================================================
-- Hubs - distribution centers
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
-- Hub staff assignments
CREATE TABLE hub_staff (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    hub_id UUID NOT NULL REFERENCES hubs(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    unassigned_at TIMESTAMPTZ
);
-- Shelters - temporary housing locations
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
-- System configuration key-value store
CREATE TABLE system_config (
    key VARCHAR(100) PRIMARY KEY,
    value TEXT NOT NULL,
    description VARCHAR(255),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
-- ============================================================================
-- GROUP: CATALOG & INVENTORY
-- ============================================================================
-- Item categories - hierarchical catalog
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
-- Hub accepted categories - which items each hub accepts
CREATE TABLE hub_accepted_categories (
    hub_id UUID NOT NULL REFERENCES hubs(id) ON DELETE CASCADE,
    item_category_id UUID NOT NULL REFERENCES item_categories(id) ON DELETE CASCADE,
    PRIMARY KEY (hub_id, item_category_id)
);
-- Hub inventories - current stock at each hub
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
-- Inventory logs - audit trail for inventory changes
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
-- ============================================================================
-- GROUP: REQUESTS
-- ============================================================================
-- SOS requests - emergency rescue requests
CREATE TABLE sos_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    requester_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
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
-- Aid requests - supply/delivery requests
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
-- Aid request items - items requested in aid request
CREATE TABLE aid_request_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    aid_request_id UUID NOT NULL REFERENCES aid_requests(id) ON DELETE CASCADE,
    item_category_id UUID NOT NULL REFERENCES item_categories(id) ON DELETE CASCADE,
    quantity INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_aid_item_qty CHECK (quantity > 0)
);
-- ============================================================================
-- GROUP: DONATIONS
-- ============================================================================
-- Donations - sponsor donations to hubs
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
-- Donation items - items in a donation
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
-- ============================================================================
-- GROUP: MISSIONS
-- ============================================================================
-- Missions - rescue or delivery tasks assigned to volunteers
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
-- Dispatch attempts - tracking volunteer dispatch attempts
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
-- ============================================================================
-- GROUP: COMMUNICATION
-- ============================================================================
-- Chat messages - mission-related messaging
CREATE TABLE chat_messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    mission_id UUID NOT NULL REFERENCES missions(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message_type message_type NOT NULL,
    message_text TEXT,
    image_url VARCHAR(500),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    -- TEXT must have message_text, IMAGE must have image_url (XOR logic)
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
-- Ratings - post-mission feedback
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
-- Notifications - push notification records
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