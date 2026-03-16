# Victim Flow — Business Requirements

> **Scope:** 11 màn hình trong luồng VICTIM (người dùng đã đăng nhập với `role = VICTIM`).
> **Nguồn:** Figma `victim.png` + `requirements_prompt.md` §4.
> **Điều kiện tiên quyết:** User đã đăng nhập, `is_verified = TRUE`, `is_active = TRUE`, `role = VICTIM`.

---

## Tổng quan luồng

```
[Đăng nhập thành công] → Bottom Nav (3 tabs): Bản đồ | Cứu hộ | Cá nhân
                                                    │            │
                          ┌─────────────────────────┘            └─────────────────────────────┐
                          ▼                                                                     ▼
                   S3: Action Hub (Cứu hộ tab)                                        S1/S2: Hồ sơ (Cá nhân tab)
                   ├── Nhấn SOS ───────────────→ S4: SOS Form → S11: SOS Success               ├── Theo dõi hành trình → S9: Live Tracking
                   ├── Gửi SOS hộ người thân → S6: On-behalf Form → S7: On-behalf Success      └── Lịch sử yêu cầu  → S8: Request History
                   └── Yêu cầu Tiếp tế ───────→ S5: Aid Request Form → S10: Aid Success
```

---

## S1/S2 — Hồ sơ cá nhân (Cá nhân tab)

> Hai trạng thái UI của cùng một màn hình.
> S1 = trạng thái bình thường. S2 = trạng thái khi focus vào "Đăng xuất".

### Data Points

**Hiển thị (System):**

| # | Field | Nguồn dữ liệu | Ghi chú |
|---|-------|--------------|---------|
| 1 | Avatar người dùng | `users.avatar_url` | Hiển thị ảnh hoặc placeholder icon |
| 2 | Họ tên | `users.full_name` | — |
| 3 | Trạng thái tài khoản | `users.is_active` → "ĐANG HOẠT ĐỘNG" | Label dưới tên |
| 4 | Nút "Theo dõi hành trình" | Điều hướng → S9 | Chỉ hiển thị khi có mission đang IN_TRANSIT |
| 5 | Mục "Lịch sử yêu cầu" | Điều hướng → S8 | Luôn hiển thị |
| 6 | Mục "Thông tin cá nhân" | Điều hướng → màn hình chỉnh sửa profile | Luôn hiển thị |
| 7 | Nút "Đăng xuất" | Action | Màu đỏ/cảnh báo |

### Business Rules

- **BR-S12-01:** Nút "Theo dõi hành trình" chỉ active khi có `missions` đang ở trạng thái `ASSIGNED`, `PICKING_UP`, `PICKED_UP`, hoặc `IN_TRANSIT` mà `aid_request.requester_id = current_user`. Nếu không có mission active → button disabled hoặc ẩn.
- **BR-S12-02:** "Đăng xuất" → revoke refresh_token (UPDATE `is_revoked = TRUE`), add access_token vào Redis blacklist, clear local session. Navigate về SOS Landing (màn hình Guest).
- **BR-S12-03:** "Thông tin cá nhân" → màn hình edit profile cho phép thay đổi `full_name`, `phone_number`, `avatar`. **Không** cho phép đổi `email` hoặc `role` trực tiếp (cần luồng riêng).

### State Management

> Không có state change khi xem profile. Chỉ có state change khi thực hiện "Đăng xuất" hoặc "Chỉnh sửa thông tin".

| Action | State change |
|--------|-------------|
| Đăng xuất | `refresh_tokens.is_revoked = TRUE`; Redis JWT blacklist |
| Cập nhật thông tin | `users.full_name / phone_number / avatar_url` UPDATE |

---

## S3 — Action Hub (Cứu hộ tab — default state)

> Màn hình trung tâm cho tất cả hành động của Victim.

### Data Points

**Hiển thị (System):**

| # | Element | Nguồn dữ liệu | Điều kiện |
|---|---------|--------------|-----------|
| 1 | Nút SOS (vòng tròn đỏ, icon !) | — | Luôn hiển thị, nổi bật |
| 2 | Label dưới SOS | — | "Nhấn để gửi yêu cầu cứu hộ" |
| 3 | Mục "Nhập yêu cầu tiếp tế bằng văn bản" | Điều hướng → S5 | Luôn hiển thị |
| 4 | Mục "Gửi yêu cầu tiếp tế bằng giọng nói" | Trigger voice input → S5 | Luôn hiển thị |

### Business Rules

- **BR-S3-01:** Tap vào SOS button → mở S4 (SOS Form) với dữ liệu pre-fill từ tài khoản.
- **BR-S3-02:** "Nhập yêu cầu tiếp tế bằng văn bản" → navigate đến S5 (Aid Request Form) ở chế độ text.
- **BR-S3-03:** "Gửi yêu cầu bằng giọng nói" → mở S5 (Aid Request Form) đồng thời kích hoạt microphone ngay lập tức (AI Voice mode).
- **BR-S3-04:** Bottom nav luôn hiển thị 3 tab: Bản đồ | Cứu hộ (active) | Cá nhân.

### State Management

> Read-only navigation screen. Không có state change.

---

## S4 — Form SOS Khẩn cấp (Authenticated)

> SOS Form với dữ liệu pre-fill từ tài khoản. Khác với Guest: có `requester_id`.

### Data Points

**Input (User):**

| # | Field | DB Column | Pre-fill | Bắt buộc |
|---|-------|-----------|---------|---------|
| 1 | Họ tên | `sos_requests.requester_name` | `users.full_name` | ✅ |
| 2 | Số điện thoại | `sos_requests.requester_phone` | `users.phone_number` | ✅ |
| 3 | Tình trạng sức khỏe / mô tả | `sos_requests.description` | — | ✅ |
| 4 | Đặc điểm căn nhà | Thêm vào `description` | — | ❌ optional |
| 5 | Vị trí (`victim_location`) | `sos_requests.victim_location` | GPS auto | ✅ (auto) |
| 6 | Số người lớn | Tính vào `sos_requests.people_count` | — | ✅ (min 1) |
| 7 | Số người già | Tính vào `sos_requests.people_count` | — | ❌ optional |
| 8 | Trẻ em | Tính vào `sos_requests.people_count` | — | ❌ optional |
| 9 | Toggle "Gửi hộ người thân" | `sos_requests.is_on_behalf` | FALSE | ❌ |

> Toggle "Gửi hộ người thân" khi bật → chuyển sang luồng S6 (On-behalf SOS Form).

### Business Rules

- **BR-S4-01:** Các field Họ tên và SĐT được **pre-fill** từ `users.full_name` và `users.phone_number`. User có thể sửa (để báo hộ hoặc dùng SĐT khác cho trường hợp này). Dữ liệu cứng trong DB là `users` — việc sửa ở đây chỉ ảnh hưởng đến `sos_requests`, không UPDATE `users`.
- **BR-S4-02:** `people_count = adults + elderly + children`. Tổng phải ≥ 1 (DB CHECK).
- **BR-S4-03:** GPS tự động lấy khi vào form. Fallback: nhập địa chỉ tay nếu GPS bị từ chối.
- **BR-S4-04:** `requester_id = current_user.id` (khác Guest = NULL). Lịch sử gắn với tài khoản.
- **BR-S4-05:** Nút Submit → tạo `sos_requests` + kích hoạt Dispatch SOS (BROADCAST). Navigate đến S11.
- **BR-S4-06:** AI phân loại `urgency_level` từ `description` phía backend (không block submit phía client).

### State Management

| Bảng | Thay đổi |
|------|---------|
| `sos_requests` | INSERT: `requester_id = user.id`, `status = PENDING`, `is_on_behalf = FALSE` |
| `attachments` | INSERT nếu có ảnh đính kèm |
| Dispatch Engine | Trigger BROADCAST cho Top 10 volunteers online |

---

## S5 — Yêu cầu Tiếp tế (Aid Request Form)

> Form yêu cầu nhu yếu phẩm. Chỉ dành cho user đã đăng nhập (requester_id bắt buộc).

### Data Points

**Input — Bước 1: Chọn loại hàng (category chips):**

| # | Category | Leaf items |
|---|----------|-----------|
| 1 | Thuốc | Cảm/sốt, Tiêu hóa, Băng gạc |
| 2 | Quần áo | Bộ quần áo, Chăn màn, Áo mưa |
| 3 | Thức ăn | Gạo, Mì tôm, Đồ hộp |
| 4 | Nước uống | Nước đóng chai, Sữa, Nước điện giải |
| 5 | Khác | Tã |

> UI cho phép chọn **nhiều category** cùng lúc (multi-select chips). Khi chọn một category, hiển thị sub-categories của category đó.

**Input — Bước 2: Số lượng người cần hỗ trợ:**

| # | Field | DB Column | Default | Bắt buộc |
|---|-------|-----------|---------|---------|
| 1 | Người trưởng thành | `aid_requests.adults_count` | 0 | ✅ (tổng ≥ 1) |
| 2 | Người già | `aid_requests.elderly_count` | 0 | ❌ |
| 3 | Trẻ em | `aid_requests.children_count` | 0 | ❌ |

> Input type: stepper (+/-) button, không nhập số trực tiếp.

**Input — Bước 3: AI Voice Support (optional):**
- Microphone button để dictate yêu cầu bằng giọng nói.
- AI backend phân loại voice → tự động điền categories và quantity.

**Input — Bổ sung:**

| # | Field | DB Column | Bắt buộc |
|---|-------|-----------|---------|
| 1 | Ghi chú thêm (`notes`) | `aid_requests.notes` | ❌ |
| 2 | Vị trí (`location`) | `aid_requests.location` | ✅ (auto GPS) |

### Business Rules

- **BR-S5-01 (Categories):** Phải chọn ít nhất 1 category. Mỗi category được chọn → hệ thống tạo ít nhất 1 `aid_request_items` row với `item_category_id` của leaf node đại diện; hoặc user chọn leaf sub-category cụ thể.
- **BR-S5-02 (Quantity auto-calc):** Backend tính `quantity` cho mỗi line item dựa trên tổng số người: `quantity = adults + elderly + children`. Ví dụ: 2 người lớn + 1 trẻ em → 3 gói gạo, 3 bộ quần áo.
- **BR-S5-03 (People count):** `(adults + elderly + children) > 0` — DB level CHECK trên `aid_requests`. Validate phía client trước submit.
- **BR-S5-04 (Voice input):** Khi user dùng voice → AI transcribe + classify → điền categories và `notes`. User xem lại và confirm trước khi submit. AI không tự submit.
- **BR-S5-05 (Location):** GPS auto-captured. Fallback nhập địa chỉ tay nếu GPS bị từ chối.
- **BR-S5-06 (Submit):** Nút "Gửi yêu cầu" → tạo `aid_requests` + `aid_request_items[]` → kích hoạt Dispatch DELIVERY (Sequential Batches). Navigate đến S10.
- **BR-S5-07:** Chỉ leaf categories được lưu vào `aid_request_items` (DB trigger `fn_assert_leaf_category` enforce).
- **BR-S5-08:** `urgency_level` trên `aid_requests` được AI phân loại server-side từ `notes` + category combination.

### State Management

| Bảng | Thay đổi |
|------|---------|
| `aid_requests` | INSERT: `requester_id = user.id`, `status = PENDING`, `urgency_level` (AI gán) |
| `aid_request_items` | INSERT N rows (1 per leaf category, quantity = tổng người) |
| Dispatch Engine | Trigger SEQUENTIAL BATCHES dispatch |

---

## S6 — Gửi SOS Hộ người thân (On-Behalf SOS Form)

> Victim đã đăng nhập gửi SOS thay cho người khác ở địa điểm khác.

### Data Points

**Hiển thị (System — pre-filled, read-only):**

| # | Field | Nguồn dữ liệu | Ghi chú |
|---|-------|--------------|---------|
| 1 | Bản đồ tương tác | Google Maps SDK | Cho phép kéo/thả pin để chọn vị trí người cần cứu |
| 2 | Thông tin tài khoản người gửi | `users` record | Hiển thị để xác nhận (read-only) |

**Input bắt buộc (thông tin NGƯỜI CẦN CỨU):**

| # | Field | DB Column | Bắt buộc |
|---|-------|-----------|---------|
| 1 | Họ tên người cần cứu | `sos_requests.victim_name` | ✅ |
| 2 | SĐT người cần cứu | `sos_requests.victim_phone` | ✅ |
| 3 | Địa chỉ / vị trí trên bản đồ | `sos_requests.victim_location` + `victim_address` | ✅ |
| 4 | Mô tả tình trạng | `sos_requests.description` | ✅ |
| 5 | Số người cần cứu | `sos_requests.people_count` | ✅ (> 0) |

**Input tùy chọn:**

| # | Field | DB Column |
|---|-------|-----------|
| 1 | Voice input cho `description` | AI transcribe → `description` |
| 2 | Hình ảnh | `attachments[]` |

### Business Rules

- **BR-S6-01:** Bản ghi `sos_requests` có `is_on_behalf = TRUE`.
- **BR-S6-02:** `requester_id` = current user (Victim đang đăng nhập). `victim_name`, `victim_phone`, `victim_location`, `victim_address` = thông tin người cần cứu thực sự.
- **BR-S6-03:** `requester_name` và `requester_phone` lưu từ `users.full_name` và `users.phone_number` (người gửi). Không cho phép sửa ở màn hình này.
- **BR-S6-04:** Vị trí trên bản đồ là của **người cần cứu**, không phải người gửi. Bản đồ cho phép kéo pin hoặc search địa chỉ.
- **BR-S6-05:** Submit → tạo `sos_requests` với `is_on_behalf = TRUE` → Dispatch BROADCAST → Navigate đến S7.

### State Management

| Bảng | Thay đổi |
|------|---------|
| `sos_requests` | INSERT: `is_on_behalf = TRUE`, `requester_id = user.id`, `victim_name`, `victim_phone`, `victim_location` từ map pin |
| Dispatch Engine | Trigger BROADCAST tại vị trí `victim_location` (không phải vị trí Victim đang ngồi) |

---

## S7 — Thành công — Gửi SOS Hộ

### Data Points

| # | Element | Nội dung |
|---|---------|---------|
| 1 | Icon | Checkmark xanh lá |
| 2 | Tiêu đề | "Gửi SOS hộ thành công!" |
| 3 | CTA | "Đóng" |

### Business Rules

- **BR-S7-01:** "Đóng" → navigate về S3 (Cứu hộ tab).
- **BR-S7-02:** Khác với SOS Success thông thường (S11), màn hình này không có prompt đánh giá volunteer vì Victim là người trung gian báo hộ.

### State Management

> Không có state change. `sos_requests` đã INSERT từ S6.

---

## S8 — Lịch sử yêu cầu (Request History)

### Data Points

**Header:**
| # | Element | Ghi chú |
|---|---------|---------|
| 1 | Tiêu đề "Lịch sử yêu cầu" | — |
| 2 | Navigation back | Về S1/S2 (Cá nhân tab) |

**Mỗi item trong danh sách:**

| # | Field | Nguồn dữ liệu | Ghi chú |
|---|-------|--------------|---------|
| 1 | Loại yêu cầu | `sos_requests.type` hoặc `aid_requests` | "Emergency SOS" / "Food Request" / "SOS for Relative" |
| 2 | Tóm tắt nội dung | `description` hoặc tên items | Ví dụ: "Mì tôm, Gạo" |
| 3 | Ngày gửi | `created_at` | Format: DD/MM/YYYY |
| 4 | Địa điểm | `address` hoặc reverse-geocode từ GPS | Ví dụ: "Quận 1, TP.HCM" |
| 5 | Trạng thái badge | `status` ENUM | Màu sắc theo trạng thái (xem bảng dưới) |

**Bảng màu trạng thái badge:**

| Status | Label hiển thị | Màu |
|--------|---------------|-----|
| `PENDING` | Đang chờ | Xám |
| `DISPATCHING` | Đang tìm TNV | Cam |
| `ASSIGNED` / `PICKING_UP` / `IN_TRANSIT` | Đang xử lý | Xanh dương |
| `COMPLETED` | Hoàn thành | Xanh lá |
| `CANCELLED` | Đã hủy | Đỏ |

### Business Rules

- **BR-S8-01:** Chỉ hiển thị requests của `current_user` (`requester_id = user.id`).
- **BR-S8-02:** Offline cache: danh sách được lưu vào Room DB, xem được khi không có mạng. Hiển thị timestamp "Cập nhật lúc: [time]" nếu đang offline.
- **BR-S8-03:** Bộ lọc thời gian: mặc định 7 ngày gần nhất. Cho phép chọn khoảng thời gian khác.
- **BR-S8-04:** Tap vào một item → xem chi tiết (địa chỉ, items, trạng thái cập nhật gần nhất). Out of scope chi tiết cho sprint này, ghi nhận để backlog.
- **BR-S8-05:** Hiển thị cả `sos_requests` (SOS thường + SOS hộ) và `aid_requests` trong cùng một list, sắp xếp theo `created_at DESC`.

### State Management

> Read-only. Không có state change.

---

## S9 — Theo dõi hành trình (Live Tracking)

### Data Points

**Hiển thị (System — realtime):**

| # | Element | Nguồn dữ liệu | Ghi chú |
|---|---------|--------------|---------|
| 1 | Bản đồ với marker volunteer | Redis Geo structure | Cập nhật realtime qua WebSocket |
| 2 | Avatar và tên volunteer | `users.full_name`, `users.avatar_url` | Hiển thị trên bottom panel |
| 3 | Tên volunteer (trên map) | `users.full_name` | Label trên marker di chuyển |
| 4 | ETA / khoảng cách còn lại | Tính từ vị trí volunteer đến victim_location | "Còn X phút" hoặc "Còn X km" |
| 5 | Nút "Nhắn tin với tình nguyện viên" | Điều hướng → Chat screen | Active khi mission đang IN_TRANSIT |

### Business Rules

- **BR-S9-01:** Màn hình này chỉ khả dụng khi có mission active ở trạng thái `ASSIGNED`, `PICKING_UP`, `PICKED_UP`, hoặc `IN_TRANSIT` liên kết với request của Victim.
- **BR-S9-02:** Vị trí volunteer lấy từ **Redis Geo structure** (không từ PostgreSQL) qua WebSocket/STOMP. Mỗi khi volunteer update vị trí → server push xuống client ngay lập tức.
- **BR-S9-03:** Khi mission chuyển sang `COMPLETED` hoặc `CANCELLED` → màn hình tự động navigate về S3 (Cứu hộ tab) hoặc hiển thị popup kết quả.
- **BR-S9-04:** "Nhắn tin với tình nguyện viên" → mở Chat screen (scoped theo `mission_id`, bảng `chat_messages`). Hỗ trợ TEXT và IMAGE.
- **BR-S9-05:** Nếu mission chuyển về `COMPLETED`: hiển thị prompt "Đánh giá tình nguyện viên" (1–5 sao + comment tùy chọn) → INSERT vào `ratings`.

### State Management

**Khi Victim gửi rating:**

| Bảng | Thay đổi |
|------|---------|
| `ratings` | INSERT: `mission_id`, `rater_id = user.id`, `ratee_id = volunteer_profile_id`, `score` |
| `volunteer_profiles` | UPDATE `avg_rating` (rolling average) |

---

## S10 — Thành công — Yêu cầu Tiếp tế

| # | Element | Nội dung |
|---|---------|---------|
| 1 | Icon | Checkmark xanh dương |
| 2 | Tiêu đề | "Gửi yêu cầu thành công!" |
| 3 | Mô tả | "Yêu cầu của bạn đã được ghi nhận, hệ thống đang tìm tình nguyện viên phù hợp." |
| 4 | CTA | "Xác nhận" |

### Business Rules

- **BR-S10-01:** Chỉ hiển thị sau HTTP 201 từ backend (tạo `aid_requests` thành công).
- **BR-S10-02:** "Xác nhận" → navigate về S3 (Cứu hộ tab).
- **BR-S10-03:** Victim có thể theo dõi tiến trình qua Notifications (FCM) hoặc vào S9 khi volunteer được assign.

### State Management

> `aid_requests.status = PENDING`, Dispatch Engine đang chạy Sequential Batches.

---

## S11 — Thành công — SOS Khẩn cấp

| # | Element | Nội dung |
|---|---------|---------|
| 1 | Icon | Checkmark xanh dương |
| 2 | Tiêu đề | "Gửi SOS thành công!" |
| 3 | CTA | "Đóng" |

### Business Rules

- **BR-S11-01:** "Đóng" → navigate về S3 (Cứu hộ tab).
- **BR-S11-02:** Chỉ hiển thị sau HTTP 201 từ backend.
- **BR-S11-03:** Dispatch BROADCAST đang chạy background. Victim nhận FCM notification khi Volunteer được assign.

### State Management

> `sos_requests.status = PENDING`, Dispatch BROADCAST đang chạy.

---

## Tổng hợp trạng thái dữ liệu sau toàn bộ Victim Flow

| Kịch bản | Bảng | Trạng thái cuối |
|----------|------|----------------|
| Gửi SOS (authenticated) | `sos_requests` | `status = PENDING` → `DISPATCHING` → `ASSIGNED` → ... → `COMPLETED` |
| Gửi SOS hộ người thân | `sos_requests` | `is_on_behalf = TRUE`, `status = PENDING` → flow tương tự |
| Gửi Aid Request | `aid_requests` + `aid_request_items` | `status = PENDING` → `DISPATCHING` → `ASSIGNED` → `PICKED_UP` → `IN_TRANSIT` → `COMPLETED` |
| Đánh giá Volunteer | `ratings` + `volunteer_profiles` | `ratings` INSERT; `avg_rating` UPDATE |
| Đăng xuất | `refresh_tokens` | `is_revoked = TRUE` |

---

## Câu hỏi mở cần xác nhận với team

| # | Câu hỏi | Ảnh hưởng |
|---|---------|-----------|
| Q1 | Khi tap SOS Button → form expand inline trong tab hay navigate sang màn hình mới? | Navigation stack UX |
| Q2 | Aid Request categories: chọn parent → tự động chọn tất cả leaf, hay user phải chọn từng leaf? | `aid_request_items` generation logic |
| Q3 | Badge "Đã lưu" trong lịch sử là trạng thái gì? (`PENDING`? hay offline cache?) | Status enum mapping |
| Q4 | Live Tracking khi cả SOS và Aid Request đều active cùng lúc → hiển thị tracking nào? (1 user có thể có nhiều request active?) | Mission scoping rule |
| Q5 | Sau khi mission `COMPLETED`, rating prompt là popup hay màn hình riêng? | UX flow |
| Q6 | Victim có thể **hủy** một request đang PENDING không? Nếu có → thêm nút Cancel vào History? | Cancellation flow |
