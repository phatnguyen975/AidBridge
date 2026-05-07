package com.drc.aidbridge.modules.mission.internal.repository;

import com.drc.aidbridge.modules.shared.enums.MissionStatus;
import com.drc.aidbridge.modules.shared.enums.MissionType;
import com.drc.aidbridge.modules.mission.internal.entity.Mission;
import com.drc.aidbridge.modules.mission.internal.repository.projection.MissionHistoryProjection;
import com.drc.aidbridge.modules.mission.internal.repository.projection.MissionHistoryFullProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.drc.aidbridge.modules.sos.internal.entity.SosRequest;
import com.drc.aidbridge.modules.aid.internal.entity.AidRequest;

@Repository
public interface MissionJpaRepository extends JpaRepository<Mission, UUID> {

        List<MissionStatus> TERMINAL_STATUSES = List.of(
                        MissionStatus.COMPLETED,
                        MissionStatus.CANCELLED,
                        MissionStatus.DISPATCH_FAILED);

        Optional<Mission> findBySosRequestId(UUID sosRequestId);
        List<Mission> findBySosRequestIdIn(List<UUID> sosRequestIds);

        Optional<Mission> findByAidRequestId(UUID aidRequestId);
        List<Mission> findByAidRequestIdIn(List<UUID> aidRequestIds);

        @Query("SELECT m FROM Mission m WHERE m.sosRequestId IN (SELECT s.id FROM SosRequest s WHERE s.requesterId = :requesterId)")
        List<Mission> findMissionsBySosRequesterId(@Param("requesterId") UUID requesterId);

        @Query("SELECT m FROM Mission m WHERE m.aidRequestId IN (SELECT a.id FROM AidRequest a WHERE a.requesterId = :requesterId)")
        List<Mission> findMissionsByAidRequesterId(@Param("requesterId") UUID requesterId);

        Optional<Mission> findByCodeNameIgnoreCase(String codeName);

        boolean existsByCodeName(String codeName);

        Page<Mission> findByMissionType(MissionType missionType, Pageable pageable);

        Page<Mission> findByStatus(MissionStatus status, Pageable pageable);

        Page<Mission> findByVolunteerId(UUID volunteerId, Pageable pageable);

        Page<Mission> findByMissionTypeAndStatus(MissionType missionType, MissionStatus status, Pageable pageable);

        Page<Mission> findByMissionTypeAndVolunteerId(MissionType missionType, UUID volunteerId, Pageable pageable);

        Page<Mission> findByStatusAndVolunteerId(MissionStatus status, UUID volunteerId, Pageable pageable);

        Page<Mission> findByMissionTypeAndStatusAndVolunteerId(
                        MissionType missionType, MissionStatus status, UUID volunteerId, Pageable pageable);

        List<Mission> findByVolunteerIdAndStatusIn(UUID volunteerId, List<MissionStatus> statuses);

        long countByStatus(MissionStatus status);

        long countByVolunteerIdAndStatus(UUID volunteerId, MissionStatus status);

        /**
         * Find missions that are not in a terminal state.
         */
        @Query("SELECT m FROM Mission m WHERE m.status NOT IN (:terminalStatuses) ORDER BY m.createdAt DESC")
        Page<Mission> findAllActiveWithStatuses(@Param("terminalStatuses") List<MissionStatus> terminalStatuses,
                        Pageable pageable);

        /**
         * Wrapper method với default params
         */
        default Page<Mission> findAllActive(Pageable pageable) {
                return findAllActiveWithStatuses(TERMINAL_STATUSES, pageable);
        }

        /**
         * Find missions that are not in a terminal state by hub.
         */
        @Query("SELECT m FROM Mission m WHERE m.hubId = :hubId AND m.status NOT IN (:terminalStatuses) ORDER BY m.createdAt DESC")
        Page<Mission> findActiveByHubIdWithStatuses(@Param("hubId") UUID hubId,
                        @Param("terminalStatuses") List<MissionStatus> terminalStatuses,
                        Pageable pageable);

        /**
         * Wrapper method với default params
         */
        default Page<Mission> findActiveByHubId(UUID hubId, Pageable pageable) {
                return findActiveByHubIdWithStatuses(hubId, TERMINAL_STATUSES, pageable);
        }

        /**
         * Find missions that are not in a terminal state by type.
         */
        @Query("SELECT m FROM Mission m WHERE m.missionType = :type AND m.status NOT IN (:terminalStatuses) ORDER BY m.createdAt DESC")
        Page<Mission> findActiveByTypeWithStatuses(@Param("type") MissionType type,
                        @Param("terminalStatuses") List<MissionStatus> terminalStatuses,
                        Pageable pageable);

        /**
         * Wrapper method với default params
         */
        default Page<Mission> findActiveByType(MissionType type, Pageable pageable) {
                return findActiveByTypeWithStatuses(type, TERMINAL_STATUSES, pageable);
        }

        /**
         * Find missions that are not in a terminal state by hub and type.
         */
        @Query("SELECT m FROM Mission m WHERE m.hubId = :hubId AND m.missionType = :type AND m.status NOT IN (:terminalStatuses) ORDER BY m.createdAt DESC")
        Page<Mission> findActiveByHubIdAndTypeWithStatuses(@Param("hubId") UUID hubId, @Param("type") MissionType type,
                        @Param("terminalStatuses") List<MissionStatus> terminalStatuses,
                        Pageable pageable);

        /**
         * Wrapper method với default params
         */
        default Page<Mission> findActiveByHubIdAndType(UUID hubId, MissionType type, Pageable pageable) {
                return findActiveByHubIdAndTypeWithStatuses(hubId, type, TERMINAL_STATUSES, pageable);
        }

        Page<Mission> findByHubIdAndMissionTypeAndStatusInOrderByCreatedAtAsc(UUID hubId,
                                                                               MissionType missionType,
                                                                               List<MissionStatus> statuses,
                                                                               Pageable pageable);

        /**
         * Đếm missions theo status (cho stats)
         */
        long countByMissionTypeAndStatus(MissionType missionType, MissionStatus status);

        /**
         * Đếm missions đang active theo type
         */
        @Query("SELECT COUNT(m) FROM Mission m WHERE m.missionType = :type AND m.status NOT IN (:terminalStatuses)")
        long countActiveByTypeWithStatuses(@Param("type") MissionType type,
                        @Param("terminalStatuses") List<MissionStatus> terminalStatuses);

        /**
         * Wrapper method với default params
         */
        default long countActiveByType(MissionType type) {
                return countActiveByTypeWithStatuses(type, TERMINAL_STATUSES);
        }

        /**
         * Đếm tổng missions đang active
         */
        @Query("SELECT COUNT(m) FROM Mission m WHERE m.status NOT IN (:terminalStatuses)")
        long countAllActiveWithStatuses(@Param("terminalStatuses") List<MissionStatus> terminalStatuses);

        /**
         * Wrapper method với default params
         */
        default long countAllActive() {
                return countAllActiveWithStatuses(TERMINAL_STATUSES);
        }

        /**
         * Tìm missions của volunteer đã hoàn thành hoặc bị hủy (history)
         */
        @Query("SELECT m FROM Mission m WHERE m.volunteerId = :volunteerId AND m.status IN (:completed, :cancelled) ORDER BY m.updatedAt DESC")
        Page<Mission> findHistoryByVolunteerIdWithStatuses(@Param("volunteerId") UUID volunteerId,
                        @Param("completed") MissionStatus completed, @Param("cancelled") MissionStatus cancelled,
                        Pageable pageable);

        /**
         * Wrapper method với default params
         */
        default Page<Mission> findHistoryByVolunteerId(UUID volunteerId, Pageable pageable) {
                return findHistoryByVolunteerIdWithStatuses(volunteerId, MissionStatus.COMPLETED,
                                MissionStatus.CANCELLED, pageable);
        }

        /**
         * Join-based volunteer mission history for API response without facade calls.
         */
        @Query(value = "SELECT " +
                        "CAST(m.mission_type AS text) AS \"missionType\", " +
                        "m.completed_at AS \"completedAt\", " +
                        "CASE " +
                        "WHEN m.mission_type = 'RESCUE' THEN s.address " +
                        "WHEN m.mission_type = 'DELIVERY' THEN a.address " +
                        "ELSE NULL END AS \"address\" " +
                        "FROM missions m " +
                        "LEFT JOIN sos_requests s ON s.id = m.sos_request_id " +
                        "LEFT JOIN aid_requests a ON a.id = m.aid_request_id " +
                        "WHERE m.volunteer_id = :volunteerId " +
                        "AND m.status = 'COMPLETED' " +
                        "ORDER BY m.completed_at DESC", countQuery = "SELECT COUNT(*) FROM missions m WHERE m.volunteer_id = :volunteerId AND m.status = 'COMPLETED'", nativeQuery = true)
        Page<MissionHistoryProjection> findHistoryProjectionByVolunteerId(@Param("volunteerId") UUID volunteerId,
                        Pageable pageable);

        @Query(value = "SELECT " +
                        "m.*, " +
                        "ST_Y(CAST(m.victim_location AS geometry)) AS \"victimLat\", " +
                        "ST_X(CAST(m.victim_location AS geometry)) AS \"victimLng\", " +
                        "CAST(m.mission_type AS text) AS \"missionType\", " +
                        "m.completed_at AS \"completedAt\", " +
                        "da.radius_km AS \"radiusKm\", " +
                        "CASE " +
                        "WHEN m.mission_type = 'RESCUE' THEN s.address " +
                        "WHEN m.mission_type = 'DELIVERY' THEN a.address " +
                        "ELSE NULL END AS \"address\", " +
                        "CASE " +
                        "WHEN m.mission_type = 'RESCUE' THEN s.description " +
                        "WHEN m.mission_type = 'DELIVERY' THEN a.description " +
                        "ELSE NULL END AS \"description\" " +
                        "FROM missions m " +
                        "LEFT JOIN sos_requests s ON s.id = m.sos_request_id " +
                        "LEFT JOIN aid_requests a ON a.id = m.aid_request_id " +
                        "LEFT JOIN dispatch_attempts da ON da.mission_id = m.id " +
                        "AND da.volunteer_id = m.volunteer_id " +
                        "AND da.response = 'ACCEPTED' " +
                        "WHERE m.volunteer_id = :volunteerId " +
                        "ORDER BY m.created_at DESC", countQuery = "SELECT COUNT(*) FROM missions m WHERE m.volunteer_id = :volunteerId", nativeQuery = true)
        Page<MissionHistoryFullProjection> findFullHistoryByVolunteerId(@Param("volunteerId") UUID volunteerId,
                        Pageable pageable);

        @Query(value = "SELECT " +
                        "m.*, " +
                        "ST_Y(CAST(m.victim_location AS geometry)) AS \"victimLat\", " +
                        "ST_X(CAST(m.victim_location AS geometry)) AS \"victimLng\", " +
                        "CAST(m.mission_type AS text) AS \"missionType\", " +
                        "m.completed_at AS \"completedAt\", " +
                        "da.radius_km AS \"radiusKm\", " +
                        "CASE " +
                        "WHEN m.mission_type = 'RESCUE' THEN s.address " +
                        "WHEN m.mission_type = 'DELIVERY' THEN a.address " +
                        "ELSE NULL END AS \"address\", " +
                        "CASE " +
                        "WHEN m.mission_type = 'RESCUE' THEN s.description " +
                        "WHEN m.mission_type = 'DELIVERY' THEN a.description " +
                        "ELSE NULL END AS \"description\" " +
                        "FROM missions m " +
                        "LEFT JOIN sos_requests s ON s.id = m.sos_request_id " +
                        "LEFT JOIN aid_requests a ON a.id = m.aid_request_id " +
                        "LEFT JOIN dispatch_attempts da ON da.mission_id = m.id " +
                        "AND da.volunteer_id = m.volunteer_id " +
                        "AND da.response = 'ACCEPTED' " +
                        "WHERE m.volunteer_id = :volunteerId " +
                        "AND m.status IN ('ASSIGNED', 'PICKING_UP', 'PICKED_UP', 'IN_TRANSIT') " +
                        "ORDER BY m.created_at DESC LIMIT 1", nativeQuery = true)
        Optional<MissionHistoryFullProjection> findCurrentFullMissionByVolunteerId(
                        @Param("volunteerId") UUID volunteerId);

        /**
         * Statistics: Đếm missions trong khoảng thời gian
         */
        @Query("SELECT COUNT(m) FROM Mission m WHERE m.createdAt >= :from AND m.createdAt <= :to")
        long countByCreatedAtBetween(@Param("from") Instant from, @Param("to") Instant to);

        /**
         * Statistics: Đếm missions theo type và status trong khoảng thời gian
         */
        @Query("SELECT COUNT(m) FROM Mission m WHERE m.missionType = :type AND m.status = :status AND m.createdAt >= :from AND m.createdAt <= :to")
        long countByTypeAndStatusAndCreatedAtBetween(
                        @Param("type") MissionType type,
                        @Param("status") MissionStatus status,
                        @Param("from") Instant from,
                        @Param("to") Instant to);

        /**
         * Statistics: Đếm missions theo type trong khoảng thời gian
         */
        @Query("SELECT COUNT(m) FROM Mission m WHERE m.missionType = :type AND m.createdAt >= :from AND m.createdAt <= :to")
        long countByTypeAndCreatedAtBetween(
                        @Param("type") MissionType type,
                        @Param("from") Instant from,
                        @Param("to") Instant to);

        /**
         * Statistics: Tính trung bình thời gian hoàn thành (phút) theo type.
         * Sử dụng native query PostgreSQL vì Hibernate 7 HQL không hỗ trợ
         * EXTRACT(EPOCH FROM Duration) - Duration là kết quả của phép trừ 2 Instant.
         */
        @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (m.completed_at - m.accepted_at)) / 60) " +
                        "FROM missions m " +
                        "WHERE m.mission_type = CAST(:type AS mission_type) " +
                        "AND m.status = 'COMPLETED' " +
                        "AND m.completed_at IS NOT NULL " +
                        "AND m.accepted_at IS NOT NULL " +
                        "AND m.created_at >= :from " +
                        "AND m.created_at <= :to", nativeQuery = true)
        Double avgCompletionMinutesByTypeAndCreatedAtBetween(
                        @Param("type") String type,
                        @Param("from") Instant from,
                        @Param("to") Instant to);

        /**
         * Tìm mission đang active của volunteer (1 mission active tại 1 thời điểm)
         */
        @Query("SELECT m FROM Mission m WHERE m.volunteerId = :volunteerId AND m.status IN (:statuses)")
        Optional<Mission> findActiveByVolunteerIdWithStatuses(@Param("volunteerId") UUID volunteerId,
                        @Param("statuses") List<MissionStatus> statuses);

        /**
         * Wrapper method với default params
         */
        default Optional<Mission> findActiveByVolunteerId(UUID volunteerId) {
                return findActiveByVolunteerIdWithStatuses(volunteerId,
                                List.of(MissionStatus.ASSIGNED, MissionStatus.PICKING_UP, MissionStatus.PICKED_UP,
                                                MissionStatus.IN_TRANSIT));
        }

        /**
         * Tìm missions đang chờ dispatch (PENDING hoặc DISPATCHING)
         */
        @Query("SELECT m FROM Mission m WHERE m.status IN (:statuses) ORDER BY m.priorityScore DESC, m.createdAt ASC")
        List<Mission> findPendingDispatchWithStatuses(@Param("statuses") List<MissionStatus> statuses);

        /**
         * Wrapper method với default params
         */
        default List<Mission> findPendingDispatch() {
                return findPendingDispatchWithStatuses(List.of(MissionStatus.PENDING, MissionStatus.DISPATCHING));
        }

        /**
         * Tìm các missions cần retry dispatch dựa trên status, số lần retry và thời gian dispatch cuối
         */
        @Query("SELECT m FROM Mission m WHERE m.status IN (:statuses) " +
               "AND m.retryCount < :maxRetry " +
               "AND (m.lastDispatchAt IS NULL OR m.lastDispatchAt < :threshold) " +
               "ORDER BY m.priorityScore DESC, m.createdAt ASC")
        List<Mission> findMissionsForRetry(@Param("statuses") List<MissionStatus> statuses,
                                          @Param("threshold") Instant threshold,
                                          @Param("maxRetry") int maxRetry);

        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Transactional
        @Query("UPDATE Mission m SET m.status = :status, m.retryCount = :retryCount, m.lastDispatchAt = :lastDispatchAt " +
                        "WHERE m.id IN (:missionIds)")
        int markDispatchesFailed(@Param("missionIds") List<UUID> missionIds,
                        @Param("status") MissionStatus status,
                        @Param("retryCount") int retryCount,
                        @Param("lastDispatchAt") Instant lastDispatchAt);

        /**
         * JPQL join to get SosRequest entities based on Mission status and date range
         */
        @Query(value = "SELECT sr.* FROM sos_requests sr " +
                        "INNER JOIN missions m ON m.sos_request_id = sr.id " +
                        "WHERE m.status = CAST(:status AS VARCHAR)::mission_status " +
                        "AND (CAST(:start AS TIMESTAMPTZ) IS NULL OR m.created_at >= CAST(:start AS TIMESTAMPTZ)) " +
                        "AND (CAST(:end AS TIMESTAMPTZ) IS NULL OR m.created_at <= CAST(:end AS TIMESTAMPTZ))", nativeQuery = true)
        List<SosRequest> findSosByMissionStatus(
                        @Param("status") String status,
                        @Param("start") Instant start,
                        @Param("end") Instant end);

        /**
         * Native SQL query to get AidRequest entities based on Mission status and date
         * range
         */
        @Query(value = "SELECT ar.* FROM aid_requests ar " +
                        "INNER JOIN missions m ON m.aid_request_id = ar.id " +
                        "WHERE m.status = CAST(:status AS VARCHAR)::mission_status " +
                        "AND (CAST(:start AS TIMESTAMPTZ) IS NULL OR m.created_at >= CAST(:start AS TIMESTAMPTZ)) " +
                        "AND (CAST(:end AS TIMESTAMPTZ) IS NULL OR m.created_at <= CAST(:end AS TIMESTAMPTZ))", nativeQuery = true)
        List<AidRequest> findAidByMissionStatus(
                        @Param("status") String status,
                        @Param("start") Instant start,
                        @Param("end") Instant end);
}
