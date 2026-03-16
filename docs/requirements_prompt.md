# AidBridge — Requirements Prompt (AI-Optimized Reference)

> **Mục đích:** File này là nguồn sự thật duy nhất (single source of truth) dành cho AI/LLM khi thực hiện bất kỳ tác vụ sinh code, thiết kế API, hoặc viết logic cho dự án AidBridge. Đọc toàn bộ trước khi bắt đầu bất kỳ tác vụ nào.

---

## 1. Bối cảnh hệ thống

**Tên dự án:** AidBridge — Disaster Relief Coordinator (DRC)

**Mô hình vận hành:** Kết nối Nạn nhân ↔ Tình nguyện viên ↔ Mạnh thường quân thông qua mạng lưới **Trạm trung chuyển (Hub)** vật lý.

**Vấn đề cốt lõi giải quyết:**
- Mất cân bằng nguồn lực cứu trợ (nơi thừa, nơi thiếu) trong thiên tai.
- Luồng cứu trợ truyền thống qua mạng xã hội là rời rạc, không thể điều phối tập trung.
- Tình nguyện viên không biết ưu tiên trường hợp nào, đi đường nào an toàn.

**Giải pháp:** Nền tảng di động tập trung với:
- Bản đồ thời gian thực hiển thị toàn bộ trạng thái hệ thống.
- Thuật toán AI tự động phân loại độ khẩn cấp và dispatch tình nguyện viên.
- Chuỗi cung ứng QR có kiểm soát từ mạnh thường quân → trạm → nạn nhân.

---

## 2. Kiến trúc tổng quan

```
[Android App (Java 17, MVVM)]
    ↕ REST/WebSocket
[Spring Boot 4 Backend (Java 25)]
    ↕
[PostgreSQL / Supabase + PostGIS]  ←→  [Redis]
    ↕
[Firebase FCM]  ←→  [Google Maps SDK]  ←→  [AI Service]
```

**6 Vai trò người dùng:**

| Role Enum | Tên hiển thị | Mô tả ngắn |
|-----------|-------------|-----------|
| `GUEST` | Người dùng nặc danh | Không cần đăng nhập, chỉ SOS và xem bản đồ |
| `VICTIM` | Nạn nhân | Gửi yêu cầu cứu hộ / tiếp tế, theo dõi trực tiếp |
| `VOLUNTEER` | Tình nguyện viên | Thực hiện nhiệm vụ cứu hộ và giao hàng |
| `SPONSOR` | Mạnh thường quân | Đóng góp hàng cứu trợ qua hệ thống ký gửi QR |
| `STAFF` | Nhân viên trực trạm | Quản lý nhập/xuất kho tại Hub bằng QR scan |
| `ADMIN` | Quản trị viên | Giám sát toàn hệ thống, điều chỉnh thuật toán |

---

## 3. Phân hệ GUEST — Người dùng nặc danh

### 3.1 Quick SOS (Khẩn cấp không cần đăng nhập)

**Mục đích:** Đảm bảo tốc độ tối đa trong tình huống nguy cấp, không yêu cầu tài khoản.

> **UI Flow:** SOS Landing → Quick SOS Form → SOS Success screen. Chi tiết xem `docs/figma_flows/guest_br.md` §S1.2.

**Input bắt buộc:**
- `requester_name` (string): họ tên người gửi.
- `requester_phone` (string): SĐT liên lạc.
- `description` (string): đặc điểm căn nhà, số người, tình trạng sức khỏe.
- `people_count` (int > 0): số người cần cứu.
- `victim_location` (GPS — tự động lấy từ thiết bị; fallback: nhập địa chỉ tay nếu GPS bị từ chối).

**Input tùy chọn:**
- `attachments[]`: hình ảnh đính kèm.

**Hành vi backend:**
- Tạo bản ghi `sos_requests` với `requester_id = NULL` (anonymous).
- `urgency_level` do AI phân loại từ `description` → `CRITICAL | HIGH | MEDIUM | LOW`.
- `ai_summary` được sinh tự động để hiển thị trên Admin dashboard.
- Kích hoạt luồng **Dispatch SOS** (xem Mục 7).

**Ràng buộc:**
- `people_count > 0` (DB level CHECK).
- `requester_name` và `requester_phone` luôn lưu vào bản ghi SOS dù user có đăng nhập hay không (để xử lý đồng nhất).
- Guest **không có** màn hình theo dõi trạng thái sau khi submit. Để xem trạng thái → cần tạo tài khoản.

---

### 3.2 Xem bản đồ cứu trợ công cộng (Public Map)

Bản đồ read-only, không cần đăng nhập. Hiển thị 4 lớp:

| Lớp | Icon | Dữ liệu nguồn | Khi tap vào |
|-----|------|--------------|------------|
| **Hub Marker** | Ngôi nhà xanh / Hộp tiếp tế | `hubs` (status=ACTIVE) | Tên trạm, tóm tắt tồn kho, khoảng cách |
| **Shelter Marker** | Lá cờ / Hình người | `shelters` | Sức chứa hiện tại, tiện ích (điện, nước) |
| **Safe Path** | Polyline màu xanh | `safe_paths` (is_active=TRUE) | — |
| **SOS Heatmap** | Quầng Đỏ/Cam | Mật độ `sos_requests` + `aid_requests` PENDING | — |

**Heatmap logic:** Tính mật độ điểm ghim SOS/AID đang PENDING trong DB. Khu vực có nhiều request trong bán kính hẹp → màu đậm hơn. Tính server-side, client nhận polygon/density data.

**Safe Path caching:** Backend tính polyline từ Google Directions API → cache vào `safe_paths` với TTL 6 giờ. Khi Hub chuyển sang `EMERGENCY` hoặc TTL hết hạn → đánh dấu `is_active = FALSE`. Client đọc từ cache; nếu cache miss → **không hiển thị polyline** (không gọi API realtime để tránh tốn quota).

**Hub popup** (khi tap vào marker): Tên trạm, địa chỉ, tình trạng kho dạng text ngắn ("Còn: Gạo, Nước • Hết: Thuốc"), khoảng cách, nút "Chỉ đường" (deeplink Google Maps).

**Cập nhật dữ liệu bản đồ (GUEST):** Polling mỗi 60 giây hoặc pull-to-refresh. WebSocket chỉ dành cho màn hình đã đăng nhập.

---

### 3.3 Đăng ký / Đăng nhập

**Đăng ký:**
- Fields: `full_name`, `email`, `phone_number`, `password` (plain → bcrypt hash server-side), `role`, checkbox đồng ý điều khoản.
- `role` chỉ gồm 3 lựa chọn: `VICTIM | VOLUNTEER | SPONSOR`. `STAFF` và `ADMIN` **không** tự đăng ký được.
- Ràng buộc: ít nhất một trong `email` hoặc `phone_number` phải có.
- **Password policy:** tối thiểu 8 ký tự, ít nhất 1 chữ hoa, 1 chữ số. Không yêu cầu ký tự đặc biệt.
- Checkbox điều khoản phải được tick; nút submit disabled nếu chưa tick.
- `email` và `phone_number` phải **unique** trong `users`. Trùng → lỗi rõ ràng.
- Sau khi đăng ký: tạo user với `is_verified = FALSE` → gửi OTP 6 chữ số qua Email (ưu tiên) hoặc SMS. OTP TTL lấy từ `system_config['otp.ttl_minutes']` (default 10 phút).
- Tài khoản chỉ hoạt động khi `is_verified = TRUE`.

**Đăng nhập:**
- Input: `email` + `password`.
- Output: `access_token` (JWT ngắn hạn) + `refresh_token` (lưu hash SHA-256 vào `refresh_tokens`).
- Cập nhật `users.fcm_token` khi đăng nhập.
- Sai credentials → thông báo chung (không tiết lộ field nào sai).
- `is_active = FALSE` → "Tài khoản đã bị tạm khóa."
- `is_verified = FALSE` → "Tài khoản chưa xác thực." + option resend OTP.

**OTP Verification:**
- 6 chữ số, auto-advance giữa các ô.
- Resend cooldown: hiển thị countdown, sau khi hết → link "Gửi lại" active.
- Max 5 lần sai → yêu cầu resend (chống brute force).
- Resend → sinh OTP mới, invalidate OTP cũ trong Redis.
- Thành công → `is_verified = TRUE`. Thất bại → giữ `is_verified = FALSE`.

**Refresh Token:** Lưu hash vào `refresh_tokens` (không lưu raw token). Redis blacklist cho JWT revocation tức thì; `refresh_tokens` là durability backup khi Redis restart.

---

## 4. Phân hệ VICTIM — Nạn nhân

> Victim là user đã đăng nhập với `role = VICTIM`.

### 4.1 SOS Khẩn cấp (Authenticated SOS)

> **UI Flow:** Cứu hộ tab → Nhấn SOS → SOS Form → S11 Success. Chi tiết xem `docs/figma_flows/victim_br.md` §S4.

Giống 3.1 nhưng:
- `requester_id` = user ID đang đăng nhập.
- Form **pre-fill** `requester_name` và `requester_phone` từ `users.full_name` / `users.phone_number`. User có thể sửa (chỉ ảnh hưởng `sos_requests`, không UPDATE `users`).
- `people_count = adults + elderly + children` (nhập bằng stepper +/−, tổng ≥ 1).
- Toggle "Gửi hộ người thân" trong form → chuyển sang On-behalf SOS (§4.3).
- Lịch sử SOS gắn với tài khoản, xem lại được tại History screen.

---

### 4.2 Yêu cầu tiếp tế (Aid Request)

> **UI Flow:** Cứu hộ tab → "Nhập bằng văn bản" hoặc "Gửi bằng giọng nói" → Aid Form → S10 Success. Chi tiết xem `docs/figma_flows/victim_br.md` §S5.

**Mô hình dữ liệu:** Header `aid_requests` + line items `aid_request_items`.

**Input:**
- Categories: multi-select chips (Thuốc, Quần áo, Thức ăn, Nước uống, Khác). Chọn parent → chọn leaf sub-category cụ thể.
- `adults_count`, `elderly_count`, `children_count` — nhập bằng **stepper +/−** (ít nhất một > 0).
- `notes` (optional), `location` (GPS auto), `address` (text fallback).

**Danh mục 2 cấp (item_categories tree):**
```
Thuốc          → Cảm/sốt | Tiêu hóa | Băng gạc
Quần áo        → Bộ quần áo | Chăn màn | Áo mưa
Thức ăn        → Gạo | Mì tôm | Đồ hộp
Nước uống      → Nước đóng chai | Sữa | Nước điện giải
Khác           → Tã
```

**Tính toán số lượng tự động:** Backend tính `quantity = adults + elderly + children` cho mỗi leaf item.

**AI Voice Support:** Microphone button trên Aid Form (và trên Cứu hộ tab). Khi chọn voice mode: AI transcribe + classify → điền categories và `notes`. User xem lại và confirm trước khi submit — AI không tự submit.

**Sau khi submit:** Kích hoạt luồng **Dispatch Delivery** — Sequential Batches (xem Mục 7).

---

### 4.3 Gửi SOS hộ người thân (On-Behalf SOS)

> Chi tiết xem `docs/figma_flows/victim_br.md` §S6.

- Bản ghi `sos_requests` có `is_on_behalf = TRUE`.
- `requester_id` = Victim đang đăng nhập (người gửi); `requester_name/phone` pre-fill từ account (read-only).
- User nhập thông tin người cần cứu: `victim_name`, `victim_phone`, `victim_address`.
- `victim_location` được chọn **trên bản đồ tương tác** (kéo pin hoặc search địa chỉ) — không phải GPS thiết bị.
- Dispatch BROADCAST được kích hoạt tại tọa độ `victim_location`, không phải vị trí người gửi.

---

### 4.4 Live Tracking

> Chi tiết xem `docs/figma_flows/victim_br.md` §S9.

- Khả dụng khi mission đang ở `ASSIGNED / PICKING_UP / PICKED_UP / IN_TRANSIT`.
- Vị trí volunteer lấy từ **Redis Geo structure** (không PostgreSQL), push qua WebSocket/STOMP.
- Hiển thị: marker volunteer di chuyển trên map, ETA, tên + avatar volunteer.
- Nút "Nhắn tin với tình nguyện viên" → mở Chat screen (scoped `mission_id`).
- Khi mission → `COMPLETED`: hiển thị rating prompt (1–5 sao + comment tùy chọn) → INSERT `ratings`, UPDATE `volunteer_profiles.avg_rating`.

---

### 4.5 Chatbox với Tình nguyện viên

- Chat được scoped theo `mission_id` (bảng `chat_messages`).
- Hỗ trợ `message_type`: `TEXT` hoặc `IMAGE` (ảnh upload → URL lưu qua `attachments`).
- `CHECK` ràng buộc: TEXT message phải có `message_text`, không có `attachment_id`; IMAGE message phải có `attachment_id`, không có `message_text`.
- Lịch sử chat được lưu sau khi mission hoàn thành.

---

### 4.6 Quản lý hồ sơ & Lịch sử

- **Lịch sử:** Hiển thị cả `sos_requests` (SOS thường + SOS hộ) và `aid_requests`, sắp xếp theo `created_at DESC`.
- Badge trạng thái màu sắc: Xám (PENDING), Cam (DISPATCHING), Xanh dương (đang xử lý), Xanh lá (COMPLETED), Đỏ (CANCELLED).
- Bộ lọc thời gian: mặc định 7 ngày gần nhất, cho phép chọn khoảng khác.
- Offline cache: lưu vào Room DB, xem được khi không có mạng (hiển thị timestamp "Cập nhật lúc: [time]").

---

## 5. Phân hệ VOLUNTEER — Tình nguyện viên

> Volunteer là user đã đăng nhập với `role = VOLUNTEER`. Có bản ghi `volunteer_profiles` kèm theo.

### 5.1 Trạng thái hoạt động (Online/Offline Toggle)

> Chi tiết xem `docs/figma_flows/volunteer_br.md` §S1.

- Toggle ON → `volunteer_profiles.is_online = TRUE`; bắt đầu broadcast GPS vào `current_location`.
- Toggle OFF → `volunteer_profiles.is_online = FALSE`; không nhận dispatch.
- **Không được tắt offline khi đang có mission active** — hiển thị cảnh báo: "Bạn đang có nhiệm vụ đang thực hiện."
- Đăng xuất tự động set `is_online = FALSE`.
- Partial GIST index `idx_volunteers_online WHERE is_online = TRUE` đảm bảo dispatch query nhanh.

---

### 5.2 Tiếp nhận nhiệm vụ tự động (Dispatch)

> Chi tiết xem `docs/figma_flows/volunteer_br.md` §S3 (DELIVERY) và §S8 (RESCUE).

**Volunteer không chủ động chọn đơn** (trừ tính năng 5.8). Hệ thống push nhiệm vụ qua FCM, hiển thị popup interrupt bất kể Volunteer đang ở màn hình nào.

**Countdown theo loại dispatch:**
- RESCUE Broadcast: **30 giây** — popup màu đỏ, alert âm thanh.
- DELIVERY Sequential Top-1: **15 giây**.
- DELIVERY Sequential Batch: **20 giây**.

**Quy tắc accept:**
- **1 mission tại một thời điểm** — Volunteer đang có mission active → popup không hiển thị.
- Backend dùng **Redis lock** để chống race condition khi nhiều Volunteer cùng accept một Batch.
- Nếu lock đã bị chiếm → thông báo "Nhiệm vụ đã được nhận bởi người khác."
- Accept thành công → ghi `accepted_at`, UPDATE `avg_response_seconds`.

**Từ chối / Timeout:**
- "Từ chối" → `dispatch_response = REJECTED` ngay lập tức (không cần đợi countdown).
- Hết thời gian → backend set `dispatch_response = TIMEOUT`.

---

### 5.3 Màn hình nhiệm vụ hiện tại

> Chi tiết xem `docs/figma_flows/volunteer_br.md` §S4 và §S5.

**Thanh trạng thái lộ trình (Progress Bar):**

| Mission Type | Các bước |
|---|---|
| DELIVERY | Đến trạm → Lấy hàng → Đến nhà nạn nhân → Hoàn thành |
| RESCUE | Đến nhà nạn nhân → Hoàn thành |

**Thông tin hiển thị:**
- Điểm đến tiếp theo (Hub hoặc Victim), địa chỉ, khoảng cách realtime.
- Tên, SĐT nạn nhân (tap SĐT → gọi điện).
- Ghi chú vị trí / mô tả căn nhà, số người.
- Danh sách vật phẩm yêu cầu (DELIVERY only).
- **QR nhiệm vụ** (DELIVERY only, bước 1-2): hiển thị để Staff quét tại trạm → xem S7.

**Chat với nạn nhân:**
- DELIVERY: chỉ bật từ trạng thái `PICKED_UP` trở đi.
- RESCUE: bật từ trạng thái `ASSIGNED`.

**Xác nhận hoàn thành:**
- Nút "Xác nhận hoàn thành" chỉ active khi Volunteer trong bán kính **≤ 500m** từ điểm đến (geofence check).
- Chụp ảnh xác nhận → `missions.confirmation_image_url`.
- Sau COMPLETED: `total_tasks_completed += 1`, FCM notify Victim prompt rating.

---

### 5.4 Bản đồ điều hướng

> Chi tiết xem `docs/figma_flows/volunteer_br.md` §S6.

- Hiển thị đường đi tối ưu; ưu tiên `safe_paths` cache, fallback Google Directions API.
- Điểm đến thay đổi theo step: step 1-2 DELIVERY → Hub; step 3 DELIVERY / RESCUE → victim_location.
- GPS Volunteer cập nhật vào **Redis Geo structure** mỗi **3-5 giây** (cho Victim live tracking).
- Nút "Xác nhận hoàn thành" chỉ active khi `ST_Distance(volunteer, destination) ≤ 500m`.
- Khi màn hình này active: override screen timeout (giữ màn hình sáng).

---

### 5.5 Chatbox với Nạn nhân

- Giống 4.5, cùng bảng `chat_messages`, scoped theo `mission_id`.

---

### 5.6 Lịch sử nhiệm vụ & Thống kê

> Chi tiết xem `docs/figma_flows/volunteer_br.md` §S2.

**Lịch sử:**
- Hiển thị tất cả missions (DELIVERY + RESCUE), sort `created_at DESC`.
- Filter tabs: Tất cả / Đang thực hiện / Hoàn thành / Khẩn cấp (RESCUE only).
- Mỗi item: loại nhiệm vụ, địa điểm, ngày thực hiện, badge trạng thái + badge khẩn cấp.

**Dashboard thống kê (S1):**
- `total_tasks_completed`, `avg_rating` (từ `volunteer_profiles`).
- Hiển thị ngay trên Dashboard, không cần navigation riêng.

---

### 5.7 Lưu trữ & Đồng bộ Offline

- Cho phép tải trước (pre-download) bản đồ khu vực vào bộ nhớ thiết bị.
- Thông tin nhiệm vụ hiện tại (địa chỉ, thông tin nạn nhân, vật phẩm) được cache local bằng Room DB.

---

### 5.8 Danh sách nạn nhân gần nhất (Volunteer-Initiated)

- Hệ thống hiển thị danh sách các nạn nhân đang chờ dispatch, sắp xếp theo khoảng cách đến vị trí hiện tại của volunteer.
- Volunteer có thể chủ động chọn một request để nhận nhiệm vụ.
- Tính năng này **bổ sung**, không thay thế luồng dispatch tự động.

---

## 6. Phân hệ SPONSOR — Mạnh thường quân

> Sponsor là user đã đăng nhập với `role = SPONSOR`. Có bản ghi `sponsor_profiles` kèm theo.

### 6.1 Đăng ký đóng góp hàng cứu trợ

> **UI Flow:** Dashboard → Form → [Hub Selection] → QR screen. Chi tiết xem `docs/figma_flows/sponsor_br.md` §S2.

**Input (tạo bản ghi `donations` + `donation_items[]`):**
- `item_category_id` (leaf only), `quantity` (> 0), `unit` (lấy từ `item_categories.unit` — không nhập tay).
- `condition_notes` + `expiry_date` gộp vào textarea "Mô tả vật tư" (optional).
- `estimated_delivery_at` — date picker, phải ≥ ngày hiện tại.
- `attachments[]`: ảnh thực tế (optional).

**Ràng buộc:** UI hiện tại nhập **1 loại vật phẩm/lần** (cần xác nhận xem có nút "Thêm loại" không — xem `sponsor_br.md` Q1). Database hỗ trợ nhiều `donation_items` trên 1 `donations` header.

---

### 6.2 Smart Hub Selection

> Chạy ngay sau khi Sponsor submit form S2. Hiển thị **Top 3 trạm phù hợp** (modal hoặc màn hình riêng — chưa có trong Figma, xem `sponsor_br.md` Q4).

**Thuật toán:**
```sql
SELECT h.id, h.name, ST_Distance(h.location, $sponsor_gps) AS dist
FROM hubs h
JOIN hub_accepted_categories hac ON hac.hub_id = h.id
JOIN hub_inventories hi ON hi.hub_id = h.id
  AND hi.item_category_id = hac.item_category_id
WHERE h.status = 'ACTIVE'
  AND hac.item_category_id IN ($requested_category_ids)
  AND hi.current_quantity <= hi.low_stock_threshold
ORDER BY dist
LIMIT 3;
```

Sponsor chọn một trạm → backend gán `donations.hub_id` + sinh QR.

---

### 6.3 Quy trình ký gửi & Mã QR

> Chi tiết xem `docs/figma_flows/sponsor_br.md` §S4.

**Lifecycle:**
```
REGISTERED → QR_GENERATED → RECEIVED (kho cập nhật) | REJECTED (lý do ghi log)
```

**QR screen behavior:**
- Hiển thị: QR code, mã donation (#DON-XXXXX), loại vật phẩm, số lượng, tên Hub, địa chỉ Hub.
- **Override screen timeout** để QR không bị tắt khi đang cho Staff quét.
- Nút "Lưu mã QR" → lưu QR thành file PNG vào gallery thiết bị.
- QR là **single-use**: sau khi Staff quét thành công → `RECEIVED`, QR hết hiệu lực.
- Sponsor có thể xem lại QR từ S5 (Lịch sử) nếu donation vẫn đang `QR_GENERATED`.

**Thông báo:**
- Khi QR sinh: "Vui lòng mang hàng đến Trạm [Tên]."
- Sau nhập kho thành công: "Đã nhập kho [Số lượng] hàng. Cảm ơn bạn."

---

### 6.4 Theo dõi hành trình quà tặng

- Sau khi hàng được nhập kho (`RECEIVED`), Sponsor có thể xem hàng đã được xuất cho volunteer nào, giao cho nạn nhân nào.
- Tra cứu qua `inventory_logs` → `reference_id` → `missions` → `aid_requests`.

---

### 6.5 Thống kê & Gamification

> Chi tiết xem `docs/figma_flows/sponsor_br.md` §S1 và §S3.

- `sponsor_profiles`: `total_points`, `total_items_donated`, `donation_count`, `badge_level`.
- Badge levels: `BRONZE → SILVER → GOLD → PLATINUM`.
- **Badge thresholds** (tính theo `donation_count`): BRONZE ≥ 1, SILVER ≥ 10, GOLD ≥ 25, PLATINUM ≥ 50. Ngưỡng cụ thể do Admin cấu hình hoặc hardcode — cần xác nhận (xem `sponsor_br.md` Q6).
- Cập nhật nguyên tử cùng transaction lúc donation chuyển sang `RECEIVED`.
- Dashboard S1 hiển thị: số lần tặng, tổng vật phẩm, điểm tích lũy, top donations nổi bật.
- Profile S3 hiển thị: các giao dịch gần nhất (sort `RECEIVED` DESC), badge level.
- **Schema gap (Open Q):** Figma S3 hiển thị tổng giá trị tiền tệ (ví dụ: "15.000.550đ") nhưng `sponsor_profiles` hiện **không có field monetary**. Cần xác nhận với team có thêm `unit_value` vào `donation_items` và `total_value_donated` vào `sponsor_profiles` không (xem `sponsor_br.md` Q2).
- Lịch sử đóng góp (S5) có 4 filter tabs: Tất cả / Chờ nhận (`QR_GENERATED`) / Đã nhận (`RECEIVED`) / Từ chối (`REJECTED`).

---

## 7. Hệ thống Dispatch tự động

> Đây là lõi thuật toán của hệ thống. Chạy mỗi khi có `sos_requests` hoặc `aid_requests` mới ở trạng thái `PENDING`.

### 7.1 Priority Score Formula

$$S = (D \times 40\%) + (R \times 20\%) + (T \times 15\%) + (A \times 15\%) + (E \times 10\%)$$

| Factor | Column nguồn | Cách tính |
|--------|-------------|----------|
| **D** Distance | `volunteer_profiles.current_location` | `ST_Distance` → normalize → score cao = gần hơn |
| **R** Rating | `volunteer_profiles.avg_rating` | Trực tiếp (1.0 – 5.0) |
| **T** Tasks | `volunteer_profiles.total_tasks_completed` | Normalize trong batch hiện tại |
| **A** Avg Response | `volunteer_profiles.avg_response_seconds` | Thấp hơn = score cao hơn |
| **E** Area Exp | `volunteer_area_experiences.experience_score` | `ST_Contains(area, victim_location)` |

**Trọng số (weights) có thể điều chỉnh runtime qua `system_config`:**
- `dispatch.weight.distance` = 0.40
- `dispatch.weight.rating` = 0.20
- `dispatch.weight.tasks` = 0.15
- `dispatch.weight.response_time` = 0.15
- `dispatch.weight.area_experience` = 0.10

---

### 7.2 Luồng Dispatch (5 bước)

**Bước 1: Tiếp nhận & Phân loại**
- Nhận request mới → xác định `victim_location`, `item_categories[]`, `urgency_level`.
- `mission_type`: SOS → `RESCUE`; Aid Request → `DELIVERY`.

**Bước 2: Quét TNV Online**
- Tìm Volunteer có `is_online = TRUE` trong bán kính mở rộng dần:
  - Step 1: 1 km
  - Step 2: 3 km
  - Step 3: 5 km
  - Step 4: 10 km
- Bán kính lấy từ `system_config['dispatch.radius.step{n}_km']`.

**Bước 3: Tính Priority Score**
- Tính S cho từng volunteer tìm được.
- Lấy **Top 10** volunteer điểm cao nhất.

**Bước 4: Dispatch theo loại nhiệm vụ**

**A. RESCUE (SOS Khẩn cấp) → BROADCAST:**
- Logic: Tốc độ là ưu tiên tuyệt đối.
- Gửi FCM đồng thời cho **Top 5 hoặc Top 10** volunteer.
- Ai chấp nhận trước → được phân công (race condition, dùng Redis lock).
- Window: `dispatch.sos.window_seconds` = 30 giây.
- Ghi mỗi volunteer một `dispatch_attempts` row với `dispatch_type = BROADCAST`, `batch_number = 1`.

**B. DELIVERY (Tiếp tế) → SEQUENTIAL BATCHES:**
- Logic: Tối ưu hóa chi phí vận hành, không cần phản hồi ngay trong 5 giây.
- Round 1: Gửi độc quyền cho **#1** (điểm cao nhất). Window: `dispatch.delivery.window_1` = 15 giây.
- Round 2: Nếu timeout → gửi Broadcast cho **#2, #3, #4**. Window: `dispatch.delivery.window_batch` = 20 giây.
- Round 3+: Nếu vẫn không ai nhận → mở rộng bán kính lên step tiếp theo, lặp lại.
- Ghi `dispatch_attempts` với `batch_number` tăng dần theo từng round.

**Bước 5: Kích hoạt nhiệm vụ**
- Volunteer xác nhận → mission `status = ASSIGNED`.
- Ghi `missions.priority_score` (snapshot điểm tại thời điểm assign).
- Cập nhật `volunteer_profiles.avg_response_seconds`.
- Gửi thông báo cho Victim: thông tin TNV (Tên, SĐT, phương tiện).
- Bắt đầu WebSocket Live Tracking.

---

## 8. Phân hệ STAFF — Nhân viên trực trạm

> Staff là user đã đăng nhập với `role = STAFF`. Được assign vào Hub qua bảng `hub_staff`.
> **Bottom nav:** Bản đồ | Kho | Quét | Cá nhân.
> Chi tiết xem `docs/figma_flows/staff_br.md`.

### 8.0 Dashboard & Profile (Cá nhân tab)

> Chi tiết xem `docs/figma_flows/staff_br.md` §S1.

- Hiển thị: tên, avatar, số điện thoại, email, ngày tham gia (`hub_staff.assigned_at`).
- **Toggle "Trạng thái hoạt động"** (Sẵn sàng / Ngoài tuyến): cập nhật trạng thái Staff để hệ thống biết có gửi FCM thông báo Volunteer/Sponsor đến hay không.
- **Schema gap:** `hub_staff` hiện chưa có `is_available` — cần thêm column hoặc dùng cơ chế khác (xem `staff_br.md` Q1).
- Preview tồn kho cuối trang → shortcut đến màn hình Kho.

---

### 8.1 Quản lý Nhập kho (Inbound — Quét tab)

> Chi tiết xem `docs/figma_flows/staff_br.md` §S5 và §S6.

**Kích hoạt:** Khi Sponsor đến trạm với QR ký gửi.

**Quy trình:**
1. Staff mở tab "Quét" → màn hình QR scanner (CameraX + ML Kit).
2. Scan `donations.qr_code_token`. Fallback: nhập mã thủ công.
3. Backend verify: `donations.status = QR_GENERATED`, hub khớp.
4. Màn hình chi tiết lô hàng: tên Sponsor, danh sách items + số lượng, ảnh thực tế, ghi chú tình trạng.
5. Staff kiểm tra thực tế → Xác nhận nhập kho → **atomic transaction:**
   - `donations.status = RECEIVED`, ghi `received_by`, `received_at`.
   - Mỗi `donation_items`: `hub_inventories.current_quantity += quantity` (UPSERT).
   - INSERT `inventory_logs`: `change_type = INBOUND`, `reference_type = DONATION`.
   - Tăng `sponsor_profiles.total_items_donated`, `donation_count`, `total_points`.
6. FCM notify Sponsor.

**Reject case:** "Từ chối" → nhập lý do (bắt buộc) → `donations.status = REJECTED`, `rejection_reason` lưu lại → FCM notify Sponsor kèm lý do.

---

### 8.2 Quản lý Xuất kho (Outbound — Kho tab)

> Chi tiết xem `docs/figma_flows/staff_br.md` §S3 và §S4.
> **⚠ Conflict:** Figma cho thấy Staff dùng **list-based queue** (tìm mission theo tên Volunteer) chứ không quét QR Volunteer. Cần xác nhận với team (xem `staff_br.md` Q4).

**Kích hoạt:** Khi Volunteer đến trạm lấy hàng cho nhiệm vụ DELIVERY.

**Quy trình (Figma-confirmed):**
1. Staff mở tab "Kho" → sub-section "Xuất kho vật tư".
2. Danh sách missions `status = ASSIGNED` tại hub hiện tại (sort FIFO). Tìm kiếm theo tên Volunteer.
3. Tap "Xuất kho" → xem chi tiết: thông tin Volunteer, danh sách items, số lượng yêu cầu vs tồn kho.
4. Nếu item thiếu hàng → cảnh báo đỏ + dialog confirm "Xuất phần có trong kho?".
5. Xác nhận → **atomic transaction:**
   - `missions.status = PICKED_UP`, ghi `picked_up_at`.
   - Mỗi item: `hub_inventories.current_quantity -= quantity`.
   - INSERT `inventory_logs`: `change_type = OUTBOUND`, `reference_type = MISSION`.
   - `CHECK (current_quantity >= 0)` ngăn hàng âm (DB level).
6. WebSocket push → Volunteer: "Đã xác nhận! Đến giao cho nạn nhân."
7. FCM notify Victim: "TNV đã lấy hàng, đang trên đường."

---

### 8.3 Giám sát tồn kho Hub (Kho tab)

> Chi tiết xem `docs/figma_flows/staff_br.md` §S2.

- Xem tồn kho theo từng `item_category`, filter/search theo tên.
- **Badge tồn kho 3 mức:**
  - `current_quantity == 0` → **CẦN BỔ SUNG GẤP** (đỏ, sort lên đầu).
  - `current_quantity <= low_stock_threshold` → **SẮP HẾT** (cam).
  - `current_quantity > low_stock_threshold` → **BÌNH THƯỜNG** (xanh lá).
- Filter tabs dynamic theo các categories đang có hàng tại hub.
- Timestamp "CẬP NHẬT [N] PHÚT TRƯỚC" hiển thị độ tươi dữ liệu.
- Số lượng **không chỉnh sửa trực tiếp** — mọi thay đổi qua luồng nhập/xuất kho.
- Bật/tắt trạng thái Hub (`ACTIVE | INACTIVE | EMERGENCY`).
- Khi Hub → `EMERGENCY`: hệ thống tự động điều phối lại các mission ASSIGNED đang chờ tại trạm này.

---

## 9. Phân hệ ADMIN — Quản trị viên

> Admin là user với `role = ADMIN`. Không có profile riêng ngoài `users`.
> **Bottom nav:** Dashboard | AI | Cá nhân (3 tabs — đơn giản nhất trong tất cả roles).
> Chi tiết xem `docs/figma_flows/admin_br.md`.
> ⚠ **Scope gap:** §9.1 (quản lý user), §9.3 (manual override), §9.4 (broadcast alert) **không xuất hiện trong Figma** — cần xác nhận có trong mobile app hay dành cho web admin panel (xem `admin_br.md` Q4–Q6).

### 9.1 Quản lý người dùng & Phân quyền *(không có trong Figma)*

- Xem danh sách toàn bộ users, filter theo role/status.
- **Chặn tài khoản spam:** cập nhật `users.is_active = FALSE` (soft-block).
- **Phân công Staff vào Hub:** INSERT vào `hub_staff` với `assigned_at`. Hủy phân công: UPDATE `unassigned_at`.
  - Partial UNIQUE index `(hub_id, user_id) WHERE unassigned_at IS NULL` ngăn duplicate active assignments.
  - CHECK: chỉ user `role = STAFF` được phép vào `hub_staff`.

---

### 9.2 Quản lý mạng lưới Trạm trung chuyển

> **UI Flow:** Dashboard → "Quản lý trạm" → S2 Hub List → tap Hub → S3 Hub Detail → "Thêm vật tư" → S4 Form. Chi tiết xem `docs/figma_flows/admin_br.md` §S2, §S3, §S4.

- **Danh sách Hub (S2):** Search theo tên/địa chỉ. Badge status 3 loại: ACTIVE (xanh), EMERGENCY (đỏ), INACTIVE (xám). Nút "Vô hiệu hóa / Kích hoạt" per hub. FAB (+) tạo hub mới.
- **Chi tiết Hub (S3):** Xem inventory per category với badge cảnh báo thiếu hàng. Thống kê tổng nhập kho + lượt cứu trợ. Mini map vị trí Hub.
- **Thêm vật tư vào Hub (S4):** Chọn leaf category → auto-fill unit → nhập `low_stock_threshold` → INSERT `hub_accepted_categories` + `hub_inventories`.
- **Quản lý Shelter & Safe Path:** CRUD cho `shelters` và `safe_paths` (không thể hiện rõ trong Figma — xem backlog).

---

### 9.3 Giám sát điều phối *(không có trong Figma)*

- Bản đồ tổng quan: Hubs, Shelters, SOS active, Aid Requests active, Volunteer online (accessible qua "Xem bản đồ cứu trợ" button ở S1).
- **Manual Override:** Admin assign thủ công volunteer → mission (bỏ qua thuật toán).
- **Điều chỉnh trọng số Priority Score:** qua "Cài đặt hệ thống" ở S7 Profile → UPDATE `system_config` → apply ngay, không restart service.

---

### 9.4 Cảnh báo khẩn cấp *(không có trong Figma)*

- Soạn thông báo, chọn đối tượng (All / role / khu vực), gửi **FCM + Email** batch.
- Lưu vào `notifications` table.

---

### 9.5 AI Analytics & Báo cáo

> **UI Flow:** Bottom nav "AI" tab → S5 AI Summary → S6 AI Chatbot. Chi tiết xem `docs/figma_flows/admin_br.md` §S5 và §S6.

**AI Summary (S5) — auto-generated:**
- Báo cáo ngày/tuần/tháng: hàng hóa phân phối, người được hỗ trợ, alert hubs có vấn đề, activity feed gần nhất.
- Bộ lọc thời gian: hôm nay / tuần này / tháng này.
- "Tạo báo cáo" → export file (CSV/Excel/PDF) async.

**AI Chatbot (S6) — interactive query:**
- Admin đặt câu hỏi bằng ngôn ngữ tự nhiên (text hoặc voice) → AI trả lời data-backed từ DB realtime.
- AI **chỉ read**, không write DB.
- Quick action buttons cho common queries.

---

### 9.6 Profile & System Config (S7)

- Xem: email, join date, role (read-only).
- **"Cài đặt hệ thống":** chỉnh sửa `system_config` keys — dispatch weights (5 tham số), bán kính mở rộng, window seconds, OTP TTL. Apply ngay không restart.
- Đăng xuất → revoke token, blacklist JWT.

---

## 10. Hệ thống Thông báo (Notifications)

> Mọi thông báo đều được: (1) push qua FCM realtime VÀ (2) lưu vào `notifications` table cho in-app inbox.

### Thông báo theo role

**VICTIM:**
| Sự kiện | Nội dung |
|---------|---------|
| SOS/Aid đã ghi nhận | "Yêu cầu đã được ghi nhận, đang tìm TNV phù hợp." |
| Đã kết nối TNV | "Đã tìm thấy TNV [Tên]. Dự kiến đến trạm sau [X] phút." |
| TNV đã lấy hàng | "TNV đã lấy hàng và đang trên đường đến chỗ bạn." |
| TNV sắp đến | "TNV chỉ còn cách bạn 500m, vui lòng chuẩn bị." |
| Hoàn thành | "Đơn cứu trợ hoàn tất. Bạn có muốn đánh giá TNV không?" |

**VOLUNTEER:**
| Sự kiện | Nội dung |
|---------|---------|
| Dispatch mới | "Nhiệm vụ mới: [Loại]. Bạn có [N] giây để chấp nhận." |
| Tin nhắn mới | "Nạn nhân vừa gửi tin nhắn mới cho bạn." |

**SPONSOR:**
| Sự kiện | Nội dung |
|---------|---------|
| QR được tạo | "Mã QR ký gửi đã sẵn sàng. Mang hàng đến Trạm [Tên]." |
| Nhập kho thành công | "Đã nhập kho [Số lượng] hàng. Cảm ơn bạn." |

**STAFF:**
| Sự kiện | Nội dung |
|---------|---------|
| Sponsor sắp đến | "Mạnh thường quân [Tên] đang di chuyển đến trạm để ký gửi [Loại hàng]." |
| Volunteer sắp đến lấy hàng | "TNV [Tên] đã chấp nhận đơn #... và đang đến trạm rút hàng." |
| Tồn kho thấp | "Mặt hàng [Tên] dưới mức tối thiểu. Đã báo Admin." |

**ADMIN:**
| Sự kiện | Nội dung |
|---------|---------|
| Tồn kho hệ thống thấp | "Trạm [Tên] thiếu [Mặt hàng]. Cần điều phối Sponsor thêm." |
| Phát hiện bất thường | "Nghi ngờ giả mạo: [N] báo cáo tại cùng vị trí trong thời gian ngắn." |
| Sự cố tại trạm | "Trạm [Tên] báo sự cố khẩn cấp và tạm ngừng. Hệ thống đã điều phối lại." |

**ALL USERS:**
| Sự kiện | Nội dung |
|---------|---------|
| Trạm mới | "Trạm trung chuyển mới tại [Địa điểm]. Người dân có thể đến nhận nhu yếu phẩm." |

---

## 11. Luồng xử lý đầy đủ (End-to-End Flows)

### Flow A: SOS Khẩn cấp (Guest/Victim → Volunteer → Hoàn thành)

```
1. User nhấn SOS → điền form → submit
2. Backend: tạo sos_requests, gọi AI phân loại urgency
3. Dispatch algorithm: scan volunteers online trong bán kính → tính Priority Score → Broadcast Top 10
4. Volunteer nhận FCM, popup 30 giây → Volunteer A nhận trước
5. Redis lock: chỉ Volunteer A được phép accept → mission status = ASSIGNED
6. Victim nhận FCM: thông tin Volunteer A
7. Volunteer A di chuyển → Live Tracking bắt đầu
8. Volunteer A đến nơi → mark COMPLETED → chụp ảnh xác nhận
9. Victim nhận FCM: hoàn thành → prompt rating
10. Victim rating 1-5 sao → cập nhật volunteer_profiles.avg_rating
```

### Flow B: Tiếp tế (Victim → Sponsor → Staff → Volunteer → Victim)

```
1. Victim submit Aid Request → Dispatch DELIVERY algorithm
2. System tính Priority Score → Round 1: send to #1 volunteer (15s)
3. Nếu timeout → Round 2: broadcast #2,#3,#4 (20s) → ...
4. Volunteer nhận → ASSIGNED
5. Volunteer di chuyển đến Hub → quét QR tại Hub
6. Staff xác nhận xuất kho → hub_inventories -= quantity → mission = PICKED_UP
7. Victim nhận FCM: TNV đã lấy hàng
8. Volunteer di chuyển đến nhà Victim → Live Tracking
9. Giao hàng → Volunteer mark COMPLETED → chụp ảnh
10. Victim rating → avg_rating update
```

### Flow C: Ký gửi (Sponsor → Staff → Kho)

```
1. Sponsor đăng ký lô hàng → Smart Hub Selection gợi ý 3 trạm
2. Sponsor chọn trạm → QR được tạo → donation.status = QR_GENERATED
3. Sponsor mang hàng đến trạm
4. Staff quét QR → xem thông tin lô hàng → kiểm tra thực tế
5. Xác nhận nhập kho (hoặc từ chối):
   ACCEPT: donation.status = RECEIVED + hub_inventories += qty + inventory_log
   REJECT: donation.status = REJECTED + rejection_reason
6. Sponsor nhận FCM xác nhận
```

---

## 12. Ràng buộc nghiệp vụ quan trọng (Business Rules)

| Rule | Enforcement |
|------|------------|
| Anonymous (Guest) SOS không cần account | `sos_requests.requester_id` nullable |
| Tồn kho không bao giờ âm | `CHECK (current_quantity >= 0)` trên `hub_inventories` |
| Chỉ leaf categories được dùng trong stock/request/donation | DB trigger `fn_assert_leaf_category` |
| RESCUE mission không có Hub; DELIVERY phải có Hub | `missions_hub_delivery_only` CHECK |
| Mission chỉ link 1 loại request (SOS hoặc AID, không cả hai) | `missions_type_request_integrity` CHECK |
| Mỗi mission chỉ có 1 rating | `UNIQUE(mission_id)` trên `ratings` |
| Chỉ STAFF được assign vào hub_staff | CHECK constraint trên `hub_staff` |
| Dispatch weights có thể thay đổi live | `system_config` table, Spring Boot reads periodically |
| OTP 6 chữ số, single-use, TTL 10 phút | `is_used` flag + `expires_at`; Redis O(1) lookup |
| OTP blocked sau 5 lần sai | Application-enforced, buộc resend |
| Refresh token lưu hash SHA-256, không lưu raw | Application-enforced |
| Password: min 8 ký tự, 1 chữ hoa, 1 số | Application-enforced (validate trước khi hash) |
| Chỉ 3 role tự đăng ký: VICTIM, VOLUNTEER, SPONSOR | STAFF và ADMIN chỉ do Admin tạo |
| Guest không có màn hình theo dõi trạng thái SOS | UX rule — buộc đăng ký để theo dõi |

---

## 13. Tham chiếu bảng dữ liệu cốt lõi

| Nhóm | Bảng | Mô tả ngắn |
|------|------|-----------|
| Auth | `users`, `otp_verifications`, `refresh_tokens` | Identity + session |
| Profiles | `volunteer_profiles`, `volunteer_area_experiences`, `sponsor_profiles` | Extended role data |
| Infrastructure | `hubs`, `hub_accepted_categories`, `hub_staff`, `shelters`, `safe_paths` | Physical network |
| Catalog | `item_categories` | Two-level aid item tree |
| Inventory | `hub_inventories`, `inventory_logs` | Stock + audit trail |
| Requests | `sos_requests`, `aid_requests`, `aid_request_items` | Victim demands |
| Donations | `donations`, `donation_items` | Sponsor supply |
| Mission | `missions`, `dispatch_attempts` | Work units + algo audit |
| Communication | `chat_messages`, `ratings`, `notifications` | In-app comms |
| Attachments | `attachments` | Polymorphic file store |
| Config | `system_config` | Runtime algorithm params |

---

## 14. Tech Stack Constraints (tóm tắt cho code generation)

**Android (Client):**
- Java 17 only — **không dùng Kotlin**.
- XML Layouts only — **không dùng Jetpack Compose**.
- Architecture: MVVM + Clean Architecture (UI → ViewModel → UseCase → Repository).
- DI: Dagger-Hilt.
- Network: Retrofit2 + OkHttp3.
- Local DB: Room.
- Maps: Google Maps SDK.
- Push: Firebase FCM.
- Camera/QR: CameraX + ML Kit.

**Spring Boot (Backend):**
- Java 25 + Spring Boot 4.x.
- Database: PostgreSQL với PostGIS extension.
- ORM: Spring Data JPA + Hibernate.
- Security: Spring Security + JWT.
- Cache: Redis.
- Realtime: WebSocket/STOMP.
- Geospatial queries: `ST_DWithin`, `ST_Distance`, `ST_Contains`.

**Design patterns áp dụng:** Strategy (dispatch algorithm), Observer (notifications), Repository (data layer), MVVM (presentation layer).

---

## 15. Flow-Specific Business Requirements (Figma-Based)

Các file BR chi tiết theo từng luồng UI (Data Points, Business Rules, State Management, Open Questions):

| Luồng | File |
|-------|------|
| **GUEST** — SOS Landing, Register, Login, OTP, Public Map | `docs/figma_flows/guest_br.md` |
| **VICTIM** — Profile, Cứu hộ tab, SOS Form, Aid Request, On-behalf, Live Tracking, History | `docs/figma_flows/victim_br.md` |
| **VOLUNTEER** — Dashboard, History, Dispatch (DELIVERY+RESCUE), Mission Screens, Navigation, QR | `docs/figma_flows/volunteer_br.md` |
| **SPONSOR** — Dashboard, Registration Form, Hub Selection (missing từ Figma), QR Code, Profile, History | `docs/figma_flows/sponsor_br.md` |
| **STAFF** — Dashboard, Inventory (Kho tab), Outbound Queue, Outbound Detail, Inbound QR Scan, Inbound Detail, Success | `docs/figma_flows/staff_br.md` |
| **ADMIN** — Dashboard, Hub Management, Hub Detail, Add Item Form, AI Summary, AI Chatbot, Profile+Config | `docs/figma_flows/admin_br.md` |
