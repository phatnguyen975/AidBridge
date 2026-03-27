# AidBridge — Data Architecture

> Phân chia dữ liệu giữa các hệ thống: PostgreSQL, Redis, FCM, Cloud Storage

---

## 1. Tổng quan Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              MOBILE APPS                                     │
│                    (Victim / Volunteer / Sponsor / Staff)                    │
└─────────────────────────────────┬───────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              API SERVER                                      │
│                           (NestJS / Express)                                 │
└───────┬─────────────┬─────────────┬─────────────┬─────────────┬─────────────┘
        │             │             │             │             │
        ▼             ▼             ▼             ▼             ▼
┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐
│ Supabase  │  │   Redis   │  │    FCM    │  │ Cloudinary│  │ Supabase  │
│ Postgres  │  │           │  │           │  │ / S3      │  │ Realtime  │
├───────────┤  ├───────────┤  ├───────────┤  ├───────────┤  ├───────────┤
│ Source of │  │ Cache     │  │ Push      │  │ Files     │  │ Live      │
│ Truth     │  │ + Temp    │  │ Notifs    │  │ Storage   │  │ Updates   │
└───────────┘  └───────────┘  └───────────┘  └───────────┘  └───────────┘
```

---

## 2. Data Distribution Matrix

### 2.1. Theo loại dữ liệu

| Dữ liệu            | PostgreSQL  |   Redis    | Supabase Realtime |     FCM      | Cloud Storage |
| ------------------ | :---------: | :--------: | :---------------: | :----------: | :-----------: |
| Users & Auth       | ✅ Primary  | 🔶 Session |         -         |   🔶 Token   |   🔶 Avatar   |
| Volunteer Location | ✅ Snapshot |  ✅ Live   |   ✅ Broadcast    |      -       |       -       |
| Missions           | ✅ Primary  | 🔶 Active  |     ✅ Status     |  ✅ Alerts   |   🔶 Images   |
| Chat Messages      | ✅ Primary  | 🔶 Recent  |      ✅ Live      |  ✅ New msg  |   🔶 Images   |
| Donations          | ✅ Primary  |     -      |     ✅ Status     |  ✅ Updates  |   🔶 Images   |
| Inventory          | ✅ Primary  |  🔶 Stock  |         -         | 🔶 Low stock |       -       |
| SOS/Aid Requests   | ✅ Primary  | 🔶 Pending |     ✅ Status     |  ✅ Alerts   |   🔶 Images   |
| Notifications      | ✅ History  |     -      |         -         | ✅ Delivery  |       -       |
| System Config      | ✅ Primary  |  ✅ Cache  |         -         |      -       |       -       |

**Legend:** ✅ Required | 🔶 Optional/Conditional

---

## 3. Chi tiết từng Data Store

### 3.1. PostgreSQL (Supabase) — Source of Truth

> Lưu trữ tất cả dữ liệu persistent, có tính ACID

```sql
-- 22 Tables (đầy đủ trong schema.sql)
├── AUTH
│   ├── users                 -- Thông tin người dùng
│   └── refresh_tokens        -- JWT refresh tokens
├── PROFILES
│   ├── volunteer_profiles    -- Stats, rating, vehicle
│   └── sponsor_profiles      -- Points, badge, donation count
├── INFRASTRUCTURE
│   ├── hubs                  -- Trung tâm phân phối
│   ├── hub_staff             -- Staff assignments
│   ├── shelters              -- Điểm trú ẩn
│   └── system_config         -- Cấu hình hệ thống
├── CATALOG & INVENTORY
│   ├── item_categories       -- Danh mục vật phẩm
│   ├── hub_accepted_categories
│   ├── hub_inventories       -- Tồn kho
│   └── inventory_logs        -- Audit trail
├── REQUESTS
│   ├── sos_requests          -- Yêu cầu cứu hộ
│   ├── aid_requests          -- Yêu cầu vật phẩm
│   └── aid_request_items
├── DONATIONS
│   ├── donations             -- Đợt quyên góp
│   └── donation_items        -- Chi tiết vật phẩm
├── MISSIONS
│   ├── missions              -- Nhiệm vụ
│   └── dispatch_attempts     -- Lịch sử dispatch
└── COMMUNICATION
    ├── chat_messages         -- Tin nhắn
    ├── ratings               -- Đánh giá
    └── notifications         -- Lịch sử thông báo
```

**Khi nào write vào PostgreSQL:**

- Mọi thao tác CRUD chính
- Sau khi validate business logic
- Cần audit trail / history

---

### 3.2. Redis — Cache & Real-time Data

> Lưu trữ dữ liệu tạm thời, hot data, và pub/sub

#### 3.2.1. Volunteer Locations (Geo)

```redis
# Key pattern: volunteer:locations
# Type: Sorted Set với Geo commands

GEOADD volunteer:locations <lng> <lat> <volunteer_id>
GEORADIUS volunteer:locations <lng> <lat> <radius> km WITHDIST

# TTL: Tự động expire sau 5 phút không update
# Sync: Snapshot vào PostgreSQL mỗi 30 giây
```

**Data flow:**

```
Mobile App (GPS update mỗi 10s)
    │
    ▼
┌─────────────────────────────────────────┐
│              API Server                  │
├─────────────────────────────────────────┤
│ 1. GEOADD vào Redis (real-time)         │
│ 2. Batch update PostgreSQL (mỗi 30s)    │
│ 3. Broadcast qua Supabase Realtime      │
└─────────────────────────────────────────┘
```

#### 3.2.2. Session & Auth

```redis
# Active sessions
SET session:{user_id}:{device_id} {token_data} EX 3600

# OTP rate limiting
INCR otp:attempts:{phone}
EXPIRE otp:attempts:{phone} 300

# Blacklisted tokens
SADD blacklist:tokens {token_hash}
```

#### 3.2.3. Dispatch Queue

```redis
# Pending dispatches (Priority Queue)
ZADD dispatch:pending {priority_score} {mission_id}

# Volunteer response timeout tracking
SET dispatch:timeout:{attempt_id} {mission_id} EX 120
```

#### 3.2.4. Cache Patterns

```redis
# System config cache
HSET config dispatch_radius_km "5" timeout_seconds "120"

# Hub inventory cache (hot data)
HSET hub:{hub_id}:inventory {category_id} {quantity}

# User profile cache
SET user:{user_id}:profile {json_data} EX 300
```

#### 3.2.5. Redis Data Summary

| Key Pattern             | Type       | TTL | Purpose                  |
| ----------------------- | ---------- | --- | ------------------------ |
| `volunteer:locations`   | Geo        | 5m  | Live volunteer positions |
| `session:{uid}:{did}`   | String     | 1h  | Active sessions          |
| `otp:attempts:{phone}`  | Counter    | 5m  | Rate limiting            |
| `dispatch:pending`      | Sorted Set | -   | Priority queue           |
| `dispatch:timeout:{id}` | String     | 2m  | Response timeout         |
| `config`                | Hash       | 1h  | System config cache      |
| `hub:{id}:inventory`    | Hash       | 5m  | Inventory cache          |
| `user:{id}:profile`     | String     | 5m  | Profile cache            |
| `blacklist:tokens`      | Set        | 30d | Revoked tokens           |

---

### 3.3. Supabase Realtime — Live Updates

> WebSocket-based subscriptions cho real-time UI updates

#### 3.3.1. Subscribed Tables

```typescript
// Client-side subscriptions

// 1. Mission status changes (Volunteer app)
supabase
  .channel("missions")
  .on(
    "postgres_changes",
    {
      event: "UPDATE",
      schema: "public",
      table: "missions",
      filter: `volunteer_id=eq.${volunteerId}`,
    },
    handleMissionUpdate,
  )
  .subscribe();

// 2. Chat messages (Mission chat)
supabase
  .channel("chat")
  .on(
    "postgres_changes",
    {
      event: "INSERT",
      schema: "public",
      table: "chat_messages",
      filter: `mission_id=eq.${missionId}`,
    },
    handleNewMessage,
  )
  .subscribe();

// 3. SOS requests (Staff dashboard)
supabase
  .channel("sos")
  .on(
    "postgres_changes",
    {
      event: "*",
      schema: "public",
      table: "sos_requests",
    },
    handleSOSChange,
  )
  .subscribe();
```

#### 3.3.2. Broadcast Channels (Custom Events)

```typescript
// Volunteer location broadcast (từ Redis, không qua DB)
supabase
  .channel("volunteer-locations")
  .on("broadcast", { event: "location-update" }, handleLocationUpdate)
  .subscribe();

// Server broadcast location
supabase.channel("volunteer-locations").send({
  type: "broadcast",
  event: "location-update",
  payload: { volunteer_id, lat, lng, timestamp },
});
```

#### 3.3.3. Realtime Subscription Matrix

| Channel                   | Event         | Subscriber      | Trigger                     |
| ------------------------- | ------------- | --------------- | --------------------------- |
| `missions:{volunteer_id}` | UPDATE        | Volunteer App   | Mission status change       |
| `missions:{requester_id}` | UPDATE        | Victim App      | Volunteer assigned/arriving |
| `chat:{mission_id}`       | INSERT        | Both parties    | New message                 |
| `sos_requests`            | INSERT/UPDATE | Staff Dashboard | New SOS / status change     |
| `donations:{sponsor_id}`  | UPDATE        | Sponsor App     | Donation received/rejected  |
| `volunteer-locations`     | broadcast     | Staff Dashboard | Live map                    |
| `dispatch:{volunteer_id}` | broadcast     | Volunteer App   | New dispatch request        |

---

### 3.4. FCM — Push Notifications

> Đánh thức device khi app không chạy

#### 3.4.1. Notification Triggers

```typescript
// notification-triggers.ts

interface FCMTrigger {
  event: string;
  recipients: string;
  priority: "high" | "normal";
  template: NotificationTemplate;
}

const FCM_TRIGGERS: FCMTrigger[] = [
  // === CRITICAL (high priority) ===
  {
    event: "dispatch.created",
    recipients: "volunteer",
    priority: "high",
    template: {
      title: "🆘 Yêu cầu cứu trợ mới",
      body: "Có người cần giúp đỡ cách bạn {distance}km",
      data: { type: "DISPATCH", mission_id: "{id}" },
    },
  },
  {
    event: "sos.assigned",
    recipients: "victim",
    priority: "high",
    template: {
      title: "✅ Đã tìm được người hỗ trợ",
      body: "{volunteer_name} đang đến, dự kiến {eta} phút",
      data: { type: "MISSION_UPDATE", mission_id: "{id}" },
    },
  },
  {
    event: "mission.volunteer_arrived",
    recipients: "victim",
    priority: "high",
    template: {
      title: "📍 Tình nguyện viên đã đến",
      body: "{volunteer_name} đang ở vị trí của bạn",
      data: { type: "VOLUNTEER_ARRIVED", mission_id: "{id}" },
    },
  },

  // === IMPORTANT (high priority) ===
  {
    event: "donation.received",
    recipients: "sponsor",
    priority: "high",
    template: {
      title: "🎉 Quyên góp đã được tiếp nhận",
      body: "Cảm ơn bạn! +{points} điểm",
      data: { type: "DONATION_RECEIVED", donation_id: "{id}" },
    },
  },
  {
    event: "donation.rejected",
    recipients: "sponsor",
    priority: "high",
    template: {
      title: "❌ Quyên góp bị từ chối",
      body: "Lý do: {reason}",
      data: { type: "DONATION_REJECTED", donation_id: "{id}" },
    },
  },
  {
    event: "chat.new_message",
    recipients: "other_party",
    priority: "high",
    template: {
      title: "💬 Tin nhắn mới",
      body: "{sender_name}: {message_preview}",
      data: { type: "CHAT", mission_id: "{id}" },
    },
  },

  // === NORMAL ===
  {
    event: "mission.completed",
    recipients: "volunteer",
    priority: "normal",
    template: {
      title: "✅ Hoàn thành nhiệm vụ",
      body: "Cảm ơn bạn đã giúp đỡ!",
      data: { type: "MISSION_COMPLETED", mission_id: "{id}" },
    },
  },
  {
    event: "rating.received",
    recipients: "volunteer",
    priority: "normal",
    template: {
      title: "⭐ Bạn nhận được đánh giá mới",
      body: "{score}/5 sao - {comment_preview}",
      data: { type: "RATING", rating_id: "{id}" },
    },
  },
  {
    event: "inventory.low_stock",
    recipients: "hub_staff",
    priority: "normal",
    template: {
      title: "⚠️ Sắp hết hàng",
      body: "{item_name}: còn {quantity} {unit}",
      data: { type: "LOW_STOCK", hub_id: "{hub_id}" },
    },
  },
];
```

#### 3.4.2. FCM Data Structure

```typescript
// Lưu trong PostgreSQL notifications table
interface NotificationRecord {
  id: string;
  user_id: string;
  title: string;
  body: string;
  related_type:
    | "MISSION"
    | "DONATION"
    | "SOS_REQUEST"
    | "AID_REQUEST"
    | "SYSTEM";
  related_id: string;
  is_read: boolean;
  created_at: Date;
}

// Gửi qua FCM
interface FCMPayload {
  token: string; // user.fcm_token
  notification: {
    title: string;
    body: string;
  };
  data: {
    type: string;
    related_id: string;
    click_action: string;
  };
  android: {
    priority: "high" | "normal";
    notification: {
      channel_id: string; // 'emergency' | 'updates' | 'chat'
    };
  };
  apns: {
    payload: {
      aps: {
        sound: "default" | "emergency.wav";
        badge: number;
      };
    };
  };
}
```

#### 3.4.3. Notification Channels (Android)

| Channel ID  | Importance | Sound         | Vibrate | Use Case                   |
| ----------- | ---------- | ------------- | ------- | -------------------------- |
| `emergency` | HIGH       | emergency.wav | Long    | SOS dispatch, critical     |
| `updates`   | DEFAULT    | default       | Short   | Mission updates, donations |
| `chat`      | DEFAULT    | message.wav   | Short   | Chat messages              |
| `system`    | LOW        | none          | No      | System info, promotions    |

---

### 3.5. Cloud Storage — Binary Files

> Lưu trữ images, documents

#### 3.5.1. Bucket Structure

```
aidbridge-storage/
├── avatars/
│   └── {user_id}/
│       └── avatar.jpg              # 200x200, max 500KB
│
├── sos-requests/
│   └── {request_id}/
│       └── evidence-{timestamp}.jpg  # Ảnh hiện trường
│
├── donations/
│   └── {donation_id}/
│       └── items/
│           └── {item_id}.jpg        # Ảnh vật phẩm
│
├── missions/
│   └── {mission_id}/
│       ├── confirmation.jpg          # Ảnh xác nhận hoàn thành
│       └── chat/
│           └── {message_id}.jpg      # Ảnh trong chat
│
└── temp/
    └── {upload_id}.jpg               # Temporary uploads (TTL: 1h)
```

#### 3.5.2. URL Patterns

```typescript
// Stored in PostgreSQL as relative path or full URL
const IMAGE_BASE_URL = "https://storage.aidbridge.vn";

// Examples:
users.avatar_url = "/avatars/uuid/avatar.jpg";
sos_requests.image_url = "/sos-requests/uuid/evidence-1710000000.jpg";
donation_items.image_url = "/donations/uuid/items/item-uuid.jpg";
missions.confirmation_image_url = "/missions/uuid/confirmation.jpg";
chat_messages.image_url = "/missions/uuid/chat/msg-uuid.jpg";
```

#### 3.5.3. Upload Flow

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│ Mobile App  │────▶│ API Server  │────▶│  Cloudinary │
└─────────────┘     └─────────────┘     └─────────────┘
      │                   │                    │
      │ 1. Request        │                    │
      │    presigned URL  │                    │
      │◀──────────────────│                    │
      │                   │                    │
      │ 2. Direct upload  │                    │
      │────────────────────────────────────────▶
      │                   │                    │
      │ 3. Confirm upload │                    │
      │──────────────────▶│                    │
      │                   │ 4. Save URL to DB  │
      │                   │                    │
```

---

## 4. Data Sync Patterns

### 4.1. Write Pattern

```typescript
async function createSOSRequest(data: SOSRequestInput) {
  // 1. Validate
  const validated = await validateSOSRequest(data);

  // 2. Upload image (if any)
  let imageUrl = null;
  if (data.image) {
    imageUrl = await uploadToStorage("sos-requests", data.image);
  }

  // 3. Write to PostgreSQL (source of truth)
  const sosRequest = await supabase
    .from("sos_requests")
    .insert({ ...validated, image_url: imageUrl })
    .select()
    .single();

  // 4. Cache pending request in Redis
  await redis.zadd("sos:pending", {
    score: urgencyScore(sosRequest.urgency_level),
    member: sosRequest.id,
  });

  // 5. Supabase Realtime tự động broadcast (via postgres_changes)

  // 6. Trigger FCM to nearby volunteers (async)
  dispatchQueue.add("dispatch-sos", { sosRequestId: sosRequest.id });

  return sosRequest;
}
```

### 4.2. Read Pattern

```typescript
async function getNearbyVolunteers(lat: number, lng: number, radiusKm: number) {
  // 1. Try Redis first (real-time locations)
  const cached = await redis.georadius(
    "volunteer:locations",
    lng,
    lat,
    radiusKm,
    "km",
    "WITHDIST",
    "ASC",
  );

  if (cached.length > 0) {
    // 2. Enrich with profile data from cache/DB
    const volunteerIds = cached.map((v) => v.member);
    const profiles = await getVolunteerProfiles(volunteerIds);

    return cached.map((v) => ({
      ...profiles[v.member],
      distance_km: v.distance,
      location: { lat: v.lat, lng: v.lng },
    }));
  }

  // 3. Fallback to PostgreSQL (stale but available)
  return await supabase
    .from("volunteer_profiles")
    .select("*, users(full_name, phone_number)")
    .eq("is_online", true)
    .not("current_lat", "is", null);
}
```

### 4.3. Location Sync Pattern

```typescript
// Mobile app: Update location every 10 seconds
async function updateVolunteerLocation(
  volunteerId: string,
  lat: number,
  lng: number,
) {
  const timestamp = Date.now();

  // 1. Always update Redis (real-time)
  await redis.geoadd("volunteer:locations", lng, lat, volunteerId);
  await redis.set(`volunteer:${volunteerId}:last_seen`, timestamp, "EX", 300);

  // 2. Broadcast to Supabase Realtime
  await supabase.channel("volunteer-locations").send({
    type: "broadcast",
    event: "location-update",
    payload: { volunteer_id: volunteerId, lat, lng, timestamp },
  });

  // 3. Batch update PostgreSQL (debounced, every 30 seconds)
  locationBatcher.add(volunteerId, { lat, lng, timestamp });
}

// Background job: Sync Redis → PostgreSQL
const locationBatcher = new Batcher({
  maxSize: 100,
  maxWait: 30000, // 30 seconds
  async flush(updates) {
    const values = updates
      .map((u) => `('${u.id}', ${u.lat}, ${u.lng}, NOW())`)
      .join(",");

    await supabase.rpc("batch_update_volunteer_locations", { values });
  },
});
```

---

## 5. Data Retention & Cleanup

| Data Type                   | Hot Storage | Cold Storage | Delete After |
| --------------------------- | ----------- | ------------ | ------------ |
| Volunteer locations (Redis) | 5 minutes   | -            | Auto-expire  |
| Active sessions (Redis)     | 1 hour      | -            | Auto-expire  |
| Chat messages               | Always      | -            | Never\*      |
| Completed missions          | 1 year      | Archive      | 5 years      |
| Inventory logs              | 6 months    | Archive      | 3 years      |
| Notifications               | 30 days     | -            | 90 days      |
| Temp uploads                | 1 hour      | -            | Auto-delete  |

\*Chat messages có thể archive sau 1 năm nếu cần

---

## 6. Failure Handling

### 6.1. Redis Down

```typescript
async function getVolunteerLocation(volunteerId: string) {
  try {
    // Try Redis
    return await redis.geopos("volunteer:locations", volunteerId);
  } catch (error) {
    // Fallback to PostgreSQL
    const profile = await supabase
      .from("volunteer_profiles")
      .select("current_lat, current_lng")
      .eq("user_id", volunteerId)
      .single();

    return profile ? [profile.current_lng, profile.current_lat] : null;
  }
}
```

### 6.2. FCM Failure

```typescript
async function sendNotification(userId: string, notification: Notification) {
  // 1. Always save to database first
  await supabase.from("notifications").insert({
    user_id: userId,
    title: notification.title,
    body: notification.body,
    related_type: notification.type,
    related_id: notification.relatedId,
  });

  // 2. Try FCM
  try {
    const user = await getUser(userId);
    if (user.fcm_token) {
      await fcm.send({ token: user.fcm_token, ...notification });
    }
  } catch (error) {
    // Log but don't fail - user can see in-app notifications
    logger.warn("FCM failed", { userId, error });
  }
}
```

---

## 7. Summary

```
┌─────────────────────────────────────────────────────────────────┐
│                        DATA FLOW SUMMARY                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  PostgreSQL (Supabase)          Redis                           │
│  ━━━━━━━━━━━━━━━━━━━━          ━━━━━                            │
│  • Source of truth              • Real-time locations            │
│  • All 22 tables                • Session cache                  │
│  • Transactions                 • Dispatch queue                 │
│  • History & audit              • Rate limiting                  │
│                                                                  │
│  Supabase Realtime              FCM                              │
│  ━━━━━━━━━━━━━━━━━              ━━━                              │
│  • Live data sync               • Background push                │
│  • When app is open             • Wake up device                 │
│  • WebSocket                    • Critical alerts                │
│                                                                  │
│  Cloud Storage                                                   │
│  ━━━━━━━━━━━━━                                                   │
│  • Images                                                        │
│  • Avatars                                                       │
│  • Evidence photos                                               │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```
