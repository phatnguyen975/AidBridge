# Guest Flow — Business Requirements

> **Scope:** Toàn bộ 8 màn hình trong luồng GUEST (người dùng chưa đăng nhập).
> **Nguồn:** Figma `guest.png` + `requirements_prompt.md` §3.
> **Mục tiêu của luồng:** Cho phép bất kỳ người dùng nào gửi SOS khẩn cấp, xem bản đồ cứu trợ, đăng ký và xác thực tài khoản — không bị chặn bởi bước đăng nhập.

---

## Tổng quan luồng

```
[App khởi động]
      ↓
  S1: SOS Landing
  ├── Nhấn SOS ──────────────→ [SOS Quick Form*] → S8: SOS Success
  ├── Nhấn ĐĂNG NHẬP ─────→  S3: Login
  ├── Nhấn Đăng ký ────────→  S2: Register → S4: OTP
  │                                               ├── Đúng → S5: OTP Success
  │                                               └── Sai  → S6: OTP Failure
  └── Nhấn tab Bản đồ ─────→  S7: Public Map
```

> **(*) Lưu ý thiếu màn hình:** Figma hiện chưa thể hiện màn hình Quick SOS Form (giữa S1 và S8). BR của màn hình này được đặc tả tại Mục S1.2 dưới đây.

---

## S1 — Cứu hộ Khẩn cấp (SOS Landing)

> Màn hình chào đầu tiên khi mở app, hiển thị cho bất kỳ ai chưa đăng nhập.

### Data Points

| # | Field | Loại | Nguồn | Bắt buộc |
|---|-------|------|-------|---------|
| 1 | Trạng thái hệ thống (`APP ĐANG HOẠT ĐỘNG`) | Display | Backend health check | — |
| 2 | Nút SOS (màu đỏ) | Action | — | — |
| 3 | Nút "ĐĂNG NHẬP" | Navigation | — | — |
| 4 | Link "Đăng ký tài khoản mới" | Navigation | — | — |
| 5 | Bottom nav tab: "Bản đồ" | Navigation | — | — |
| 6 | Bottom nav tab: "Cứu hộ" (active) | Navigation | — | — |

### Business Rules

- **BR-S1-01:** Màn hình này là **default screen** khi app chưa có session hợp lệ. Không redirect tự động trừ khi có access_token còn hạn.
- **BR-S1-02:** Nút SOS và tab Bản đồ hoạt động **không yêu cầu đăng nhập**. Đây là tính năng ưu tiên tuyệt đối để tăng tốc trong thiên tai.
- **BR-S1-03:** Trạng thái `APP ĐANG HOẠT ĐỘNG` lấy từ backend health endpoint. Nếu offline → hiển thị cảnh báo nhưng **không chặn** nút SOS.
- **BR-S1-04:** Nhấn nút SOS → mở màn hình Quick SOS Form (S1.2 bên dưới), **không yêu cầu đăng nhập**.

### State Management

> Không có state change nào xảy ra trên màn hình này. Đây là navigation entry point.

---

## S1.2 — Quick SOS Form *(màn hình bị thiếu trong Figma)*

> Màn hình này được suy ra từ requirements §3.1. Cần thiết kế và bổ sung vào Figma.

### Data Points

| # | Field | Loại | Placeholder | Bắt buộc |
|---|-------|------|-------------|---------|
| 1 | Họ tên người gửi (`requester_name`) | Text input | "Họ và tên của bạn" | ✅ |
| 2 | Số điện thoại (`requester_phone`) | Phone input | "Số điện thoại liên lạc" | ✅ |
| 3 | Mô tả tình trạng (`description`) | Textarea | "Đặc điểm căn nhà, số người, tình trạng sức khỏe..." | ✅ |
| 4 | Vị trí GPS (`victim_location`) | Auto-captured | — | ✅ (auto) |
| 5 | Hình ảnh đính kèm (`attachments[]`) | Camera / Gallery | — | ❌ optional |
| 6 | Số người cần cứu (`people_count`) | Number input | "Số người" | ✅ |

### Business Rules

- **BR-S12-01:** GPS được tự động lấy khi mở form. Nếu permission bị từ chối → hiển thị field nhập địa chỉ thay thế.
- **BR-S12-02:** `requester_id = NULL` (anonymous). `requester_name` và `requester_phone` lưu trực tiếp vào `sos_requests`.
- **BR-S12-03:** `people_count > 0` (DB level CHECK). Validate phía client trước khi submit.
- **BR-S12-04:** AI backend phân loại `urgency_level` từ trường `description` → `CRITICAL | HIGH | MEDIUM | LOW`.
- **BR-S12-05:** Sau khi submit thành công → navigate đến S8 (SOS Success).

### State Management

| Bảng | Thay đổi |
|------|---------|
| `sos_requests` | INSERT row mới: `requester_id = NULL`, `status = PENDING`, `urgency_level` do AI gán |
| Dispatch Engine | Trigger ngay: **Broadcast** dispatch đến Top 10 volunteers |

---

## S2 — Đăng ký tài khoản

### Data Points

| # | Field | Loại | Validation | Bắt buộc |
|---|-------|------|-----------|---------|
| 1 | Họ và tên (`full_name`) | Text input | Min 2 ký tự | ✅ |
| 2 | Email (`email`) | Email input | Format hợp lệ, unique trong DB | ✅ |
| 3 | Số điện thoại (`phone_number`) | Phone input | Unique trong DB | ❌ (*) |
| 4 | Mật khẩu (`password`) | Password (masked) | Xem BR-S2-03 | ✅ |
| 5 | Vai trò (`role`) | Radio selector 3 lựa chọn | VICTIM / VOLUNTEER / SPONSOR | ✅ |
| 6 | Đồng ý điều khoản | Checkbox | Phải được tick | ✅ |

> (*) Ít nhất một trong `email` hoặc `phone_number` phải có (DB CHECK constraint). UI hiện tại yêu cầu cả hai → cần xác nhận lại với team.

### Business Rules

- **BR-S2-01:** Chỉ 3 vai trò được tự đăng ký: `VICTIM`, `VOLUNTEER`, `SPONSOR`. Vai trò `STAFF` và `ADMIN` **không** có trong danh sách — chỉ Admin tạo được.
- **BR-S2-02:** `email` và `phone_number` phải là **unique** trong bảng `users`. Nếu trùng → hiển thị lỗi cụ thể: "Email đã được sử dụng."
- **BR-S2-03 (Password Policy):**
  - Độ dài tối thiểu: **8 ký tự**.
  - Phải chứa ít nhất 1 chữ hoa, 1 chữ số.
  - Không yêu cầu ký tự đặc biệt (để giảm ma sát trong tình huống khẩn cấp).
  - Hash bằng **bcrypt** phía server. **Tuyệt đối không log plain-text password.**
- **BR-S2-04:** Checkbox "Điều khoản" phải được tick. Nút "ĐĂNG KÝ NGAY" disabled nếu chưa tick.
- **BR-S2-05:** Submit thành công → tạo user với `is_verified = FALSE`, `is_active = TRUE` → tự động gửi OTP (ưu tiên Email nếu có, fallback sang SMS).
- **BR-S2-06:** Nút "Eye" trên password field toggle `text` / `password` input type.

### State Management

| Bảng | Thay đổi |
|------|---------|
| `users` | INSERT: `role`, `is_verified = FALSE`, `is_active = TRUE`, `password_hash` (bcrypt) |
| `volunteer_profiles` | INSERT nếu `role = VOLUNTEER` (row mới, `is_online = FALSE`) |
| `sponsor_profiles` | INSERT nếu `role = SPONSOR` (row mới, điểm = 0) |
| `otp_verifications` | INSERT OTP mới, `is_used = FALSE`, `expires_at = now() + otp.ttl_minutes` |
| Redis | SET `otp:{user_id}:{type}` với TTL |

> Sau màn hình này: navigate đến **S4 (Nhập OTP)**.

---

## S3 — Đăng nhập

### Data Points

| # | Field | Loại | Placeholder | Bắt buộc |
|---|-------|------|-------------|---------|
| 1 | Email (`email`) | Email input | "Nhập email của bạn" | ✅ |
| 2 | Mật khẩu (`password`) | Password (masked) | "Nhập mật khẩu" | ✅ |
| 3 | Nút "Eye" | Toggle | — | — |
| 4 | Link "Quên mật khẩu?" | Navigation | — | — |

### Business Rules

- **BR-S3-01:** Sai email hoặc mật khẩu → **thông báo chung**: "Email hoặc mật khẩu không đúng." Không tiết lộ field nào sai (chống email enumeration).
- **BR-S3-02:** Tài khoản `is_active = FALSE` (bị Admin khóa) → thông báo: "Tài khoản đã bị tạm khóa. Vui lòng liên hệ hỗ trợ."
- **BR-S3-03:** Tài khoản `is_verified = FALSE` → thông báo: "Tài khoản chưa được xác thực. Kiểm tra email/SMS của bạn." + Option resend OTP.
- **BR-S3-04:** Đăng nhập thành công → server cấp `access_token` (JWT ngắn hạn) + `refresh_token`. Cập nhật `users.fcm_token` cho thiết bị hiện tại.
- **BR-S3-05:** "Quên mật khẩu?" → luồng Password Reset (sử dụng `otp_type = PASSWORD_RESET`). Out of scope cho MVP, ghi nhận để backlog.

### State Management

| Bảng | Thay đổi |
|------|---------|
| `users` | UPDATE `fcm_token` = token thiết bị mới |
| `refresh_tokens` | INSERT row mới (hash SHA-256 của token, không lưu raw) |
| Redis | SET JWT blacklist, SET dispatch state nếu là VOLUNTEER |

---

## S4 — Nhập OTP

### Data Points

| # | Field | Loại | Ghi chú |
|---|-------|------|---------|
| 1 | 6 ô nhập OTP (digit 1–6) | 6x Numeric input | Auto-focus sang ô tiếp theo khi nhập |
| 2 | Nút "XÁC NHẬN" | Button | Disabled cho đến khi đủ 6 chữ số |
| 3 | Countdown "Gửi lại (5s)" | Timer + Link | Hiển thị sau khi hết đếm → active link "Gửi lại" |

### Business Rules

- **BR-S4-01:** OTP gồm **6 chữ số**. Auto-advance focus từ ô này sang ô tiếp theo khi nhập xong.
- **BR-S4-02:** OTP TTL = `system_config['otp.ttl_minutes']` (default 10 phút). Nếu hết hạn → thông báo "Mã đã hết hạn" → navigate sang S6 (Xác thực thất bại).
- **BR-S4-03:** OTP là **single-use**. Sau khi xác thực thành công, `otp_verifications.is_used = TRUE`. Submit lại OTP đã dùng → lỗi.
- **BR-S4-04 (Resend Cooldown):** Countdown bắt đầu từ **60 giây** (UI hiển thị "5s" nhưng nên là 60s để tránh spam — cần xác nhận với team). Trong thời gian đếm, link "Gửi lại" bị disable.
- **BR-S4-05 (Max Attempts):** Sau **5 lần sai** liên tiếp → khóa OTP và yêu cầu resend (chống brute force).
- **BR-S4-06 (Resend):** Khi user nhấn "Gửi lại" → tạo OTP mới trong `otp_verifications`, xóa/invalidate OTP cũ trong Redis, reset countdown.

### State Management

**Trường hợp OTP đúng:**

| Bảng | Thay đổi |
|------|---------|
| `users` | UPDATE `is_verified = TRUE` |
| `otp_verifications` | UPDATE `is_used = TRUE` |
| Redis | DELETE key `otp:{user_id}:{type}` |

> Navigate đến **S5 (Thành công)**.

**Trường hợp OTP sai / hết hạn:**

> Không có state change. Navigate đến **S6 (Xác thực thất bại)**.

---

## S5 — Xác thực thành công

### Data Points

| # | Element | Nội dung hiển thị |
|---|---------|-----------------|
| 1 | Icon | Checkmark màu xanh lá |
| 2 | Tiêu đề | "Xác thực thành công!" |
| 3 | Mô tả | "Tài khoản của bạn đã được kích hoạt. Bắt đầu ngay!" |
| 4 | CTA button | "Bắt đầu ngay" |

### Business Rules

- **BR-S5-01:** Màn hình này chỉ xảy ra sau khi `users.is_verified` được UPDATE thành `TRUE`.
- **BR-S5-02:** Nhấn "Bắt đầu ngay" → navigate đến màn hình chính tương ứng với `role`:
  - `VICTIM` → Màn hình chính Nạn nhân
  - `VOLUNTEER` → Màn hình chính Tình nguyện viên
  - `SPONSOR` → Màn hình chính Mạnh thường quân

### State Management

> Không có thêm state change. Tài khoản đã active ở bước S4.

---

## S6 — Xác thực thất bại

### Data Points

| # | Element | Nội dung hiển thị |
|---|---------|-----------------|
| 1 | Icon | X màu đỏ |
| 2 | Tiêu đề | "Xác thực thất bại" |
| 3 | Mô tả | "Mã OTP không chính xác hoặc đã hết hạn. Vui lòng thử lại." |
| 4 | CTA chính | "THỬ LẠI" |
| 5 | CTA phụ | "Gửi lại mã" |

### Business Rules

- **BR-S6-01:** "THỬ LẠI" → quay về S4 với **6 ô trống** (cleared), countdown reset.
- **BR-S6-02:** "Gửi lại mã" → trigger resend OTP logic (xem BR-S4-06), sau đó navigate về S4.
- **BR-S6-03:** Phân biệt 2 loại lỗi nhưng hiển thị **chung** (không tiết lộ lý do cụ thể) để chống brute-force inference:
  - OTP sai → "Mã OTP không chính xác hoặc đã hết hạn."
  - OTP hết hạn → cùng thông báo trên.

### State Management

> Không có state change. `users.is_verified` vẫn là `FALSE`.

---

## S7 — Bản đồ Cứu trợ công cộng

### Data Points

**Input (User):**

| # | Field | Loại | Ghi chú |
|---|-------|------|---------|
| 1 | Search box | Text input | "Tìm kiếm địa điểm cứu trợ..." |
| 2 | Filter tabs | Toggle (multi-select) | Trạm Trung Chuyển / Điểm trú ẩn / [Tab 3*] |
| 3 | Vị trí GPS thiết bị | Auto-captured | Required để hiển thị khoảng cách |

> (*) Tab 3 bị cắt trong Figma. Theo requirements §3.2, tab thứ 3 có thể là "Tuyến đường an toàn" (Safe Path).

**Hiển thị (System):**

| # | Element | Nguồn dữ liệu | Điều kiện hiển thị |
|---|---------|--------------|-----------------|
| 1 | Hub Marker | `hubs` (status = ACTIVE) | Filter "Trạm Trung Chuyển" bật |
| 2 | Shelter Marker | `shelters` | Filter "Điểm trú ẩn" bật |
| 3 | Safe Path Polyline | `safe_paths` (is_active = TRUE) | Filter tương ứng bật |
| 4 | SOS Heatmap | Density của `sos_requests` + `aid_requests` PENDING | Luôn hiển thị |
| 5 | Hub Popup: Tên trạm | `hubs.name` | Khi tap Hub Marker |
| 6 | Hub Popup: Địa chỉ | `hubs.address` | Khi tap Hub Marker |
| 7 | Hub Popup: Tình trạng kho | `hub_inventories` (JOIN `item_categories`) | Khi tap Hub Marker |
| 8 | Hub Popup: Khoảng cách | Tính realtime từ GPS người dùng | Khi tap Hub Marker |
| 9 | Hub Popup: Nút "Chỉ đường" | Deeplink Google Maps | Khi tap Hub Marker |

### Business Rules

- **BR-S7-01:** Màn hình này **read-only**, không yêu cầu đăng nhập.
- **BR-S7-02:** Khi GPS permission bị từ chối → center map vào vị trí mặc định (ví dụ: tọa độ trung tâm Hà Nội). Không block người dùng.
- **BR-S7-03:** Tình trạng kho Hub hiển thị theo dạng **text ngắn**: "Còn: [items]" + "Hết: [items]". Backend tổng hợp từ `hub_inventories` theo ngưỡng `low_stock_threshold`.
- **BR-S7-04:** Nút "Chỉ đường" → deeplink sang Google Maps với tọa độ Hub. Nếu GG Maps chưa cài → mở browser.
- **BR-S7-05:** Heatmap được tính server-side dựa trên mật độ requests PENDING trong bán kính nhỏ. Client chỉ nhận polygon/density data, không tính phía client.
- **BR-S7-06:** Marker và heatmap **không realtime** (polling mỗi 60 giây hoặc pull-to-refresh). Live WebSocket chỉ dành cho màn hình đã đăng nhập.
- **BR-S7-07:** Safe Path polyline đọc từ cache `safe_paths`. Nếu không có cache (cache miss hoặc `is_active = FALSE`) → không hiển thị polyline cho Hub đó (không gọi API realtime để tránh tốn quota).

### State Management

> Không có state change. Toàn bộ là read operation.

---

## S8 — Gửi yêu cầu thành công (SOS Success)

### Data Points

| # | Element | Nội dung hiển thị |
|---|---------|-----------------|
| 1 | Icon | Checkmark màu xanh dương |
| 2 | Tiêu đề | "Gửi yêu cầu thành công!" |
| 3 | Mô tả | "Yêu cầu của bạn đã được ghi nhận, hệ thống đang tìm tình nguyện viên phù hợp." |
| 4 | CTA | "Xác nhận" |

### Business Rules

- **BR-S8-01:** Màn hình này chỉ hiển thị sau khi backend **xác nhận** tạo `sos_requests` thành công (HTTP 201). Nếu lỗi mạng → hiển thị màn hình lỗi, cho phép retry.
- **BR-S8-02:** Nhấn "Xác nhận" → navigate về **S1 (SOS Landing)**.
- **BR-S8-03:** Guest **không** có màn hình theo dõi trạng thái SOS. Để xem trạng thái → cần đăng ký tài khoản.

### State Management

| Bảng | Trạng thái tại thời điểm hiển thị màn hình này |
|------|----------------------------------------------|
| `sos_requests` | `status = PENDING`, `urgency_level` đã được AI gán |
| Dispatch Engine | **Đang chạy** Broadcast dispatch cho Top 10 volunteers |

---

## Tổng hợp trạng thái dữ liệu sau toàn bộ Guest Flow

| Kịch bản | Bảng được tạo/cập nhật | Trạng thái cuối |
|----------|----------------------|----------------|
| Guest gửi Quick SOS | `sos_requests` (INSERT) | `sos_requests.status = PENDING` |
| Đăng ký thành công + OTP đúng | `users`, `*_profiles`, `otp_verifications`, `refresh_tokens` | `users.is_verified = TRUE`, `is_active = TRUE` |
| Đăng ký + OTP sai | `users` (tồn tại), `otp_verifications` | `users.is_verified = FALSE` — chưa hoạt động |
| Đăng nhập thành công | `users.fcm_token`, `refresh_tokens` | Session active |
| Xem bản đồ | Không có | — (read-only) |

---

## Câu hỏi mở cần xác nhận với team

| # | Câu hỏi | Ảnh hưởng |
|---|---------|----------|
| Q1 | OTP resend countdown là 5 giây hay 60 giây? (UI hiện viết "5s") | BR-S4-04 |
| Q2 | `phone_number` có bắt buộc hay chỉ cần 1 trong email/phone? (UI hiển thị cả hai) | BR-S2, DB constraint |
| Q3 | Tab thứ 3 trên bản đồ là "Tuyến đường an toàn" hay tên khác? | BR-S7 filter logic |
| Q4 | Guest Quick SOS Form cần thiết kế UI (màn hình bị thiếu trong Figma) | S1.2 |
| Q5 | Sau khi Guest gửi SOS thành công, có hiển thị số điện thoại hotline để liên hệ thêm không? | S8 UX |
