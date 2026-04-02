-- ============================================================
-- PostGIS Migration Script for AidBridge Disaster Relief System
-- Version: 2.0
-- Description: Migrate lat/lng columns to GEOGRAPHY(POINT, 4326)
-- ============================================================
-- ============================================================
-- STEP 1: Enable PostGIS Extension (Supabase có sẵn, chỉ cần enable)
-- ============================================================
-- PostGIS cung cấp các kiểu dữ liệu và hàm không gian địa lý
-- GEOGRAPHY sử dụng hệ tọa độ cầu, tính khoảng cách bằng mét
CREATE EXTENSION IF NOT EXISTS postgis;
-- ============================================================
-- STEP 2: Add location columns to all affected tables
-- ============================================================
-- 2.1 aid_requests - Yêu cầu cứu trợ vật phẩm
-- GEOGRAPHY(POINT, 4326) = Point geometry với SRID 4326 (WGS84)
ALTER TABLE public.aid_requests
ADD COLUMN IF NOT EXISTS location GEOGRAPHY(POINT, 4326);
-- 2.2 sos_requests - Yêu cầu cứu hộ khẩn cấp
ALTER TABLE public.sos_requests
ADD COLUMN IF NOT EXISTS location GEOGRAPHY(POINT, 4326);
-- 2.3 help_requests - Thông tin nạn nhân cần cứu hộ
ALTER TABLE public.help_requests
ADD COLUMN IF NOT EXISTS location GEOGRAPHY(POINT, 4326);
-- 2.4 hubs - Trung tâm cứu trợ
ALTER TABLE public.hubs
ADD COLUMN IF NOT EXISTS location GEOGRAPHY(POINT, 4326);
-- 2.5 shelters - Nơi trú ẩn
ALTER TABLE public.shelters
ADD COLUMN IF NOT EXISTS location GEOGRAPHY(POINT, 4326);
-- 2.6 volunteer_profiles - Vị trí real-time của tình nguyện viên
-- Thêm cột current_location thay cho current_lat, current_lng
ALTER TABLE public.volunteer_profiles
ADD COLUMN IF NOT EXISTS current_location GEOGRAPHY(POINT, 4326);
-- 2.7 missions - Vị trí nạn nhân trong nhiệm vụ
-- Thêm cột victim_location thay cho victim_lat, victim_lng
ALTER TABLE public.missions
ADD COLUMN IF NOT EXISTS victim_location GEOGRAPHY(POINT, 4326);
-- ============================================================
-- STEP 3: Migrate existing data
-- ST_MakePoint(lng, lat) - Lưu ý: longitude trước, latitude sau
-- ST_SetSRID() gán hệ tọa độ WGS84 (SRID 4326)
-- ::geography chuyển đổi sang kiểu GEOGRAPHY
-- ============================================================
-- 3.1 aid_requests
UPDATE public.aid_requests
SET location = ST_SetSRID(ST_MakePoint(lng::float, lat::float), 4326)::geography
WHERE lat IS NOT NULL
    AND lng IS NOT NULL
    AND location IS NULL;
-- 3.2 sos_requests
UPDATE public.sos_requests
SET location = ST_SetSRID(ST_MakePoint(lng, lat), 4326)::geography
WHERE lat IS NOT NULL
    AND lng IS NOT NULL
    AND location IS NULL;
-- 3.3 help_requests
UPDATE public.help_requests
SET location = ST_SetSRID(ST_MakePoint(lng::float, lat::float), 4326)::geography
WHERE lat IS NOT NULL
    AND lng IS NOT NULL
    AND location IS NULL;
-- 3.4 hubs
UPDATE public.hubs
SET location = ST_SetSRID(ST_MakePoint(lng::float, lat::float), 4326)::geography
WHERE lat IS NOT NULL
    AND lng IS NOT NULL
    AND location IS NULL;
-- 3.5 shelters
UPDATE public.shelters
SET location = ST_SetSRID(ST_MakePoint(lng::float, lat::float), 4326)::geography
WHERE lat IS NOT NULL
    AND lng IS NOT NULL
    AND location IS NULL;
-- 3.6 volunteer_profiles
UPDATE public.volunteer_profiles
SET current_location = ST_SetSRID(
        ST_MakePoint(current_lng::float, current_lat::float),
        4326
    )::geography
WHERE current_lat IS NOT NULL
    AND current_lng IS NOT NULL
    AND current_location IS NULL;
-- 3.7 missions
UPDATE public.missions
SET victim_location = ST_SetSRID(
        ST_MakePoint(victim_lng::float, victim_lat::float),
        4326
    )::geography
WHERE victim_lat IS NOT NULL
    AND victim_lng IS NOT NULL
    AND victim_location IS NULL;
-- ============================================================
-- STEP 4: Add NOT NULL constraints (nếu cần thiết)
-- Chỉ áp dụng cho các bảng bắt buộc có vị trí
-- ============================================================
-- aid_requests - location bắt buộc
ALTER TABLE public.aid_requests
ALTER COLUMN location
SET NOT NULL;
-- sos_requests - location bắt buộc cho cứu hộ khẩn cấp
ALTER TABLE public.sos_requests
ALTER COLUMN location
SET NOT NULL;
-- hubs - location bắt buộc cho trung tâm cứu trợ
ALTER TABLE public.hubs
ALTER COLUMN location
SET NOT NULL;
-- shelters - location bắt buộc cho nơi trú ẩn
ALTER TABLE public.shelters
ALTER COLUMN location
SET NOT NULL;
-- help_requests và volunteer_profiles có thể nullable (chưa có vị trí)
-- missions.victim_location có thể nullable (DELIVERY mission không cần)
-- ============================================================
-- STEP 5: Create GIST Spatial Indexes
-- GIST index tối ưu cho các truy vấn không gian (nearest neighbor, within)
-- ============================================================
-- 5.1 aid_requests - tìm yêu cầu cứu trợ gần nhất
CREATE INDEX IF NOT EXISTS idx_aid_requests_location ON public.aid_requests USING GIST(location);
-- 5.2 sos_requests - tìm SOS gần nhất (critical cho dispatch)
CREATE INDEX IF NOT EXISTS idx_sos_requests_location ON public.sos_requests USING GIST(location);
-- 5.3 help_requests - tìm nạn nhân cần cứu hộ
CREATE INDEX IF NOT EXISTS idx_help_requests_location ON public.help_requests USING GIST(location);
-- 5.4 hubs - tìm hub gần nhất
CREATE INDEX IF NOT EXISTS idx_hubs_location ON public.hubs USING GIST(location);
-- 5.5 shelters - tìm nơi trú ẩn gần nhất
CREATE INDEX IF NOT EXISTS idx_shelters_location ON public.shelters USING GIST(location);
-- 5.6 volunteer_profiles - tìm volunteer gần nhất (HOT PATH)
-- Index này cực kỳ quan trọng cho dispatch algorithm
CREATE INDEX IF NOT EXISTS idx_volunteer_profiles_current_location ON public.volunteer_profiles USING GIST(current_location);
-- Composite index: online + location cho tìm volunteer có sẵn
CREATE INDEX IF NOT EXISTS idx_volunteer_profiles_online_location ON public.volunteer_profiles (is_online)
WHERE is_online = true;
-- 5.7 missions - tìm mission theo vị trí nạn nhân
CREATE INDEX IF NOT EXISTS idx_missions_victim_location ON public.missions USING GIST(victim_location);
-- ============================================================
-- STEP 6: Drop old lat/lng columns (CAREFUL - chạy sau khi verify)
-- Chỉ chạy sau khi đã verify data migration thành công
-- ============================================================
-- 6.1 aid_requests
ALTER TABLE public.aid_requests DROP COLUMN IF EXISTS lat;
ALTER TABLE public.aid_requests DROP COLUMN IF EXISTS lng;
-- 6.2 sos_requests
ALTER TABLE public.sos_requests DROP COLUMN IF EXISTS lat;
ALTER TABLE public.sos_requests DROP COLUMN IF EXISTS lng;
-- 6.3 help_requests
ALTER TABLE public.help_requests DROP COLUMN IF EXISTS lat;
ALTER TABLE public.help_requests DROP COLUMN IF EXISTS lng;
-- 6.4 hubs
ALTER TABLE public.hubs DROP COLUMN IF EXISTS lat;
ALTER TABLE public.hubs DROP COLUMN IF EXISTS lng;
-- 6.5 shelters
ALTER TABLE public.shelters DROP COLUMN IF EXISTS lat;
ALTER TABLE public.shelters DROP COLUMN IF EXISTS lng;
-- 6.6 volunteer_profiles
ALTER TABLE public.volunteer_profiles DROP COLUMN IF EXISTS current_lat;
ALTER TABLE public.volunteer_profiles DROP COLUMN IF EXISTS current_lng;
-- 6.7 missions
ALTER TABLE public.missions DROP COLUMN IF EXISTS victim_lat;
ALTER TABLE public.missions DROP COLUMN IF EXISTS victim_lng;
-- ============================================================
-- STEP 7: Helper Functions for Supabase/PostgREST compatibility
-- ============================================================
-- 7.1 Function để extract lat từ geography (cho API response)
CREATE OR REPLACE FUNCTION get_latitude(loc GEOGRAPHY) RETURNS DOUBLE PRECISION AS $$ BEGIN RETURN ST_Y(loc::geometry);
END;
$$ LANGUAGE plpgsql IMMUTABLE;
-- 7.2 Function để extract lng từ geography
CREATE OR REPLACE FUNCTION get_longitude(loc GEOGRAPHY) RETURNS DOUBLE PRECISION AS $$ BEGIN RETURN ST_X(loc::geometry);
END;
$$ LANGUAGE plpgsql IMMUTABLE;
-- 7.3 Function tạo geography point từ lat/lng (cho API input)
CREATE OR REPLACE FUNCTION make_point(lat DOUBLE PRECISION, lng DOUBLE PRECISION) RETURNS GEOGRAPHY AS $$ BEGIN RETURN ST_SetSRID(ST_MakePoint(lng, lat), 4326)::geography;
END;
$$ LANGUAGE plpgsql IMMUTABLE;
-- ============================================================
-- STEP 8: Create Views for PostgREST API (backward compatibility)
-- Views này giúp API response vẫn trả về lat/lng riêng biệt
-- ============================================================
-- 8.1 View cho hubs với lat/lng tách biệt
CREATE OR REPLACE VIEW public.hubs_with_coords AS
SELECT id,
    name,
    address,
    ST_Y(location::geometry) as lat,
    ST_X(location::geometry) as lng,
    location,
    phone_number,
    image_url,
    status,
    operating_hours,
    created_at,
    updated_at
FROM public.hubs;
-- 8.2 View cho shelters với lat/lng tách biệt
CREATE OR REPLACE VIEW public.shelters_with_coords AS
SELECT id,
    hub_id,
    name,
    address,
    ST_Y(location::geometry) as lat,
    ST_X(location::geometry) as lng,
    location,
    max_capacity,
    current_capacity,
    phone_number,
    image_url,
    is_active,
    created_at,
    updated_at
FROM public.shelters;
-- 8.3 View cho online volunteers với location
CREATE OR REPLACE VIEW public.online_volunteers AS
SELECT vp.id,
    vp.user_id,
    u.full_name,
    u.phone_number,
    u.avatar_url,
    ST_Y(vp.current_location::geometry) as lat,
    ST_X(vp.current_location::geometry) as lng,
    vp.current_location,
    vp.avg_rating,
    vp.total_tasks_completed,
    vp.badge,
    vp.vehicle_type,
    vp.last_location_update
FROM public.volunteer_profiles vp
    JOIN public.users u ON vp.user_id = u.id
WHERE vp.is_online = true
    AND vp.current_location IS NOT NULL
    AND u.is_active = true;
-- ============================================================
-- STEP 9: Update Comments
-- ============================================================
COMMENT ON COLUMN public.aid_requests.location IS 'Vị trí yêu cầu cứu trợ (GEOGRAPHY POINT, SRID 4326)';
COMMENT ON COLUMN public.sos_requests.location IS 'Vị trí SOS khẩn cấp (GEOGRAPHY POINT, SRID 4326)';
COMMENT ON COLUMN public.help_requests.location IS 'Vị trí nạn nhân cần cứu hộ (GEOGRAPHY POINT, SRID 4326)';
COMMENT ON COLUMN public.hubs.location IS 'Vị trí trung tâm cứu trợ (GEOGRAPHY POINT, SRID 4326)';
COMMENT ON COLUMN public.shelters.location IS 'Vị trí nơi trú ẩn (GEOGRAPHY POINT, SRID 4326)';
COMMENT ON COLUMN public.volunteer_profiles.current_location IS 'Vị trí hiện tại của volunteer (real-time tracking)';
COMMENT ON COLUMN public.missions.victim_location IS 'Vị trí nạn nhân trong mission RESCUE';
COMMENT ON INDEX public.idx_volunteer_profiles_current_location IS 'GIST index cho tìm kiếm volunteer gần nhất - HOT PATH';
COMMENT ON INDEX public.idx_sos_requests_location IS 'GIST index cho tìm SOS requests theo khoảng cách';
-- ============================================================
-- END OF MIGRATION
-- ============================================================