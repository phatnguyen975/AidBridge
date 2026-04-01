# SOS Domain - Spring Backend

## 1. Tổng quan
Domain SOS quản lý các yêu cầu cứu trợ khẩn cấp (SOS requests) và liên kết với module mission để tạo nhiệm vụ cứu hộ.

Phạm vi chính của module SOS:
- Tạo yêu cầu SOS của người dùng đã xác thực (`CreateSosRequestUseCase`)
- Tạo yêu cầu SOS của guest/khách mà không cần user ID (`CreateGuestSosRequestUseCase`)
- Truy vấn chi tiết yêu cầu SOS theo `id` (`GetSosRequestUseCase`)
- Liệt kê tất cả yêu cầu SOS (`ListSosRequestsUseCase`)
- Lưu trữ dữ liệu qua `SosJpaRepository`
- Map entity sang response qua `SosMapper`
- Cung cấp facade chung `SosFacade` để các module khác gọi

## 2. Endpoint gọi SOS
Hiện tại controller chính là `SosController` với base path:

- `POST /sos-request`
- `GET /sos-request/{id}`
- `GET /sos-request`

### `POST /sos-request`
- Payload: `CreateSosRequest`
- Yêu cầu authenticated user
- `@AuthenticationPrincipal UUID userId` được đưa vào `CreateSosRequestUseCase`
- Mission creation được xử lý bởi listener khi `SosRequestCreatedEvent` được publish

### `GET /api/victim/sos-requests/{id}`
- Trả về chi tiết yêu cầu SOS kèm mission liên quan nếu có

### `GET /api/victim/sos-requests`
- Trả về danh sách tất cả SOS request kèm mission tương ứng nếu tìm thấy

> Lưu ý: Mặc dù có `CreateGuestSosRequestUseCase`, trong mã hiện tại chưa thấy controller public để gọi endpoint guest.

## 3. Luồng xử lý chính

### 3.1 CreateSosRequestUseCase
Class: `spring-backend/src/main/java/com/drc/aidbridge/modules/sos/internal/usecase/CreateSosRequestUseCase.java`

Flow:
1. Lấy thông tin requester bằng `UserFacade.getUserById(requesterId)`
2. Tạo entity `SosRequest` với trạng thái `SosStatus.PENDING`
3. Lưu `SosRequest` qua `SosJpaRepository.save(...)`
4. Tạo mission cứu hộ qua `MissionFacade.createRescueMission(...)`
5. Map entity + mission sang `SosRequestResponse`

Kết quả trả về: `SosRequestResponse`

### 3.2 CreateGuestSosRequestUseCase
Class: `spring-backend/src/main/java/com/drc/aidbridge/modules/sos/internal/usecase/CreateGuestSosRequestUseCase.java`

Flow gần giống `CreateSosRequestUseCase`, nhưng:
- `requesterId` được đặt `null`
- Không gọi `UserFacade`
- Vẫn lưu SOS request và tạo mission cứu trợ

### 3.3 GetSosRequestUseCase
Class: `spring-backend/src/main/java/com/drc/aidbridge/modules/sos/internal/usecase/GetSosRequestUseCase.java`

Flow:
1. Tìm SOS request theo `id` qua `SosJpaRepository.findById(id)`
2. Nếu không tìm thấy, ném `ResourceNotFoundException`
3. Gọi `MissionFacade.findMissionBySosRequestId(sos.getId())`
4. Map kết quả sang `SosRequestResponse`

### 3.4 ListSosRequestsUseCase
Class: `spring-backend/src/main/java/com/drc/aidbridge/modules/sos/internal/usecase/ListSosRequestsUseCase.java`

Flow:
1. Lấy tất cả SOS request qua `SosJpaRepository.findAll()`
2. Với mỗi request, thử lấy mission liên quan qua `MissionFacade.findMissionBySosRequestId(...)`
3. Map mỗi request sang response

## 4. Entity và dữ liệu SOS
Class: `spring-backend/src/main/java/com/drc/aidbridge/modules/sos/internal/entity/SosRequest.java`

Các trường chính:
- `UUID id`
- `UUID requesterId` (có thể `null` nếu guest)
- `Double lat`, `Double lng`
- `String address`, `String description`
- `Integer peopleCount`
- `UrgencyLevel urgencyLevel`
- `SosStatus status`
- `String imageUrl`
- `Instant createdAt`, `Instant updatedAt`

## 5. Repository
Class: `spring-backend/src/main/java/com/drc/aidbridge/modules/sos/internal/repository/SosJpaRepository.java`

Thừa kế `JpaRepository<SosRequest, UUID>` và bổ sung:
- `List<SosRequest> findByRequesterId(UUID requesterId)`
- `List<SosRequest> findByStatus(SosStatus status)`

## 6. Mapping response
Class: `spring-backend/src/main/java/com/drc/aidbridge/modules/sos/internal/mapper/SosMapper.java`

Chức năng chính:
- `toDTO(SosRequest entity)`
- `toResponse(SosRequest entity, MissionDTO mission)`

`SosRequestResponse` bao gồm thêm các trường mission:
- `missionId`
- `missionType`
- `missionStatus`

## 7. Facade SOS
Interface: `spring-backend/src/main/java/com/drc/aidbridge/modules/sos/SosFacade.java`

Các phương thức:
- `Optional<SosDTO> getSosRequestById(UUID id)`
- `void updateStatus(UUID id, SosStatus status)`
- `boolean existsById(UUID id)`

Implementation: `spring-backend/src/main/java/com/drc/aidbridge/modules/sos/internal/SosFacadeImpl.java`

## 8. Tương tác với module Mission
SOS module tích hợp với `MissionFacade` để:
- tạo nhiệm vụ cứu hộ khi tạo SOS request mới: `createRescueMission(sosRequestId, lat, lng)`
- tìm mission liên quan khi truy vấn SOS request: `findMissionBySosRequestId(sosRequestId)`

## 9. Tổng kết call chain
1. Client gọi `POST /sos-request`
2. `SosController.createSosRequest(...)`
3. `CreateSosRequestUseCase.execute(userId, request)`
4. `SosJpaRepository.save(...)`
5. `eventPublisher.publishEvent(new SosRequestCreatedEvent(...))`
6. `SosMapper.toResponse(...)`
7. Trả về `ApiResponse<SosRequestResponse>`

---

## 10. Ghi chú
- `CreateGuestSosRequestUseCase` có thể dùng cho SOS không cần đăng nhập, nhưng cần thêm endpoint nếu muốn gọi từ client.
- `SosController` hiện phục vụ đường dẫn `/sos-request` (và vẫn giữ alias `/api/victim/sos-requests`) dành cho user đã xác thực.
- `status` SOS mặc định được khởi tạo là `SosStatus.PENDING`.
