# SOS, Aid Request, và Background Location Refactor Guide

Tài liệu này không thiết kế lại hệ thống từ đầu.
Mục tiêu là refactor trên codebase hiện có của `AidBridge`, ưu tiên:

1. reuse API/module/class/entity đang có
2. chỉ thêm component mới khi class cũ không đủ gánh trách nhiệm
3. không thiết kế lại backend thành hệ API mới
4. không đề xuất tạo bảng backend mới

---

# 1. Phân tích hệ thống hiện tại theo hướng reuse

## 1.1 Android

### `VictimSosViewModel`

Dùng lại được:

1. `submitRelativeSos(...)` hiện tại đang đúng vai trò của flow SOS cho người thân.
2. `loadCachedUser()` và phần lấy user cached vẫn hữu ích cho UI.
3. Pattern `Transformations.switchMap(...)` + `MediatorLiveData` đang đúng với kiến trúc frontend hiện tại.

Cần sửa:

1. `submitSelfSos(...)` hiện tại đang là form-driven SOS:
   - validate `peopleCount`
   - validate `severity`
   - đọc `note`
   - xử lý ảnh hiện trường
2. Flow này không phù hợp cho `SOS one-tap`.
3. ViewModel chưa có nhánh riêng cho quick SOS.
4. ViewModel chưa có event yêu cầu quyền location/notification/background location.
5. ViewModel chưa có khái niệm `pending quick action for retry`.

Kết luận:

1. Không cần bỏ `VictimSosViewModel`.
2. Nên refactor thành 3 nhánh trong cùng ViewModel:
   - `submitQuickSelfSos(...)`
   - `submitRelativeSos(...)`
   - `submitDetailedSelfSos(...)` nếu bạn vẫn muốn giữ form cũ như flow bổ sung sau này

### `VictimSupplyTabFragment`

Dùng lại được:

1. Flow form Aid Request hiện tại là nền tảng tốt.
2. Đã có logic:
   - chọn item
   - đếm số người
   - speech-to-text
   - current location fallback
3. Đã dùng `UserLocationManager` để lấy cached location trước khi gọi `getCurrentLocation()`.

Cần sửa:

1. Nên chuyển logic resolve location vào `UserLocationManager` nhiều hơn để tránh duplicate code giữa `VictimSupplyTabFragment` và rescue flow.
2. Chưa có retry/offline queue.
3. Chưa có `urgencyLevel` ở Android request dù backend DTO đã hỗ trợ.
4. Chưa có local pending state khi mạng lỗi.

Kết luận:

1. Giữ `VictimSupplyTabFragment` cho Aid Request.
2. Không dùng fragment này cho SOS one-tap.

### `UserLocationManager`

Dùng lại được:

1. đã wrap `FusedLocationProviderClient`
2. đã có:
   - `refreshOnce()`
   - `getFreshLocation(...)`
   - `getLatestLocation()`
   - `startForegroundTracking()`
   - `stopForegroundTracking()`
3. đã cache vị trí cuối vào `TokenManager`

Cần sửa:

1. `LocationSnapshot` mới có:
   - `latitude`
   - `longitude`
   - `updatedAtMillis`
2. Chưa có:
   - `accuracy`
   - `provider`
   - `isMock`
   - `capturedAt`
3. Chưa expose callback/listener chuẩn để service dùng lâu dài.
4. `startForegroundTracking()` hiện mới track local cache, chưa gắn với sync server.
5. Tên method `startForegroundTracking()` dễ gây hiểu lầm vì class này không phải `Service`.

Kết luận:

1. Đây là class nên reuse mạnh nhất.
2. Chỉ nên mở rộng, không nên thay thế.

### `data/local/AppDatabase`

Dùng lại được:

1. Room đã có sẵn.
2. Hilt + `DatabaseModule` đã cấu hình xong.

Thiếu:

1. chưa có local queue tối thiểu cho retry
2. chưa có DAO cho pending sync

Kết luận:

1. Không cần thiết kế local DB mới từ đầu.
2. Chỉ cần thêm một entity queue tối thiểu vào `AppDatabase`.

## 1.2 Backend

### `SosController`

Dùng lại được:

1. Endpoint create hiện có: `POST /api/sos-requests`
2. Controller đã xử lý được:
   - authenticated user
   - guest user nếu auth không có
3. Có thể tiếp tục dùng cùng endpoint cho quick SOS.

Cần sửa:

1. Request DTO hiện tại đang nghiêng về form SOS.
2. Chưa có nhánh xử lý idempotency/dedupe.
3. Chưa có action update location sau khi SOS đã được tạo.

Kết luận:

1. Không cần endpoint tạo SOS mới.
2. Nên mở rộng endpoint cũ.

### `CreateSosRequestUseCase`

Dùng lại được:

1. đang đúng vị trí use case chính của SOS
2. đang save `SosRequest`
3. đang publish `SosRequestCreatedEvent`
4. đang khớp với flow event-driven mission dispatch

Cần sửa:

1. hiện coi SOS như request có mô tả/form
2. chưa có default behavior cho quick SOS
3. chưa có dedupe khi người dùng bấm nhiều lần
4. chưa có logic update latest location của SOS đang mở

Kết luận:

1. Không cần thay use case này bằng use case mới.
2. Chỉ cần branch logic bên trong hoặc tách helper private methods.

### `AidController`

Dùng lại được:

1. `POST /api/aid-requests`
2. `GET`, `cancel`, `list`, `voice`
3. pattern controller/use case chuẩn, phù hợp mở rộng tiếp

Cần sửa:

1. nếu muốn live location cho Aid Request thì controller cần thêm action update location
2. current Android request chưa gửi `urgencyLevel`

Kết luận:

1. Giữ nguyên create/list/cancel flow.
2. Chỉ mở rộng nếu thực sự cần update location sau create.

### `modules/sos`, `modules/aid`, `modules/mission`

Dùng lại được:

1. `modules/sos` và `modules/aid` đã là resource owner đúng domain
2. `modules/mission` đã xử lý event-driven dispatch sau create
3. `missions.victim_location` đã tồn tại trong schema
4. `sos_requests.location` và `aid_requests.location` đã tồn tại

Kết luận:

1. Continuous victim location nên tận dụng các field location hiện có.
2. Không cần tạo module backend location riêng ở giai đoạn này.

---

# 2. Chỉ ra những điểm đang sai nếu dùng flow hiện tại

## 2.1 SOS hiện đang bị hiểu như form

Các dấu hiệu:

1. [VictimRescueTabFragment.java](/c:/My%20Workspace/HCMUS/Mobile/AidBridge/drc-app/app/src/main/java/com/drc/aidbridge/ui/main/fragment/victim/VictimRescueTabFragment.java:1) đang thu:
   - `fullName`
   - `peopleCount`
   - `severity`
   - `healthDetail`
   - ảnh
2. [VictimSosViewModel.java](/c:/My%20Workspace/HCMUS/Mobile/AidBridge/drc-app/app/src/main/java/com/drc/aidbridge/ui/main/viewmodel/victim/VictimSosViewModel.java:1) validate form trước khi gửi self SOS.
3. [VictimSosInputValidator.java](/c:/My%20Workspace/HCMUS/Mobile/AidBridge/drc-app/app/src/main/java/com/drc/aidbridge/domain/usecase/validation/VictimSosInputValidator.java:1) coi `peopleCount` và `severity` là required cho self SOS.

Vấn đề:

1. người dùng phải đi qua UI form mới gửi SOS
2. không đạt yêu cầu one-tap

## 2.2 Aid Request hiện đang ổn hơn SOS nhưng vẫn thiếu đồng bộ field

Các dấu hiệu:

1. Android `ReliefRequest` chưa có `urgencyLevel`
2. Backend `CreateAidRequest` có `urgencyLevel`
3. `AidRequest` entity hiện chưa persist `urgencyLevel`
4. `AidMapper` đang trả `urgencyLevel = null`

Vấn đề:

1. priority/urgency bị rơi trên đường đi
2. frontend và backend contract chưa khớp nhau hoàn toàn

## 2.3 Chưa có chiến lược location end-to-end cho victim

Hiện trạng:

1. `UserLocationManager` chỉ cache location local
2. `VolunteerHeartbeatManager` sync location cho volunteer, nhưng victim không có flow tương tự
3. backend không có action update latest location cho SOS/Aid đang mở

Vấn đề:

1. app lấy được location nhưng không có pipeline gửi location liên tục cho victim

## 2.4 Chưa có background-safe tracking cho victim

Hiện trạng:

1. logic location đang nằm trong fragment
2. fragment mất thì flow mất

Vấn đề:

1. khi app vào background hoặc task bị remove, victim tracking không bền
2. `UserLocationManager` không thay thế được `Foreground Service`

## 2.5 Chưa có offline queue

Hiện trạng:

1. `VictimSosRepositoryImpl` và `VictimSupplyRepositoryImpl` gọi Retrofit trực tiếp
2. nếu `onFailure(...)` thì chỉ trả error cho UI

Vấn đề:

1. request thất bại là mất
2. không có pending state
3. không có replay khi có mạng lại

## 2.6 Chưa có idempotency/dedupe

Hiện trạng:

1. `CreateSosRequestUseCase` save ngay request mới
2. `SosJpaRepository` chưa có logic chặn multi-tap
3. `AidController` và `CreateAidRequestUseCase` cũng chưa có retry-safe semantics

Vấn đề:

1. retry có thể tạo record trùng
2. user panic bấm SOS nhiều lần có thể tạo nhiều SOS

## 2.7 Tracking response hiện tại còn hở

`GetTrackingUseCase` + `MissionMapper.toTrackingResponse(...)` hiện không thực sự đẩy volunteer lat/lng vào response tracking.

Vấn đề:

1. nếu sau này muốn hiển thị live tracking, layer mission tracking hiện tại vẫn chưa đầy đủ

---

# 3. Thiết kế lại nhưng ưu tiên sửa trên cái đang có

## 3.1 Những phần giữ nguyên

Giữ nguyên:

1. `SosController` là controller tạo SOS
2. `CreateSosRequestUseCase` là use case tạo SOS
3. `AidController` là controller tạo Aid Request
4. `CreateAidRequestUseCase` là use case tạo Aid Request
5. event-driven dispatch từ `modules/sos` / `modules/aid` sang `modules/mission`
6. `VictimSupplyTabFragment` + `VictimSupplyViewModel` là form Aid Request
7. `UserLocationManager` là low-level location wrapper

## 3.2 Những phần nên refactor

### SOS frontend

Refactor:

1. `VictimSosViewModel`
2. `VictimRescueTabFragment`
3. `UploadSosUseCase`
4. `VictimSosRepository`
5. `VictimSosRepositoryImpl`
6. Android `CreateSosRequest`

### SOS backend

Refactor:

1. `CreateSosRequest` DTO
2. `CreateSosRequestUseCase`
3. `SosMapper`
4. `SosJpaRepository`
5. `SecurityConfig`

### Aid frontend/backend

Refactor nhẹ:

1. Android `ReliefRequest`
2. `VictimSupplyMapper`
3. `CreateAidRequestUseCase`
4. `AidMapper`
5. `AidRequest` entity nếu bạn quyết định persist priority

## 3.3 Component mới tối thiểu được phép thêm

Chỉ nên thêm các component mới sau:

1. Android `PendingSyncEntity` + `PendingSyncDao`
   - vì local queue hiện tại chưa tồn tại ở đâu
2. Android `PendingSyncWorker`
   - vì retry background không nên nhét vào fragment/viewmodel
3. Android `EmergencyTrackingService`
   - vì continuous tracking trong background không thể sống tốt trong fragment
4. Backend `UpdateSosLocationUseCase`
   - nếu cần update latest location cho SOS đã tạo
5. Backend `UpdateAidRequestLocationUseCase`
   - nếu cần update latest location cho Aid Request đã tạo
6. Backend `SosIdempotencyRedisSchema`
   - nếu muốn idempotency mà không thêm bảng/cột vào DB chính

Lý do:

1. các class cũ không có đúng lifecycle/responsibility cho các nhiệm vụ này
2. thêm mới là ít phá kiến trúc hơn là nhét tất cả vào controller hoặc fragment

## 3.4 Field nào nên thêm vào request/response/entity hiện có

### `CreateSosRequest` Android + backend DTO

Nên thêm optional fields:

1. `triggeredAt`
2. `locationCapturedAt`
3. `accuracy`
4. `deviceInfo`
5. `clientRequestId`
6. `quickSos`

Giữ nguyên các field cũ:

1. `lat`
2. `lng`
3. `address`
4. `description`
5. `peopleCount`
6. `urgencyLevel`
7. `imageUrl`

### `SosRequestResponse`

Có thể thêm optional fields:

1. `duplicateSuppressed`
2. `triggerCount`

### `ReliefRequest` Android DTO

Nên thêm:

1. `urgencyLevel`
2. `clientRequestId` nếu muốn retry an toàn hơn

### Backend entity chỉ sửa tối thiểu nếu thực sự cần

Nếu được thêm field vào entity/bảng hiện có, thứ tự ưu tiên:

1. `sos_requests.trigger_count`
2. `sos_requests.client_request_id`
3. `sos_requests.location_captured_at`
4. `sos_requests.location_accuracy_m`
5. `aid_requests.client_request_id`
6. `aid_requests.urgency_level`

Nếu chưa muốn migration ngay:

1. vẫn có thể triển khai quick SOS bằng field/location hiện tại
2. dedupe làm trước
3. full idempotency để sau qua Redis hoặc migration nhỏ

---

# 4. Sửa flow frontend Android Java trên class hiện có

## 4.1 `VictimSosViewModel` nên sửa gì để hỗ trợ quick SOS

Nên giữ class này, nhưng tách rõ 3 nhóm method:

1. `submitQuickSelfSos(...)`
2. `submitRelativeSos(...)`
3. `submitDetailedSelfSos(...)` nếu bạn vẫn giữ form cũ như optional flow

### Hướng sửa cụ thể

1. self SOS one-tap không gọi `validateSelfSos(...)` kiểu form cũ nữa
2. self SOS one-tap không xử lý ảnh trước khi gửi
3. self SOS one-tap chỉ cần:
   - resolve location tốt nhất
   - build request tối thiểu
   - gọi `UploadSosUseCase.uploadQuickSos(...)`

### Những gì nên giữ

1. `submitRelativeSos(...)`
2. `validationError`
3. `cachedUserResult`

### Những gì nên tách riêng trong cùng ViewModel

1. `quickSosResult`
2. `locationPermissionRequiredEvent`
3. `backgroundTrackingState`

## 4.2 `VictimSupplyTabFragment` nên giữ cho Aid Request như thế nào

Giữ:

1. form item selection
2. số lượng người
3. speech-to-text
4. current location fallback

Sửa:

1. thêm priority/urgency UI nếu business cần
2. khi submit thất bại do mất mạng:
   - không chỉ show error
   - phải chuyển sang pending local queue
3. dùng chung method resolve location từ `UserLocationManager`

## 4.3 `UserLocationManager` nên mở rộng gì

### Cho `current location`

Thêm method:

1. `getCurrentLocationOnce(...)`
2. timeout rõ ràng
3. fallback tự động sang `getLastLocation()`

### Cho `cached / last known location`

Giữ:

1. `getLatestLocation()`
2. `getFreshLocation(...)`

Mở rộng:

1. `LocationSnapshot` chứa thêm `accuracy`
2. `LocationSnapshot` chứa thêm `capturedAtMillis`

### Cho `continuous updates`

Thêm:

1. `startContinuousTracking(LocationTrackingConfig config, Listener listener)`
2. `stopContinuousTracking()`

## 4.4 Có cần thêm service tracking riêng không

Có, nhưng ở mức tối thiểu.

Tên gợi ý:

1. `EmergencyTrackingService`

Lý do bắt buộc:

1. fragment/viewmodel không đủ lifecycle để sống qua background
2. `UserLocationManager` không phải Android `Service`
3. continuous tracking trong tình huống SOS cần foreground service để ổn định hơn

Trách nhiệm của `EmergencyTrackingService`:

1. start foreground notification
2. gọi `UserLocationManager.startContinuousTracking(...)`
3. enqueue location sync vào local queue
4. trigger flush ngay nếu có mạng

## 4.5 Có cần local queue không, và gắn với `AppDatabase` ra sao

Có.

Nhưng chỉ cần một queue local tối thiểu.

### Đề xuất local queue tối thiểu

Thêm vào `AppDatabase`:

1. `PendingSyncEntity`
2. `PendingSyncDao`

Các type nên có:

1. `SOS_CREATE`
2. `AID_CREATE`
3. `SOS_LOCATION_UPDATE`
4. `AID_LOCATION_UPDATE`

### Lưu ý rất quan trọng

Vì backend hiện tại không có bảng history location riêng:

1. location queue không nên giữ vô hạn mọi điểm
2. chỉ cần giữ `latest unsent location` cho mỗi request đang active

Nghĩa là:

1. nếu đang có `SOS_LOCATION_UPDATE` pending cho `sosId=X`
2. nhận thêm vị trí mới
3. thì update row pending cũ bằng payload location mới nhất
4. không cần append vô hạn

Cách này vừa ít sửa local DB, vừa khớp với backend latest-location semantics.

---

# 5. Sửa flow backend Java trên API hiện có

## 5.1 `SosController`

Giữ nguyên:

1. `POST /api/sos-requests`
2. auth resolution từ `Authentication`
3. guest fallback nếu hệ thống vẫn cần guest SOS

Sửa:

1. chấp nhận quick SOS payload qua chính endpoint cũ
2. nếu request có `quickSos=true` hoặc thiếu toàn bộ form fields:
   - route vào quick behavior
3. thêm action update latest location nếu cần:
   - `POST /api/sos-requests/{id}/location`

## 5.2 `CreateSosRequestUseCase`

Giữ nguyên:

1. persist SOS
2. publish `SosRequestCreatedEvent`

Sửa:

1. nếu quick SOS:
   - default `peopleCount = 1`
   - default `urgencyLevel = CRITICAL` nếu client không gửi
   - `description = null`
   - `imageUrl = null`
2. chạy dedupe trước khi save
3. nếu là duplicate hợp lệ:
   - trả SOS cũ
   - không publish event mới

### Dedupe nên đặt ở đâu

Đặt ngay trong `CreateSosRequestUseCase`, vì:

1. đây là business rule
2. controller nên mỏng
3. repository chỉ lo query

### Query reuse từ repository hiện có

Tận dụng:

1. `findByRequesterIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(...)`

Không cần bảng mới.

## 5.3 `AidController`

Giữ nguyên:

1. `POST /api/aid-requests`
2. cancel/list/get/voice

Sửa:

1. để Android gửi đúng `urgencyLevel`
2. nếu cần continuous latest location:
   - thêm `POST /api/aid-requests/{id}/location`

## 5.4 `CreateAidRequestUseCase`

Giữ nguyên:

1. validate user tồn tại
2. save `AidRequest`
3. save items
4. publish `AidRequestCreatedEvent`

Sửa:

1. persist `urgencyLevel` nếu quyết định dùng field này thực sự
2. nếu chưa có field DB:
   - tạm thời normalize priority tại tầng business và response, nhưng sẽ không bền
3. thêm optional dedupe theo `clientRequestId` nếu sau này có Redis hoặc field DB

## 5.5 `modules/mission`

Giữ nguyên:

1. event-driven mission creation
2. `missions.victim_location`

Sửa nhẹ:

1. khi `sos_requests.location` hoặc `aid_requests.location` được update, nếu mission tương ứng đã tồn tại thì update luôn `missions.victim_location`
2. sửa `MissionMapper.toTrackingResponse(...)` để trả volunteer current location thật nếu muốn UI tracking đầy đủ

## 5.6 Idempotency/dedupe nên đặt ở đâu

### SOS

Ưu tiên 1:

1. business dedupe trong `CreateSosRequestUseCase`

Ưu tiên 2:

1. transport idempotency bằng Redis

Lý do:

1. backend đã có Redis infra sẵn
2. không cần tạo bảng mới

### Aid Request

1. có thể bắt đầu với client-side retry suppression
2. sau đó thêm Redis idempotency nếu thấy duplicate thực tế

---

# 6. Nếu cần endpoint mới thì phải chứng minh

## 6.1 Không cần endpoint mới cho tạo SOS

Lý do:

1. `SosController` và `POST /api/sos-requests` đã tồn tại
2. DTO hiện tại chỉ cần mở rộng field optional
3. quick SOS có thể đi qua cùng endpoint cũ

## 6.2 Không cần endpoint mới cho tạo Aid Request

Lý do:

1. `AidController` đã có `POST /api/aid-requests`
2. chỉ cần sửa Android request DTO + backend persistence cho `urgencyLevel`

## 6.3 Endpoint mới chỉ cần cho update location sau khi request đã được tạo

### Tại sao API cũ không đủ

1. `POST /api/sos-requests` chỉ create một lần
2. `POST /api/aid-requests` cũng chỉ create một lần
3. continuous tracking xảy ra sau create, trong background

### Tại sao không thể chỉ thêm field vào create endpoint

1. create endpoint không biết xử lý các lần ping location sau khi resource đã tồn tại
2. nếu reuse create endpoint để update thì sẽ làm semantics API sai và rất dễ tạo duplicate

### Endpoint mới tối thiểu

Chỉ nên thêm 2 sub-action dưới controller cũ:

1. `POST /api/sos-requests/{id}/location`
2. `POST /api/aid-requests/{id}/location`

Không cần:

1. controller mới
2. module mới
3. bảng mới

### Payload tối thiểu cho update location

```json
{
  "lat": 10.7769,
  "lng": 106.7009,
  "accuracy": 8.4,
  "capturedAt": "2026-04-19T08:00:00Z"
}
```

### Backend update strategy

1. update `sos_requests.location` hoặc `aid_requests.location`
2. nếu có mission liên quan thì update luôn `missions.victim_location`
3. dùng `updated_at` hiện có làm mốc server-side update time

---

# 7. Chỉ được sửa tối thiểu trên data/entity hiện có

## 7.1 Reuse entity/bảng hiện có

Tận dụng backend:

1. `sos_requests.location`
2. `aid_requests.location`
3. `missions.victim_location`
4. `sos_requests.updated_at`
5. `aid_requests.updated_at`

## 7.2 Field nào có thể thêm nếu thực sự cần

### `sos_requests`

Giá trị cao nhất nếu chỉ được thêm rất ít:

1. `trigger_count`
2. `client_request_id`
3. `location_accuracy_m`
4. `location_captured_at`

### `aid_requests`

Giá trị cao nhất:

1. `urgency_level`
2. `client_request_id`
3. `location_accuracy_m`
4. `location_captured_at`

### Không bắt buộc thêm ngay

1. `trackingSessionId`
2. `deviceInfo`

Hai field này chỉ cần khi bạn muốn debug sâu hoặc mở rộng analytics.

## 7.3 Index nào có thể cân nhắc thêm

Chỉ cân nhắc nếu thực sự cần:

1. `sos_requests(requester_id, created_at desc)`
2. `aid_requests(requester_id, created_at desc)`
3. `missions(sos_request_id)`
4. `missions(aid_request_id)`

Mục đích:

1. tăng tốc dedupe/query recent request
2. tăng tốc sync location sang mission

---

# 8. Luồng hoạt động chi tiết sau khi sửa

## 8.1 Khi user mở app

1. app check session hiện có
2. xin foreground location
3. `UserLocationManager.refreshOnce()`
4. cache location mới nhất vào `TokenManager`
5. không xin background location ngay
6. không start tracking service ngay

## 8.2 Khi app xin quyền location

1. xin `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`
2. nếu user grant:
   - warm current location
3. nếu user deny:
   - vẫn cho vào app
   - nhưng block SOS/Aid Request location-dependent flow bằng message rõ ràng

## 8.3 Khi user nhấn SOS

1. UI gọi `VictimSosViewModel.submitQuickSelfSos()`
2. ViewModel resolve vị trí theo thứ tự:
   - fresh cached location từ `UserLocationManager`
   - `getCurrentLocation()`
   - `getLastLocation()`
3. tạo `clientRequestId`
4. gửi `POST /api/sos-requests` bằng DTO cũ đã mở rộng
5. nếu online:
   - nhận `sosId`
   - start `EmergencyTrackingService` với `sosId`
6. nếu offline:
   - enqueue `SOS_CREATE` vào local queue
   - UI báo `SOS đã được lưu cục bộ, sẽ gửi lại khi có mạng`
   - chưa start location sync server cho đến khi create thành công

## 8.4 Khi backend nhận SOS

1. `SosController.createSosRequest(...)`
2. resolve user từ auth nếu có
3. `CreateSosRequestUseCase.execute(...)`
4. use case check dedupe recent SOS
5. nếu duplicate:
   - trả response của SOS cũ
6. nếu không duplicate:
   - save `SosRequest`
   - publish `SosRequestCreatedEvent`
7. `modules/mission` tạo mission rescue async như hiện tại

## 8.5 Khi user tạo Aid Request

1. `VictimSupplyTabFragment` giữ nguyên form
2. lấy current/cached location qua `UserLocationManager`
3. `VictimSupplyViewModel.submitRequest(...)`
4. `POST /api/aid-requests`
5. nếu online:
   - backend save request
   - mission được tạo async như hiện tại
6. nếu offline:
   - enqueue `AID_CREATE`
   - UI báo pending local

## 8.6 Khi app gửi location

1. chỉ gửi liên tục khi có active SOS hoặc active Aid Request cần theo dõi
2. `EmergencyTrackingService` nhận location callback
3. service enqueue `SOS_LOCATION_UPDATE` hoặc `AID_LOCATION_UPDATE`
4. nếu có mạng:
   - flush ngay tới sub-action endpoint tương ứng
5. nếu không có mạng:
   - chỉ giữ latest unsent location trong queue

## 8.7 Khi mất mạng

1. create request vào queue local
2. location update không append vô hạn
3. chỉ overwrite latest pending location của resource đang active

## 8.8 Khi app vào background

1. nếu không có active SOS/Aid tracking:
   - không chạy foreground service
2. nếu có active SOS/Aid tracking:
   - `EmergencyTrackingService` tiếp tục chạy
   - location update vẫn tiếp tục ở mức Android cho phép

## 8.9 Khi app bị vuốt khỏi recent apps

1. UI fragment/activity mất
2. nếu foreground service đang chạy:
   - service có thể tiếp tục
3. WorkManager vẫn có thể flush queue khi điều kiện phù hợp
4. không được hứa là sống mãi trên mọi OEM

## 8.10 Khi app mở lại

1. app đọc queue local
2. retry pending `SOS_CREATE` / `AID_CREATE`
3. nếu đã có active SOS/Aid đang track:
   - resume UI state từ local cache hoặc API
4. nếu foreground service không còn:
   - user có thể resume tracking bằng state hiện có

---

# 9. Background location strategy phù hợp project hiện tại

## 9.1 Phần nào tận dụng `UserLocationManager`

Dùng `UserLocationManager` cho:

1. lấy current location một lần
2. lấy cached / fresh location
3. continuous updates source cho service
4. cache latest location vào local store

## 9.2 Phần nào cần Foreground Service

Cần `EmergencyTrackingService` khi:

1. đã tạo SOS thành công và cần giữ latest location update
2. Aid Request được đánh dấu cần live location

Không cần service khi:

1. user chỉ mở app và xem map
2. user chỉ gửi Aid Request một lần rồi thôi

## 9.3 Phần nào dùng WorkManager

Dùng `WorkManager` cho:

1. flush queue local khi có mạng
2. replay pending request sau process death
3. retry exponential backoff

Không dùng WorkManager cho:

1. polling location 5-10 giây
2. continuous tracking real-time

## 9.4 Phần nào chỉ lấy cached/current location

Chỉ cần one-shot location cho:

1. quick SOS lúc bấm nút
2. Aid Request tạo mới
3. map initial state

## 9.5 Phần nào cần local queue

Queue local cần cho:

1. `SOS_CREATE`
2. `AID_CREATE`
3. latest location update cho active resource

## 9.6 Hệ thống hiện tại đang thiếu gì để chạy ổn ngoài đời

Thiếu:

1. victim-side foreground tracking service
2. queue local cho retry
3. endpoint/action update latest location sau create
4. dedupe/idempotency cho SOS
5. field alignment giữa Android Aid Request và backend

---

# 10. Offline-first / retry strategy

## 10.1 Local queue tối thiểu

Chỉ thêm một local queue table trong `AppDatabase`.

Entity tối thiểu:

1. `id`
2. `type`
3. `resourceId`
4. `payloadJson`
5. `status`
6. `retryCount`
7. `nextRetryAt`
8. `updatedAt`

## 10.2 Trạng thái tối thiểu

1. `PENDING`
2. `SYNCING`
3. `SENT`
4. `FAILED`

## 10.3 Rule flush tối thiểu

Flush khi:

1. request vừa được enqueue
2. app trở lại foreground
3. WorkManager chạy với `NetworkType.CONNECTED`
4. service đang chạy và phát hiện có mạng

## 10.4 Retry tối thiểu

Backoff:

1. 5s
2. 15s
3. 30s
4. 60s
5. 5m

## 10.5 Cách tránh duplicate khi retry

### SOS

1. client tạo `clientRequestId`
2. gửi kèm request
3. backend dedupe theo:
   - Redis idempotency key nếu có
   - hoặc recent same-user same-location rule nếu chưa có Redis

### Aid Request

1. client tạo `clientRequestId`
2. khi retry dùng lại cùng id

### Location update

1. queue chỉ giữ latest pending location
2. không append vô hạn

---

# 11. Pseudocode bám sát project hiện tại

## 11.1 Sửa `VictimSosViewModel` để gửi quick SOS

```java
class VictimSosViewModel extends BaseViewModel {

    private final UploadSosUseCase uploadSosUseCase;
    private final UserLocationManager userLocationManager;

    private final MutableLiveData<QuickSosPayload> quickSosTrigger = new MutableLiveData<>();
    private final LiveData<NetworkResultWrapper<String>> quickSosSource =
        Transformations.switchMap(quickSosTrigger, payload ->
            uploadSosUseCase.uploadQuickSos(
                payload.latitude,
                payload.longitude,
                payload.accuracy,
                payload.triggeredAt,
                payload.locationCapturedAt,
                payload.clientRequestId,
                payload.deviceInfo
            )
        );

    public void submitQuickSelfSos() {
        if (!userLocationManager.hasLocationPermission()) {
            validationError.postValue(
                ValidationResult.invalid(
                    ValidationResult.Field.LOCATION,
                    "SOS requires location permission."
                )
            );
            return;
        }

        submitQuickSosResult.postValue(NetworkResultWrapper.loading());

        ioExecutor.execute(() -> {
            UserLocationManager.LocationSnapshot snapshot =
                userLocationManager.resolveBestEffortSosLocation();

            if (snapshot == null) {
                submitQuickSosResult.postValue(
                    NetworkResultWrapper.error("Cannot resolve current location for SOS.")
                );
                return;
            }

            quickSosTrigger.postValue(
                new QuickSosPayload(
                    snapshot.getLatitude(),
                    snapshot.getLongitude(),
                    snapshot.getAccuracy(),
                    System.currentTimeMillis(),
                    snapshot.getCapturedAtMillis(),
                    UUID.randomUUID().toString(),
                    buildDeviceInfo()
                )
            );
        });
    }
}
```

## 11.2 Sửa `CreateSosRequestUseCase` để xử lý one-tap SOS

```java
public SosRequestResponse execute(UUID requesterId, CreateSosRequest createDto) {
    userFacade.getUserById(requesterId);

    boolean quickSos = Boolean.TRUE.equals(createDto.getQuickSos())
        || isBlank(createDto.getDescription()) && isBlank(createDto.getImageUrl());

    if (quickSos) {
        Optional<SosRequest> duplicate = findRecentDuplicate(requesterId, createDto);
        if (duplicate.isPresent()) {
            return sosMapper.toResponse(duplicate.get(), null);
        }
    }

    SosRequest sosRequest = SosRequest.builder()
        .requesterId(requesterId)
        .location(SosRequest.createPoint(createDto.getLat(), createDto.getLng()))
        .address(createDto.getAddress())
        .description(quickSos ? null : createDto.getDescription())
        .peopleCount(resolvePeopleCount(createDto, quickSos))
        .urgencyLevel(resolveUrgency(createDto, quickSos))
        .imageUrl(quickSos ? null : sosSceneImageService.resolveImageUrl(createDto.getImageUrl()))
        .status(SosStatus.PENDING)
        .build();

    SosRequest saved = sosRequestRepository.save(sosRequest);
    eventPublisher.publishEvent(new SosRequestCreatedEvent(saved.getId(), saved.getLat(), saved.getLng()));
    return sosMapper.toResponse(saved, null);
}
```

## 11.3 Flow lấy location tốt nhất

```java
LocationSnapshot resolveBestEffortSosLocation() {
    LocationSnapshot cached = getFreshLocation(15_000L);
    if (cached != null) {
        return cached;
    }

    LocationSnapshot current = tryGetCurrentLocation(2_500L);
    if (current != null) {
        return current;
    }

    return getLatestLocation();
}
```

## 11.4 Flow retry khi offline

```java
void enqueueAndFlush(PendingSyncEntity entity) {
    pendingSyncDao.upsert(entity);
    if (networkMonitor.isOnline()) {
        flushPendingQueue();
    } else {
        workScheduler.scheduleFlushOnReconnect();
    }
}
```

## 11.5 Flow gửi location background

```java
onLocationUpdate(snapshot) {
    PendingSyncEntity latestLocationUpdate =
        PendingSyncEntity.forLatestLocation(resourceType, resourceId, snapshot);

    pendingSyncDao.replaceLatestLocation(resourceType, resourceId, latestLocationUpdate);

    if (networkMonitor.isOnline()) {
        flushPendingQueue();
    }
}
```

---

# 12. Code skeleton Java bám theo code cũ

## 12.1 `VictimSosViewModel`

```java
public class VictimSosViewModel extends BaseViewModel {

    private final MutableLiveData<QuickSosPayload> quickSosTrigger = new MutableLiveData<>();
    private final MediatorLiveData<NetworkResultWrapper<String>> submitQuickSosResult = new MediatorLiveData<>();

    public LiveData<NetworkResultWrapper<String>> getSubmitQuickSosResult() {
        return submitQuickSosResult;
    }

    public void submitQuickSelfSos() {
        // resolve location -> quickSosTrigger.postValue(...)
    }

    private static final class QuickSosPayload {
        final double latitude;
        final double longitude;
        final Double accuracy;
        final long triggeredAt;
        final long locationCapturedAt;
        final String clientRequestId;
        final String deviceInfo;

        QuickSosPayload(double latitude,
                        double longitude,
                        Double accuracy,
                        long triggeredAt,
                        long locationCapturedAt,
                        String clientRequestId,
                        String deviceInfo) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.accuracy = accuracy;
            this.triggeredAt = triggeredAt;
            this.locationCapturedAt = locationCapturedAt;
            this.clientRequestId = clientRequestId;
            this.deviceInfo = deviceInfo;
        }
    }
}
```

## 12.2 `UserLocationManager`

```java
@Singleton
public class UserLocationManager {

    public static final long QUICK_SOS_FRESH_MAX_AGE_MS = 15_000L;

    @Nullable
    public LocationSnapshot resolveBestEffortSosLocation() {
        LocationSnapshot cached = getFreshLocation(QUICK_SOS_FRESH_MAX_AGE_MS);
        if (cached != null) {
            return cached;
        }

        refreshOnce();
        return getLatestLocation();
    }

    public void startContinuousTracking(@NonNull LocationTrackingConfig config,
                                        @NonNull LocationUpdateListener listener) {
        // build LocationRequest from config
        // requestLocationUpdates(...)
        // on callback -> update local cache + listener.onLocation(snapshot)
    }

    public void stopContinuousTracking() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    public static final class LocationSnapshot {
        private final double latitude;
        private final double longitude;
        private final long capturedAtMillis;
        @Nullable private final Double accuracy;

        public LocationSnapshot(double latitude,
                                double longitude,
                                long capturedAtMillis,
                                @Nullable Double accuracy) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.capturedAtMillis = capturedAtMillis;
            this.accuracy = accuracy;
        }

        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
        public long getCapturedAtMillis() { return capturedAtMillis; }
        @Nullable public Double getAccuracy() { return accuracy; }
    }
}
```

## 12.3 Local queue gắn với `AppDatabase`

```java
@Entity(tableName = "pending_sync_queue")
public class PendingSyncEntity {

    @PrimaryKey
    @NonNull
    public String id;

    @NonNull
    public String type;

    @Nullable
    public String resourceId;

    @NonNull
    public String payloadJson;

    @NonNull
    public String status;

    public int retryCount;
    public long nextRetryAt;
    public long updatedAt;
}
```

```java
@Dao
public interface PendingSyncDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(PendingSyncEntity entity);

    @Query("SELECT * FROM pending_sync_queue WHERE status = 'PENDING' AND nextRetryAt <= :now ORDER BY updatedAt ASC")
    List<PendingSyncEntity> findReady(long now);

    @Query("DELETE FROM pending_sync_queue WHERE id = :id")
    void deleteById(String id);
}
```

```java
@Database(
    entities = {
        AppSettingsEntity.class,
        VictimHistoryEntity.class,
        PendingSyncEntity.class
    },
    version = 2,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    public abstract AppSettingsDao appSettingsDao();
    public abstract VictimHistoryDao victimHistoryDao();
    public abstract PendingSyncDao pendingSyncDao();
}
```

## 12.4 `SosController`

```java
@PostMapping
public ResponseEntity<ApiResponse<SosRequestResponse>> createSosRequest(
        @Valid @RequestBody CreateSosRequest request,
        Authentication authentication) {

    UUID userId = resolveAuthenticatedUserId(authentication);

    SosRequestResponse response = userId != null
        ? createSosRequestUseCase.execute(userId, request)
        : createGuestSosRequestUseCase.execute(CreateGuestSosRequest.from(request));

    return ResponseEntity.ok(ApiResponse.success("SOS request created", response));
}

@PostMapping("/{id}/location")
public ResponseEntity<ApiResponse<Void>> updateSosLocation(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateRequestLocationRequest request,
        Authentication authentication) {
    UUID userId = resolveAuthenticatedUserId(authentication);
    updateSosLocationUseCase.execute(userId, id, request);
    return ResponseEntity.ok(ApiResponse.success("SOS location updated", null));
}
```

## 12.5 `CreateSosRequestUseCase`

```java
@Transactional
public SosRequestResponse execute(UUID requesterId, CreateSosRequest createDto) {
    userFacade.getUserById(requesterId);

    Optional<SosRequest> duplicate = findQuickSosDuplicate(requesterId, createDto);
    if (duplicate.isPresent()) {
        return sosMapper.toResponse(duplicate.get(), null);
    }

    SosRequest sosRequest = SosRequest.builder()
        .requesterId(requesterId)
        .location(SosRequest.createPoint(createDto.getLat(), createDto.getLng()))
        .address(createDto.getAddress())
        .description(Boolean.TRUE.equals(createDto.getQuickSos()) ? null : createDto.getDescription())
        .peopleCount(resolvePeopleCount(createDto))
        .urgencyLevel(resolveUrgencyLevel(createDto))
        .imageUrl(resolveImageIfNeeded(createDto))
        .status(SosStatus.PENDING)
        .build();

    SosRequest saved = sosRequestRepository.save(sosRequest);
    eventPublisher.publishEvent(new SosRequestCreatedEvent(saved.getId(), saved.getLat(), saved.getLng()));
    return sosMapper.toResponse(saved, null);
}
```

## 12.6 DTO/entity mở rộng tối thiểu

```java
public class CreateSosRequest {
    @NotNull private Double lat;
    @NotNull private Double lng;
    private String address;
    private String description;
    private Integer peopleCount = 1;
    private UrgencyLevel urgencyLevel;
    private String imageUrl;

    private Boolean quickSos;
    private Instant triggeredAt;
    private Instant locationCapturedAt;
    private Double accuracy;
    private String clientRequestId;
    private String deviceInfo;
}
```

```java
public class ReliefRequest {
    private final double lat;
    private final double lng;
    private final String address;
    private final int adultsCount;
    private final int elderlyCount;
    private final int childrenCount;
    private final String notes;
    private final String urgencyLevel;
    private final List<RequestedItemRequest> items;
}
```

---

# 13. Những lỗi dễ gặp khi sửa trên hệ thống sẵn có

1. Nhét quick SOS vào flow form cũ của `VictimRescueTabFragment`, dẫn tới code lẫn lộn giữa one-tap và detailed form.
2. Tạo endpoint SOS mới dù endpoint create cũ đã đủ, làm vỡ contract không cần thiết.
3. Chỉ sửa Android DTO mà quên sửa backend DTO/entity/mapper, khiến field bị rơi.
4. Thêm retry nhưng không có dedupe, dẫn tới duplicate SOS/Aid Request.
5. Thêm background tracking nhưng không có `Foreground Service`, khiến app chết khi vào background.
6. Queue mọi điểm location riêng lẻ dù backend chỉ lưu latest location, làm local queue phình vô ích.
7. Quên update `missions.victim_location` khi update `sos_requests.location` hoặc `aid_requests.location`, khiến tracking không đồng nhất.
8. Giữ `SecurityConfig` mở public toàn bộ `/api/sos-requests/**` dù app đang kỳ vọng auth user flow.
9. Cố gắng xử lý background restart quá sớm bằng `BOOT_COMPLETED`, trong khi Android hiện đại giới hạn khá chặt.
10. Sửa entity backend quá mạnh ngay từ đầu, khiến migration khó và rủi ro cao.

---

# 14. Khuyến nghị triển khai thực tế

## 14.1 Làm trước

1. Refactor `VictimSosViewModel` thành quick SOS thật sự.
2. Refactor `VictimRescueTabFragment` để nút SOS gửi ngay, không phụ thuộc form.
3. Mở rộng Android/backend `CreateSosRequest` DTO.
4. Sửa `CreateSosRequestUseCase` để support quick SOS + dedupe.
5. Sửa `VictimSupplyMapper` + `ReliefRequest` để gửi `urgencyLevel` đúng.

## 14.2 Làm sau ngay khi xong quick SOS

1. Thêm `PendingSyncEntity` vào `AppDatabase`
2. Thêm `PendingSyncWorker`
3. Cho `VictimSupplyRepositoryImpl` và `VictimSosRepositoryImpl` enqueue local khi offline

## 14.3 Làm tiếp nếu cần background tracking thực tế

1. Thêm `EmergencyTrackingService`
2. Thêm sub-action update location vào `SosController` / `AidController`
3. Sync latest location vào:
   - `sos_requests.location`
   - `aid_requests.location`
   - `missions.victim_location`

## 14.4 Tạm thời chưa cần làm ngay

1. full route history của victim
2. bảng backend location mới
3. websocket victim location feed riêng
4. boot recovery phức tạp

## 14.5 Thứ tự khuyến nghị cuối cùng

1. Quick SOS one-tap trên endpoint cũ
2. Aid Request field alignment
3. SOS dedupe
4. local queue tối thiểu
5. foreground tracking service
6. latest-location update sub-action
7. tracking UI polish / mission tracking fix

---

## Kết luận ngắn

Phương án ít phá hệ thống nhất là:

1. giữ nguyên `POST /api/sos-requests` và biến nó thành quick-SOS-capable endpoint
2. giữ nguyên `POST /api/aid-requests`
3. refactor `VictimSosViewModel` và `VictimRescueTabFragment` để SOS không còn là form
4. reuse `UserLocationManager` làm location core
5. thêm đúng 3 mảnh tối thiểu ở Android:
   - local queue
   - WorkManager flush
   - foreground tracking service
6. nếu cần live victim location sau create, thêm sub-action dưới controller cũ thay vì dựng module/API mới

Đó là cách sửa tối thiểu, hợp với kiến trúc hiện tại, và không đẩy project sang hướng greenfield.
