# Hướng dẫn Kiểm tra Luồng Socket (Live Tracking)

Tài liệu này hướng dẫn cách kiểm tra tính năng cập nhật vị trí thời gian thực giữa Volunteer và Victim bằng công cụ `socket_inspector.html`.

## 1. Chuẩn bị
1. **Mission ID**: Đảm bảo Mission ID trong file `socket_inspector.html` trùng với Mission ID đang thực hiện trong App.
2. **Trạng thái Mission**: Mission phải ở trạng thái `PROCESSING`.

## 2. Kịch bản 1: Kiểm tra Volunteer phát dữ liệu (App -> Web)
Mục tiêu: Xác nhận App Volunteer gửi tọa độ lên server thành công.

1. Mở file `socket_inspector.html` bằng trình duyệt (Chrome/Edge).
2. Kiểm tra dòng trạng thái: Phải hiển thị **✅ ĐÃ KẾT NỐI**.
3. Trên App (Emulator):
   - Đăng nhập quyền **Volunteer**.
   - Chọn Mission tương ứng.
   - Nhấn **Dẫn đường** (Navigate) tới vị trí Nạn nhân.
   - Nhấn nút **Simulate** (Chế độ Debug) để App tự chạy giả lập.
4. **Kết quả**: Trên màn hình Web, bạn sẽ thấy các dòng log `📍 Nhận vị trí` và `🛣 Nhận lộ trình` nhảy liên tục.

## 3. Kịch bản 2: Kiểm tra Victim nhận dữ liệu (Web -> App)
Mục tiêu: Xác nhận App Victim nhận dữ liệu nén (Polyline), giải mã và hiển thị được trên bản đồ.

1. Trên App (Emulator):
   - Đăng nhập quyền **Victim**.
   - Vào màn hình **Bản đồ cứu trợ** (Victim Map).
   - Đợi App kết nối tới Socket (Logcat sẽ báo `Joined tracking mission channel`).
2. Trên Web (`socket_inspector.html`):
   - Nhấn nút **1. Gửi Lộ trình (Route)**: 
     - **Web**: Gửi một chuỗi ký tự nén (ví dụ: `_p~iF~ps|U...`).
     - **App**: Nhận chuỗi, tự động giải mã (Decode) và vẽ đường đi màu xanh lên bản đồ.
   - Nhấn nút **2. Gửi Vị trí (Location)**: 
     - **Web**: Gửi tọa độ Lat/Lng.
     - **App**: Hiển thị Marker Tình nguyện viên và di chuyển theo tọa độ nhận được.
3. **Kết quả**: Bạn thấy Marker và đường đi hiển thị mượt mà trên máy ảo. Điều này chứng minh thuật toán giải mã Polyline trong App hoạt động tốt.

## 4. Các lỗi thường gặp
- **Không thấy log trên Web**: Kiểm tra xem `missionId` trong code HTML đã đúng chưa.
- **App không hiện Marker**: 
  - Kiểm tra xem Mission của bạn có đang ở trạng thái `PROCESSING` không.
  - Kiểm tra Logcat xem có lỗi `NoClassDefFoundError` (Ktor) không. Nếu có, hãy Sync Gradle lại.
- **CORS Error trên Web**: Nếu trình duyệt chặn kết nối, hãy thử mở bằng cách chuột phải vào file chọn `Open with Live Server` (nếu dùng VS Code) hoặc dùng Chrome với flag `--disable-web-security`.
