# Admin Flow — Business Requirements

> **Scope:** 7 màn hình trong luồng ADMIN (user đã đăng nhập với `role = ADMIN`).
> **Nguồn:** Figma `admin.png` + `requirements_prompt.md` §9.
> **Điều kiện tiên quyết:** User đã đăng nhập, `is_verified = TRUE`, `is_active = TRUE`, `role = ADMIN`.

---

## Tổng quan luồng

```
[Login] → S1: Admin Dashboard (Dashboard tab — default)
          ├── "Xem bản đồ cứu trợ" → [Public Map / full-screen map]
          ├── "Quản lý trạm" → S2: Quản lý trạm trung chuyển
          │         ├── Tap hub item → S3: Chi tiết trạm
          │         │         └── "Thêm vật tư" → S4: Thêm vật tư
          │         └── FAB (+) → [Form tạo hub mới — not shown in Figma]
          ├── Bottom nav "AI" tab → S5: Tóm tắt AI
          │         └── [Chat button / sub-tab] → S6: AI Chatbot
          └── Bottom nav "Cá nhân" tab → S7: Trang cá nhân
                    └── "Cài đặt hệ thống" → [System Config screen — not shown]
```

**Bottom Navigation (3 tabs):** Dashboard | AI | Cá nhân

> **Ghi chú scope Figma:** Các chức năng §9.1 (quản lý user), §9.3 (manual override), §9.4 (broadcast alert) **không xuất hiện trong Figma**. Có thể chưa thiết kế UI hoặc truy cập qua màn hình bản đồ / web admin panel. Xem Open Questions.

---

## S1 — Admin Dashboard (Dashboard tab)

> Tổng quan toàn hệ thống. Điểm khởi đầu sau đăng nhập.

### Data Points

**Header:**

| # | Element | Ghi chú |
|---|---------|---------|
| 1 | Logo "DRC Admin" | Branding |
| 2 | Nút "Xem bản đồ cứu trợ" | Navigate → full-screen system map |
| 3 | Nút "Quản lý trạm" | Navigate → S2 |

**Thống kê tổng quan (4 cards):**

| # | Metric | Nguồn dữ liệu | Ghi chú |
|---|--------|--------------|---------|
| 1 | Tổng số trạm | `COUNT(hubs)` | Tất cả trạng thái |
| 2 | Tình nguyện viên | `COUNT(volunteer_profiles WHERE is_online = TRUE)` hoặc tổng active | "1.2K" — cần xác nhận tính online hay total (Q1) |
| 3 | Nhiệm vụ hôm nay | `COUNT(missions WHERE DATE(created_at) = today)` | "156" |
| 4 | Vật phẩm phân phối | `SUM(inventory_logs.quantity_changed WHERE change_type = OUTBOUND AND today)` | "45K units" |

**Biểu đồ:**

| # | Element | Nguồn dữ liệu | Ghi chú |
|---|---------|--------------|---------|
| 1 | Tiêu đề | — | "Số lượng hàng theo loại" |
| 2 | Bar chart | `SUM(hub_inventories.current_quantity) GROUP BY item_categories.parent` | 4 cột: Y TẾ, THỰC PHẨM, NƯỚC, QUẦN ÁO |

### Business Rules

- **BR-S1-01:** Màn hình mặc định sau login. Stats load từ server — không query realtime client side (pre-aggregated hoặc cached).
- **BR-S1-02:** Bar chart hiển thị tổng tồn kho **toàn hệ thống** (tất cả hubs), group theo parent `item_categories`.
- **BR-S1-03:** "Xem bản đồ cứu trợ" → mở bản đồ tương tự Public Map nhưng có thêm layer volunteer online, missions active, SOS realtime.
- **BR-S1-04:** 4 stat cards là **snapshot** (có thể delay 5–15 phút), không phải realtime WebSocket.

### State Management

> Read-only. Không có state change.

---

## S2 — Quản lý trạm trung chuyển (Hub Management)

> Danh sách tất cả Hub toàn hệ thống. Entry point quản lý hạ tầng trạm.

### Data Points

**Controls:**

| # | Element | Ghi chú |
|---|---------|---------|
| 1 | Search bar | "Tìm kiếm theo tên trạm hoặc địa điểm..." |
| 2 | Stats summary | "Tổng số trạm: [N]" | "Đang hoạt động: [M]" |
| 3 | FAB (+) button | Tạo Hub mới — giao diện form tạo hub (không thể hiện trong Figma) |

**Mỗi Hub item trong danh sách:**

| # | Field | Nguồn dữ liệu | Ghi chú |
|---|-------|--------------|---------|
| 1 | Badge trạng thái | `hubs.status` | Xem bảng màu bên dưới |
| 2 | Tên Hub | `hubs.name` | — |
| 3 | Địa chỉ | `hubs.address` | — |
| 4 | Quản lý bởi / Tồn kho | `users.full_name` (staff) hoặc `SUM(hub_inventories)` | Xem BR-S2-03 |
| 5 | Nút "Chỉnh sửa" | Action | Navigate → S3 (chi tiết/edit) |
| 6 | Nút "Vô hiệu hóa" / "Kích hoạt" | Action | Toggle `hubs.status` |

**Bảng màu badge trạng thái Hub:**

| `hubs.status` | Màu | Ghi chú |
|--------------|-----|---------|
| `ACTIVE` | Xanh dương | Đang hoạt động bình thường |
| `EMERGENCY` | Đỏ/Cam | Trạm báo sự cố khẩn cấp |
| `INACTIVE` | Xám | Tạm ngừng hoạt động |

### Business Rules

- **BR-S2-01:** Hiển thị toàn bộ hubs — không giới hạn theo hub_id của Staff (Admin xem tất cả).
- **BR-S2-02:** Search filter theo `hubs.name` hoặc `hubs.address` (client-side hoặc server query).
- **BR-S2-03:** Label sub-info: Hub ACTIVE → hiển thị "Ql bởi: [tên Staff]" (`hub_staff JOIN users`). Hub EMERGENCY/INACTIVE → hiển thị "Tồn kho: [N] units" (`SUM hub_inventories`).
- **BR-S2-04:** "Vô hiệu hóa" → `hubs.status = INACTIVE`. Khi `INACTIVE`: hệ thống không dispatch hàng đến hub này, Sponsor không thể chọn hub này trong Smart Hub Selection.
- **BR-S2-05:** FAB (+) → form tạo Hub mới (input: name, address, GPS location, contact_phone, capacity) → INSERT `hubs`.

### State Management

| Action | Bảng | Thay đổi |
|--------|------|---------|
| Vô hiệu hóa Hub | `hubs` | `status = INACTIVE` |
| Kích hoạt Hub | `hubs` | `status = ACTIVE` |

---

## S3 — Chi tiết trạm

> Xem và chỉnh sửa thông tin đầy đủ của một Hub, bao gồm tồn kho và cấu hình categories.

### Data Points

**Thông tin Hub:**

| # | Field | Nguồn dữ liệu | Ghi chú |
|---|-------|--------------|---------|
| 1 | Tên Hub | `hubs.name` | — |
| 2 | Badge trạng thái | `hubs.status` | ĐANG HOẠT ĐỘNG / KHẨN CẤP / TẠM DỪNG |
| 3 | Địa chỉ | `hubs.address` | — |
| 4 | Tên Staff quản lý | `hub_staff JOIN users` (active assignment) | — |
| 5 | Bản đồ mini | Google Maps Static API | Hiển thị vị trí Hub trên map thumbnail |

**Section "Mức dự trữ" (Inventory per category):**

| # | Field | Nguồn dữ liệu | Ghi chú |
|---|-------|--------------|---------|
| 1 | Tiêu đề | — | "Mức dự trữ — [N] danh mục" |
| 2 | Tên danh mục | `item_categories.name_vi` | — |
| 3 | Số lượng tồn kho | `hub_inventories.current_quantity` + `unit` | "Tồn kho: [N] units" |
| 4 | Badge cảnh báo | So sánh vs `low_stock_threshold` | "Ổn" (xanh) / "CẢO BÁO THIẾU" (đỏ/cam) |
| 5 | Nút "Chỉnh sửa" (per category) | Action | Sửa `low_stock_threshold` hoặc thêm vật tư |
| 6 | Nút "Thêm vật tư" | Action | Navigate → S4 |

**Thống kê Hub:**

| # | Metric | Nguồn dữ liệu | Ghi chú |
|---|--------|--------------|---------|
| 1 | Tổng nhập kho | `SUM(inventory_logs WHERE hub_id AND change_type = INBOUND)` | Tổng lịch sử |
| 2 | Lượt cứu trợ | `COUNT(missions WHERE hub_id AND status = COMPLETED)` | — |

### Business Rules

- **BR-S3-01:** Badge "CẢO BÁO THIẾU" (warning) hiển thị khi `current_quantity <= low_stock_threshold`. Tương tự badge "SẮP HẾT" trong Staff view nhưng từ góc Admin.
- **BR-S3-02:** "Thêm vật tư" → mở S4 để khai báo một category mới cho hub này (`hub_accepted_categories` + `low_stock_threshold`).
- **BR-S3-03:** "Chỉnh sửa" per category → inline edit (hoặc S4 ở chế độ edit) để cập nhật `low_stock_threshold`.
- **BR-S3-04:** Bản đồ mini → tap → mở full-screen map center vào hub location.

### State Management

> Phần lớn read-only. State thay đổi chỉ khi Admin chỉnh sửa threshold hoặc thêm category.

---

## S4 — Thêm vật tư (Add Item Category to Hub)

> Form khai báo một loại vật tư mới được chấp nhận tại Hub, kèm ngưỡng tồn kho tối thiểu.

### Data Points

**Input (User):**

| # | Field | DB Column | UI Control | Bắt buộc |
|---|-------|-----------|-----------|---------|
| 1 | Loại vật tư | `hub_accepted_categories.item_category_id` | Dropdown — chỉ leaf categories | ✅ |
| 2 | Mức tối thiểu | `hub_inventories.low_stock_threshold` | Number input | ✅ (> 0) |
| 3 | Đơn vị | `item_categories.unit` (readonly, auto-fill từ category) | Dropdown / label | ✅ (auto) |
| 4 | Ghi chú | Memo field (không persist vào DB chính) | Textarea | ❌ optional |

**Action:**

| # | Button | Hành vi |
|---|--------|---------|
| 1 | "Lưu vật tư" | INSERT/UPDATE `hub_accepted_categories` + set `low_stock_threshold` |
| 2 | Back | Về S3, không lưu |

### Business Rules

- **BR-S4-01:** Dropdown "Loại vật tư" chỉ hiển thị **leaf categories** (`item_categories.is_leaf = TRUE`). Trigger `fn_assert_leaf_category` enforce ở backend.
- **BR-S4-02:** "Đơn vị" tự động điền từ `item_categories.unit` sau khi chọn category. Không nhập tay.
- **BR-S4-03:** "Mức tối thiểu" > 0 — validate client-side trước submit.
- **BR-S4-04:** Submit → INSERT `hub_accepted_categories` (hub_id, item_category_id) nếu chưa tồn tại. Sau đó INSERT/UPDATE `hub_inventories` với `low_stock_threshold` (current_quantity có thể = 0 ban đầu).
- **BR-S4-05:** Nếu category đã tồn tại trong hub → chế độ edit (UPDATE `low_stock_threshold`), không insert duplicate.

### State Management

| Action | Bảng | Thay đổi |
|--------|------|---------|
| Lưu vật tư (mới) | `hub_accepted_categories` | INSERT (hub_id, item_category_id) |
| | `hub_inventories` | INSERT với `low_stock_threshold`, `current_quantity = 0` |
| Lưu vật tư (edit) | `hub_inventories` | UPDATE `low_stock_threshold` |

---

## S5 — Tóm tắt AI (AI Summary tab)

> Báo cáo hệ thống được tổng hợp tự động bởi AI mỗi ngày. Tab mặc định khi vào "AI".

### Data Points

**Header:**

| # | Element | Ghi chú |
|---|---------|---------|
| 1 | Tiêu đề | "Tóm tắt AI" |
| 2 | Bộ lọc thời gian | "hôm nay" / "tuần này" / "tháng này" |

**Section "Báo cáo cứu trợ [kỳ]":**

| # | Element | Nguồn dữ liệu | Ghi chú |
|---|---------|--------------|---------|
| 1 | AI summary text | `sos_requests.ai_summary` + generated report | Tóm tắt ngôn ngữ tự nhiên từ AI |
| 2 | Hàng hóa | `SUM(inventory_logs WHERE OUTBOUND AND kỳ)` | "1,649 kiện đang [xử lý/đã phân phối]" |
| 3 | Người được hỗ trợ | `COUNT(missions WHERE COMPLETED AND kỳ)` | Ước tính từ `sos_requests.people_count` |

**Section Cảnh báo (Alerts):**

| # | Element | Nguồn dữ liệu | Ghi chú |
|---|---------|--------------|---------|
| 1 | Danh sách alert | AI-generated insights | Hub thiếu hàng, anomaly, overload |
| 2 | Mỗi alert: Hub tên + mô tả | `hubs.name` + AI analysis | Ví dụ: "Hub Quận 7: Cần xem xét bổ sung cho tình nguyện viên" |

**Section "Hoạt động gần đây":**

| # | Element | Nguồn dữ liệu | Ghi chú |
|---|---------|--------------|---------|
| 1 | Activity feed | `inventory_logs` + `missions` + `sos_requests` (sort DESC) | 5-10 sự kiện gần nhất |
| 2 | Mỗi activity: mô tả ngắn + timestamp | — | Ví dụ: "Xử lý khoản #DRC-102 đã điền Hub Quận 8" |

**Actions:**

| # | Button | Hành vi |
|---|--------|---------|
| 1 | "Xem toàn bộ" | Mở full activity log / report detail |
| 2 | "Tạo báo cáo" | Export toàn bộ dữ liệu kỳ ra file (Excel/PDF) |
| 3 | [icon chat / tab] | Navigate → S6 (AI Chatbot) |

### Business Rules

- **BR-S5-01:** AI Summary được **auto-generated** hàng ngày (scheduled job, ví dụ 6h sáng). Admin không trigger thủ công (nhưng có thể refresh — xem Q2).
- **BR-S5-02:** Bộ lọc thời gian thay đổi range query. Data re-fetch từ server khi đổi filter.
- **BR-S5-03:** Alerts được AI phân tích từ dữ liệu tồn kho + missions + SOS patterns — không phải rule-based cứng.
- **BR-S5-04:** "Tạo báo cáo" → trigger backend generate file (CSV/Excel/PDF) → download về thiết bị hoặc gửi email Admin.
- **BR-S5-05:** Activity feed là log kết hợp từ nhiều bảng — không thể lấy từ 1 bảng đơn lẻ. Backend cần expose một aggregate endpoint.

### State Management

> Read-only. "Tạo báo cáo" trigger export job (async) nhưng không thay đổi DB state.

---

## S6 — AI Chatbot (AI tab — sub-view)

> Giao diện chat với AI để Admin truy vấn thông tin hệ thống bằng ngôn ngữ tự nhiên.

### Data Points

**Chat interface:**

| # | Element | Nguồn dữ liệu | Ghi chú |
|---|---------|--------------|---------|
| 1 | Tiêu đề | — | "DRC Admin AI" / "AI Tư vấn hệ thống" |
| 2 | Lịch sử chat | `chat_messages` (scoped: type=ADMIN_AI, user_id=admin) hoặc session-only | Hiển thị conversation turns |
| 3 | AI message bubble | AI service response | Câu trả lời data-backed |
| 4 | User message bubble | Admin input | — |
| 5 | Input field | Text input | "Đặt câu hỏi..." |
| 6 | Microphone button | Voice input → AI transcribe | Tương tự AI Voice trong Victim flow |
| 7 | Send button | Trigger AI query | — |

**Quick action buttons (pre-defined prompts):**

| # | Button label | Prompt tương ứng |
|---|--------------|-----------------|
| 1 | "Báo cáo mới tuần tới" | "Tóm tắt hoạt động tuần này và dự báo tuần tới" |
| 2 | "Danh sách tổng lượt tuần" | "Liệt kê tất cả missions hoàn thành trong 7 ngày qua" |

### Business Rules

- **BR-S6-01:** AI truy vấn **dữ liệu thời gian thực** từ PostgreSQL (qua backend context injection). Ví dụ: "Trạm nào sắp thiếu nước uống?" → backend query `hub_inventories WHERE item_category = nước AND current_quantity <= threshold`, trả kết quả cho AI formulate.
- **BR-S6-02:** AI **không có quyền thực hiện thay đổi DB** — chỉ read và summarize. Không có tool write/mutate.
- **BR-S6-03:** Lịch sử chat có thể **session-only** (không persist) hoặc lưu per-user per-session — cần xác nhận (xem Q3).
- **BR-S6-04:** Voice input → AI transcribe (tương tự S3 Victim) → điền vào input field → Admin xem lại → submit thủ công.
- **BR-S6-05:** Quick action buttons là shortcut generate prompt có sẵn. Tap → auto-fill input → auto-submit.

### State Management

> Không có state change DB. Session chat state là client-side (hoặc ephemeral session storage).

---

## S7 — Trang cá nhân (Cá nhân tab)

### Data Points

**Thông tin tài khoản:**

| # | Field | Nguồn dữ liệu | Ghi chú |
|---|-------|--------------|---------|
| 1 | Avatar | `users.avatar_url` | Large display / banner |
| 2 | Vai trò | `users.role` | "Quản trị viên hệ thống" |
| 3 | Email | `users.email` | — |
| 4 | Ngày tham gia | `users.created_at` | Format: DD/MM/YYYY |
| 5 | Nút "ĐĂNG XUẤT" | Action | — |

**Menu điều hướng:**

| # | Item | Điều hướng | Ghi chú |
|---|------|-----------|---------|
| 1 | Họ và tên | Edit profile screen | Chỉnh sửa `users.full_name` |
| 2 | Vai trò: Administrator | Read-only | Không cho đổi role của chính mình |
| 3 | Cài đặt hệ thống | System config screen | Điều chỉnh `system_config` (dispatch weights, OTP TTL...) |
| 4 | Cài đặt hỗ trợ | Support/help settings | Nội dung TBD |

### Business Rules

- **BR-S7-01:** "Đăng xuất" → revoke refresh_token, blacklist JWT. Navigate về Login screen.
- **BR-S7-02:** "Cài đặt hệ thống" → màn hình chỉnh sửa `system_config` key-value pairs:
  - `dispatch.weight.distance/rating/tasks/response_time/area_experience` (5 trọng số Priority Score).
  - `dispatch.radius.step{n}_km` (4 bán kính mở rộng).
  - `dispatch.sos.window_seconds` / `dispatch.delivery.window_1` / `dispatch.delivery.window_batch`.
  - `otp.ttl_minutes`.
  - Thay đổi apply **ngay lập tức** mà không restart service (Spring Boot reads periodically).
- **BR-S7-03:** Admin **không thể tự đổi role của mình** qua màn hình này.
- **BR-S7-04:** Không có "Trạng thái hoạt động" toggle — Admin không có trạng thái online/offline như Volunteer/Staff.

### State Management

| Action | State change |
|--------|-------------|
| Đăng xuất | `refresh_tokens.is_revoked = TRUE`; Redis JWT blacklist |
| Cập nhật system_config | `system_config` UPDATE — apply ngay lập tức cho dispatch algorithm |

---

## Tổng hợp chức năng Admin theo danh mục

| Danh mục | Screens | Figma? |
|----------|---------|--------|
| Tổng quan hệ thống | S1 Dashboard | ✅ |
| Quản lý Hub (xem + khai báo) | S2, S3, S4 | ✅ |
| AI Analytics | S5 Summary, S6 Chatbot | ✅ |
| Profile & System Config | S7 | ✅ |
| Quản lý user (block, assign Staff) | §9.1 requirements | ❌ Không có trong Figma |
| Global map + manual override dispatch | §9.3 requirements | ❌ Chỉ có "Xem bản đồ" button |
| Broadcast alert (FCM/Email) | §9.4 requirements | ❌ Không có trong Figma |

---

## Câu hỏi mở cần xác nhận với team

| # | Câu hỏi | Ảnh hưởng |
|---|---------|-----------|
| Q1 | Dashboard S1: "Tình nguyện viên: 1.2K" là đang **online** hay tổng số **active** (is_active=TRUE)? | Metric definition |
| Q2 | AI Summary: Admin có thể **refresh thủ công** (re-generate) báo cáo, hay chỉ xem bản auto-gen định kỳ? | AI job trigger |
| Q3 | AI Chatbot: lịch sử chat có được **persist** vào DB không, hay chỉ tồn tại trong session? | `chat_messages` schema update? |
| Q4 | **Chức năng quản lý User** (§9.1 — block/unblock, assign Staff vào Hub): có màn hình trong app hay chỉ dành cho web admin panel? | MVP scope |
| Q5 | **Manual override dispatch** (§9.3): Admin có button "Gán thủ công" volunteer → mission trong app, hay chỉ trên web? | MVP scope |
| Q6 | **Broadcast alert** (§9.4 — gửi FCM/Email tất cả users): nếu có trong app, navigate từ đâu? | Navigation gap |
| Q7 | "Cài đặt hỗ trợ" trong S7: nội dung là gì? Help center, FAQs, hay contact support? | UX content |
