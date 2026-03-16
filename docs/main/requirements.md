# AidBridge - Requirements

## Phần 1. Tổng quan về đồ án

**Disaster Relief Coordinator (DRC)** là hệ thống ứng dụng di động hỗ trợ điều phối cứu trợ khẩn cấp trong thiên tai. Dự án tập trung giải quyết bài toán mất cân bằng nguồn lực (nơi thừa, nơi thiếu) bằng cách kết nối trực tiếp Nạn nhân, Mạnh thường quân và Tình nguyện viên trên một bản đồ thời gian thực. Ứng dụng sử dụng trí tuệ nhân tạo để phân loại mức độ khẩn cấp, giúp các đơn vị cứu hộ ưu tiên những trường hợp nguy hiểm đến tính mạng trước các nhu cầu nhu yếu phẩm thông thường.

## Phần 2. Mục tiêu đồ án

- **Chuyển đổi số cứu trợ:** Thay thế cách làm truyền thống qua bài đăng mạng xã hội rời rạc bằng một nền tảng dữ liệu tập trung.
- **Ứng dụng AI:** Tự động hóa việc phân loại hàng ngàn tin nhắn SOS để giảm tải cho điều phối viên.
- **Tối ưu lộ trình:** Sử dụng dữ liệu địa lý để hướng dẫn tình nguyện viên di chuyển an toàn và nhanh nhất.
- **Đảm bảo tính minh bạch:** Theo dõi hành trình quà tặng từ lúc quyên góp đến khi tận tay nạn nhân.

## Phần 3. Mô tả tính năng của Ứng dụng

Xây dựng ứng dụng Điều phối Cứu trợ Thông minh, kết nối Nạn nhân - Tình nguyện viên - Mạnh thường quân qua mô hình Trạm trung chuyển.

### 1. Phân hệ người dùng nặc danh - Guest

**1.1. Hệ thống SOS khẩn cấp (Quick SOS) - Cứu hộ**

- Người dùng nhấn nút SOS -> Điền kèm thêm là Tên, Số điện thoại, Nội dung (đặc điểm căn nhà, đang có bao nhiêu người, tình trạng sức khỏe hiện tại, ...), Hình ảnh (nếu có) -> Nhấn gửi -> Hệ thống gửi GPS + các thông tin trên.
- Không yêu cầu đăng nhập để đảm bảo tốc độ trong tình huống nguy cấp.

**1.2. Xem bản đồ cứu trợ công cộng**

- **Marker Trạm trung chuyển (Hub Marker):** Sử dụng biểu tượng Ngôi nhà màu xanh lá hoặc Hộp tiếp tế. Khi bấm vào sẽ hiển thị tên trạm, trạng thái kho (ví dụ: "Còn gạo, hết thuốc") và khoảng cách từ vị trí hiện tại.
- **Marker Điểm trú ẩn (Shelter Marker):** Sử dụng biểu tượng Lá cờ hoặc Hình người trú ẩn. Khi bấm vào sẽ hiển thị sức chứa hiện tại và các tiện ích có sẵn như điện, nước sạch.
- **Marker Tuyến đường an toàn (Safe Path):** Sử dụng Polyline màu xanh lá nối từ vị trí Nạn nhân đến Trạm/Điểm trú ẩn gần nhất.
- **Heatmap SOS (Điểm nóng cứu trợ):** Dựa trên mật độ các yêu cầu SOS và Tiếp tế đang tồn tại trong Database. Khu vực nào tập trung nhiều điểm ghim SOS trong bán kính hẹp sẽ tự động hiển thị quầng sáng màu Đỏ/Cam.

**1.3. Đăng ký / Đăng nhập**

- Nhập họ tên, email. số điện thoại, mật khẩu khi đăng ký.
- Nhập email, mật khẩu khi đăng nhập.
- Xác thực tài khoản bằng OTP qua Email / SĐT.
- Chọn vai trò Nạn nhân, Tình nguyện viên, hoặc Mạnh thường quân.
- Mật khẩu mã hóa bằng **bcrypt**.

### 2. Phân hệ người dùng nạn nhân - Victim

**2.1. Hệ thống SOS khẩn cấp (SOS) - Cứu hộ**

- Tương tự phần Guest nhưng có lưu thông tin vào tài khoản.

**2.2. Yêu cầu thực phẩm tiếp tế - Cứu trợ**

- Hệ thống 5 danh mục 2 cấp: Thuốc (Cảm/sốt, Tiêu hóa, Băng gạc), Quần áo (Bộ, Chăn màn, Áo mưa), Thức ăn (Gạo, Mì tôm, Đồ hộp), Nước uống (Nước đóng chai, Sữa, Nước điện giải), Khác (Tã).
- Nhập tên, số điện thoại, chọn nhu yếu phẩm và nhập số lượng người (Người lớn, Người già, Trẻ em), và các mô tả chi tiết hơn (nếu có). Số lượng vật phẩm tự động tính bằng số người.
- **AI Voice Support:** Hỗ trợ đọc yêu cầu bằng giọng nói, AI tự động phân loại vào 4 danh mục trên.

**2.3. Gửi SOS hộ người thân - Cứu hộ**

- Nhập thông tin người cần cứu trợ (Họ tên, số điện thoại, địa chỉ, mô tả nhà, tình trạng sức khỏe, số lượng người).

**2.4. Theo dõi hành trình live tracking**

- Xem vị trí của Tình nguyện viên đang di chuyển đến vị trí của mình.

**2.5. Chatbox**

- Chat trực tiếp with Tình nguyện viên đang thực hiện giao hàng.

**2.6. Quản lý hồ sơ & lịch sử**

- Xem lại lịch sử các yêu cầu đã gửi trong vòng 7 ngày gần nhất ngay cả khi đã offline (cho chọn khoảng thời gian).

### 3. Phân hệ người dùng tình nguyện viên - Volunteer

**3.1. Tiếp nhận nhiệm vụ tự động**

- Đảm nhận cả 2 vai trò: Giao hàng (tiếp tế) và cứu hộ (đáp ứng SOS).
- Nhận thông báo nhiệm vụ từ hệ thống và có **30 giây** để quyết định Chấp nhận/Từ chối.

**3.2. Màn hình nhiệm vụ hiện tại**

- Thanh trạng thái lộ trình:
  - Đến trạm -> Lấy hàng -> Đến nhà nạn nhân -> Hoàn thành (đối với tiếp tế).
  - Đến nhà nạn nhân -> Hoàn thành (đối với cứu hộ).
- Thông tin nạn nhân: Tên, SĐT, Ghi chú vị trí, Vật phẩm yêu cầu.

**3.3. Bản đồ điều hướng:** Hiển thị đường đi và các địa điểm cần đến để hoàn thành nhiệm vụ.

**3.4. Chatbox:** Chat trực tiếp với Nạn nhân trong quá trình làm nhiệm vụ.

**3.5. Quản lý trạng thái hoạt động:** Nút bật/tắt (Online/Offline) sẵn sàng nhận nhiệm vụ. Nếu TNV tắt trạng thái, thì họ sẽ không nhận cứu trợ nữa.

**3.6. Lịch sử nhiệm vụ & Thống kê:** Lịch sử chi tiết: Xem lại danh sách các ca cứu trợ đã hoàn thành. Bảng thành tích (Impact Dashboard) hiển thị số nhiệm vụ đã hoàn thành và điểm uy tín (Rating).

**3.7. Lưu trữ & Đồng bộ Offline:** Cho phép tải trước bản đồ của khu vực.

**3.8. Danh sách nạn nhân gần nhất:** Hệ thống đề xuất danh sách các nạn nhân cần cứu gần nhất để TNV chủ động chọn.

### 4. Phân hệ người dùng Mạnh thường quân - Sponsor

**4.1. Đăng ký hàng cứu trợ:** Nhập thông tin hàng, danh mục, số lượng, mô tả, hình ảnh (nếu có), và thời gian dự kiến giao hàng (sau đó hệ thống sẽ đề xuất trạm phù hợp để đóng góp).

**4.2. Quản lý ký gửi & Chọn trạm:** Smart Hub Selection tự động đề xuất trạm gần nhất đang thiếu hụt hàng.

**4.3. Quy trình Ký gửi hàng:** Sau khi đăng ký sẽ tạo một mã QR để Mạnh thường quân bàn giao tại trạm, nhận thông báo xác nhận.

**4.4. Thống kê:** Bảng tổng kết đóng góp và hệ thống vinh danh (huy hiệu, điểm tích lũy).

### 5. Phân hệ người dùng Quản lý trạm - Staff

**5.1. Quản lý nhập kho:** Quét mã QR của Mạnh thường quân, tự động cộng số lượng vật phẩm vào trạm.

**5.2. Quản lý Xuất kho:** Quét mã QR của Tình nguyện viên, cho phép xuất kho, thực hiện Atomic Inventory Update (trừ số lượng realtime).

**5.3. Giám sát trạng thái trạm:** Theo dõi tồn kho, bật/tắt trạng thái trạm khi có sự cố khẩn cấp để hệ thống điều phối lại TNV.

### 6. Phân hệ người dùng Quản trị viên - Admin

**6.1. Quản lý người dùng & Phân quyền:** Chặn tài khoản spam, phân bổ Staff vào các trạm.

**6.2. Quản lý Mạng lưới Trạm trung chuyển:** Khởi tạo vị trí từng trạm, thiết lập danh mục nhận hàng cho từng trạm, cài đặt ngưỡng "Tồn kho thấp".

**6.3. Giám sát Điều phối tự động:** Theo dõi Global Map, can thiệp thủ công (Manual Override), điều chỉnh trọng số Priority Score.

**6.4. Cảnh báo khẩn cấp:** Gửi Push Notification / Email khẩn cấp (Broadcast).

**6.5. Báo cáo & Minh bạch hóa:** Sử dụng AI Summary để tổng hợp báo cáo ngày, kết xuất dữ liệu (Export) ra file Excel/CSV.

### 7. Hệ thống

**7.1. Hệ thống điều phối tự động**

- **Bước 1:** Tiếp nhận yêu cầu, xác định Vị trí, Loại hàng, và Độ ưu tiên.
- **Bước 2:** Quét tìm TNV ONLINE/AVAILABLE với bán kính mở rộng dần (1km -> 3km -> 5km -> 10km).
- **Bước 3:** Tính toán Priority Score. Hệ thống tính điểm để lập danh sách **Top 10 TNV phù hợp nhất**.
- **Bước 4:**

**A. Đối với nhiệm vụ SOS Khẩn cấp (Có rủi ro tính mạng): Áp dụng Broadcast**

- **Logic:** Tốc độ là ưu tiên tuyệt đối.
- **Flow:** Hệ thống lọc ra Top 5 hoặc Top 10 tình nguyện viên có điểm Priority Score cao nhất. Bắn Broadcast cho tất cả cùng lúc. Ai nhận trước thì đi cứu. (Race condition)

**B. Đối với nhiệm vụ Tiếp tế Nhu yếu phẩm: Áp dụng Sequential Batches (Gửi tuần tự theo nhóm)**

- **Logic:** Việc giao gạo, thuốc men, quần áo không yêu cầu phải xuất phát trong vòng 5 giây tiếp theo. Ta có thể ưu tiên việc chọn đúng người (gần trạm nhất, điểm cao nhất) để tối ưu chi phí vận hành.
- **Flow:**
  - Hệ thống tìm người có điểm cao số #1. Gửi thông báo độc quyền cho người này và cho họ 15 giây để nhận.
  - Nếu người #1 bỏ qua (Timeout), hệ thống nhóm người #2, #3, #4 lại và bắn Broadcast cho 3 người này, cho họ 20 giây.
  - Nếu vẫn không ai nhận, mở rộng bán kính và gửi cho các nhóm tiếp theo.

- **Bước 5:** Kích hoạt nhiệm vụ: Khi TNV xác nhận, trạng thái đơn hàng chuyển sang ASSIGNED. Hệ thống gửi thông tin TNV (Tên, SĐT, phương tiện) cho Nạn nhân và bắt đầu luồng Live Tracking.

**C. Công thức tính Priority Score ($S$)**

$$S = (D \times 40\\%) + (R \times 20\\%) + (T \times 15\\%) + (A \times 15\\%) + (E \times 10\\%)$$

Trong đó:

- **D (Distance):** Khoảng cách.
- **R (Rating):** Điểm đánh giá.
- **T (Tasks):** Số nhiệm vụ đã hoàn thành.
- **A (Average Response):** Thời gian phản hồi trung bình.
- **E (Experience):** Kinh nghiệm khu vực.

**7.2. Quy trình ký gửi của Mạnh thường quân**

Quy trình này đảm bảo hàng hóa từ nhà hảo tâm được đưa vào hệ thống một cách có kiểm soát và đúng nơi đang thiếu hụt.

1.  **Đăng ký đóng góp:** Mạnh thường quân chọn vật phẩm từ 4 danh mục thiết yếu: Thuốc, Quần áo, Thức ăn, Nước uống. Người dùng nhập số lượng, ảnh chụp thực tế và hạn sử dụng (nếu có).
2. **Lựa chọn Trạm thông minh (Smart Hub Selection):** Hệ thống dựa trên GPS của Mạnh thường quân và dữ liệu thiếu hụt thực tế để gợi ý trạm tập kết gần nhất đang cần loại hàng đó.
3. **Tạo mã Ký gửi (Donation QR):** Sau khi xác nhận trạm, hệ thống cấp một mã QR định danh duy nhất cho lô hàng ký gửi.
4. **Bàn giao và Kiểm tra:** Mạnh thường quân mang hàng đến trạm. Nhân viên trực trạm (Staff) kiểm tra chất lượng cảm quan của hàng hóa thực tế.
5. **Xác nhận Nhập kho:** Staff dùng ứng dụng quét mã QR của Mạnh thường quân.
6. **Cập nhật tồn kho điện tử:** Ngay khi quét thành công, hệ thống thực hiện lệnh cộng số lượng vào bảng Inventories của trạm đó theo thời gian thực.
7. **Hoàn tất:** Mạnh thường quân nhận được thông báo hàng đã nhập kho thành công và bắt đầu theo dõi hành trình quà tặng

**7.3. Quy trình rút hàng của Tình nguyện viên**

Quy trình này kết nối trực tiếp kho hàng với nạn nhân thông qua sự điều phối của thuật toán tự động.

1. **Chấp nhận nhiệm vụ:** Hệ thống tự động phân công nhiệm vụ cho Tình nguyện viên (TNV) dựa trên điểm ưu tiên (Priority Score). TNV có 30 giây để xác nhận nhiệm vụ trên ứng dụng.
2. **Di chuyển đến Trạm:** App hiển thị lộ trình ngắn nhất dẫn TNV đến trạm trung chuyển đã được hệ thống chỉ định (nơi có đủ hàng cho đơn SOS).
3. **Xác nhận lấy hàng (Outbound Verification):** TNV xuất trình Mã QR nhiệm vụ trên điện thoại cho Staff tại trạm.
4. **Quét mã và Trừ kho:** Staff quét mã QR của TNV để xác nhận rút hàng. Hệ thống thực hiện một Atomic Transaction để đồng thời:
  - Trừ số lượng vật phẩm tương ứng trong kho của trạm.
  - Chuyển trạng thái nhiệm vụ từ ASSIGNED sang PICKED_UP (Đã lấy hàng).
  - Ghi nhật ký (Log) chi tiết: Ai lấy, lấy lúc nào, lấy mặt hàng gì.
5. **Bắt đầu giao hàng:** Sau khi xác nhận xuất kho, tính năng Live Tracking được kích hoạt. Nạn nhân nhận được thông báo TNV đã lấy hàng và có thể theo dõi vị trí xe trên bản đồ.
6. **Hoàn tất giao hàng:** TNV đến vị trí nạn nhân, chụp ảnh xác nhận hoặc xác nhận bằng mã của nạn nhân để đóng Ticket và cập nhật trạng thái COMPLETED.

**7.4. Hệ thống thông báo**

1. Thông báo dành cho nạn nhân

- **Xác nhận SOS/Yêu cầu:** "Yêu cầu của bạn đã được hệ thống ghi nhận và đang tìm tình nguyện viên phù hợp".
- **Kết nối thành công:** "Đã tìm thấy Tình nguyện viên [Tên]. Dự kiến đến trạm lấy hàng sau [X] phút".
- **Trạng thái Logistics:** "Tình nguyện viên đã lấy hàng thành công tại trạm và đang trên đường đến chỗ bạn".
- **Live Tracking:** "Tình nguyện viên chỉ còn cách bạn 500m, vui lòng chuẩn bị thiết bị để liên lạc".
- **Hoàn thành:** "Đơn cứu trợ đã hoàn tất. Bạn có muốn gửi lời cảm ơn hoặc đánh giá cho tình nguyện viên không?".

2. Thông báo dành cho tình nguyện viên

- **Lệnh Dispatch (30 giây):** "Nhiệm vụ mới: [Loại đơn]. Bạn có 30 giây để chấp nhận trước khi chuyển cho người khác".
- **Tin nhắn mới:** "Nạn nhân vừa gửi một tin nhắn/hình ảnh mới cho bạn".

3. Thông báo dành cho mạnh thường quân

- **Xác nhận ký gửi:** "Mã QR ký gửi hàng hóa của bạn đã được tạo. Vui lòng mang hàng đến Trạm [Tên]".
- **Nhập kho thành công:** "Nhân viên trực trạm đã xác nhận nhập kho [Số lượng] hàng của bạn. Cảm ơn tấm lòng của bạn".

4. Thông báo dành cho nhân viên trực trạm

- **Sắp có hàng nhập:** "Mạnh thường quân [Tên] đang di chuyển đến trạm để ký gửi [Loại hàng]".
- **Yêu cầu lấy hàng:** "Tình nguyện viên [Tên] đã chấp nhận đơn hàng #... và đang đến trạm để rút hàng".
- **Cảnh báo tồn kho:** "Mặt hàng [Tên] tại trạm đang dưới mức tối thiểu. Hệ thống đã báo cáo cho Admin để điều phối thêm".

5. Thông báo dành cho quản trị viên (Admin)

- **Cảnh báo tồn kho hệ thống:** "Trạm trung chuyển [Tên] đang dưới ngưỡng tồn kho thấp cho mặt hàng [Tên]. Vui lòng kiểm tra và thông báo cho Mạnh thường quân".
- **Phát hiện bất thường:** "Nghi ngờ giả mạo: Hệ thống ghi nhận [Số lượng] báo cáo Hazard tại cùng một vị trí trong thời gian ngắn. Vui lòng xác minh để làm sạch bản đồ".
- **Sự cố tại Trạm:** "Trạm [Tên] vừa báo cáo sự cố khẩn cấp (Ngập/Mất điện) và tạm ngừng hoạt động. Hệ thống đã tự động điều phối lại các đơn hàng liên quan".

6. Thông báo dành cho toàn bộ hệ thống

- **Thông báo Trạm cứu trợ mới:** "Trạm trung chuyển dã chiến vừa được thiết lập tại [Địa điểm]. Người dân có thể đến nhận nhu yếu phẩm tại đây".
