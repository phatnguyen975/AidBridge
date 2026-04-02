-- ============================================================
-- PostGIS Example Queries for AidBridge
-- Các truy vấn mẫu sử dụng PostGIS spatial functions
-- ============================================================
-- ============================================================
-- QUERY A: Find nearest volunteers within X km
-- Tìm tình nguyện viên gần nhất trong bán kính X km
-- ============================================================
-- A1. Tìm 10 volunteer gần nhất trong bán kính 5km từ vị trí SOS
-- ST_DWithin sử dụng mét (GEOGRAPHY), nên 5000 = 5km
-- ORDER BY distance sử dụng KNN operator <-> cho hiệu suất tối ưu
SELECT vp.id,
    vp.user_id,
    u.full_name,
    u.phone_number,
    vp.avg_rating,
    vp.vehicle_type,
    vp.badge,
    ST_Distance(vp.current_location, sos.location) as distance_meters,
    ROUND(
        ST_Distance(vp.current_location, sos.location) / 1000,
        2
    ) as distance_km
FROM public.volunteer_profiles vp
    JOIN public.users u ON vp.user_id = u.id
    CROSS JOIN (
        SELECT location
        FROM public.sos_requests
        WHERE id = 'sos-uuid-here'
    ) sos
WHERE vp.is_online = true
    AND vp.current_location IS NOT NULL
    AND u.is_active = true
    AND ST_DWithin(vp.current_location, sos.location, 5000) -- 5km radius
ORDER BY vp.current_location <->sos.location -- KNN optimization
LIMIT 10;
-- A2. Tìm volunteer với tọa độ cụ thể (dùng trong dispatch)
-- Input: lat = 10.762622, lng = 106.660172 (HCM City center)
SELECT vp.id,
    vp.user_id,
    u.full_name,
    vp.avg_rating,
    vp.vehicle_type,
    ST_Distance(
        vp.current_location,
        ST_SetSRID(ST_MakePoint(106.660172, 10.762622), 4326)::geography
    ) as distance_meters
FROM public.volunteer_profiles vp
    JOIN public.users u ON vp.user_id = u.id
WHERE vp.is_online = true
    AND vp.current_location IS NOT NULL
    AND ST_DWithin(
        vp.current_location,
        ST_SetSRID(ST_MakePoint(106.660172, 10.762622), 4326)::geography,
        10000 -- 10km
    )
ORDER BY distance_meters ASC
LIMIT 5;
-- ============================================================
-- QUERY B: Find nearest hub to a location
-- Tìm hub gần nhất từ một vị trí
-- ============================================================
-- B1. Tìm hub ACTIVE gần nhất từ vị trí của aid_request
SELECT h.id,
    h.name,
    h.address,
    h.phone_number,
    h.status,
    ST_Distance(h.location, ar.location) as distance_meters,
    ROUND(ST_Distance(h.location, ar.location) / 1000, 2) as distance_km
FROM public.hubs h
    CROSS JOIN (
        SELECT location
        FROM public.aid_requests
        WHERE id = 'aid-request-uuid'
    ) ar
WHERE h.status = 'ACTIVE'
ORDER BY h.location <->ar.location
LIMIT 1;
-- ============================================================
-- QUERY C: Order SOS requests by distance
-- Sắp xếp SOS requests theo khoảng cách từ volunteer
-- ============================================================
-- C1. Lấy pending SOS gần volunteer nhất (dispatch view)
SELECT sos.id,
    sos.urgency_level,
    sos.status,
    sos.people_count,
    sos.address,
    sos.description,
    ST_Y(sos.location::geometry) as lat,
    ST_X(sos.location::geometry) as lng,
    ST_Distance(sos.location, vp.current_location) as distance_meters,
    ROUND(
        ST_Distance(sos.location, vp.current_location) / 1000,
        2
    ) as distance_km,
    sos.created_at
FROM public.sos_requests sos
    CROSS JOIN (
        SELECT current_location
        FROM public.volunteer_profiles
        WHERE user_id = 'volunteer-user-uuid'
    ) vp
WHERE sos.status IN ('PENDING', 'DISPATCHING')
ORDER BY -- Priority: CRITICAL > HIGH > MEDIUM > LOW, then by distance
    CASE
        sos.urgency_level
        WHEN 'CRITICAL' THEN 1
        WHEN 'HIGH' THEN 2
        WHEN 'MEDIUM' THEN 3
        ELSE 4
    END ASC,
    sos.location <->vp.current_location ASC
LIMIT 20;
-- ============================================================
-- QUERY D: Filter missions within a radius
-- Lọc missions trong bán kính
-- ============================================================
-- D1. Lấy active missions trong bán kính từ hub
SELECT m.id,
    m.mission_type,
    m.status,
    m.priority_score,
    ST_Y(m.victim_location::geometry) as victim_lat,
    ST_X(m.victim_location::geometry) as victim_lng,
    ST_Distance(m.victim_location, h.location) as distance_meters,
    u.full_name as volunteer_name,
    m.created_at
FROM public.missions m
    JOIN public.hubs h ON m.hub_id = h.id
    LEFT JOIN public.users u ON m.volunteer_id = u.id
WHERE h.id = 'hub-uuid'
    AND m.status NOT IN ('COMPLETED', 'CANCELLED')
    AND m.victim_location IS NOT NULL
    AND ST_DWithin(m.victim_location, h.location, 15000) -- 15km
ORDER BY m.priority_score DESC,
    m.created_at ASC;
-- ============================================================
-- QUERY E: Supabase RPC Functions (PostgREST compatible)
-- ============================================================
-- E1. RPC để tìm nearest volunteers (gọi từ Supabase client)
CREATE OR REPLACE FUNCTION find_nearest_volunteers(
        p_lat DOUBLE PRECISION,
        p_lng DOUBLE PRECISION,
        p_radius_km INTEGER DEFAULT 10,
        p_limit INTEGER DEFAULT 5
    ) RETURNS TABLE (
        volunteer_id UUID,
        user_id UUID,
        full_name VARCHAR,
        phone_number VARCHAR,
        avatar_url VARCHAR,
        avg_rating NUMERIC,
        vehicle_type VARCHAR,
        badge badge_level,
        distance_meters DOUBLE PRECISION,
        distance_km NUMERIC
    ) AS $$ BEGIN RETURN QUERY
SELECT vp.id as volunteer_id,
    vp.user_id,
    u.full_name,
    u.phone_number,
    u.avatar_url,
    vp.avg_rating,
    vp.vehicle_type,
    vp.badge,
    ST_Distance(
        vp.current_location,
        ST_SetSRID(ST_MakePoint(p_lng, p_lat), 4326)::geography
    ) as distance_meters,
    ROUND(
        ST_Distance(
            vp.current_location,
            ST_SetSRID(ST_MakePoint(p_lng, p_lat), 4326)::geography
        ) / 1000,
        2
    ) as distance_km
FROM public.volunteer_profiles vp
    JOIN public.users u ON vp.user_id = u.id
WHERE vp.is_online = true
    AND vp.current_location IS NOT NULL
    AND u.is_active = true
    AND ST_DWithin(
        vp.current_location,
        ST_SetSRID(ST_MakePoint(p_lng, p_lat), 4326)::geography,
        p_radius_km * 1000
    )
ORDER BY vp.current_location <->ST_SetSRID(ST_MakePoint(p_lng, p_lat), 4326)::geography
LIMIT p_limit;
END;
$$ LANGUAGE plpgsql STABLE;
-- E2. RPC để tìm nearest hubs
CREATE OR REPLACE FUNCTION find_nearest_hubs(
        p_lat DOUBLE PRECISION,
        p_lng DOUBLE PRECISION,
        p_limit INTEGER DEFAULT 3
    ) RETURNS TABLE (
        hub_id UUID,
        name VARCHAR,
        address VARCHAR,
        lat DOUBLE PRECISION,
        lng DOUBLE PRECISION,
        phone_number VARCHAR,
        status hub_status,
        distance_meters DOUBLE PRECISION,
        distance_km NUMERIC
    ) AS $$ BEGIN RETURN QUERY
SELECT h.id as hub_id,
    h.name,
    h.address,
    ST_Y(h.location::geometry) as lat,
    ST_X(h.location::geometry) as lng,
    h.phone_number,
    h.status,
    ST_Distance(
        h.location,
        ST_SetSRID(ST_MakePoint(p_lng, p_lat), 4326)::geography
    ) as distance_meters,
    ROUND(
        ST_Distance(
            h.location,
            ST_SetSRID(ST_MakePoint(p_lng, p_lat), 4326)::geography
        ) / 1000,
        2
    ) as distance_km
FROM public.hubs h
WHERE h.status = 'ACTIVE'
ORDER BY h.location <->ST_SetSRID(ST_MakePoint(p_lng, p_lat), 4326)::geography
LIMIT p_limit;
END;
$$ LANGUAGE plpgsql STABLE;
-- E3. RPC update volunteer location (optimized)
CREATE OR REPLACE FUNCTION update_volunteer_location(
        p_user_id UUID,
        p_lat DOUBLE PRECISION,
        p_lng DOUBLE PRECISION,
        p_is_online BOOLEAN DEFAULT true
    ) RETURNS VOID AS $$ BEGIN
UPDATE public.volunteer_profiles
SET current_location = ST_SetSRID(ST_MakePoint(p_lng, p_lat), 4326)::geography,
    last_location_update = NOW(),
    is_online = p_is_online
WHERE user_id = p_user_id;
END;
$$ LANGUAGE plpgsql;
-- ============================================================
-- QUERY F: Calculate priority score for missions
-- ============================================================
CREATE OR REPLACE FUNCTION calculate_mission_priority_score(
        p_urgency_level VARCHAR,
        p_people_count INTEGER,
        p_distance_meters DOUBLE PRECISION,
        p_time_waiting_minutes INTEGER
    ) RETURNS NUMERIC AS $$
DECLARE v_urgency_weight NUMERIC;
v_distance_score NUMERIC;
v_time_score NUMERIC;
v_people_score NUMERIC;
BEGIN -- Urgency weight (CRITICAL = 100, HIGH = 70, MEDIUM = 40, LOW = 10)
v_urgency_weight := CASE
    p_urgency_level
    WHEN 'CRITICAL' THEN 100
    WHEN 'HIGH' THEN 70
    WHEN 'MEDIUM' THEN 40
    ELSE 10
END;
-- Distance score: closer = higher score (max 30 points)
v_distance_score := CASE
    WHEN p_distance_meters <= 1000 THEN 30
    WHEN p_distance_meters <= 3000 THEN 25
    WHEN p_distance_meters <= 5000 THEN 20
    WHEN p_distance_meters <= 10000 THEN 15
    ELSE 5
END;
-- Time waiting score: longer wait = higher priority (max 30 points)
v_time_score := LEAST(FLOOR(p_time_waiting_minutes / 10) * 5, 30);
-- People count score (max 20 points)
v_people_score := CASE
    WHEN p_people_count = 1 THEN 5
    WHEN p_people_count <= 3 THEN 10
    WHEN p_people_count <= 5 THEN 15
    ELSE 20
END;
-- Total score (max ~180)
RETURN v_urgency_weight + v_distance_score + v_time_score + v_people_score;
END;
$$ LANGUAGE plpgsql IMMUTABLE;
-- ============================================================
-- USAGE EXAMPLES (Supabase JS Client)
-- ============================================================
/*
 // Tìm volunteer gần nhất
 const { data, error } = await supabase
 .rpc('find_nearest_volunteers', {
 p_lat: 10.762622,
 p_lng: 106.660172,
 p_radius_km: 5,
 p_limit: 10
 });
 
 // Tìm hub gần nhất
 const { data, error } = await supabase
 .rpc('find_nearest_hubs', {
 p_lat: 10.762622,
 p_lng: 106.660172,
 p_limit: 3
 });
 
 // Update volunteer location
 const { error } = await supabase
 .rpc('update_volunteer_location', {
 p_user_id: 'user-uuid',
 p_lat: 10.800,
 p_lng: 106.700,
 p_is_online: true
 });
 */