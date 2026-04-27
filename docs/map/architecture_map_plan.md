# Thiết kế Hệ thống Bản đồ Đa vai trò (Multi-role Map Architecture)

Kế hoạch này đề xuất cấu trúc lại logic bản đồ hiện tại để hỗ trợ nhiều vai trò người dùng (Volunteer, Staff, Admin, Guest, Sponsor) một cách linh hoạt, tránh lặp lại mã nguồn và dễ dàng bảo trì.

## Mục tiêu
*   **Chia sẻ mã nguồn**: Gom các logic cơ bản của OSMDroid vào một lớp cơ sở.
*   **Tính mở rộng**: Thêm tính năng mới cho một vai trò mà không ảnh hưởng đến vai trò khác.
*   **Quản lý UI**: Sử dụng Composition để lắp ghép các bảng điều khiển (Panels) tùy theo quyền hạn.

---

## 1. Kiến trúc Đề xuất

### A. Lớp Cơ sở (Core Layer)
*   `BaseMapFragment`: Chứa boilerplate của OSMDroid (init map, lifecycle, touch, default layers). Cung cấp các hooks như `onMapReady()`, `onLocationUpdated()`.
*   `BaseMapViewModel`: Quản lý trạng thái chung: Tọa độ camera, danh sách marker cơ bản, trạng thái mạng/GPS.

### B. Thành phần Tính năng (Feature Components)
Thay vì viết code tính năng trực tiếp vào Fragment, ta tách thành các **Delegates** hoặc **Feature Managers**:
*   `RoutingFeature`: Xử lý vẽ đường đi, chỉ đường.
*   `HubSearchFeature`: Xử lý tìm kiếm trạm trực và hiển thị markers.
*   `SafetyZoneFeature`: Xử lý vùng nguy hiểm (Dangerous Zones).
*   `MemberTrackingFeature`: (Dành cho Staff/Admin) Hiển thị vị trí các thành viên khác.

### C. Vai trò Cụ thể (Role Implementation)
Mỗi vai trò sẽ kế thừa `BaseMapFragment` và chỉ "kích hoạt" các tính năng cần thiết:
*   `VolunteerMapFragment`: Base + Routing + HubSearch.
*   `StaffMapFragment`: Base + MemberTracking + AreaOverview.
*   `GuestMapFragment`: Base + SafeZones (Read-only).

---

## 2. Cấu trúc Thư mục Dự kiến

```text
drc-app/app/src/main/java/com/drc/aidbridge/ui/map/
├── base/
│   ├── BaseMapFragment.java       # Kế thừa Common logic
│   └── BaseMapViewModel.java      # State chung (camera, overlays)
├── core/
│   ├── MapOverlayManager.java     # Quản lý lớp phủ (Marker, Polyline)
│   └── MapCameraController.java  # Điều khiển zoom/focus
├── feature/                       # Các logic độc lập
│   ├── routing/                   # Xử lý đường đi
│   ├── hub/                       # Xử lý trạm trực
│   └── zones/                     # Xử lý vùng nguy hiểm
└── roles/                         # Từng vai trò cụ thể
    ├── VolunteerMapFragment.java
    ├── StaffMapFragment.java
    ├── GuestMapFragment.java
    └── AdminMapFragment.java
```

---

## 3. Quản lý UI (Layout Composition)

Sử dụng kĩ thuật **Dynamic View Injection** trong `fragment_base_map.xml`:

```xml
<!-- fragment_base_map.xml -->
<CoordinatorLayout>
    <MapView id="map" />
    
    <!-- Vùng cho các nút FAB cơ bản (Recenter, Zoom) -->
    <include layout="@layout/layout_map_common_controls" />
    
    <!-- Vùng chứa UI đặc thù của từng vai trò -->
    <FragmentContainerView 
        android:id="@+id/roleActionContainer"
        android:layout_gravity="bottom" />
        
    <!-- Drawer cho tính năng đặc thù (nếu có) -->
    <NavigationView id="roleDrawer" />
</CoordinatorLayout>
```

---

## 4. Lợi ích & Đánh giá
1.  **DRY (Don't Repeat Yourself)**: Tất cả logic khởi tạo bản đồ phức tạp chỉ viết 1 lần.
2.  **Clean Code**: `VolunteerMapFragment` sẽ giảm từ 2000+ dòng xuống còn ~300 dòng (chỉ tập trung vào business nhiệm vụ).
3.  **Dễ Test**: Có thể test độc lập `RoutingFeature` mà không cần quan tâm đến `VolunteerMapFragment`.

## Câu hỏi cho USER
1. Bạn có muốn sử dụng **Navigation Component** để chuyển đổi giữa các Fragment này không?
2. Có tính năng nào ngoài "Tọa độ" mà bạn muốn Guest có thể nhìn thấy (ví dụ: Tin tức cứu hộ trên vùng)?
