# API lấy Current Mission của Volunteer — Thiết kế, DTO và Phương pháp tốt

Mục tiêu: mô tả nơi đặt endpoint, cách lấy "current mission" cho volunteer, các lựa chọn join/assemble dữ liệu và một thiết kế DTO mẫu cùng best-practices để triển khai nhanh và an toàn.

---

## 1) Endpoint nên đặt ở module nào?
- Nguyên tắc: dữ liệu và logic thuộc về `mission` module; nhưng endpoint REST có thể nằm ở `volunteer` module nếu đó là API đặc thù cho volunteer (ví dụ: `GET /volunteers/me/mission/current`).
- Khuyến nghị: đặt controller endpoint trong module `volunteer` (ví dụ `VolunteerController`) và delegate toàn bộ business logic vào `MissionFacade` thuộc module `mission`.
  - Lý do: rõ ràng về ranh giới tài nguyên, dễ kiểm soát quyền truy cập (chỉ volunteer được), nhưng logic truy vấn/assemble vẫn thuộc `mission`.

## 2) Định nghĩa "current mission"
- Cần thống nhất: "current" = mission đang active (chưa hoàn tất/không bị hủy).
- Ví dụ định nghĩa: status NOT IN {COMPLETED, CANCELLED} và chọn bản mới nhất theo `updatedAt` (hoặc theo `acceptedAt`/`startedAt` tùy nghiệp vụ).

## 3) Thu thập các bảng liên quan — join hay nhiều query?
- Hai lựa chọn chính:
  - A) Refactor entity sang JPA relation (`@ManyToOne`) và dùng fetch-join / EntityGraph / projection để lấy tất cả trong 1 query (hiệu năng tốt cho nhiều join).
  - B) Giữ nguyên `Mission` với các UUID và dùng `MissionFacade` để: lấy mission rồi gọi các repository khác theo id, assemble DTO. (Ít thay đổi schema, an toàn, bắt đầu nhanh.)
- Khuyến nghị: bắt đầu với B nếu muốn thay đổi nhỏ. Nếu thấy cần tối ưu sau này (N quries), cân nhắc A hoặc native query projection.

## 4) Repository: ví dụ query lấy current mission
- Theo Spring Data (option B):

```java
public interface MissionRepository extends JpaRepository<Mission, UUID> {
    @Query("select m from Mission m where m.volunteerId = :volunteerId and m.status not in :finalStatuses order by m.updatedAt desc")
    Optional<Mission> findCurrentByVolunteerId(@Param("volunteerId") UUID volunteerId, @Param("finalStatuses") Collection<MissionStatus> finalStatuses);
}
```
- Hoặc helper:

```java
Optional<Mission> findFirstByVolunteerIdAndStatusNotInOrderByUpdatedAtDesc(UUID volunteerId, Collection<MissionStatus> statuses);
```

## 5) DTO — structure mẫu
- Nguyên tắc: trả về summary/object nhẹ cho các liên quan (không trả full entity nếu không cần). Tránh dữ liệu nhạy cảm.

JSON ví dụ `MissionDto` (tối thiểu):

```json
{
  "id": "uuid",
  "missionType": "SOS|AID|HELP",
  "status": "PENDING|ACCEPTED|...",
  "priorityScore": 4.25,
  "victim": { "lat": 10.123, "lng": 106.123 },
  "timestamps": {
    "createdAt": "...",
    "acceptedAt": "...",
    "startedAt": "...",
    "completedAt": "..."
  },
  "volunteer": { "id": "uuid", "name": "Nguyen A", "phone": "..." },
  "hub": { "id": "uuid", "name": "Hub X", "lat": 10.1, "lng": 106.1 },
  "sosRequest": { "id": "uuid", "shortInfo": "..." }
}
```

Java DTO sketch (ví dụ):

```java
@Data
@Builder
public class MissionDto {
  private UUID id;
  private MissionType missionType;
  private MissionStatus status;
  private BigDecimal priorityScore;
  private BigDecimal victimLat;
  private BigDecimal victimLng;
  private Instant createdAt;
  private Instant acceptedAt;
  // nested summaries
  private EntitySummary volunteer;
  private EntitySummary hub;
  private EntitySummary sosRequest;
}

@Data
@Builder
public class EntitySummary {
  private UUID id;
  private String name;
  private String phone;
  private BigDecimal lat;
  private BigDecimal lng;
}
```

## 6) Mẫu flow implementation (Option B - facade)
1. `VolunteerController` nhận request (xác thực -> lấy `volunteerId`).
2. Gọi `missionFacade.getCurrentMissionForVolunteer(volunteerId)`.
3. Trong `MissionFacade`:
   - Gọi `missionRepository.findCurrentByVolunteerId(...)`.
   - Nếu có mission, thu thập các id liên quan (`hubId`, `volunteerId`, `sosRequestId`, ...).
   - Gọi các repository tương ứng: `hubRepo.findById(...)`, `volunteerRepo.findById(...)`, `sosRepo.findById(...)` (sử dụng `findAllById(List<UUID>)` nếu gọi nhiều lần để tránh N+1).
   - Map thành `MissionDto` (MapStruct recommended).
4. Trả về `200 OK` + `MissionDto` hoặc `204 No Content` nếu không có mission.

Pseudo-code:

```java
public Optional<MissionDto> getCurrentMissionForVolunteer(UUID volunteerId) {
  List<MissionStatus> finalStatuses = List.of(MissionStatus.COMPLETED, MissionStatus.CANCELLED);
  Mission m = missionRepo.findCurrentByVolunteerId(volunteerId, finalStatuses).orElse(null);
  if (m == null) return Optional.empty();

  // fetch related
  Hub hub = hubRepo.findById(m.getHubId()).orElse(null);
  Volunteer vol = volunteerRepo.findById(m.getVolunteerId()).orElse(null);
  SosRequest sos = sosRepo.findById(m.getSosRequestId()).orElse(null);

  return Optional.of(mapToDto(m, hub, vol, sos));
}
```

## 7) Hiệu năng & tối ưu
- Nếu call nhiều repo, dùng `findAllById(List<UUID>)` để gom các call.
- Nếu cần cực nhanh/ít IO: refactor sang JPA `@ManyToOne` và dùng fetch-join hoặc viết native SQL projection trả về DTO (1 query).
- Dùng `MapStruct` cho mapping giữa entity → DTO để code sạch.
- Cache thông tin tĩnh (hub list) nếu cần.

## 8) Bảo mật
- Kiểm tra authorization: chỉ trả mission cho volunteer tương ứng hoặc admin role.
- Không trả fields nhạy cảm (tokens, internal notes).

## 9) Test
- Unit test cho `MissionFacade` (mock repository responses).
- Integration test cho endpoint (`@SpringBootTest` hoặc testcontainer DB) để verify join/assemble.

## 10) Gợi ý endpoint REST
- `GET /api/volunteers/me/mission/current` — volunteer lấy mission hiện tại của chính họ.
- `GET /api/volunteers/{id}/missions?status=...` — admin hoặc support lấy danh sách (có phân trang).

---

## Kết luận & bước tiếp theo
- Bắt đầu nhanh: implement `MissionRepository.findCurrentByVolunteerId(...)` + `MissionFacade.getCurrentMissionForVolunteer(...)` + endpoint trong `volunteer` module.
- Khi cần, mình sẽ tạo code mẫu (repository, facade, DTO, controller) và tests.
