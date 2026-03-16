# Sponsor Flow — Business Requirements

> **Scope:** 5 màn hình trong luồng SPONSOR (user đã đăng nhập với `role = SPONSOR`).
> **Nguồn:** Figma `sponsor.png` + `requirements_prompt.md` §6.
> **Điều kiện tiên quyết:** User đã đăng nhập, `is_verified = TRUE`, `is_active = TRUE`, `role = SPONSOR`. Bản ghi `sponsor_profiles` tồn tại.

---

## Tổng quan luồng

```
[Login] → S1: Main Screen — tab "Đóng góp" (default)
           ├── Tap "Đăng ký đóng góp" → S2: Registration Form
           │         → [Smart Hub Selection*] → S4: QR Code screen
           ├── Tap tab "Lịch sử đóng góp"  → S5: Donation History
           └── Bottom nav "Cá nhân"        → S3: Profile (Cá nhân tab)
```

> **(*) Smart Hub Selection** không có màn hình riêng trong Figma — xem Open Question Q1.

---

## S1 — Dashboard (tab "Đóng góp")

> Màn hình mặc định sau đăng nhập. Hiển thị tóm tắt hoạt động và điểm vào đăng ký đóng góp mới.

### Data Points

**Tabs điều hướng (top):**

| Tab | Trạng thái | Điều hướng |
|-----|-----------|-----------|
| "Đóng góp" | Active (default) | Màn hình hiện tại |
| "Lịch sử đóng góp" | Inactive | → S5 |

**Hiển thị (System):**

| # | Element | Nguồn dữ liệu | Ghi chú |
|---|---------|--------------|---------|
| 1 | Nút "Đăng ký đóng góp" | Action | CTA chính, màu xanh lam, nổi bật |
| 2 | Số lần đã tặng | `sponsor_profiles.donation_count` | "N lần tặng trước đây" |
| 3 | Điểm tích lũy | `sponsor_profiles.total_points` | Hiển thị số: "450" |
| 4 | Tổng vật phẩm đã tặng | `sponsor_profiles.total_items_donated` | Hiển thị số: "105" |
| 5 | "Thành tích đóng góp tốt nhất" | `donations` (RECEIVED, sort by quantity) | Hiển thị 2-3 donation nổi bật nhất |
| 6 | Mỗi donation item: tên loại | `item_categories.name_vi` | — |
| 7 | Mỗi donation item: thời gian | `donations.received_at` | "N tháng trước" format |

### Business Rules

- **BR-S1-01:** Tab "Đóng góp" là default khi vào màn hình Sponsor. Dữ liệu stats load từ `sponsor_profiles` (pre-aggregated, không query tổng hợp realtime).
- **BR-S1-02:** "Thành tích đóng góp tốt nhất" hiển thị top donations của user này, filter `donations.status = RECEIVED` (đã nhập kho thành công), không hiển thị REJECTED hoặc REGISTERED.
- **BR-S1-03:** Tap "Đăng ký đóng góp" → navigate đến S2.
- **BR-S1-04:** Badge level (`sponsor_profiles.badge_level`) hiển thị như icon hoặc color border của avatar (BRONZE / SILVER / GOLD / PLATINUM) — optional UX enhancement.

### State Management

> Read-only dashboard. Không có state change khi xem.

---

## S2 — Đăng ký đóng góp (Registration Form)

> Form nhập thông tin lô hàng muốn đóng góp. Đây là bước đầu tiên của quy trình ký gửi.

### Data Points

**Input (User):**

| # | Field | DB Column | UI Control | Bắt buộc |
|---|-------|-----------|-----------|---------|
| 1 | Loại vật phẩm (`item_category_id`) | `donation_items.item_category_id` | Dropdown selector | ✅ |
| 2 | Số lượng (`quantity`) | `donation_items.quantity` | Number input (default: 1) | ✅ (> 0) |
| 3 | Đơn vị tính (`unit`) | `item_categories.unit` | Dropdown (ví dụ: Thùng, Túi, Gói, Kg, Lít) | ✅ |
| 4 | Mô tả vật tư (`condition_notes` + `expiry_date`) | `donation_items.condition_notes` | Textarea | ❌ optional |
| 5 | Ảnh thực tế (`attachments[]`) | `attachments` (entity_type=DONATION) | Camera / Gallery upload | ❌ optional |
| 6 | Thời gian có thể giao (`estimated_delivery_at`) | `donations.estimated_delivery_at` | Date picker (dd/mm/yyyy) | ✅ |

> **Ghi chú đơn vị tính:** `unit` là thuộc tính của `item_categories`, không phải input riêng. UI hiển thị unit đã cấu hình sẵn cho category được chọn (ví dụ: chọn "Gạo" → unit = "Kg"; chọn "Nước đóng chai" → unit = "Thùng").

### Business Rules

- **BR-S2-01 (Category selection):** Chỉ được chọn **leaf categories** (`is_leaf = TRUE`). Dropdown hiển thị theo cấu trúc: `Parent > Child` (ví dụ: "Thức ăn > Gạo"). DB trigger `fn_assert_leaf_category` enforce ở backend.
- **BR-S2-02 (Quantity):** `quantity > 0` — DB CHECK. Validate phía client trước khi submit.
- **BR-S2-03 (Multi-item question):** UI hiện tại hiển thị form **1 loại vật phẩm**. Database hỗ trợ nhiều `donation_items` trên 1 `donations` header. → **Cần xác nhận** (xem Q1 trong Open Questions): thêm nút "Thêm loại hàng khác" hay giới hạn 1 loại/lần.
- **BR-S2-04 (Photo upload):** Ảnh được upload lên server → URL lưu vào `attachments` liên kết với `donations` row. Không có ảnh cũng được submit.
- **BR-S2-05 (Date validation):** `estimated_delivery_at` phải ≥ ngày hiện tại. Không cho phép chọn ngày trong quá khứ.
- **BR-S2-06 (Submit flow):**
  1. Validate input phía client.
  2. POST tới backend → tạo `donations` (status = `REGISTERED`) + `donation_items`.
  3. Backend chạy **Smart Hub Selection** và trả về danh sách Top 3 Hub gợi ý (xem §6.2 requirements_prompt.md).
  4. Hiển thị Hub Selection step (modal hoặc màn hình riêng) → Sponsor chọn Hub.
  5. Backend sinh `qr_code_token` → UPDATE `donations.status = QR_GENERATED` + `hub_id`.
  6. Navigate đến S4 (QR screen).

### State Management

| Bước | Bảng | Trạng thái |
|------|------|-----------|
| Submit form | `donations` | `status = REGISTERED` |
| | `donation_items` | INSERT N rows |
| | `attachments` | INSERT nếu có ảnh |
| Chọn Hub xong | `donations` | `status = QR_GENERATED`, `hub_id` gán |
| | FCM notify Staff trạm | "Mạnh thường quân [Tên] đăng ký ký gửi [Loại hàng]" |

---

## S3 — Trang cá nhân (Cá nhân tab)

> Profile và thống kê đóng góp của Sponsor.

### Data Points

**Hiển thị (System):**

| # | Element | Nguồn dữ liệu | Ghi chú |
|---|---------|--------------|---------|
| 1 | Câu chào | — | "Chào mừng quay trở lại!" |
| 2 | Tổng giá trị đóng góp (*) | Chưa có trong DB schema | "15.000.550đ" — xem Open Question Q2 |
| 3 | Tổng số lần đóng góp | `sponsor_profiles.donation_count` | "42" |
| 4 | "Giao dịch gần đây" | `donations` (RECEIVED, sort DESC, limit 5) | Danh sách donation gần nhất đã nhập kho |
| 5 | Mỗi transaction: tên tổ chức/địa điểm nhận | Hub name hoặc `aid_requests` location | Xem BR-S3-02 |
| 6 | Mỗi transaction: giá trị (*) | Xem Open Question Q2 | "+1.200.000đ" format |
| 7 | "Xem tất cả" link | → S5 (Donation History) | — |
| 8 | Badge level | `sponsor_profiles.badge_level` | BRONZE/SILVER/GOLD/PLATINUM |

> **(*) Cảnh báo schema gap:** Figma hiển thị tổng giá trị tiền tệ ("15.000.550đ") nhưng `sponsor_profiles` hiện tại **không có field monetary**. Xem Open Question Q2.

### Business Rules

- **BR-S3-01:** Stats từ `sponsor_profiles` (pre-aggregated). Không query tổng hợp realtime.
- **BR-S3-02:** "Giao dịch gần đây" hiển thị tên Hub mà Sponsor đã ký gửi thành công, không phải tên nạn nhân cụ thể (privacy protection). Format: "[Tên Hub] — [Ngày nhập kho]".
- **BR-S3-03:** "Thông tin cá nhân" → màn hình edit profile (full_name, phone, avatar). Không đổi email hoặc role.
- **BR-S3-04:** Badge level quyết định bởi `donation_count` hoặc `total_points` — ngưỡng cụ thể do Admin cấu hình (hoặc hardcode): BRONZE ≥ 1, SILVER ≥ 10, GOLD ≥ 25, PLATINUM ≥ 50.

### State Management

> Read-only. Không có state change khi xem profile.

---

## S4 — Mã QR Đóng góp (Donation QR Code)

> Màn hình hiển thị QR cho Sponsor mang đến trạm. Bước cuối cùng trước khi giao hàng thực tế.

### Data Points

**Hiển thị (System):**

| # | Element | Nguồn dữ liệu | Ghi chú |
|---|---------|--------------|---------|
| 1 | QR Code hình ảnh | Render từ `donations.qr_code_token` | Hiển thị to, rõ; tự động tăng độ sáng màn hình |
| 2 | Mã quyên góp | `donations.id` (format: #DON-XXXXX) | Human-readable reference code |
| 3 | Loại vật phẩm | `donation_items[0].item_category_id` → `item_categories.name_vi` | Ví dụ: "Thực phẩm & Nước uống" |
| 4 | Số lượng | `donation_items[0].quantity` + `item_categories.unit` | Ví dụ: "50 Thùng" |
| 5 | Tên Hub được chọn | `hubs.name` | Trạm Sponsor cần mang hàng đến |
| 6 | Địa chỉ Hub | `hubs.address` | Để Sponsor biết chỗ mang hàng |
| 7 | Hướng dẫn | Static text | "Xuất trình mã này cho nhân viên khi đến trạm" |
| 8 | Nút "Lưu mã QR" | Action | Lưu QR thành ảnh vào bộ nhớ thiết bị |
| 9 | Nút X / "Đóng" | Navigation | Navigate về S1 (Dashboard) |

### Business Rules

- **BR-S4-01:** Màn hình này chỉ hiển thị khi `donations.status = QR_GENERATED`. Nếu navigate trực tiếp đến một donation đã `RECEIVED` hoặc `REJECTED` → không hiển thị QR, chỉ hiển thị trạng thái.
- **BR-S4-02:** **Override screen timeout** khi màn hình này active (giữ màn hình sáng) để Sponsor không bị lock màn hình khi đang cho Staff quét.
- **BR-S4-03 (Lưu QR):** Nút "Lưu mã QR" → render QR thành file ảnh PNG → lưu vào gallery thiết bị (`MediaStore` trên Android). Yêu cầu permission `WRITE_EXTERNAL_STORAGE` (Android < 10) hoặc `MediaColumns` scoped storage (Android 10+).
- **BR-S4-04:** QR là **single-use**. Sau khi Staff quét xác nhận nhập kho thành công → `donations.status = RECEIVED` → QR này không còn hiệu lực.
- **BR-S4-05:** Nếu Sponsor đóng màn hình và muốn xem lại QR → vào S5 (Lịch sử) → tap donation đang `QR_GENERATED` → navigate trở lại S4.
- **BR-S4-06:** FCM notify Staff trạm khi Sponsor xem màn hình QR (tức là đang trên đường đến): "Mạnh thường quân [Tên] đang di chuyển đến trạm để ký gửi [Loại hàng]." *(hoặc trigger này xảy ra ngay lúc submit form — cần xác nhận Q3)*.

### State Management

> Không có state change khi xem màn hình QR. State đã là `QR_GENERATED` từ bước S2. State tiếp theo xảy ra phía Staff (bảng `donations.status → RECEIVED` khi Staff quét).

---

## S5 — Lịch sử đóng góp (tab "Lịch sử đóng góp")

### Data Points

**Filter tabs:**

| Tab | Filter DB | Ghi chú |
|-----|-----------|---------|
| "Tất cả" | Không filter status | Default active |
| "Chờ nhận" | `status = QR_GENERATED` | Đã có QR, chưa đến trạm |
| "Đã nhận" | `status = RECEIVED` | Đã nhập kho thành công |
| "Từ chối" | `status = REJECTED` | Staff từ chối lô hàng |

**Mỗi item trong danh sách:**

| # | Field | Nguồn dữ liệu | Ghi chú |
|---|-------|--------------|---------|
| 1 | Loại vật phẩm | `item_categories.name_vi` (từ `donation_items`) | "Thực phẩm & Nước uống", "Thuốc men",... |
| 2 | Số lượng + đơn vị | `donation_items.quantity` + `unit` | "50 Thùng" |
| 3 | Thời gian | `donations.created_at` | "N tháng trước" format |
| 4 | Badge trạng thái | `donations.status` | Xem bảng màu dưới đây |
| 5 | Thumbnail | `attachments` (entity=DONATION) hoặc default icon | Ảnh thực tế lô hàng |
| 6 | Tên Hub nhận | `hubs.name` | Trạm đã/sẽ nhận hàng |

**Bảng màu badge trạng thái:**

| Status | Label | Màu |
|--------|-------|-----|
| `REGISTERED` | Đã đăng ký | Xám |
| `QR_GENERATED` | Chờ nhận | Cam |
| `RECEIVED` | Đã nhận | Xanh lá |
| `REJECTED` | Từ chối | Đỏ |

### Business Rules

- **BR-S5-01:** Chỉ hiển thị donations của `current_user` (`donations.sponsor_id = user.id`).
- **BR-S5-02:** Sắp xếp: `donations.created_at DESC` (mới nhất lên đầu).
- **BR-S5-03:** Tap vào donation đang `QR_GENERATED` → navigate đến S4 (hiển thị lại QR).
- **BR-S5-04:** Tap vào donation đang `REJECTED` → xem lý do từ chối (`donations.rejection_reason`).
- **BR-S5-05:** Tap vào donation `RECEIVED` → xem chi tiết: Hub nhận, thời gian nhập kho, Staff xác nhận (read-only).
- **BR-S5-06:** Thumbnail là ảnh Sponsor đã upload trong S2 (`attachments`). Nếu không có ảnh → hiển thị icon category mặc định.

### State Management

> Read-only. Không có state change khi xem lịch sử.

---

## Tổng hợp lifecycle `donations`

```
Sponsor điền form (S2)
    ↓ Submit
REGISTERED  →  [Smart Hub Selection]
    ↓ Chọn Hub + system sinh QR
QR_GENERATED  →  Sponsor xem QR (S4)
    ↓ Staff quét QR + xác nhận
RECEIVED  (inventory updated, sponsor notified)
    ↓ (thay thế)
REJECTED  (if staff từ chối, lý do lưu vào rejection_reason)
```

---

## Câu hỏi mở cần xác nhận với team

| # | Câu hỏi | Ảnh hưởng |
|---|---------|-----------|
| Q1 | Form S2 chỉ nhập **1 loại vật phẩm** hay có nút "Thêm loại hàng"? | `donation_items` generation; UX flow |
| Q2 | Profile S3 hiển thị **tổng giá trị tiền tệ** ("15.000.550đ") — có cần thêm field `unit_value` vào `donation_items` và `total_value_donated` vào `sponsor_profiles` không? | DB schema change |
| Q3 | FCM notify Staff "Sponsor đang đến" được gửi lúc **submit form** hay lúc Sponsor **mở màn hình QR**? | Timing of Staff notification |
| Q4 | Smart Hub Selection: hiển thị như **modal sau khi submit S2** hay như **step trong form S2** (chọn Hub trước khi ấn "Đăng ký")? | Missing screen in Figma |
| Q5 | Thumbnail trong S5 History: là ảnh **Sponsor upload** hay là ảnh **Staff chụp** khi nhập kho? | Image ownership |
| Q6 | Badge level thresholds (BRONZE/SILVER/GOLD/PLATINUM): ngưỡng cụ thể là gì? Tính theo `donation_count` hay `total_points`? | Gamification rule |
