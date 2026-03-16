# Volunteer Flow — Business Requirements

> **Scope:** 8 màn hình trong luồng VOLUNTEER (user đã đăng nhập với `role = VOLUNTEER`).
> **Nguồn:** Figma `volunteer.png` + `requirements_prompt.md` §5.
> **Điều kiện tiên quyết:** User đã đăng nhập, `is_verified = TRUE`, `is_active = TRUE`, `role = VOLUNTEER`. Bản ghi `volunteer_profiles` tồn tại.

---

## Tổng quan luồng

```
[Login] → S1: Dashboard
           ├── Toggle ONLINE → Eligible for dispatch
           │     ├── FCM push: DELIVERY mission → S3: Dispatch Popup → Accept
           │     │     → S4: Mission Screen (Đến trạm + Lấy hàng)
           │     │           → S7: QR tại Trạm (Staff quét)
           │     │                 → S5: Mission Screen (Đến nhà nạn nhân)
           │     │                       → S6: Navigation Map → Complete
           │     └── FCM push: RESCUE mission  → S8: Dispatch Popup → Accept
           │                 → S5: Mission Screen (Đến nhà nạn nhân)
           │                       → S6: Navigation Map → Complete
           ├── Tap "Lịch sử Nhiệm vụ" → S2: Mission History
           └── Đăng xuất → Guest SOS Landing
```

---

## S1 — Dashboard (Volunteer)

> Màn hình chủ sau khi đăng nhập. Trung tâm điều phối của Volunteer.

### Data Points

**Hiển thị (System):**

| # | Element | Nguồn dữ liệu | Ghi chú |
|---|---------|--------------|---------|
| 1 | Avatar | `users.avatar_url` | Ảnh hoặc placeholder |
| 2 | Họ tên | `users.full_name` | — |
| 3 | Điểm đánh giá | `volunteer_profiles.avg_rating` | Hiển thị sao + số thập phân |
| 4 | Toggle "Trạng thái hoạt động" | `volunteer_profiles.is_online` | ON = xanh lam, OFF = xám |
| 5 | Label trạng thái | — | ON: "Đang nhận nhiệm vụ" / OFF: "Để nhận nhiệm vụ" |
| 6 | Số task hoàn thành | `volunteer_profiles.total_tasks_completed` | Ví dụ: "128" |
| 7 | Mini-map nhiệm vụ hiện tại | `missions` (status active) | Chỉ hiện khi có mission active |
| 8 | Danh sách thông báo gần đây | `notifications` (sorted `created_at DESC`) | Tối đa 5 items gần nhất |
| 9 | Nút "Đăng xuất" | Action | Màu đỏ/cảnh báo |

### Business Rules

- **BR-S1-01:** Toggle Online/Offline → UPDATE `volunteer_profiles.is_online`. Khi bật Online, bắt đầu đẩy GPS updates lên server theo interval (vào `volunteer_profiles.current_location`). Khi tắt → dừng GPS broadcast, không nhận dispatch mới.
- **BR-S1-02:** Nếu Volunteer đang có mission active (`missions.status` không phải COMPLETED/CANCELLED) thì **không được tắt** trạng thái Online. Hiển thị cảnh báo: "Bạn đang có nhiệm vụ đang thực hiện."
- **BR-S1-03:** Danh sách thông báo hiển thị các mission đã nhận và dispatch invitations gần đây. Tap vào → navigate đến màn hình liên quan.
- **BR-S1-04:** Mini-map chỉ hiển thị khi `missions` có row với `volunteer_id = current_user.volunteer_profile_id` và `status IN (ASSIGNED, PICKING_UP, PICKED_UP, IN_TRANSIT)`.
- **BR-S1-05:** "Đăng xuất" → revoke refresh_token, blacklist JWT, set `is_online = FALSE`, navigate về Guest SOS Landing.

### State Management

| Action | State change |
|--------|-------------|
| Toggle ON | `volunteer_profiles.is_online = TRUE`; GPS broadcast bắt đầu |
| Toggle OFF | `volunteer_profiles.is_online = FALSE`; GPS broadcast dừng |
| Đăng xuất | `refresh_tokens.is_revoked = TRUE`; `volunteer_profiles.is_online = FALSE` |

---

## S2 — Lịch sử Nhiệm vụ (Mission History)

### Data Points

**Controls:**

| # | Element | Ghi chú |
|---|---------|---------|
| 1 | Back button | Navigate về S1 (Dashboard) |
| 2 | Filter tabs | "Tất cả" / "Đang thực hiện" / "Hoàn thành" / "Khẩn cấp" |

**Mỗi item trong danh sách:**

| # | Field | Nguồn dữ liệu | Ghi chú |
|---|-------|--------------|---------|
| 1 | Thumbnail ảnh | `missions.confirmation_image_url` hoặc ảnh địa điểm | Map snapshot hoặc ảnh xác nhận |
| 2 | Loại nhiệm vụ | `missions.mission_type` | "Cứu hộ" / "Tiếp tế" |
| 3 | Địa điểm | `sos_requests.victim_address` hoặc `aid_requests.address` | Ví dụ: "Quận 1, TP.HCM" |
| 4 | Ngày thực hiện | `missions.completed_at` hoặc `missions.created_at` | Format: DD/MM/YYYY |
| 5 | Badge trạng thái | `missions.status` ENUM | Xem bảng màu bên dưới |
| 6 | Badge khẩn cấp | `sos_requests.urgency_level` hoặc `aid_requests.urgency_level` | "KHẨN" badge đỏ cam |

**Bảng màu badge trạng thái:**

| Status | Label | Màu |
|--------|-------|-----|
| `ASSIGNED` / `PICKING_UP` | Đang thực hiện | Xanh dương |
| `PICKED_UP` / `IN_TRANSIT` | Đang giao | Xanh dương sáng |
| `COMPLETED` | Hoàn thành | Xanh lá |
| `CANCELLED` | Đã hủy | Đỏ |

### Business Rules

- **BR-S2-01:** Chỉ hiển thị missions thuộc `volunteer_profiles.user_id = current_user.id`.
- **BR-S2-02:** Filter tabs:
  - "Tất cả" → không filter status.
  - "Đang thực hiện" → `status IN (ASSIGNED, PICKING_UP, PICKED_UP, IN_TRANSIT)`.
  - "Hoàn thành" → `status = COMPLETED`.
  - "Khẩn cấp" → `mission_type = RESCUE`.
- **BR-S2-03:** Sắp xếp mặc định: `created_at DESC` (mới nhất lên trên).
- **BR-S2-04:** Tap vào mission đang active → navigate đến S4/S5 (Mission Screen hiện tại). Tap vào mission đã hoàn thành → xem chi tiết (read-only, out of scope sprint này).

### State Management

> Read-only. Không có state change.

---

## S3 — Tiếp nhận Nhiệm vụ — DELIVERY (Dispatch Popup)

> Màn hình xuất hiện khi hệ thống dispatch một nhiệm vụ GIAO HÀNG tới Volunteer.
> Có thể là overlay toàn màn hình (interrupt bất kể Volunteer đang ở màn hình nào).

### Data Points

**Hiển thị (System):**

| # | Element | Nguồn dữ liệu | Ghi chú |
|---|---------|--------------|---------|
| 1 | Header "NHIỆM VỤ MỚI" | — | Text lớn, nổi bật |
| 2 | Countdown timer | Tính từ lúc FCM sent | Đếm ngược realtime (circular progress) |
| 3 | Giây còn lại | `dispatch_attempts.sent_at` + window | Số giây còn lại |
| 4 | Map mini (tuyến đường) | Google Maps Static API | Hub → Victim location |
| 5 | Badge loại nhiệm vụ | `missions.mission_type = DELIVERY` | "TIẾP TẾ" badge xanh/cam |
| 6 | Tên nạn nhân | `aid_requests` → `users.full_name` | — |
| 7 | Địa điểm nạn nhân | `aid_requests.address` | — |
| 8 | Danh sách vật phẩm | `aid_request_items` JOIN `item_categories` | Ví dụ: "Gạo x3, Mì tôm x3" |
| 9 | Trạm lấy hàng | `hubs.name` + `hubs.address` | — |
| 10 | Khoảng cách đến trạm | `ST_Distance(volunteer_location, hub_location)` | Tính lúc dispatch |
| 11 | Ghi chú: sẽ chuyển sang volunteer khác nếu timeout | — | Static text |

**Actions:**

| # | Button | Hành vi |
|---|--------|---------|
| 1 | "Chấp nhận Nhiệm vụ" | POST accept → mission `ASSIGNED` → navigate S4 |
| 2 | "Từ chối" | POST reject → `dispatch_response = REJECTED` → dismiss popup |

### Business Rules

- **BR-S3-01:** Popup hiển thị với countdown dựa trên loại dispatch:
  - Sequential Top-1: **15 giây**.
  - Sequential Batch: **20 giây**.
  - Hết thời gian → tự động ghi `TIMEOUT`, dismiss popup, Volunteer không nhận mission này.
- **BR-S3-02:** "Chấp nhận Nhiệm vụ":
  - Gửi request lên backend.
  - Backend dùng **Redis lock** để đảm bảo chỉ 1 Volunteer được assign (tránh race condition với Batch dispatch).
  - Nếu lock đã bị Volunteer khác chiếm → hiển thị thông báo "Nhiệm vụ đã được nhận bởi người khác" → dismiss.
  - Nếu thành công → `missions.status = ASSIGNED`, ghi `accepted_at`, cập nhật `avg_response_seconds`.
- **BR-S3-03:** "Từ chối" → ghi `dispatch_attempts.response = REJECTED` ngay lập tức, không đợi countdown.
- **BR-S3-04:** Volunteer **không thể** có 2 mission active cùng lúc. Nếu đang có mission, dispatch popup không hiển thị.
- **BR-S3-05:** Map mini là **static image** (Google Maps Static API), không phải interactive map.

### State Management

| Action | Bảng | Thay đổi |
|--------|------|---------|
| Accept | `missions` | `status = ASSIGNED`, `volunteer_id`, `accepted_at` |
| Accept | `dispatch_attempts` | `response = ACCEPTED`, `responded_at` |
| Accept | `volunteer_profiles` | UPDATE `avg_response_seconds` (rolling avg) |
| Accept | Victim FCM | Notify: "Đã tìm thấy Tình nguyện viên [Tên]" |
| Reject | `dispatch_attempts` | `response = REJECTED`, `responded_at` |
| Timeout | `dispatch_attempts` | `response = TIMEOUT` (backend-set) |

---

## S4 — Nhiệm vụ hiện tại — DELIVERY Steps 1-2 (Đến trạm → Lấy hàng)

> Màn hình theo dõi tiến độ nhiệm vụ DELIVERY trong 2 bước đầu.

### Data Points

**Header / Progress Bar:**

| # | Element | Ghi chú |
|---|---------|---------|
| 1 | Tiêu đề "Nhiệm vụ hiện tại" | — |
| 2 | Progress bar (4 bước) | Đến trạm → Lấy hàng → Đến nhà nạn nhân → Hoàn thành |
| 3 | Step hiện tại được highlight | Màu xanh, các bước chưa đến = xám |

**Section 1 — Nhiệm vụ trước mắt (điểm đến tiếp theo):**

| # | Field | Nguồn | Ghi chú |
|---|-------|-------|---------|
| 1 | Loại điểm đến | Logic: step 1 = Hub, step 2+ = Hub | "Đến Trạm [Tên trạm]" |
| 2 | Địa chỉ Hub | `hubs.address` | — |
| 3 | Khoảng cách còn lại | Tính từ `volunteer_profiles.current_location` | Realtime |
| 4 | Nút "Tới đó đến nơi" | → navigate đến S6 (Map) | Blue CTA button |

**Section 2 — Thông tin nạn nhân:**

| # | Field | Nguồn | Ghi chú |
|---|-------|-------|---------|
| 1 | Họ tên nạn nhân | `aid_requests` → `users.full_name` | — |
| 2 | Địa chỉ nạn nhân | `aid_requests.address` | — |
| 3 | SĐT nạn nhân | `users.phone_number` | Tap → gọi điện |
| 4 | Ghi chú | `aid_requests.notes` | Đặc điểm nhà, tình trạng sức khỏe |
| 5 | Danh sách vật phẩm | `aid_request_items` JOIN `item_categories` | "Gạo x3, Mì tôm x3" |
| 6 | Mã QR nhiệm vụ | `missions.qr_code_token` | Hiển thị QR để Staff quét tại trạm |

### Business Rules

- **BR-S4-01:** Tap "Tới đó đến nơi" → navigate đến S6 (Navigation Map), center vào Hub location.
- **BR-S4-02:** Khi Volunteer đến Hub và Staff quét QR (xem S7) → mission step tự động chuyển sang "Lấy hàng" → "Đến nhà nạn nhân", progress bar update.
- **BR-S4-03:** SĐT nạn nhân tap → mở dialer hệ thống (`tel:` intent).
- **BR-S4-04:** QR code hiển thị luôn luôn visible (không cần Volunteer initiate). Staff là người chủ động quét.
- **BR-S4-05:** Không có Chat với nạn nhân ở bước này (chỉ bắt đầu sau khi lấy hàng tại trạm, bước PICKED_UP trở đi).

### State Management

> State chỉ thay đổi khi Staff quét QR (xem S7). Màn hình này read-heavy, update realtime qua polling hoặc WebSocket push.

---

## S5 — Nhiệm vụ hiện tại — Step cuối (Đến nhà nạn nhân / Rescue)

> S4 và S5 là 2 trạng thái của cùng màn hình "Nhiệm vụ hiện tại", nhưng sau khi hàng đã được lấy.
> Màn hình này dùng cho cả DELIVERY (step 3) và toàn bộ RESCUE.

### Data Points

**Progress Bar:**

| Loại nhiệm vụ | Bước hiện tại hiển thị |
|---|---|
| DELIVERY | Bước 3 active: "Đến nhà nạn nhân" |
| RESCUE | Bước 1 active: "Đến nhà nạn nhân" (chỉ có 2 bước) |

**Section 1 — ETA / Thời gian dự kiến đến:**

| # | Field | Nguồn | Ghi chú |
|---|-------|-------|---------|
| 1 | ETA đến nơi | Tính từ Google Maps + current location | "~15 phút" |
| 2 | Khoảng cách còn lại | Realtime | "2.3 km" |

**Section 2 — Thông tin nạn nhân:**

| # | Field | Nguồn | Ghi chú |
|---|-------|-------|---------|
| 1 | Họ tên | `users.full_name` (victim) | — |
| 2 | SĐT | `users.phone_number` (victim) | Tap → gọi điện |
| 3 | Địa chỉ / Mô tả nhà | `sos_requests.description` hoặc `aid_requests.notes` | — |
| 4 | Số người | `people_count` (SOS) hoặc `adults+elderly+children` (Aid) | — |

**Chat (in-screen):**

| # | Element | Ghi chú |
|---|---------|---------|
| 1 | Khung chat hiển thị | Lịch sử tin ngắn từ `chat_messages` (scoped `mission_id`) |
| 2 | Input field / button "Nhắn tin" | Gửi TEXT hoặc IMAGE |

**Actions:**

| # | Button | Loại mission | Hành vi |
|---|--------|-------------|---------|
| 1 | "Tới đó đến nơi" | DELIVERY / RESCUE | S6 (Navigation Map) |
| 2 | "Xác nhận hoàn thành" | Chỉ khi đã đến nơi | Mở xác nhận delivery (chụp ảnh / nhập mã) |

### Business Rules

- **BR-S5-01:** Chat được bật từ bước PICKED_UP (sau khi lấy hàng tại trạm). Với RESCUE: từ lúc mission ASSIGNED.
- **BR-S5-02:** "Xác nhận hoàn thành" chỉ hiện khi Volunteer trong bán kính GPS nhất định từ victim location (ví dụ: ≤ 500m). Điều này ngăn hoàn thành ảo từ xa.
- **BR-S5-03:** Xác nhận hoàn thành = chụp ảnh (`confirmation_image_url`) hoặc nhập mã nạn nhân. Sau khi xác nhận → `missions.status = COMPLETED`.
- **BR-S5-04:** Sau COMPLETED:
  - Cập nhật `volunteer_profiles.total_tasks_completed += 1`.
  - Gửi FCM cho nạn nhân: "Hoàn thành. Bạn có muốn đánh giá Tình nguyện viên?"
  - Navigate về S1 (Dashboard).

### State Management

| Event | Bảng | Thay đổi |
|-------|------|---------|
| Chat message sent | `chat_messages` | INSERT row |
| Xác nhận hoàn thành | `missions` | `status = COMPLETED`, `completed_at`, `confirmation_image_url` |
| Hoàn thành | `volunteer_profiles` | `total_tasks_completed += 1` |
| Hoàn thành | Victim FCM | "Đơn cứu trợ hoàn tất. Đánh giá TNV?" |

---

## S6 — Bản đồ Điều hướng (Navigation Map)

> Màn hình dẫn đường full-screen. Dùng chung cho mọi bước cần di chuyển.

### Data Points

**Hiển thị (System):**

| # | Element | Nguồn | Ghi chú |
|---|---------|-------|---------|
| 1 | Bản đồ full-screen | Google Maps SDK (authenticated) | — |
| 2 | Vị trí hiện tại của Volunteer | GPS device | Marker di chuyển realtime |
| 3 | Tuyến đường (polyline) | Google Directions API hoặc `safe_paths` cache | Màu xanh lam |
| 4 | Marker điểm đến | Hub hoặc Victim location | Label: "Trạm [Tên]" hoặc "Nhà nạn nhân" |
| 5 | Distance + ETA bar | Google Maps | "Còn 2.3 km — ~12 phút" |
| 6 | Nút "Xác nhận hoàn thành" | — | Active khi trong bán kính ≤ 500m từ điểm đến |

### Business Rules

- **BR-S6-01:** Mỗi bước di chuyển, điểm đến thay đổi theo step hiện tại của mission:
  - Step 1 (DELIVERY): Điểm đến = Hub location.
  - Step 3 (DELIVERY) hoặc Step 1 (RESCUE): Điểm đến = victim_location.
- **BR-S6-02:** Ưu tiên tuyến đường từ `safe_paths` cache (is_active=TRUE). Nếu không có → gọi Google Directions API.
- **BR-S6-03:** GPS của Volunteer được cập nhật lên Redis Geo structure (cho Victim live tracking) mỗi 3-5 giây khi màn hình này active.
- **BR-S6-04:** Nút "Xác nhận hoàn thành" chỉ bật khi `ST_Distance(volunteer_location, destination) ≤ 500m`. Nếu xa hơn → disabled với tooltip "Hãy đến gần điểm giao hàng hơn."
- **BR-S6-05:** Back button → navigate về S4 hoặc S5 (tùy bước hiện tại). GPS vẫn tiếp tục broadcast khi rời màn hình này.

### State Management

| Event | Ghi chú |
|-------|---------|
| GPS update | Redis Geo: `GEOADD volunteer_locations {lat} {lon} {volunteer_id}` mỗi 3-5s |
| Tap "Xác nhận hoàn thành" | Navigate về S5 để chụp ảnh / nhập mã xác nhận cuối |

---

## S7 — Xác nhận tại Trạm (QR Pickup Confirmation)

> Volunteer đến Hub, hiển thị QR cho Staff quét. Bước xác nhận lấy hàng.

### Data Points

**Hiển thị (System):**

| # | Element | Nguồn | Ghi chú |
|---|---------|-------|---------|
| 1 | Tiêu đề | — | "Xác nhận lấy hàng" |
| 2 | Hướng dẫn | — | "Xuất trình mã này cho nhân viên trực trạm" |
| 3 | QR Code | `missions.qr_code_token` → render QR | Hiển thị to, rõ, độ sáng tự động tăng |
| 4 | Tên Trạm | `hubs.name` | — |
| 5 | SĐT Trạm | `hubs.contact_phone` | Tap → gọi điện nếu cần liên hệ Staff |
| 6 | Danh sách vật phẩm cần lấy | `aid_request_items` JOIN `item_categories` | Reference list |
| 7 | Trạng thái: "Đang chờ Staff quét..." | Realtime từ WebSocket | Chuyển thành "Đã xác nhận" khi Staff quét |

### Business Rules

- **BR-S7-01:** QR code **chỉ hiển thị ở màn hình Volunteer**. Staff chủ động quét bằng thiết bị của họ (camera/app Staff).
- **BR-S7-02:** Sau khi Staff quét thành công (server-side xử lý):
  - `missions.status = PICKED_UP`, ghi `picked_up_at`.
  - `hub_inventories.current_quantity -= quantity` cho mỗi item (atomic transaction).
  - Màn hình Volunteer nhận WebSocket push → hiển thị popup "Đã xác nhận! Đến giao cho nạn nhân."
  - User tap OK → navigate về S5 (step 3: Đến nhà nạn nhân).
  - Victim nhận FCM: "TNV đã lấy hàng và đang trên đường."
- **BR-S7-03:** Màn hình **giữ active** (không tắt màn hình) để QR luôn hiển thị. Override screen timeout khi màn hình này active.
- **BR-S7-04:** Nếu Staff từ chối xuất kho (tồn kho không đủ hoặc lý do khác) → hiển thị thông báo lỗi, liên hệ Admin. Mission không tiến triển.
- **BR-S7-05:** Nút "Gọi nhân viên trạm" → deeplink `tel:{hubs.contact_phone}`.

### State Management

| Event | Bảng | Thay đổi |
|-------|------|---------|
| Staff quét QR thành công | `missions` | `status = PICKED_UP`, `picked_up_at` |
| | `hub_inventories` | `current_quantity -= quantity` (atomic) |
| | `inventory_logs` | INSERT OUTBOUND log |
| | Victim FCM | "TNV đã lấy hàng, đang trên đường đến chỗ bạn" |

---

## S8 — Tiếp nhận Nhiệm vụ — RESCUE (Dispatch Popup)

> Tương tự S3 nhưng hiển thị thông tin cho nhiệm vụ CỨU HỘ (RESCUE).

### Data Points

**Khác biệt so với S3 (DELIVERY):**

| # | Element | S3 DELIVERY | S8 RESCUE |
|---|---------|-------------|-----------|
| 1 | Badge loại | "TIẾP TẾ" (cam) | "CỨU HỘ" (đỏ) |
| 2 | Countdown | 15s (Top-1) / 20s (Batch) | **30 giây** (Broadcast) |
| 3 | Nội dung | Danh sách vật phẩm + Hub | Mô tả SOS + urgency |
| 4 | Thông tin Hub | Có (Hub phải đến lấy hàng) | **Không** (đến thẳng nơi nạn nhân) |
| 5 | Thông tin SOS | Không | SOS description, `urgency_level`, số người |

**Hiển thị (System — RESCUE specific):**

| # | Element | Nguồn | Ghi chú |
|---|---------|-------|---------|
| 1 | Badge "CỨU HỘ" đỏ | `missions.mission_type = RESCUE` | Nổi bật màu đỏ |
| 2 | Urgency level | `sos_requests.urgency_level` | CRITICAL/HIGH/MEDIUM/LOW với màu tương ứng |
| 3 | Mô tả SOS (AI Summary) | `sos_requests.ai_summary` | Tóm tắt ngắn của AI |
| 4 | Số người cần cứu | `sos_requests.people_count` | — |
| 5 | Khoảng cách đến nạn nhân | Tính từ volunteer location | Không qua Hub |
| 6 | Tên nạn nhân | `sos_requests.victim_name` | Hoặc "Ẩn danh" nếu Guest |

### Business Rules

- **BR-S8-01:** RESCUE dispatch dùng cơ chế **Broadcast**: gửi đồng thời cho Top 5/10 volunteers, ai accept trước được phân công. Countdown = **30 giây**.
- **BR-S8-02:** RESCUE ưu tiên tốc độ → countdown hiển thị visual alert rõ ràng hơn DELIVERY (màu đỏ, có tiếng động nếu thiết bị không tắt tiếng).
- **BR-S8-03:** Accept RESCUE:
  - `missions.status = ASSIGNED`.
  - **Không cần đến Hub** → skip bước "Đến trạm" và "Lấy hàng".
  - Navigate thẳng đến S5 (chỉ có 2 bước: "Đến nhà nạn nhân → Hoàn thành").
- **BR-S8-04:** Nếu nạn nhân là Guest (anonymous), hiển thị tên từ `sos_requests.requester_name`, phone từ `sos_requests.requester_phone`.
- **BR-S8-05:** Logic Redis lock và race condition tương tự BR-S3-02.

### State Management

| Action | Bảng | Thay đổi |
|--------|------|---------|
| Accept RESCUE | `missions` | `status = ASSIGNED`, `accepted_at`, `hub_id = NULL` |
| Accept | Victim/Guest FCM | "TNV [Tên] đang đến chỗ bạn" |
| Reject/Timeout | `dispatch_attempts` | `response = REJECTED / TIMEOUT` |

---

## Tổng hợp trạng thái mission lifecycle

### DELIVERY

```
PENDING → DISPATCHING → ASSIGNED (accept) → PICKING_UP (đến Hub) → PICKED_UP (Staff quét QR) → IN_TRANSIT → COMPLETED
```

### RESCUE

```
PENDING → DISPATCHING → ASSIGNED (accept) → IN_TRANSIT (di chuyển) → COMPLETED
```

---

## Câu hỏi mở cần xác nhận với team

| # | Câu hỏi | Ảnh hưởng |
|---|---------|-----------|
| Q1 | Dispatch popup là **overlay** trên màn hình hiện tại hay **màn hình riêng** trong back stack? | Navigation architecture |
| Q2 | Khi màn hình S7 (QR) active, có cần **giữ sáng màn hình** (override screen timeout)? | Android `FLAG_KEEP_SCREEN_ON` |
| Q3 | Nút "Xác nhận hoàn thành" ở S6: radius 500m có phải là ngưỡng cứng không, hay Admin có thể cấu hình qua `system_config`? | Geofence threshold |
| Q4 | Chat (S5): chỉ bắt đầu sau PICKED_UP, hay ngay từ ASSIGNED? | `chat_messages` access rule |
| Q5 | Volunteer có thể **hủy mission** đang ASSIGNED không? Nếu có → flow hủy thế nào (reassign tự động)? | Mission cancellation flow |
| Q6 | Dashboard S1: danh sách thông báo dưới cùng là `notifications` table hay chỉ filter từ `missions`? | Data source |
