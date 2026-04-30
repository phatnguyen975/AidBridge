# Kế hoạch triển khai: Hệ thống điều phối Tình nguyện viên (Hybrid Rescue Dispatcher)

Tài liệu này phác thảo kiến trúc và các bước triển khai để tối ưu hóa thuật toán khớp lệnh cứu trợ, nâng cấp từ ST_Distance sang Uber H3 kết hợp Distance Matrix Routing.

## 1. PHẦN 1: Tổ chức dữ liệu & Lọc thô bằng Uber H3

### a. Thay đổi Database Schema
- Bảng `volunteer_profiles` bổ sung thêm cột `h3_index` (VARCHAR(15)).
- Tạo B-Tree Index để tối ưu `SELECT ... IN`:
  ```sql
  CREATE INDEX idx_volunteer_h3_index ON volunteer_profiles(h3_index);
  ```

### b. Logic cập nhật vị trí (Update Location API)
- Dùng `h3-java` (resolution: 8) chuyển tọa độ sang `h3_index`.
- Logic Java:
  ```java
  String h3Address = h3.latLngToCellAddress(lat, lng, 8); // H3 v4
  // hoặc h3.geoToH3Address(lat, lng, 8); // H3 v3
  ```

### c. Lọc thô danh sách (Candidate Pool)
- Lấy danh sách mã lục giác xung quanh nạn nhân (bán kính 2 vòng):
  ```java
  List<String> kRingList = h3.gridDisk(centerHex, 2); // H3 v4
  // hoặc h3.kRing(centerHex, 2); // H3 v3
  ```

## 2. PHẦN 2: Tính toán ETA & Xếp hạng

### a. Routing Matrix API
- Gọi API khoảng cách của GraphHopper hoặc OSRM:
  ```json
  POST /table
  {
    "sources": [[lng1, lat1], [lng2, lat2]],
    "destinations": [[lng_victim, lat_victim]]
  }
  ```
- Trích xuất `duration` (ETA - giây) để sắp xếp.

## 3. PHẦN 3: Chiến lược điều phối (Hybrid Dispatch)

### Nhánh A: CRITICAL (Khẩn cấp) - SMART BROADCAST
- Chọn Top 10 ứng viên có ETA nhanh nhất.
- Phát tín hiệu WebSocket / Push Notification đồng loạt.

### Nhánh B: NORMAL (Thường) - SEQUENTIAL BATCHING
- Chọn Top 3 ứng viên đầu tiên. Đếm ngược 15s.
- Nếu chưa ai nhận, chuyển tiếp đến Top 3 tiếp theo.

---

## Kế hoạch thực hiện mã nguồn:
- `H3Config.java`: Bean đăng ký `H3Core`.
- `VolunteerRepository.java`: Thêm Native Query hỗ trợ lọc danh sách bằng `h3_index`.
- `DispatchMissionUseCase.java`: Điều chỉnh phân luồng nghiệp vụ.
