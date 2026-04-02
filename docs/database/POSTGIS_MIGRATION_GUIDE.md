# PostGIS Migration Guide for AidBridge

## Tổng quan

Migration này chuyển đổi các cột `lat/lng` riêng biệt sang kiểu `GEOGRAPHY(POINT, 4326)` của PostGIS, giúp tối ưu các truy vấn không gian địa lý trong hệ thống cứu trợ thiên tai.

## Tại sao dùng GEOGRAPHY thay vì GEOMETRY?

| Aspect             | GEOGRAPHY                  | GEOMETRY                |
| ------------------ | -------------------------- | ----------------------- |
| Đơn vị khoảng cách | Mét                        | Đơn vị của SRID         |
| Độ chính xác       | Cao (tính trên bề mặt cầu) | Phụ thuộc projection    |
| Performance        | Chậm hơn một chút          | Nhanh hơn               |
| Use case           | Khoảng cách thực tế        | Spatial analysis cục bộ |

**Chọn GEOGRAPHY** vì AidBridge cần tính khoảng cách thực tế (mét/km) để dispatch volunteer.

## Các bảng được migrate

| Bảng                 | Cột cũ                     | Cột mới            |
| -------------------- | -------------------------- | ------------------ |
| `aid_requests`       | `lat, lng`                 | `location`         |
| `sos_requests`       | `lat, lng`                 | `location`         |
| `help_requests`      | `lat, lng`                 | `location`         |
| `hubs`               | `lat, lng`                 | `location`         |
| `shelters`           | `lat, lng`                 | `location`         |
| `volunteer_profiles` | `current_lat, current_lng` | `current_location` |
| `missions`           | `victim_lat, victim_lng`   | `victim_location`  |

## Cách chạy migration

### 1. Trên Supabase Dashboard

```sql
-- Chạy trong SQL Editor
-- Copy nội dung từ V2__postgis_migration.sql
```

### 2. Sử dụng Supabase CLI

```bash
supabase db push
```

## Spatial Indexes

GIST indexes được tạo cho tất cả location columns:

```sql
CREATE INDEX idx_volunteer_profiles_current_location
ON volunteer_profiles USING GIST(current_location);
```

**Tại sao GIST?**

- Tối ưu cho nearest neighbor queries (`<->` operator)
- Hỗ trợ ST_DWithin, ST_Distance hiệu quả
- Là index type được khuyến nghị cho PostGIS

## RPC Functions cho Supabase Client

### find_nearest_volunteers

```javascript
const { data } = await supabase.rpc("find_nearest_volunteers", {
  p_lat: 10.762622,
  p_lng: 106.660172,
  p_radius_km: 5,
  p_limit: 10,
});
```

### find_nearest_hubs

```javascript
const { data } = await supabase.rpc("find_nearest_hubs", {
  p_lat: 10.762622,
  p_lng: 106.660172,
  p_limit: 3,
});
```

### update_volunteer_location

```javascript
await supabase.rpc("update_volunteer_location", {
  p_user_id: "uuid",
  p_lat: 10.8,
  p_lng: 106.7,
  p_is_online: true,
});
```

## Real-time Location Updates - Best Practices

### 1. Throttling

Không update mỗi giây. Khuyến nghị:

- **Khi di chuyển**: Update mỗi 10-15 giây
- **Khi đứng yên**: Update mỗi 30-60 giây
- **Khi offline**: Không update

### 2. Client-side optimization

```javascript
// Chỉ update khi di chuyển đáng kể (>50m)
const MINIMUM_DISTANCE = 50; // meters

function shouldUpdate(newLat, newLng, oldLat, oldLng) {
  const distance = haversineDistance(newLat, newLng, oldLat, oldLng);
  return distance > MINIMUM_DISTANCE;
}
```

### 3. Supabase Realtime với Postgres Changes

```javascript
// Subscribe to volunteer location changes
const channel = supabase
  .channel("volunteer-locations")
  .on(
    "postgres_changes",
    {
      event: "UPDATE",
      schema: "public",
      table: "volunteer_profiles",
      filter: "is_online=eq.true",
    },
    (payload) => {
      updateMapMarker(payload.new);
    },
  )
  .subscribe();
```

## Priority Score Algorithm

```
priority_score = urgency_weight + distance_score + time_score + people_score
```

| Factor    | Max Points | Logic                                        |
| --------- | ---------- | -------------------------------------------- |
| Urgency   | 100        | CRITICAL=100, HIGH=70, MEDIUM=40, LOW=10     |
| Distance  | 30         | <1km=30, <3km=25, <5km=20, <10km=15, >10km=5 |
| Wait Time | 30         | +5 points per 10 minutes                     |
| People    | 20         | 1=5, 2-3=10, 4-5=15, >5=20                   |

**Max total: ~180 points**

## Backward Compatibility

Views được tạo để API vẫn trả về `lat/lng` riêng:

```sql
-- View hubs_with_coords
SELECT
  id, name,
  ST_Y(location::geometry) as lat,
  ST_X(location::geometry) as lng,
  ...
FROM hubs;
```

## Lưu ý quan trọng

1. **Thứ tự tham số**: `ST_MakePoint(lng, lat)` - longitude trước!
2. **SRID 4326**: Luôn dùng WGS84 coordinate system
3. **Cast to geography**: `::geography` để tính khoảng cách bằng mét
4. **KNN operator**: Dùng `<->` thay vì `ORDER BY ST_Distance()` cho performance

## Rollback (nếu cần)

```sql
-- Thêm lại cột lat/lng
ALTER TABLE hubs ADD COLUMN lat NUMERIC;
ALTER TABLE hubs ADD COLUMN lng NUMERIC;

-- Copy data back
UPDATE hubs SET
  lat = ST_Y(location::geometry),
  lng = ST_X(location::geometry);

-- Drop location column
ALTER TABLE hubs DROP COLUMN location;
```
