# Staff Flow — Business Requirements

> **Scope:** 7 màn hình trong luồng STAFF (user đã đăng nhập với `role = STAFF`).
> **Nguồn:** Figma `staff.png` + `requirements_prompt.md` §8.
> **Điều kiện tiên quyết:** User đã đăng nhập, `is_verified = TRUE`, `is_active = TRUE`, `role = STAFF`. Bản ghi `hub_staff` tồn tại với `hub_id` đang active.

---

## Tổng quan luồng

```
[Login] → S1: Dashboard (Cá nhân tab — default)
Bottom Navigation: Bản đồ | Kho | Quét | Cá nhân

Kho tab   → S2: Quản lý tồn kho (inventory list)
                └── [button/sub-nav] → S3: Xuất kho vật tư (outbound queue)
                                           → S4: Chi tiết xuất kho
                                                 → S7: Thành công

Quét tab  → S5: Nhập kho — QR Scanner (inbound)
                → S6: Chi tiết lô hàng (confirmation screen sau khi quét)
                      → [Success toast / popup]

Cá nhân   → S1: Dashboard
```

---

## S1 — Dashboard (Cá nhân tab)

> Màn hình hiển thị thông tin cá nhân Staff và trạng thái hoạt động.

### Data Points

**Hiển thị (System):**

| # | Element | Nguồn dữ liệu | Ghi chú |
|---|---------|--------------|---------|
| 1 | Avatar | `users.avatar_url` | Ảnh hoặc placeholder |
| 2 | Họ tên | `users.full_name` | — |
| 3 | Vai trò | — | "Nhân viên quản lý tồn kho" |
| 4 | Toggle "Trạng thái hoạt động" | `hub_staff.is_available` (*) | ON: "Sẵn sàng" (xanh) / OFF: "Ngoài tuyến" (xám) |
| 5 | Số điện thoại | `users.phone_number` | — |
| 6 | Email | `users.email` | — |
| 7 | Ngày bắt đầu tham gia | `hub_staff.assigned_at` | Format: DD/MM/YYYY |
| 8 | Danh sách tồn kho tóm tắt | `hub_inventories` (hub hiện tại) | Scroll preview — dẫn đến S2 |
| 9 | Nút "Đăng xuất" | Action | Màu đỏ |

> **(*) Schema note:** `hub_staff` hiện chưa có cột `is_available`. Figma rõ ràng có toggle này — xem Open Question Q1.

### Business Rules

- **BR-S1-01:** Màn hình mặc định khi đăng nhập. Load thông tin từ `users` và `hub_staff` JOIN.
- **BR-S1-02:** Toggle "Sẵn sàng / Ngoài tuyến" → cập nhật trạng thái sẵn sàng của Staff (xem Q1). Khi "Ngoài tuyến", Staff vẫn có thể thao tác trực tiếp trên app nhưng hệ thống không gửi FCM thông báo Volunteer/Sponsor đến.
- **BR-S1-03:** Tóm tắt tồn kho ở cuối màn hình là preview (shortcut) — tap vào → navigate đến S2.
- **BR-S1-04:** "Đăng xuất" → revoke refresh_token (`is_revoked = TRUE`), blacklist JWT. Navigate về màn hình Login.

### State Management

| Action | State change |
|--------|-------------|
| Toggle ON | `hub_staff.is_available = TRUE` (*) |
| Toggle OFF | `hub_staff.is_available = FALSE` (*) |
| Đăng xuất | `refresh_tokens.is_revoked = TRUE` |

---

## S2 — Quản lý tồn kho (Kho tab)

> Màn hình xem và tra cứu tồn kho tại Hub được giao. Read-only với filter/search.

### Data Points

**Controls:**

| # | Element | Ghi chú |
|---|---------|---------|
| 1 | Search bar | "Tìm kiếm nhu yếu phẩm..." — filter theo tên item |
| 2 | Filter tabs (theo category) | "Tất cả" + các category đang có hàng tại hub: Nước uống, Mì gói, Chăn... |
| 3 | Label "Danh sách vật tư" | Header section |
| 4 | Timestamp cập nhật | "CẬP NHẬT [N] PHÚT TRƯỚC" — dựa trên lần query cuối hoặc TTL cache |

**Mỗi item trong danh sách:**

| # | Field | Nguồn dữ liệu | Ghi chú |
|---|-------|--------------|---------|
| 1 | Tên vật phẩm | `item_categories.name_vi` | — |
| 2 | Danh mục cha | `item_categories.name_vi` (parent) | Ví dụ: "Sản phẩm y tế", "Nước uống" |
| 3 | Số lượng tồn | `hub_inventories.current_quantity` + `item_categories.unit` | Ví dụ: "450 thùng", "12 túi" |
| 4 | Badge trạng thái tồn kho | Logic so sánh `current_quantity` vs `low_stock_threshold` | Xem bảng màu bên dưới |

**Bảng màu badge tồn kho:**

| Điều kiện | Badge | Màu |
|-----------|-------|-----|
| `current_quantity == 0` | CẦN BỔ SUNG GẤP | Đỏ |
| `current_quantity <= low_stock_threshold` | SẮP HẾT | Cam/Vàng |
| `current_quantity > low_stock_threshold` | BÌNH THƯỜNG | Xanh lá / Teal |

### Business Rules

- **BR-S2-01:** Chỉ hiển thị inventory của Hub mà Staff đang được assign (`hub_staff.hub_id`).
- **BR-S2-02:** Search filter theo `item_categories.name_vi` (client-side filter trên danh sách đã load, hoặc server-side query).
- **BR-S2-03:** Filter tabs hiển thị dynamic theo các category đang có `hub_inventories` row tại hub hiện tại. Không cứng danh sách.
- **BR-S2-04:** Badge "CẦN BỔ SUNG GẤP" ưu tiên hiển thị (sort lên đầu). Sau đó "SẮP HẾT", rồi "BÌNH THƯỜNG".
- **BR-S2-05:** Màn hình này **không cho sửa số lượng trực tiếp**. Mọi thay đổi số lượng đi qua luồng nhập kho (S5→S6) hoặc xuất kho (S3→S4→S7).
- **BR-S2-06:** Timestamp "Cập nhật" phản ánh thời điểm data được load/refetch lần cuối — nhắc nhở Staff biết độ tươi của dữ liệu.

### State Management

> Read-only. Không có state change.

---

## S3 — Xuất kho vật tư (Outbound Queue)

> Danh sách các nhiệm vụ DELIVERY đang ở trạng thái `ASSIGNED`, Volunteer đến Hub chờ nhận hàng.

### Data Points

**Controls:**

| # | Element | Ghi chú |
|---|---------|---------|
| 1 | Search bar | "Tìm nhiệm vụ theo Volunteer..." — tìm theo tên Volunteer |
| 2 | Back / navigation | Về S2 hoặc bottom nav |

**Mỗi card trong danh sách:**

| # | Field | Nguồn dữ liệu | Ghi chú |
|---|-------|--------------|---------|
| 1 | Badge trạng thái | `missions.status = ASSIGNED` | "CHỜ NHẬN HÀNG" — màu xanh dương |
| 2 | Mã nhiệm vụ | `missions.id` | Format: #TK-XXXX |
| 3 | Avatar Volunteer | `users.avatar_url` | — |
| 4 | Tên Volunteer | `users.full_name` | — |
| 5 | Địa chỉ giao | `aid_requests.address` | "Điểm giao: Quận 1, TP.HCM" |
| 6 | Nút "Xuất kho" | Action | Navigate → S4 |

### Business Rules

- **BR-S3-01:** Chỉ hiển thị missions thuộc Hub hiện tại (`missions.hub_id = hub_staff.hub_id`) và `missions.status = ASSIGNED`.
- **BR-S3-02:** Sort mặc định: `missions.created_at ASC` (nhiệm vụ chờ lâu nhất lên đầu — FIFO).
- **BR-S3-03:** Search filter client-side theo `users.full_name` của volunteer.
- **BR-S3-04:** Tap "Xuất kho" → navigate đến S4 với `mission_id` được truyền theo.
- **BR-S3-05:** Nếu không có nhiệm vụ nào → hiển thị empty state: "Không có Tình nguyện viên nào đang chờ nhận hàng."

### State Management

> Read-only. State chỉ thay đổi sau khi confirm tại S4.

---

## S4 — Chi tiết xuất kho

> Màn hình xác nhận xuất hàng cho một Volunteer cụ thể.

### Data Points

**Header thông tin nhiệm vụ:**

| # | Element | Nguồn dữ liệu | Ghi chú |
|---|---------|--------------|---------|
| 1 | Badge trạng thái | `missions.status` | "ĐANG XỬ LÝ" khi đang prepare xuất kho |
| 2 | Mã nhiệm vụ | `missions.id` | "MÃ NHIỆM VỤ: #DC-XXXX" |
| 3 | Tên Hub | `hubs.name` | "Hub Quận 1" |

**Thông tin Volunteer:**

| # | Field | Nguồn dữ liệu | Ghi chú |
|---|-------|--------------|---------|
| 1 | Tên Volunteer | `users.full_name` | — |
| 2 | Số điện thoại | `users.phone_number` | Tap → gọi điện |
| 3 | Địa chỉ điểm giao | `aid_requests.address` | — |

**Danh sách hàng cần xuất:**

| # | Field | Nguồn dữ liệu | Ghi chú |
|---|-------|--------------|---------|
| 1 | Tiêu đề section | — | "Danh sách hàng xuất — [N] loại mặt hàng" |
| 2 | Tên vật phẩm | `item_categories.name_vi` | — |
| 3 | Số lượng yêu cầu | `aid_request_items.quantity` | "Yêu cầu: X thùng" |
| 4 | Số lượng hiện có trong kho | `hub_inventories.current_quantity` | Hiển thị để Staff đối chiếu |
| 5 | Warning tồn kho không đủ | Logic: `current_quantity < requested_quantity` | Badge đỏ trên item đó |
| 6 | Tổng số kiện | Tổng `aid_request_items.quantity` | "Tổng hàng cần xử lý: [N] kiện" |

**Actions:**

| # | Button | Điều kiện | Hành vi |
|---|--------|-----------|---------|
| 1 | "XÁC NHẬN XUẤT KHO" | Primary CTA | Kích hoạt atomic outbound transaction |
| 2 | Back | — | Về S3 (không thay đổi trạng thái) |

### Business Rules

- **BR-S4-01:** Màn hình này **chỉ đọc** thông tin — Staff không nhập số lượng, chỉ xác nhận.
- **BR-S4-02 (Kiểm tra tồn kho):** Trước khi hiển thị màn hình, backend kiểm tra từng item:
  - `current_quantity >= requested_quantity` → hiển thị bình thường.
  - `current_quantity < requested_quantity` → highlight warning đỏ trên item đó.
- **BR-S4-03 (Xác nhận khi thiếu hàng):** Nếu có item bị shortage, nút "XÁC NHẬN XUẤT KHO" vẫn **hiển thị** nhưng hiện dialog cảnh báo: "Một số mặt hàng không đủ số lượng. Bạn có muốn xuất phần có trong kho không?" → Staff quyết định confirm hay hủy.
  - Nếu Staff confirm xuất thiếu → xuất `min(current_quantity, requested_quantity)` cho item đó. Phần còn lại xử lý theo nghiệp vụ (cần xác nhận — xem Q2).
- **BR-S4-04 (Confirm thành công):** Tap "XÁC NHẬN XUẤT KHO" (không có warning) → gọi backend API → atomic transaction (xem §8.2 requirements_prompt.md) → navigate đến S7.
- **BR-S4-05:** SĐT Volunteer tap → `tel:` deeplink gọi điện trực tiếp.

### State Management

| Action | Bảng | Thay đổi |
|--------|------|---------|
| Xác nhận xuất kho | `missions` | `status = PICKED_UP`, `picked_up_at = now()` |
| | `hub_inventories` | `current_quantity -= quantity` (mỗi item, atomic) |
| | `inventory_logs` | INSERT `change_type = OUTBOUND`, `reference_type = MISSION`, `reference_id = mission_id` |
| | `volunteer_profiles` | (không thay đổi ở bước này) |
| | Victim FCM | "TNV đã lấy hàng và đang trên đường đến chỗ bạn." |
| | Volunteer WebSocket | Push: "Đã xác nhận! Đến giao cho nạn nhân." |

---

## S5 — Nhập kho — QR Scanner

> Màn hình quét QR của Sponsor khi họ mang hàng đến trạm.

### Data Points

**Hiển thị (System):**

| # | Element | Nguồn dữ liệu | Ghi chú |
|---|---------|--------------|---------|
| 1 | Camera viewfinder | CameraX | Khung QR scan ở giữa màn hình |
| 2 | Hướng dẫn | Static text | "Đưa mã QR của Mạnh thường quân vào trong khung hình để quét" |
| 3 | Nút "Nhập mã thủ công" | Action | Fallback: hiển thị text field nhập `qr_code_token` hoặc donation ID |
| 4 | Flash toggle | Action | Bật/tắt đèn flash khi tối |

### Business Rules

- **BR-S5-01:** Sử dụng **CameraX + ML Kit Barcode Scanning** để decode QR. Không cần kết nối internet để decode — chỉ cần internet để gọi backend verify.
- **BR-S5-02:** Sau khi decode QR code → gọi backend API với `qr_code_token` → backend trả về thông tin lô hàng (sponsor name, item list, quantity). Navigate → S6.
- **BR-S5-03 (QR không hợp lệ):** Nếu `qr_code_token` không tìm thấy hoặc `donations.status != QR_GENERATED` → hiển thị toast lỗi: "Mã QR không hợp lệ hoặc đã được xử lý." Không navigate.
- **BR-S5-04 (Manual fallback):** Tap "Nhập mã thủ công" → hiển thị text input để nhập donation ID hoặc token → submit → cùng flow như quét QR.
- **BR-S5-05:** Màn hình này chỉ xử lý **INBOUND** (quét QR Sponsor). Outbound (Volunteer) sử dụng luồng list-based tại S3.

### State Management

> Không có state change tại màn hình này. State thay đổi sau khi Staff confirm tại S6.

---

## S6 — Chi tiết lô hàng nhập kho *(màn hình inferred — không rõ trong Figma)*

> Hiển thị thông tin lô hàng sau khi quét QR thành công để Staff kiểm tra trước khi xác nhận.

> **Ghi chú Figma:** Màn hình này **không thể hiện rõ trong Figma** — có thể là modal overlay hoặc màn hình riêng. Nội dung được suy ra từ `requirements_prompt.md` §8.1 và nghiệp vụ thực tế. Cần thiết kế UI bổ sung.

### Data Points

**Thông tin lô hàng (System, từ backend response):**

| # | Field | Nguồn dữ liệu | Ghi chú |
|---|-------|--------------|---------|
| 1 | Mã đóng góp | `donations.id` | "#DON-XXXXX" |
| 2 | Tên Mạnh thường quân | `users.full_name` (sponsor) | — |
| 3 | Danh sách vật phẩm | `donation_items` JOIN `item_categories` | Tên + số lượng + đơn vị |
| 4 | Ngày dự kiến giao | `donations.estimated_delivery_at` | — |
| 5 | Tình trạng vật tư | `donation_items.condition_notes` | Ghi chú từ Sponsor |
| 6 | Ảnh thực tế | `attachments` (entity=DONATION) | Ảnh Sponsor đã upload khi đăng ký |

**Actions:**

| # | Button | Hành vi |
|---|--------|---------|
| 1 | "XÁC NHẬN NHẬP KHO" | Atomic inbound transaction + notify Sponsor |
| 2 | "Từ chối" | Mở dialog nhập lý do → `donations.status = REJECTED` |

### Business Rules

- **BR-S6-01:** Staff kiểm tra thực tế (số lượng, chất lượng) đối chiếu với thông tin trên màn hình trước khi xác nhận.
- **BR-S6-02 (Confirm nhập kho):** Tap "XÁC NHẬN NHẬP KHO" → backend chạy atomic transaction:
  - `donations.status = RECEIVED`, ghi `received_by = user.id`, `received_at = now()`.
  - Mỗi `donation_items`: `hub_inventories.current_quantity += quantity` (UPSERT — tạo row nếu chưa có).
  - INSERT `inventory_logs`: `change_type = INBOUND`.
  - Tăng `sponsor_profiles.total_items_donated`, `donation_count`, `total_points` (đồng nguyên tử).
  - FCM notify Sponsor: "Đã nhập kho [số lượng] hàng. Cảm ơn bạn."
- **BR-S6-03 (Từ chối):** Tap "Từ chối" → bắt buộc nhập lý do (`rejection_reason`, không được để trống) → `donations.status = REJECTED`. FCM notify Sponsor lý do từ chối.
- **BR-S6-04:** QR `single-use` — sau khi xác nhận (RECEIVED hoặc REJECTED), QR token này không thể submit lại.

### State Management

| Action | Bảng | Thay đổi |
|--------|------|---------|
| Xác nhận nhập kho | `donations` | `status = RECEIVED`, `received_by`, `received_at` |
| | `hub_inventories` | `current_quantity += quantity` (UPSERT mỗi item) |
| | `inventory_logs` | INSERT INBOUND log |
| | `sponsor_profiles` | `total_items_donated`, `donation_count`, `total_points` += |
| | Sponsor FCM | "Đã nhập kho thành công. Cảm ơn bạn!" |
| Từ chối | `donations` | `status = REJECTED`, `rejection_reason` |
| | Sponsor FCM | Nội dung lý do từ chối |

---

## S7 — Thành công — Xuất kho

### Data Points

| # | Element | Nội dung |
|---|---------|---------|
| 1 | Icon | Checkmark xanh lam (tương tự Victim success screens) |
| 2 | Tiêu đề | "Xuất kho thành công!" |
| 3 | CTA | "Xác nhận" |

### Business Rules

- **BR-S7-01:** Chỉ hiển thị sau khi backend trả về HTTP 200/201 từ outbound confirm API.
- **BR-S7-02:** "Xác nhận" → navigate về S3 (Xuất kho vật tư) để tiếp tục xử lý các Volunteer khác đang chờ.
- **BR-S7-03:** Không có màn hình success riêng cho **nhập kho (inbound)** trong Figma → có thể dùng toast notification hoặc cần thiết kế bổ sung (xem Q3).

### State Management

> State đã thay đổi tại S4. Màn hình này chỉ là confirmation UI — không có thêm state change.

---

## Tổng hợp lifecycle

### Inbound (Sponsor → Kho)

```
Sponsor đến trạm với QR
    ↓ Staff quét (S5)
Backend verify donations.status = QR_GENERATED
    ↓ Hiển thị chi tiết (S6)
Staff kiểm tra thực tế
    ↓ Xác nhận
donations.status = RECEIVED
hub_inventories.current_quantity += qty
inventory_logs INSERT (INBOUND)
sponsor_profiles UPDATE
FCM → Sponsor ✅
    ↓ (hoặc)
Staff từ chối
donations.status = REJECTED
FCM → Sponsor ❌ (kèm lý do)
```

### Outbound (Kho → Volunteer)

```
Volunteer đến trạm
    ↓ Staff mở S3, tìm mission
S3: Danh sách CHỜ NHẬN HÀNG (ASSIGNED missions)
    ↓ Tap "Xuất kho"
S4: Kiểm tra danh sách item + tồn kho
    ↓ Nếu đủ: confirm trực tiếp
    ↓ Nếu thiếu: cảnh báo + dialog confirm
Atomic transaction:
    missions.status = PICKED_UP
    hub_inventories -= qty
    inventory_logs INSERT (OUTBOUND)
    FCM → Victim ✅
    WebSocket → Volunteer ✅
    ↓
S7: Thành công
```

---

## Câu hỏi mở cần xác nhận với team

| # | Câu hỏi | Ảnh hưởng |
|---|---------|-----------|
| Q1 | Toggle "Trạng thái hoạt động" trên S1 map sang field nào? `hub_staff` hiện không có `is_available` — cần thêm column hay dùng cách khác? | DB schema change |
| Q2 | Khi xuất kho thiếu hàng (S4 shortage), Staff có thể xuất **phần còn lại** hay phải đợi nhập thêm? Logic xử lý item thiếu hàng thế nào? | Outbound partial fulfillment rule |
| Q3 | Nhập kho thành công (inbound - S6 confirm): có màn hình success riêng (như S7) hay chỉ hiển thị toast/snackbar rồi về S2? | UX design gap |
| Q4 | Outbound flow: Volunteer phải **xuất trình QR** tại quầy Staff (Staff quét bằng thiết bị riêng) hay Staff **chọn từ danh sách** (như S3→S4)? Figma cho thấy list-based nhưng §8.2 mô tả "Staff quét QR Volunteer". | Conflict giữa Figma và requirements §8.2 |
| Q5 | Staff có thể quản lý (tạo/sửa) tồn kho **thủ công** (manual adjustment) không, ví dụ khi hàng bị hỏng/mất? | inventory_logs `change_type = ADJUSTMENT`? |
| Q6 | Tab "Bản đồ" trong bottom nav của Staff: hiển thị Public Map (như Guest) hay bản đồ hub nội bộ (vị trí Volunteers đang đến trạm)? | Staff map scope |
