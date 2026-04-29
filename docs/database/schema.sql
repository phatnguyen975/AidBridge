CREATE TYPE user_role AS ENUM (
    'VICTIM',
    'VOLUNTEER',
    'SPONSOR',
    'STAFF',
    'ADMIN'
);
CREATE TYPE hub_status AS ENUM ('ACTIVE', 'INACTIVE', 'EMERGENCY');
CREATE TYPE urgency_level AS ENUM ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW');
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
CREATE TYPE badge_level AS ENUM ('BRONZE', 'SILVER', 'GOLD', 'PLATINUM');
-- WARNING: This schema is for context only and is not meant to be run.
-- Table order and constraints may not be valid for execution.
-- WARNING: This schema is for context only and is not meant to be run.
-- Table order and constraints may not be valid for execution.
CREATE TABLE public.aid_request_items (
    id uuid NOT NULL DEFAULT uuid_generate_v4(),
    aid_request_id uuid NOT NULL,
    item_category_id uuid NOT NULL,
    CONSTRAINT aid_request_items_pkey PRIMARY KEY (id),
    CONSTRAINT aid_request_items_aid_request_id_fkey FOREIGN KEY (aid_request_id) REFERENCES public.aid_requests(id),
    CONSTRAINT aid_request_items_item_category_id_fkey FOREIGN KEY (item_category_id) REFERENCES public.item_categories(id)
);
CREATE TABLE public.aid_requests (
    id uuid NOT NULL DEFAULT uuid_generate_v4(),
    requester_id uuid NOT NULL,
    status character varying NOT NULL DEFAULT 'PENDING'::aid_status,
    address character varying,
    description text,
    number_elderly integer NOT NULL DEFAULT 0,
    number_adult integer NOT NULL DEFAULT 0,
    number_children integer NOT NULL DEFAULT 0,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    location USER - DEFINED NOT NULL,
    CONSTRAINT aid_requests_pkey PRIMARY KEY (id),
    CONSTRAINT aid_requests_requester_id_fkey FOREIGN KEY (requester_id) REFERENCES public.users(id)
);
CREATE TABLE public.attachments (
    id uuid NOT NULL,
    cloudinary_public_id character varying NOT NULL UNIQUE,
    created_at timestamp with time zone NOT NULL,
    file_name character varying NOT NULL,
    file_size bigint NOT NULL,
    mime_type character varying NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    uploaded_by uuid NOT NULL,
    url character varying NOT NULL,
    reference_id uuid,
    reference_type character varying,
    CONSTRAINT attachments_pkey PRIMARY KEY (id)
);
CREATE TABLE public.chat_messages (
    id uuid NOT NULL DEFAULT uuid_generate_v4(),
    mission_id uuid NOT NULL,
    sender_id uuid NOT NULL,
    message_type character varying NOT NULL DEFAULT 'TEXT'::character varying,
    message_text text,
    image_url character varying,
    is_read boolean NOT NULL DEFAULT false,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT chat_messages_pkey PRIMARY KEY (id),
    CONSTRAINT chat_messages_mission_id_fkey FOREIGN KEY (mission_id) REFERENCES public.missions(id),
    CONSTRAINT chat_messages_sender_id_fkey FOREIGN KEY (sender_id) REFERENCES public.users(id)
);
CREATE TABLE public.dispatch_attempts (
    id uuid NOT NULL DEFAULT uuid_generate_v4(),
    mission_id uuid NOT NULL,
    volunteer_id uuid NOT NULL,
    dispatch_type character varying NOT NULL,
    batch_number integer NOT NULL DEFAULT 1,
    radius_km numeric,
    response character varying NOT NULL DEFAULT 'PENDING'::dispatch_response,
    responded_at timestamp with time zone,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT dispatch_attempts_pkey PRIMARY KEY (id),
    CONSTRAINT dispatch_attempts_mission_id_fkey FOREIGN KEY (mission_id) REFERENCES public.missions(id),
    CONSTRAINT dispatch_attempts_volunteer_id_fkey FOREIGN KEY (volunteer_id) REFERENCES public.users(id)
);
CREATE TABLE public.donation_items (
    id uuid NOT NULL DEFAULT uuid_generate_v4(),
    donation_id uuid NOT NULL,
    item_category_id uuid NOT NULL,
    quantity integer NOT NULL CHECK (quantity > 0),
    unit character varying,
    description text,
    expiry_date date,
    image_url character varying,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT donation_items_pkey PRIMARY KEY (id),
    CONSTRAINT donation_items_donation_id_fkey FOREIGN KEY (donation_id) REFERENCES public.donations(id),
    CONSTRAINT donation_items_item_category_id_fkey FOREIGN KEY (item_category_id) REFERENCES public.item_categories(id)
);
CREATE TABLE public.donations (
    id uuid NOT NULL DEFAULT uuid_generate_v4(),
    sponsor_id uuid NOT NULL,
    hub_id uuid NOT NULL,
    qr_code_token character varying UNIQUE,
    status USER - DEFINED NOT NULL DEFAULT 'REGISTERED'::donation_status,
    notes text,
    received_at timestamp with time zone,
    received_by uuid,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT donations_pkey PRIMARY KEY (id),
    CONSTRAINT donations_sponsor_id_fkey FOREIGN KEY (sponsor_id) REFERENCES public.users(id),
    CONSTRAINT donations_hub_id_fkey FOREIGN KEY (hub_id) REFERENCES public.hubs(id),
    CONSTRAINT donations_received_by_fkey FOREIGN KEY (received_by) REFERENCES public.users(id)
);
CREATE TABLE public.hub_accepted_categories (
    hub_id uuid NOT NULL,
    item_category_id uuid NOT NULL,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT hub_accepted_categories_pkey PRIMARY KEY (hub_id, item_category_id),
    CONSTRAINT hub_accepted_categories_hub_id_fkey FOREIGN KEY (hub_id) REFERENCES public.hubs(id),
    CONSTRAINT hub_accepted_categories_item_category_id_fkey FOREIGN KEY (item_category_id) REFERENCES public.item_categories(id)
);
CREATE TABLE public.hub_inventories (
    id uuid NOT NULL DEFAULT uuid_generate_v4(),
    hub_id uuid NOT NULL,
    item_category_id uuid NOT NULL,
    current_quantity integer NOT NULL DEFAULT 0 CHECK (current_quantity >= 0),
    low_stock_threshold integer NOT NULL DEFAULT 10,
    last_restocked_at timestamp with time zone,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT hub_inventories_pkey PRIMARY KEY (id),
    CONSTRAINT hub_inventories_hub_id_fkey FOREIGN KEY (hub_id) REFERENCES public.hubs(id),
    CONSTRAINT hub_inventories_item_category_id_fkey FOREIGN KEY (item_category_id) REFERENCES public.item_categories(id)
);
CREATE TABLE public.hub_staff (
    id uuid NOT NULL DEFAULT uuid_generate_v4(),
    hub_id uuid NOT NULL,
    user_id uuid NOT NULL,
    is_available boolean NOT NULL DEFAULT true,
    assigned_at timestamp with time zone NOT NULL DEFAULT now(),
    unassigned_at timestamp with time zone,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT hub_staff_pkey PRIMARY KEY (id),
    CONSTRAINT hub_staff_hub_id_fkey FOREIGN KEY (hub_id) REFERENCES public.hubs(id),
    CONSTRAINT hub_staff_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id)
);
CREATE TABLE public.hubs (
    id uuid NOT NULL DEFAULT uuid_generate_v4(),
    name character varying NOT NULL,
    address character varying,
    phone_number character varying,
    image_url character varying,
    status USER - DEFINED NOT NULL DEFAULT 'ACTIVE'::hub_status,
    operating_hours character varying,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    location USER - DEFINED NOT NULL,
    CONSTRAINT hubs_pkey PRIMARY KEY (id)
);
CREATE TABLE public.inventory_logs (
    id uuid NOT NULL DEFAULT uuid_generate_v4(),
    hub_inventory_id uuid NOT NULL,
    change_type character varying NOT NULL,
    quantity_delta integer NOT NULL CHECK (quantity_delta <> 0),
    quantity_after integer NOT NULL,
    reference_type character varying,
    reference_id uuid,
    notes text,
    created_by uuid,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT inventory_logs_pkey PRIMARY KEY (id),
    CONSTRAINT inventory_logs_hub_inventory_id_fkey FOREIGN KEY (hub_inventory_id) REFERENCES public.hub_inventories(id),
    CONSTRAINT inventory_logs_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users(id)
);
CREATE TABLE public.item_categories (
    id uuid NOT NULL DEFAULT uuid_generate_v4(),
    parent_id uuid,
    name character varying NOT NULL,
    unit character varying NOT NULL,
    icon_url character varying,
    is_leaf boolean NOT NULL DEFAULT true,
    sort_order integer DEFAULT 0,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT item_categories_pkey PRIMARY KEY (id),
    CONSTRAINT item_categories_parent_id_fkey FOREIGN KEY (parent_id) REFERENCES public.item_categories(id)
);
CREATE TABLE public.missions (
    id uuid NOT NULL DEFAULT uuid_generate_v4(),
    mission_type USER - DEFINED NOT NULL,
    sos_request_id uuid,
    aid_request_id uuid,
    volunteer_id uuid,
    hub_id uuid,
    status USER - DEFINED NOT NULL DEFAULT 'PENDING'::mission_status,
    qr_code_token character varying UNIQUE,
    priority_score numeric DEFAULT 0.00,
    cancellation_reason text,
    image_url character varying,
    comment text,
    started_at timestamp with time zone,
    completed_at timestamp with time zone,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    accepted_at timestamp with time zone,
    cancelled_at timestamp with time zone,
    confirmation_image_url character varying,
    picked_up_at timestamp with time zone,
    victim_location USER - DEFINED,
    CONSTRAINT missions_pkey PRIMARY KEY (id),
    CONSTRAINT missions_sos_request_id_fkey FOREIGN KEY (sos_request_id) REFERENCES public.sos_requests(id),
    CONSTRAINT missions_aid_request_id_fkey FOREIGN KEY (aid_request_id) REFERENCES public.aid_requests(id),
    CONSTRAINT missions_volunteer_id_fkey FOREIGN KEY (volunteer_id) REFERENCES public.users(id),
    CONSTRAINT missions_hub_id_fkey FOREIGN KEY (hub_id) REFERENCES public.hubs(id)
);
CREATE TABLE public.notifications (
    id uuid NOT NULL DEFAULT uuid_generate_v4(),
    user_id uuid NOT NULL,
    title character varying NOT NULL,
    body text,
    related_type character varying,
    related_id uuid,
    is_read boolean NOT NULL DEFAULT false,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT notifications_pkey PRIMARY KEY (id),
    CONSTRAINT notifications_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id)
);
CREATE TABLE public.refresh_tokens (
    id uuid NOT NULL DEFAULT uuid_generate_v4(),
    user_id uuid NOT NULL,
    token_hash character varying NOT NULL,
    device_info character varying,
    is_revoked boolean NOT NULL DEFAULT false,
    expires_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id),
    CONSTRAINT refresh_tokens_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id)
);
CREATE TABLE public.shelters (
    id uuid NOT NULL DEFAULT uuid_generate_v4(),
    hub_id uuid,
    name character varying NOT NULL,
    address character varying,
    max_capacity integer NOT NULL,
    current_capacity integer NOT NULL DEFAULT 0,
    phone_number character varying,
    image_url character varying,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    location USER - DEFINED NOT NULL,
    CONSTRAINT shelters_pkey PRIMARY KEY (id),
    CONSTRAINT shelters_hub_id_fkey FOREIGN KEY (hub_id) REFERENCES public.hubs(id)
);
CREATE TABLE public.sos_requests (
    id uuid NOT NULL DEFAULT uuid_generate_v4(),
    requester_id uuid,
    urgency USER - DEFINED NOT NULL DEFAULT 'MEDIUM'::urgency_level,
    status USER - DEFINED NOT NULL DEFAULT 'PENDING'::sos_status,
    address character varying,
    description text,
    people_count integer NOT NULL DEFAULT 1 CHECK (people_count > 0),
    image_url character varying,
    client_request_id character varying(100),
    source character varying(30),
    quick_sos boolean,
    accuracy double precision,
    triggered_at timestamp with time zone,
    location_captured_at timestamp with time zone,
    device_info character varying(500),
    sender_phone character varying(50),
    raw_message text,
    received_at_gateway_millis bigint,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    urgency_level character varying NOT NULL CHECK (
        urgency_level::text = ANY (
            ARRAY ['CRITICAL'::character varying, 'HIGH'::character varying, 'MEDIUM'::character varying, 'LOW'::character varying]::text []
        )
    ),
    location USER - DEFINED NOT NULL,
    CONSTRAINT sos_requests_pkey PRIMARY KEY (id),
    CONSTRAINT sos_requests_client_request_id_key UNIQUE (client_request_id),
    CONSTRAINT sos_requests_requester_id_fkey FOREIGN KEY (requester_id) REFERENCES public.users(id)
);
CREATE TABLE public.spatial_ref_sys (
    srid integer NOT NULL CHECK (
        srid > 0
        AND srid <= 998999
    ),
    auth_name character varying,
    auth_srid integer,
    srtext character varying,
    proj4text character varying,
    CONSTRAINT spatial_ref_sys_pkey PRIMARY KEY (srid)
);
CREATE TABLE public.sponsor_profiles (
    id uuid NOT NULL DEFAULT uuid_generate_v4(),
    user_id uuid NOT NULL UNIQUE,
    organization_name character varying,
    organization_type character varying,
    donation_count integer NOT NULL DEFAULT 0,
    total_donated_value numeric DEFAULT 0.00,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT sponsor_profiles_pkey PRIMARY KEY (id),
    CONSTRAINT sponsor_profiles_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id)
);
CREATE TABLE public.staff (
    id uuid NOT NULL DEFAULT uuid_generate_v4(),
    user_id uuid NOT NULL UNIQUE,
    start_date date NOT NULL DEFAULT CURRENT_DATE,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT staff_pkey PRIMARY KEY (id),
    CONSTRAINT staff_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id)
);
CREATE TABLE public.system_config (
    id uuid NOT NULL DEFAULT uuid_generate_v4(),
    config_key character varying NOT NULL UNIQUE,
    config_value text NOT NULL,
    description character varying,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT system_config_pkey PRIMARY KEY (id)
);
CREATE TABLE public.users (
    id uuid NOT NULL DEFAULT uuid_generate_v4(),
    email character varying UNIQUE,
    phone_number character varying UNIQUE,
    password_hash character varying NOT NULL,
    full_name character varying,
    avatar_url character varying,
    role character varying NOT NULL DEFAULT 'VICTIM'::user_role,
    is_active boolean NOT NULL DEFAULT true,
    is_verified boolean NOT NULL DEFAULT false,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    fcm_token character varying,
    CONSTRAINT users_pkey PRIMARY KEY (id)
);
CREATE TABLE public.volunteer_profiles (
    id uuid NOT NULL DEFAULT uuid_generate_v4(),
    user_id uuid NOT NULL UNIQUE,
    is_online boolean NOT NULL DEFAULT false,
    avg_rating numeric DEFAULT 0.00,
    total_tasks_completed integer NOT NULL DEFAULT 0,
    badge USER - DEFINED DEFAULT 'BRONZE'::badge_level,
    last_active_at timestamp with time zone,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    avg_response_seconds integer NOT NULL,
    vehicle_type character varying CHECK (
        vehicle_type::text = ANY (
            ARRAY ['MOTORBIKE'::character varying, 'CAR'::character varying, 'TRUCK'::character varying, 'BICYCLE'::character varying, 'WALK'::character varying]::text []
        )
    ),
    current_location USER - DEFINED,
    CONSTRAINT volunteer_profiles_pkey PRIMARY KEY (id),
    CONSTRAINT volunteer_profiles_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id)
);
CREATE OR REPLACE FUNCTION update_updated_at_column() RETURNS TRIGGER AS $$ BEGIN NEW.updated_at = NOW();
RETURN NEW;
END;
$$ language 'plpgsql';
-- Apply trigger to all tables with updated_at
CREATE TRIGGER update_users_updated_at BEFORE
UPDATE ON users FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_staff_updated_at BEFORE
UPDATE ON staff FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_volunteer_profiles_updated_at BEFORE
UPDATE ON volunteer_profiles FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_sponsor_profiles_updated_at BEFORE
UPDATE ON sponsor_profiles FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_hubs_updated_at BEFORE
UPDATE ON hubs FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_hub_staff_updated_at BEFORE
UPDATE ON hub_staff FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_shelters_updated_at BEFORE
UPDATE ON shelters FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_system_config_updated_at BEFORE
UPDATE ON system_config FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_item_categories_updated_at BEFORE
UPDATE ON item_categories FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_hub_inventories_updated_at BEFORE
UPDATE ON hub_inventories FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_sos_requests_updated_at BEFORE
UPDATE ON sos_requests FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_aid_requests_updated_at BEFORE
UPDATE ON aid_requests FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_donations_updated_at BEFORE
UPDATE ON donations FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_help_requests_updated_at BEFORE
UPDATE ON help_requests FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_missions_updated_at BEFORE
UPDATE ON missions FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
-- ============================================================
-- COMMENTS
-- ============================================================
COMMENT ON TABLE users IS 'Người dùng hệ thống: VICTIM, VOLUNTEER, SPONSOR, STAFF, ADMIN';
COMMENT ON TABLE refresh_tokens IS 'JWT refresh tokens cho authentication';
COMMENT ON TABLE otp IS 'OTP codes cho xác thực người dùng';
COMMENT ON TABLE staff IS 'Thông tin nhân viên (STAFF role)';
COMMENT ON TABLE volunteer_profiles IS 'Profile tình nguyện viên với vị trí real-time';
COMMENT ON TABLE sponsor_profiles IS 'Profile nhà tài trợ/quyên góp';
COMMENT ON TABLE hubs IS 'Trung tâm cứu trợ - điểm phân phối';
COMMENT ON TABLE hub_staff IS 'Phân công nhân viên vào hub';
COMMENT ON TABLE shelters IS 'Nơi trú ẩn cho nạn nhân';
COMMENT ON TABLE system_config IS 'Cấu hình hệ thống key-value';
COMMENT ON TABLE item_categories IS 'Danh mục vật phẩm cứu trợ (hierarchical)';
COMMENT ON TABLE hub_accepted_categories IS 'Danh mục vật phẩm hub chấp nhận';
COMMENT ON TABLE hub_inventories IS 'Tồn kho tại từng hub';
COMMENT ON TABLE inventory_logs IS 'Lịch sử thay đổi tồn kho';
COMMENT ON TABLE sos_requests IS 'Yêu cầu cứu hộ khẩn cấp';
COMMENT ON TABLE aid_requests IS 'Yêu cầu cứu trợ vật phẩm';
COMMENT ON TABLE aid_request_items IS 'Chi tiết vật phẩm trong yêu cầu cứu trợ';
COMMENT ON TABLE donations IS 'Đợt quyên góp từ sponsor';
COMMENT ON TABLE donation_items IS 'Chi tiết vật phẩm quyên góp';
COMMENT ON TABLE help_requests IS 'Thông tin nạn nhân cần cứu hộ';
COMMENT ON TABLE missions IS 'Nhiệm vụ: RESCUE hoặc DELIVERY';
COMMENT ON TABLE dispatch_attempts IS 'Lịch sử điều phối volunteer';
COMMENT ON TABLE chat_messages IS 'Tin nhắn trong mission';
COMMENT ON TABLE ratings IS 'Đánh giá volunteer sau mission';
COMMENT ON TABLE notifications IS 'Thông báo đẩy cho user';
