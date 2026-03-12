# AidBridge – Kế hoạch Phát triển Android (Implementation Plan)

> **Ngôn ngữ:** Java 17 | **UI:** XML Layouts thuần túy | **Kiến trúc:** MVVM + Clean Architecture

> **Min SDK:** 29 | **Target SDK:** 36 | **Build:** Gradle 8.13+ với Version Catalog

## Tổng quan các Phase

| #   | Phase                                 | Mục tiêu chính                                     | Độ phức tạp |
| --- | ------------------------------------- | -------------------------------------------------- | ----------- |
| 1   | Nền tảng & Kiến trúc                  | Project skeleton, DI, Network, DB, Theme           | ⭐⭐        |
| 2   | Xác thực & Phiên làm việc             | Login, Register đa bước, OTP, JWT                  | ⭐⭐⭐      |
| 3   | Bản đồ & SOS Khẩn cấp (Guest)         | Google Maps, Marker, Heatmap, Quick SOS            | ⭐⭐⭐⭐    |
| 4   | Luồng Nạn nhân                        | Yêu cầu tiếp tế, Live Tracking, Chat, Lịch sử      | ⭐⭐⭐⭐    |
| 5   | Luồng Tình nguyện viên                | Dispatch 30s, Màn hình nhiệm vụ, Điều hướng bản đồ | ⭐⭐⭐⭐⭐  |
| 6   | Luồng Mạnh thường quân & Quản lý trạm | Đăng ký đóng góp, QR, Quản lý kho                  | ⭐⭐⭐⭐    |
| 7   | Thông báo, Offline & Hoàn thiện       | FCM đầy đủ, Room offline sync, Background service  | ⭐⭐⭐      |

## Phase 1 – Nền tảng & Kiến trúc (Foundation)

### Mục tiêu

Xây dựng bộ khung sườn dự án hoàn chỉnh: DI container, HTTP client, Room database, theme hệ thống, base classes và navigation graph. Đây là nền tảng bắt buộc phải hoàn thiện trước khi viết bất kỳ feature nào.

### Tầng Data

#### Package: `data/local/`

```
AppDatabase.java               – Room @Database: liệt kê tất cả entities
TokenDao.java                  – DAO: lưu/đọc/xóa JWT token (dùng EncryptedSharedPreferences, không vào Room)
```

#### Package: `data/remote/`

```
ApiService.java                – Interface Retrofit: khai báo placeholder endpoint (chưa cần implement)
AuthInterceptor.java           – OkHttp Interceptor: đọc token từ EncryptedSharedPreferences, gán header Authorization: Bearer
TokenRefreshInterceptor.java   – OkHttp Interceptor: bắt 401, tự động gọi /auth/refresh-token, retry request
NetworkResultWrapper.java      – Sealed-class-like wrapper: Success<T>, Error, Loading để ViewModel xử lý
```

#### Package: `data/repository/`

```
(Trống ở Phase này – sẽ thêm dần theo từng feature)
```

### Tầng Domain

#### Package: `domain/model/`

```
User.java                      – Domain model cơ bản: id, name, email, phone, role
```

#### Package: `domain/repository/`

```
(Trống ở Phase này)
```

#### Package: `domain/usecase/`

```
(Trống ở Phase này)
```

### Tầng UI

#### Package: `ui/base/`

```
BaseActivity.java              – extends AppCompatActivity: inject Hilt, setup WindowManager
BaseFragment.java              – extends Fragment: cung cấp helper showLoading(), hideLoading(), showError()
BaseViewModel.java             – extends ViewModel: giữ MutableLiveData<Boolean> isLoading, MutableLiveData<String> errorMessage
```

#### Package: `ui/splash/`

```
SplashActivity.java            – Kiểm tra token còn hạn → route đến MainActivity hoặc AuthActivity
```

#### Package: `di/`

```
AppModule.java                 – @Module: provide Context, SharedPreferences
NetworkModule.java             – @Module: provide OkHttpClient, Retrofit, ApiService
DatabaseModule.java            – @Module: provide AppDatabase, các DAO
AidBridgeApplication.java      – @HiltAndroidApp
```

#### Package: `utils/`

```
Constants.java                 – BASE_URL, SharedPrefs keys, timeout values
TokenManager.java              – Wrapper EncryptedSharedPreferences: saveTokens(), getAccessToken(), clearTokens()
PermissionHelper.java          – Utility check/request LOCATION, CAMERA, POST_NOTIFICATIONS
NetworkUtils.java              – isConnected()
```

#### Navigation (`res/navigation/`)

```
nav_graph.xml                  – Khai báo NavGraph tổng: auth_graph (nested), main_graph (nested)
auth_nav_graph.xml             – SplashActivity → LoginFragment → RegisterFragment → OtpFragment
main_nav_graph.xml             – HomeMapFragment, ProfileFragment, HistoryFragment, MissionFragment (placeholder)
```

#### Theme (`res/values/`)

```
themes.xml                     – Theme.AidBridge extends Material3: colorPrimary=#2E7D32, colorSecondary=#F57F17
colors.xml                     – Bảng màu hệ thống: sos_red, safe_green, warning_orange, hub_blue
strings.xml                    – Chuỗi toàn cục: app_name, loading, error_network, error_generic
dimens.xml                     – spacing_xs(4dp), spacing_sm(8dp), spacing_md(16dp), spacing_lg(24dp)
```

### Thư viện cần tích hợp (libs.versions.toml)

```toml
# libs.versions.toml – khai báo đầy đủ ở Phase 1
hilt             = "2.56"
retrofit         = "2.11.0"
okhttp           = "4.12.0"
room             = "2.7.0"
lifecycle        = "2.9.0"
navigation       = "2.9.0"
appcompat        = "1.7.1"
material         = "1.13.0"
constraintlayout = "2.2.1"
security-crypto  = "1.1.0-alpha06"
```

### Màn hình UI/Figma cần cung cấp

| Màn hình                     | Mô tả                                                   |
| ---------------------------- | ------------------------------------------------------- |
| **Splash Screen**            | Logo AidBridge, loading animation, tự động chuyển trang |
| **Skeleton / Loading State** | Shimmer placeholder dùng chung cho toàn app             |

## Phase 2 – Xác thực & Phiên làm việc (Authentication)

### Mục tiêu

Triển khai toàn bộ luồng đăng ký đa bước (multi-step wizard), đăng nhập, xác thực OTP qua email/SĐT, chọn vai trò, và quản lý JWT access/refresh token. Đây là cửa ngõ bắt buộc để mở khóa các tính năng theo role.

### Tầng Data

#### Package: `data/remote/dto/`

```
LoginRequest.java              – { email, password }
RegisterRequest.java           – { name, email, phone, password, role }
OtpVerifyRequest.java          – { email/phone, otpCode }
AuthResponse.java              – { accessToken, refreshToken, user: UserDto }
UserDto.java                   – { id, name, email, phone, role, avatarUrl }
```

#### Package: `data/remote/`

```
AuthApiService.java            – Interface Retrofit riêng cho auth (không cần token):
                                 @POST("/auth/login") → Call<AuthResponse>
                                 @POST("/auth/register") → Call<AuthResponse>
                                 @POST("/auth/verify-otp") → Call<Void>
                                 @POST("/auth/resend-otp") → Call<Void>
                                 @POST("/auth/refresh-token") → Call<AuthResponse>
```

#### Package: `data/repository/`

```
AuthRepositoryImpl.java        – implements AuthRepository: gọi AuthApiService, lưu kết quả vào TokenManager
```

### Tầng Domain

#### Package: `domain/repository/`

```
AuthRepository.java            – Interface: login(), register(), verifyOtp(), resendOtp()
```

#### Package: `domain/usecase/auth/`

```
LoginUseCase.java              – Validate input → gọi AuthRepository.login() → trả về LiveData<User>
RegisterUseCase.java           – Validate form đa bước → gọi AuthRepository.register()
VerifyOtpUseCase.java          – Kiểm tra định dạng OTP → gọi AuthRepository.verifyOtp()
```

#### Package: `domain/model/`

```
UserRole.java                  – Enum: GUEST, VICTIM, VOLUNTEER, SPONSOR, STAFF, ADMIN
```

### Tầng UI

#### Package: `ui/auth/`

```
AuthActivity.java              – Hosts auth_nav_graph.xml, không cần toolbar phức tạp
LoginFragment.java             – email + password, nút "Đăng nhập", link "Đăng ký", "Quên mật khẩu"
LoginViewModel.java            – LoginUseCase, xử lý trạng thái loading/error

RegisterFragment.java          – Host ViewPager2 chứa 3 bước wizard (xem dưới)
RegisterStep1Fragment.java     – Bước 1: Họ tên, Email, SĐT
RegisterStep2Fragment.java     – Bước 2: Mật khẩu, Xác nhận mật khẩu
RegisterStep3Fragment.java     – Bước 3: Chọn vai trò (3 Card: Nạn nhân / TNV / MTS)
RegisterViewModel.java         – RegisterUseCase, giữ state toàn bộ wizard

OtpFragment.java               – Nhập 6 chữ số OTP, countdown resend 60s
OtpViewModel.java              – VerifyOtpUseCase, ResendOtpUseCase, countdown Timer

ForgotPasswordFragment.java    – Nhập email để nhận link đặt lại mật khẩu
```

#### Layout XML cần tạo

```
fragment_login.xml
fragment_register.xml           – Chứa ViewPager2 + StepIndicator (3 dots)
fragment_register_step1.xml
fragment_register_step2.xml
fragment_register_step3.xml     – 3 MaterialCardView cho role selection
fragment_otp.xml                – 6 ô OTP riêng biệt (hoặc 1 TextInputEditText)
fragment_forgot_password.xml
```

### Thư viện cần tích hợp

```toml
# Đã có từ Phase 1, sử dụng thêm:
viewpager2 = "1.1.0"           # Wizard đăng ký đa bước
```

### Màn hình UI/Figma cần cung cấp

| Màn hình              | Chi tiết cần thiết                                           |
| --------------------- | ------------------------------------------------------------ |
| **Login Screen**      | Layout email/password, vị trí logo, màu nút chính            |
| **Register – Bước 1** | Form thông tin cá nhân, validation hints                     |
| **Register – Bước 2** | Form mật khẩu, strength indicator (nếu có)                   |
| **Register – Bước 3** | 3 card vai trò với icon và mô tả ngắn mỗi vai trò            |
| **OTP Verification**  | Kiểu nhập OTP (6 ô riêng hay 1 field), countdown timer style |
| **Forgot Password**   | Chỉ cần 1 email field + nút gửi                              |

## Phase 3 – Bản đồ & SOS Khẩn cấp (Guest)

### Mục tiêu

Triển khai màn hình bản đồ chính (Home Map) với đầy đủ các loại marker, heatmap SOS, polyline tuyến đường an toàn, và luồng Quick SOS không cần đăng nhập. Đây là feature cốt lõi nhất, hiển thị ngay khi mở app.

### Tầng Data

#### Package: `data/remote/dto/`

```
HubDto.java                    – { id, name, lat, lng, status, inventorySummary: List<InventoryItemDto> }
ShelterDto.java                – { id, name, lat, lng, capacity, currentOccupancy, hasElectricity, hasWater }
SosRequestDto.java             – { id, lat, lng, status, type } (dùng cho heatmap)
SafePathDto.java               – { points: List<LatLngDto> } (polyline từ backend)
QuickSosRequest.java           – { name, phone, description, lat, lng, imageBase64 (nullable) }
InventoryItemDto.java          – { category, itemName, quantity }
LatLngDto.java                 – { lat, lng }
```

#### Package: `data/remote/`

```
MapApiService.java             – @GET("/map/hubs") → List<HubDto>
                                 @GET("/map/shelters") → List<ShelterDto>
                                 @GET("/map/sos-heatmap") → List<SosRequestDto>
                                 @GET("/map/safe-path") → SafePathDto
SosApiService.java             – @POST("/sos/quick") → Call<Void>  (unauthenticated)
```

#### Package: `data/repository/`

```
MapRepositoryImpl.java         – Gọi MapApiService, cache kết quả vào Room (MapEntity)
SosRepositoryImpl.java         – Gọi SosApiService; Phase này chỉ xử lý Quick SOS
```

#### Package: `data/local/entity/`

```
HubEntity.java                 – @Entity: cache hub data offline
ShelterEntity.java             – @Entity: cache shelter data offline
```

#### Package: `data/local/dao/`

```
HubDao.java                    – @Dao: insertAll, getAll, deleteAll
ShelterDao.java                – @Dao: insertAll, getAll, deleteAll
```

### Tầng Domain

#### Package: `domain/repository/`

```
MapRepository.java             – getHubs(), getShelters(), getSosHeatmap(), getSafePath()
SosRepository.java             – submitQuickSos()
```

#### Package: `domain/usecase/map/`

```
GetMapDataUseCase.java         – Gọi song song hubs + shelters + heatmap, trả về MapData wrapper
GetSafePathUseCase.java        – Nhận origin LatLng → gọi MapRepository.getSafePath()
```

#### Package: `domain/usecase/sos/`

```
SubmitQuickSosUseCase.java     – Validate (name, phone không rỗng, có GPS) → gọi SosRepository.submitQuickSos()
```

#### Package: `domain/model/`

```
Hub.java                       – { id, name, LatLng, status, inventoryItems }
Shelter.java                   – { id, name, LatLng, capacity, currentOccupancy, hasElectricity, hasWater }
SosHeatPoint.java              – { LatLng, weight } (dùng cho HeatmapTileProvider)
SafePath.java                  – { List<LatLng> }
```

### Tầng UI

#### Package: `ui/map/`

```
HomeMapFragment.java           – Fragment chính: chứa SupportMapFragment, FAB SOS, BottomSheet hub detail
HomeMapViewModel.java          – GetMapDataUseCase, LiveData<MapState> (hubs, shelters, heatPoints)
MapRenderer.java               – Helper class: addHubMarkers(), addShelterMarkers(), drawSafePath(), drawHeatmap()
HubInfoBottomSheet.java        – BottomSheetDialogFragment: hiển thị tên trạm, tồn kho, khoảng cách
ShelterInfoBottomSheet.java    – BottomSheetDialogFragment: hiển thị sức chứa, tiện ích
```

#### Package: `ui/sos/`

```
QuickSosFragment.java          – Form SOS nhanh: Name, Phone, Description, ảnh tùy chọn, GPS tự động
QuickSosViewModel.java         – SubmitQuickSosUseCase, xử lý location permission
SosConfirmationFragment.java   – Màn hình xác nhận "SOS đã gửi thành công"
```

#### Package: `ui/main/`

```
MainActivity.java              – Host main_nav_graph.xml + BottomNavigationView (Map, History, Profile)
```

#### Layout XML cần tạo

```
fragment_home_map.xml          – FrameLayout: SupportMapFragment + FAB_SOS + FAB_MyLocation
bottom_sheet_hub_info.xml      – Tên trạm, list item tồn kho, nút "Tuyến đường"
bottom_sheet_shelter_info.xml  – Sức chứa, badge tiện ích (điện/nước)
fragment_quick_sos.xml         – ScrollView: các field + nút chụp ảnh + nút GPS + nút Gửi SOS
fragment_sos_confirmation.xml  – Icon checkmark + text xác nhận + nút về trang chủ
activity_main.xml              – CoordinatorLayout + BottomNavigationView + NavHostFragment
```

#### Drawable cần thiết

```
ic_marker_hub.xml              – Vector: Ngôi nhà/Hộp tiếp tế màu xanh lá
ic_marker_shelter.xml          – Vector: Lá cờ/Hình người trú ẩn màu xanh dương
ic_sos_button.xml              – Vector: nút SOS đỏ, dạng tròn
```

### Thư viện cần tích hợp

```toml
play-services-maps     = "19.1.0"    # Google Maps SDK: Marker, Polyline, HeatmapTileProvider
play-services-location = "21.2.0"    # FusedLocationProviderClient
```

> **Lưu ý:** Cần thêm `MAPS_API_KEY` vào `local.properties` và khai báo trong `AndroidManifest.xml`.

### Màn hình UI/Figma cần cung cấp

| Màn hình                      | Chi tiết cần thiết                                           |
| ----------------------------- | ------------------------------------------------------------ |
| **Home Map**                  | FAB SOS vị trí, BottomNav tabs, style khi không có marker    |
| **Hub Info Bottom Sheet**     | Layout list item tồn kho (icon danh mục + tên + số lượng)    |
| **Shelter Info Bottom Sheet** | Badge tiện ích style (điện/nước icon), progress bar sức chứa |
| **Quick SOS Form**            | Thứ tự các field, màu nút SOS, layout ảnh preview            |
| **SOS Confirmation**          | Animation/illustration xác nhận thành công                   |

## Phase 4 – Luồng Nạn nhân (Victim Flows)

### Mục tiêu

Triển khai tất cả tính năng dành riêng cho role Victim: gửi SOS hộ người thân, yêu cầu tiếp tế 5 danh mục, theo dõi vị trí TNV real-time (Live Tracking), chat WebSocket, xem lịch sử offline 7 ngày.

### Tầng Data

#### Package: `data/remote/dto/`

```
SosForOtherRequest.java        – { victimName, phone, address, houseDesc, healthStatus, numberOfPeople, lat, lng }
ReliefRequest.java             – { name, phone, items: List<ReliefItemDto>, adults, elderly, children, note }
ReliefItemDto.java             – { category, subCategory, quantity }
ReliefStatusResponse.java      – { id, status, volunteerInfo: VolunteerInfoDto, estimatedArrival }
VolunteerInfoDto.java          – { name, phone, vehicleType, lat, lng }
ChatMessageDto.java            – { senderId, senderRole, content, timestamp }
RequestHistoryDto.java         – { id, type, status, createdAt, items }
```

#### Package: `data/remote/`

```
VictimApiService.java          – @POST("/victim/sos-for-other") → Call<Void>
                                 @POST("/victim/relief-request") → Call<ReliefStatusResponse>
                                 @GET("/victim/requests") → Call<List<RequestHistoryDto>>
                                 @GET("/victim/requests/{id}/volunteer-location") → Call<VolunteerInfoDto>
```

#### Package: `data/local/entity/`

```
RequestHistoryEntity.java      – @Entity(tableName="request_history"): id, type, status, createdAt, summaryJson
```

#### Package: `data/local/dao/`

```
RequestHistoryDao.java         – insert(), getAll(), getByDateRange(), deleteOlderThan7Days()
```

#### Package: `data/repository/`

```
VictimRepositoryImpl.java      – Gọi VictimApiService; lưu kết quả vào RequestHistoryDao; đọc offline khi offline
```

### Tầng Domain

#### Package: `domain/repository/`

```
VictimRepository.java          – submitSosForOther(), submitReliefRequest(), getRequestHistory(), getVolunteerLocation()
```

#### Package: `domain/usecase/victim/`

```
SubmitSosForOtherUseCase.java  – Validate người thân form → gọi VictimRepository
SubmitReliefRequestUseCase.java– Validate danh mục, tính số lượng theo số người → gọi VictimRepository
GetRequestHistoryUseCase.java  – Đọc Room nếu offline, fetch API nếu online, sync 2 chiều
```

#### Package: `domain/model/`

```
ReliefCategory.java            – Enum: MEDICINE, CLOTHING, FOOD, WATER, OTHER (với 2 cấp subcategory)
RequestHistory.java            – { id, type, status, createdAt, items }
VolunteerLocation.java         – { lat, lng, name, phone }
```

### Tầng UI

#### Package: `ui/victim/`

```
SosForOtherFragment.java       – Form nhập thông tin người thân cần cứu, có địa chỉ + GPS
SosForOtherViewModel.java      – SubmitSosForOtherUseCase

ReliefRequestFragment.java     – Host ViewPager2 qua 2 bước: Bước 1 chọn danh mục; Bước 2 nhập số người
ReliefCategoryFragment.java    – RecyclerView 2 cấp: 5 danh mục cha → expand subcategory
ReliefPeopleCountFragment.java – 3 stepper (Người lớn / Người già / Trẻ em), tự tính số lượng vật phẩm
ReliefRequestViewModel.java    – SubmitReliefRequestUseCase, logic tính số lượng tự động

RequestStatusFragment.java     – Trạng thái đơn hàng + thông tin TNV + nút "Xem bản đồ" → LiveTrackingFragment
LiveTrackingFragment.java      – Google Maps hiển thị vị trí TNV cập nhật qua WebSocket
LiveTrackingViewModel.java     – Kết nối STOMP topic /topic/tracking/{requestId}, cập nhật marker

ChatFragment.java              – RecyclerView tin nhắn + EditText + nút gửi
ChatViewModel.java             – Kết nối STOMP /app/chat/{requestId}, subscribe /topic/chat/{requestId}

RequestHistoryFragment.java    – RecyclerView lịch sử + DatePicker filter + offline fallback
RequestHistoryViewModel.java   – GetRequestHistoryUseCase, xử lý online/offline

VictimDashboardFragment.java   – Home sau login: thống kê nhanh + nút SOS nổi bật + nút Yêu cầu tiếp tế
```

#### Package: `ui/chat/`

```
ChatMessageAdapter.java        – RecyclerView Adapter: 2 ViewType (tin nhắn mình gửi / tin nhắn nhận)
ChatMessage.java               – Model UI: { content, timestamp, isSentByMe }
```

#### Layout XML cần tạo

```
fragment_sos_for_other.xml
fragment_relief_request.xml
fragment_relief_category.xml       – RecyclerView với ExpandableList style
item_relief_category.xml
item_relief_subcategory.xml
fragment_relief_people_count.xml   – 3 nhóm stepper +/-
fragment_request_status.xml        – Progress stepper ngang + card thông tin TNV
fragment_live_tracking.xml         – SupportMapFragment toàn màn hình + BottomSheet thông tin TNV
fragment_chat.xml                  – RecyclerView + InputLayout ở dưới
item_chat_sent.xml
item_chat_received.xml
fragment_request_history.xml       – RecyclerView + DateRangePicker button
item_request_history.xml
```

### Thư viện cần tích hợp

```toml
stomp-android  = "1.6.6"    # StompProtocolAndroid: WebSocket chat + live tracking
rxjava3        = "3.1.9"    # Required by StompProtocolAndroid
rxandroid3     = "3.0.2"    # Android scheduler cho RxJava3
glide          = "4.16.0"   # Load ảnh SOS, avatar TNV
swiperefresh   = "1.2.0"    # Pull-to-refresh trên danh sách lịch sử
```

### Màn hình UI/Figma cần cung cấp

| Màn hình                           | Chi tiết cần thiết                                                        |
| ---------------------------------- | ------------------------------------------------------------------------- |
| **Victim Dashboard**               | Sau login trông như thế nào? Bố cục Home role Victim                      |
| **SOS for Other – Form**           | Layout form, cách chọn địa chỉ (tay nhập hay map pin)                     |
| **Relief Request – Chọn danh mục** | Kiểu expandable list hay grid card cho 5 danh mục? Subcategory style      |
| **Relief Request – Số người**      | Stepper +/- style, hiển thị số lượng tự tính như thế nào                  |
| **Request Status**                 | Progress stepper ngang (4 bước), card thông tin TNV                       |
| **Live Tracking**                  | Bản đồ chiếm bao nhiêu % màn hình, bottom info card style                 |
| **Chat Screen**                    | Bubble chat style, header thông tin cuộc trò chuyện                       |
| **Request History**                | List item lịch sử: badge trạng thái, icon loại yêu cầu, date filter style |

## Phase 5 – Luồng Tình nguyện viên (Volunteer Flows)

### Mục tiêu

Đây là phase phức tạp nhất: xử lý FCM dispatch notification với countdown 30 giây, màn hình nhiệm vụ với thanh tiến trình, điều hướng bản đồ, chat hai chiều với nạn nhân, bật/tắt trạng thái Online, và dashboard thống kê thành tích.

### Tầng Data

#### Package: `data/remote/dto/`

```
MissionDto.java                – { id, type (SOS/RELIEF), victimName, victimPhone, victimAddress,
                                    victimNote, items: List<ReliefItemDto>, hubId, hubName, hubAddress,
                                    hubLat, hubLng, victimLat, victimLng, expiresAt }
MissionStatusUpdate.java       – { missionId, newStatus } (ACCEPTED, DECLINED, PICKED_UP, COMPLETED)
VolunteerStatsDto.java         – { totalCompleted, rating, rank, badges: List<String> }
NearbyVictimDto.java           – { requestId, name, address, distance, type, priority }
```

#### Package: `data/remote/`

```
VolunteerApiService.java       – @PATCH("/volunteer/status") → void (online/offline)
                                 @POST("/volunteer/missions/{id}/accept") → Call<MissionDto>
                                 @POST("/volunteer/missions/{id}/decline") → Call<Void>
                                 @POST("/volunteer/missions/{id}/picked-up") → Call<Void>
                                 @POST("/volunteer/missions/{id}/complete") → Call<Void>
                                 @GET("/volunteer/stats") → Call<VolunteerStatsDto>
                                 @GET("/volunteer/nearby-victims") → Call<List<NearbyVictimDto>>
```

#### Package: `data/repository/`

```
VolunteerRepositoryImpl.java   – Gọi VolunteerApiService, lưu mission hiện tại vào Room
```

#### Package: `data/local/entity/`

```
MissionEntity.java             – @Entity: lưu mission đang thực hiện để phục hồi khi app bị kill
```

#### Package: `data/local/dao/`

```
MissionDao.java                – insertOrUpdate(), getCurrentMission(), clearMission()
```

### Tầng Domain

#### Package: `domain/repository/`

```
VolunteerRepository.java       – setOnlineStatus(), acceptMission(), declineMission(),
                                 updateMissionStatus(), getStats(), getNearbyVictims()
```

#### Package: `domain/usecase/volunteer/`

```
HandleDispatchUseCase.java     – Nhận FCM payload → parse MissionDto → lưu Room → trigger UI
AcceptMissionUseCase.java      – Gọi repository.acceptMission() → bắt đầu location service
DeclineMissionUseCase.java     – Gọi repository.declineMission() → clear local state
UpdateMissionStatusUseCase.java– PICKED_UP / COMPLETED với validation business rule
GetVolunteerStatsUseCase.java  – Fetch stats + map badges
```

### Tầng UI

#### Package: `ui/volunteer/`

```
VolunteerDashboardFragment.java – Home role TNV: nút Online/Offline toggle, card nhiệm vụ hiện tại, stats summary
VolunteerDashboardViewModel.java

DispatchDialogFragment.java    – DialogFragment toàn màn hình: thông tin nhiệm vụ + countdown 30s +
                                  nút CHẤP NHẬN (xanh) / TỪ CHỐI (đỏ)
DispatchViewModel.java         – CountDownTimer 30 giây, AcceptMissionUseCase, DeclineMissionUseCase

CurrentMissionFragment.java    – Hiển thị thanh tiến trình (Step indicator) + thông tin nạn nhân/trạm
                                  + thông tin vật phẩm + nút cập nhật trạng thái tiếp theo
CurrentMissionViewModel.java   – UpdateMissionStatusUseCase, observe Room cho mission state

MissionMapFragment.java        – Google Maps: polyline từ vị trí TNV → trạm → nạn nhân (2 đoạn)
MissionMapViewModel.java       – GetSafePathUseCase (tái dụng từ Phase 3), FusedLocationProvider

MissionChatFragment.java       – Tái dụng ChatFragment (Phase 4), subscriber topic khác

VolunteerHistoryFragment.java  – Danh sách nhiệm vụ đã hoàn thành với filter (tháng/tuần)
VolunteerHistoryViewModel.java

VolunteerStatsFragment.java    – Dashboard thành tích: số nhiệm vụ, rating sao, badge, rank
VolunteerStatsViewModel.java   – GetVolunteerStatsUseCase

NearbyVictimsFragment.java     – RecyclerView các nạn nhân gần, sort theo distance/priority
NearbyVictimsViewModel.java    – GetNearbyVictimsUseCase, RefreshHandler
```

#### Package: `services/location/`

```
LocationTrackingService.java   – Foreground Service: cập nhật GPS mỗi 5 giây khi TNV đang có nhiệm vụ,
                                  publish lên STOMP /app/location/{missionId}
```

#### Layout XML cần tạo

```
fragment_volunteer_dashboard.xml     – Toggle Online/Offline + card nhiệm vụ + stats mini
dialog_dispatch.xml                  – Màn hình toàn màn hình: loại đơn, địa chỉ, countdown circle timer
fragment_current_mission.xml         – Horizontal StepView (2–4 bước) + thông tin phía dưới
fragment_mission_map.xml             – Maps fullscreen + bottom drawer thông tin
fragment_volunteer_history.xml
item_mission_history.xml
fragment_volunteer_stats.xml         – Achievement cards, rating stars, badge grid
fragment_nearby_victims.xml
item_nearby_victim.xml               – Distance badge, loại đơn, mức ưu tiên
```

### Thư viện cần tích hợp

```toml
# Đã có từ Phase trước, không cần thêm mới
# Sử dụng: StompProtocolAndroid (real-time location publish)
#           FusedLocationProvider (GPS tracking)
#           FCM (nhận dispatch notification)
```

### Màn hình UI/Figma cần cung cấp

| Màn hình                          | Chi tiết cần thiết                                                                   |
| --------------------------------- | ------------------------------------------------------------------------------------ |
| **Volunteer Dashboard**           | Layout nút Online/Offline (toggle switch hay button lớn?), card nhiệm vụ hiện tại    |
| **Dispatch Dialog (30s)**         | Countdown timer style (arc circle hay linear), cách phân biệt SOS vs tiếp tế         |
| **Current Mission – SOS flow**    | Thanh tiến trình 2 bước: "Đến nhà nạn nhân" → "Hoàn thành"                           |
| **Current Mission – Relief flow** | Thanh tiến trình 4 bước: "Đến trạm" → "Lấy hàng" → "Đến nhà nạn nhân" → "Hoàn thành" |
| **Mission Map**                   | Split view hay fullscreen map? Bottom drawer thông tin                               |
| **Completion Confirmation**       | Màn hình chụp ảnh xác nhận hoặc nhập mã nạn nhân                                     |
| **Volunteer Stats Dashboard**     | Layout badge, số liệu thống kê, ranking style                                        |
| **Nearby Victims List**           | Card style, cách hiển thị badge ưu tiên (cao/thấp)                                   |

## Phase 6 – Luồng Mạnh thường quân & Quản lý trạm (Sponsor & Staff)

### Mục tiêu

Triển khai luồng đăng ký đóng góp của Sponsor (với Smart Hub Selection và tạo QR), và luồng quản lý kho của Staff (quét QR nhập kho/xuất kho, giám sát tồn kho).

### Tầng Data

#### Package: `data/remote/dto/`

```
DonationRequest.java           – { items: List<DonationItemDto>, expectedDeliveryDate, note, imageBase64 }
DonationItemDto.java           – { category, itemName, quantity, expiryDate }
SmartHubSuggestion.java        – { hubId, hubName, address, distance, matchScore, neededItems: List<String> }
DonationQrResponse.java        – { donationId, qrCodeBase64, expiresAt }
DonationHistoryDto.java        – { id, status, items, hubName, createdAt, confirmedAt }
SponsorStatsDto.java           – { totalDonations, totalItems, badges: List<String>, points }

StaffInventoryDto.java         – { hubName, items: List<InventoryItemDto>, lastUpdated }
QrScanResultDto.java           – { type (DONATION/DISPATCH), id, summary, senderName }
InventoryUpdateRequest.java    – { qrCode, action (CHECK_IN/CHECK_OUT) }
```

#### Package: `data/remote/`

```
SponsorApiService.java         – @POST("/sponsor/donations") → Call<DonationQrResponse>
                                 @GET("/sponsor/hub-suggestions") → Call<List<SmartHubSuggestion>>
                                 @GET("/sponsor/donations") → Call<List<DonationHistoryDto>>
                                 @GET("/sponsor/stats") → Call<SponsorStatsDto>

StaffApiService.java           – @POST("/staff/inventory/scan") → Call<QrScanResultDto>
                                 @GET("/staff/inventory") → Call<StaffInventoryDto>
                                 @PATCH("/staff/hub/status") → Call<Void> (bật/tắt trạm)
```

#### Package: `data/repository/`

```
SponsorRepositoryImpl.java
StaffRepositoryImpl.java
```

### Tầng Domain

#### Package: `domain/repository/`

```
SponsorRepository.java         – registerDonation(), getHubSuggestions(), getDonationHistory(), getStats()
StaffRepository.java           – scanQr(), getInventory(), updateHubStatus()
```

#### Package: `domain/usecase/sponsor/`

```
RegisterDonationUseCase.java   – Validate vật phẩm, gọi SponsorRepository.registerDonation()
GetHubSuggestionsUseCase.java  – Gọi SponsorRepository.getHubSuggestions() với GPS hiện tại
```

#### Package: `domain/usecase/staff/`

```
ProcessQrScanUseCase.java      – Parse QR result → xác định CHECK_IN hay CHECK_OUT → gọi StaffRepository.scanQr()
```

### Tầng UI

#### Package: `ui/sponsor/`

```
SponsorDashboardFragment.java      – Home role MTS: nút đăng ký + lịch sử + stats
RegisterDonationFragment.java      – ViewPager2 2 bước: Bước 1 danh mục/số lượng; Bước 2 ngày dự kiến + ảnh
DonationItemsFragment.java         – RecyclerView 4 danh mục + thêm/bớt item dynamically
DonationDetailsFragment.java       – DatePicker, ghi chú, chụp ảnh hàng hóa
RegisterDonationViewModel.java

HubSelectionFragment.java          – RecyclerView danh sách đề xuất trạm (matchScore cao nhất lên đầu)
                                      Mỗi item: tên trạm + khoảng cách + danh mục đang thiếu
HubSelectionViewModel.java         – GetHubSuggestionsUseCase

DonationQrFragment.java            – Hiển thị QR code (từ Base64), countdown hết hạn, nút chia sẻ
DonationQrViewModel.java

DonationHistoryFragment.java       – RecyclerView lịch sử đóng góp, timeline status
DonationHistoryViewModel.java

SponsorStatsFragment.java          – Dashboard: tổng đóng góp, badge, điểm vinh danh
SponsorStatsViewModel.java
```

#### Package: `ui/staff/`

```
StaffDashboardFragment.java        – Home role Staff: nút quét QR + xem tồn kho + bật/tắt trạm
QrScannerFragment.java             – CameraX PreviewView + ML Kit overlay + xét kết quả
QrScannerViewModel.java            – ProcessQrScanUseCase, xử lý camera permission
QrResultFragment.java              – Xác nhận kết quả: loại QR, tóm tắt, nút Xác nhận / Hủy
InventoryFragment.java             – Danh sách tồn kho theo danh mục, badge "Sắp hết"
InventoryViewModel.java            – GetInventoryUseCase, subscribe WebSocket inventory events
HubStatusFragment.java             – Toggle trạm ACTIVE/INACTIVE + lý do (nếu tắt)
```

#### Layout XML cần tạo

```
fragment_sponsor_dashboard.xml
fragment_register_donation.xml
fragment_donation_items.xml          – RecyclerView + FAB thêm item
item_donation_category.xml
fragment_donation_details.xml        – DatePicker, ImagePicker
fragment_hub_selection.xml           – RecyclerView
item_hub_suggestion.xml              – MatchScore bar, neededItems chips
fragment_donation_qr.xml             – ImageView QR lớn + countdown + share button
fragment_donation_history.xml
item_donation_history.xml
fragment_sponsor_stats.xml

fragment_staff_dashboard.xml
fragment_qr_scanner.xml              – PreviewView + overlay rectangle
fragment_qr_result.xml               – Card xác nhận: icon SUCCESS/ERROR, summary
fragment_inventory.xml               – RecyclerView grouped by category
item_inventory_item.xml              – Progress bar lượng tồn kho thấp/cao
fragment_hub_status.xml
```

### Thư viện cần tích hợp

```toml
mlkit-barcode    = "17.3.0"    # ML Kit Barcode Scanning
camerax-core     = "1.4.1"    # CameraX lifecycle abstraction
camerax-camera2  = "1.4.1"    # Hardware camera implementation
camerax-lifecycle= "1.4.1"    # Bind camera to lifecycle
camerax-view     = "1.4.1"    # PreviewView widget
glide            = "4.16.0"   # Load ảnh đóng góp, hiển thị QR Base64 → Bitmap
```

### Màn hình UI/Figma cần cung cấp

| Màn hình                              | Chi tiết cần thiết                                                              |
| ------------------------------------- | ------------------------------------------------------------------------------- |
| **Sponsor Dashboard**                 | Layout tổng quan: quick stats + nút CTA chính                                   |
| **Register Donation – Chọn vật phẩm** | Dynamic add/remove item style, dropdown subcategory                             |
| **Hub Suggestion List**               | Item card: match score bar, cách hiển thị danh mục đang thiếu (chips)           |
| **Donation QR Screen**                | Kích thước QR, có watermark AidBridge không, nút share                          |
| **Donor History**                     | Timeline status (Đã tạo → Đã bàn giao → Nhập kho)                               |
| **Sponsor Stats**                     | Badge design, cách hiển thị điểm vinh danh/bảng xếp hạng                        |
| **QR Scanner**                        | Overlay hình chữ nhật, flash button, kết quả hiển thị dạng overlay hay navigate |
| **Inventory Screen**                  | Grouped by category, màu sắc badge "Còn ít / Đủ / Hết"                          |

## Phase 7 – Thông báo, Offline & Hoàn thiện (Notifications, Offline & Polish)

### Mục tiêu

Kết nối toàn bộ FCM notification flow (6 loại thông báo), hoàn thiện offline sync với Room 7 ngày, background location service cho TNV, xử lý edge cases permission, và polish toàn bộ UX (error states, empty states, shimmer loading).

### Tầng Data

#### Package: `data/remote/dto/`

```
FcmPayload.java                – { notificationType, title, body, data: Map<String,String> }
                                  notificationType: SOS_CONFIRMED | VOLUNTEER_ASSIGNED |
                                  VOLUNTEER_PICKED_UP | TRACKING_NEAR | MISSION_COMPLETED |
                                  DISPATCH_REQUEST | DONATION_CONFIRMED | BROADCAST_ALERT
```

#### Package: `data/local/entity/`

```
NotificationEntity.java        – @Entity: lưu thông báo đã nhận (id, type, title, body, isRead, createdAt)
```

#### Package: `data/local/dao/`

```
NotificationDao.java           – insertAll(), getUnread(), markAsRead(), deleteOlderThan30Days()
```

### Tầng Domain

#### Package: `domain/usecase/notification/`

```
HandleFcmNotificationUseCase.java – Phân loại FcmPayload theo type → trigger action phù hợp:
                                    DISPATCH_REQUEST → mở DispatchDialogFragment
                                    TRACKING_NEAR → trigger vibration + sound
                                    BROADCAST_ALERT → hiển thị full-screen alert
```

### Tầng UI

#### Package: `ui/notification/`

```
NotificationFragment.java      – Danh sách thông báo đã nhận, đánh dấu đã đọc, badge count
NotificationViewModel.java
BroadcastAlertFragment.java    – DialogFragment toàn màn hình cho thông báo khẩn cấp từ Admin
```

### Tầng Services

#### Package: `services/messaging/`

```
AidBridgeFcmService.java       – extends FirebaseMessagingService:
                                  onMessageReceived() → gọi HandleFcmNotificationUseCase
                                  onNewToken() → gửi token lên backend
                                  Xử lý thông báo khi app foreground (hiện Snackbar)
                                  Xử lý thông báo khi app background (hệ thống hiện notification)
```

#### Package: `services/location/`

```
LocationTrackingService.java   – (Đã tạo Phase 5): hoàn thiện Persistent Notification,
                                  startForeground() đúng chuẩn Android 14+
```

### Các cải tiến cross-cutting

#### Package: `ui/base/`

```
ErrorStateView.java            – Custom View: illustration + message + nút Thử lại
EmptyStateView.java            – Custom View: illustration + message khi danh sách trống
LoadingOverlay.java            – Semi-transparent overlay với CircularProgressIndicator
```

#### Package: `utils/`

```
DateUtils.java                 – formatRelativeTime() (5 phút trước, hôm qua...), formatDateRange()
ValidationUtils.java           – isValidPhone(), isValidEmail(), isValidOtp()
ImageUtils.java                – bitmapToBase64(), resizeBitmap() (trước khi upload SOS photo)
UiUtils.java                   – showSnackbar(), showToast(), hideKeyboard()
```

#### Hoàn thiện `res/`

```
anim/                          – slide_in_right.xml, slide_out_left.xml, fade_in.xml (navigation transitions)
drawable/                      – ic_empty_*.xml (empty states cho từng màn hình)
                                 ic_error_network.xml
layout/view_error_state.xml
layout/view_empty_state.xml
```

### Thư viện cần tích hợp

```toml
firebase-bom       = "33.9.0"           # Firebase BOM
firebase-messaging = "(managed by BOM)" # FCM nhận thông báo dispatch, status, broadcast
```

### Màn hình UI/Figma cần cung cấp

| Màn hình                          | Chi tiết cần thiết                                                  |
| --------------------------------- | ------------------------------------------------------------------- |
| **Notification List**             | Item style (icon theo type, badge đọc/chưa đọc, timestamp)          |
| **Broadcast Alert Dialog**        | Full-screen alert: màu đỏ khẩn cấp, content warning style           |
| **Error State**                   | Illustration + message + nút retry (dùng chung toàn app)            |
| **Empty State**                   | Illustration riêng mỗi danh sách (lịch sử trống, nhiệm vụ trống...) |
| **Persistent Notification (TNV)** | Style thông báo thanh trạng thái khi đang tracking                  |

## Tóm tắt Timeline (Ước lượng Sprint)

| Phase   | Nội dung                         | Sprint              |
| ------- | -------------------------------- | ------------------- |
| Phase 1 | Foundation + Architecture        | Sprint 1 (3–4 ngày) |
| Phase 2 | Authentication                   | Sprint 1 (tiếp)     |
| Phase 3 | Map + Quick SOS                  | Sprint 2 (5–6 ngày) |
| Phase 4 | Victim Flows                     | Sprint 3 (6–7 ngày) |
| Phase 5 | Volunteer Flows                  | Sprint 4 (7–8 ngày) |
| Phase 6 | Sponsor + Staff                  | Sprint 5 (6–7 ngày) |
| Phase 7 | Notifications + Offline + Polish | Sprint 6 (4–5 ngày) |

## Checklist tổng – Màn hình Figma cần cung cấp theo thứ tự ưu tiên

> **Phase 1–2 (Cần NGAY để bắt đầu code):**
>
> - [ ] Splash Screen
> - [ ] Login Screen
> - [ ] Register Wizard (3 bước)
> - [ ] OTP Verification

> **Phase 3 (Cần trước khi code Map):**
>
> - [ ] Home Map (cấu trúc tổng thể, FAB, BottomNav)
> - [ ] Hub Info Bottom Sheet
> - [ ] Shelter Info Bottom Sheet
> - [ ] Quick SOS Form + Confirmation

> **Phase 4 (Cần trước khi code Victim):**
>
> - [ ] Victim Dashboard
> - [ ] Relief Request Wizard (2 bước)
> - [ ] Request Status + Live Tracking
> - [ ] Chat Screen
> - [ ] Request History

> **Phase 5 (Cần trước khi code Volunteer):**
>
> - [ ] Volunteer Dashboard
> - [ ] Dispatch Dialog (30s countdown)
> - [ ] Current Mission (SOS & Relief flows)
> - [ ] Mission Map
> - [ ] Volunteer Stats Dashboard

> **Phase 6 (Cần trước khi code Sponsor/Staff):**
>
> - [ ] Sponsor Dashboard + Register Donation
> - [ ] Hub Suggestion List + Donation QR
> - [ ] QR Scanner + Inventory Screen

> **Phase 7 (Cần để hoàn thiện):**
>
> - [ ] Notification List
> - [ ] Error State + Empty State illustrations
> - [ ] Broadcast Alert Dialog
