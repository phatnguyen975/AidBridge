# Tài liệu Thiết kế: Giao diện Cấu hình Vùng nguy hiểm (Dangerous Zones)

## 1. Tổng quan
Tính năng này cho phép Quản trị viên (Admin) thiết lập các khu vực địa lý cần tránh (ngập lụt, sạt lở, vùng có nguy cơ cao) trực tiếp trên bản đồ. Các vùng này sẽ được sử dụng bởi thuật toán định tuyến để tìm đường đi an toàn nhất cho cứu trợ.

## 2. Các chế độ hiển thị

### 2.1 Chế độ Bình thường (View Mode)
- **Mục tiêu:** Giúp Admin bao quát các vùng đang hoạt động mà không gây cản trở các tác vụ khác.
- **Hiển thị:**
    - Vùng nguy hiểm hiện ra dưới dạng các Polygon màu đỏ nhạt (Semi-transparent Red, Alpha ~30%).
    - Không thể tương tác trực tiếp (để tránh chạm nhầm khi đang xem SOS/Aid markers).
    - Các Layer khác (SOS, Aid, Route) hiển thị bình thường.

### 2.2 Chế độ Tiêu điểm (Focus/Edit Mode)
- **Mục tiêu:** Tập trung tối đa vào việc vẽ và chỉnh sửa.
- **Hiển thị:**
    - **Dọn dẹp hiện trường (UI Clean-up):** Toàn bộ các thành phần giao diện khác trên bản đồ (Nút tìm kiếm, Panel lọc Admin, FAB điều hướng...) sẽ **biến mất hoàn toàn**.
    - **Raw Map Focus:** Chỉ hiển thị duy nhất lớp bản đồ nền (Raw Map), các Vùng nguy hiểm hiện có, và bộ công cụ chỉnh sửa chuyên dụng. Điều này giúp loại bỏ mọi sự xao nhãng và tránh việc bấm nhầm vào các thành phần UI khác.
    - **Lớp phủ Dim:** Bản đồ nền được phủ một lớp tối nhẹ (Opacity 20%) để làm nổi bật vùng đang chỉnh sửa.

---

## 3. Quy trình Thao tác Chi tiết

### 3.1 Tạo mới Vùng nguy hiểm (Creation)
Để đạt độ chính xác cao trên màn hình cảm ứng, hệ thống sử dụng cơ chế **Tâm ngắm cố định (Fixed Crosshair)**:

1.  **Kích hoạt:** Admin nhấn nút "Thêm vùng mới" từ Panel Admin.
2.  **Xác định đỉnh (Vertex):**
    - Một tâm ngắm xuất hiện ở chính giữa màn hình.
    - Admin di chuyển (Pan) bản đồ để tâm ngắm trùng với góc của vùng cần vẽ.
    - Nhấn nút **[+] (Add Point)**: Một đỉnh được cố định. Một đường kẻ mờ (Rubber-band) nối từ đỉnh cuối đến tâm ngắm để Admin xem trước cạnh tiếp theo.
3.  **Hoàn tất:** Sau khi chọn ít nhất 3 điểm, nhấn nút **[Done]**: Hệ thống tự động nối điểm cuối về điểm đầu và hiện Modal nhập thông tin (Tên vùng, Mức độ nguy hiểm).

### 3.2 Chỉnh sửa Vùng hiện có (Modification)
Khi chọn một vùng để sửa, Polygon sẽ hiện các nút nắm (Handles):

*   **Điều chỉnh đỉnh (Move Vertex):**
    - Chạm vào một đỉnh để chọn. Tâm ngắm sẽ tự động nhảy về đỉnh đó.
    - Admin di chuyển bản đồ để dời đỉnh đến vị trí mới và nhấn **[Confirm Move]**.
*   **Thêm đỉnh mới (Add Vertex - Ghost Points):**
    - Giữa mỗi cạnh của Polygon sẽ có một **"Điểm ảo" (Ghost Point)** mờ.
    - Admin chạm và kéo điểm ảo này -> Nó lập tức trở thành một đỉnh thật, cho phép tạo ra các hình dáng phức tạp hơn.
*   **Xóa đỉnh (Delete Vertex):**
    - Chạm chọn một đỉnh. Một icon **Thùng rác** nhỏ sẽ hiện ngay bên cạnh.
    - Nhấn icon để xóa đỉnh. Polygon tự động nối lại theo các đỉnh còn lại.

### 3.3 Xóa Vùng nguy hiểm
- Có thể xóa từ danh sách quản lý (Side Panel) hoặc nhấn giữ vào vùng đó trên bản đồ để chọn **[Xóa vùng]**.

---

## 4. Các yếu tố UX Polish (Độ tinh tế)

*   **Nút Hoàn tác (Undo):** Cho phép rút lại đỉnh vừa đặt sai khi đang vẽ.
*   **Hệ thống Nam châm (Snapping):** Khi di chuyển một đỉnh lại gần đường giao thông hoặc đỉnh của một vùng khác, nó sẽ tự động "hít" vào để đảm bảo dữ liệu địa lý khít nhau.
*   **Màu sắc theo mức độ:**
    - **Đỏ đậm:** Nguy hiểm cao (Cấm tuyệt đối).
    - **Cam/Vàng:** Nguy hiểm trung bình (Cảnh báo, hạn chế đi qua).

## 5. Cấu trúc Dữ liệu (Tham khảo)
Vùng nguy hiểm được lưu trữ dưới dạng GeoJSON Polygon:
```json
{
  "id": "zone_001",
  "name": "Ngập lụt Nguyễn Văn Linh",
  "severity": "HIGH",
  "geometry": {
    "type": "Polygon",
    "coordinates": [[[lng1, lat1], [lng2, lat2], ...]]
  }
}
```
