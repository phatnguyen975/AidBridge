package com.drc.aidbridge.modules.mission.internal.repository;

import com.drc.aidbridge.modules.shared.enums.MissionStatus;
import com.drc.aidbridge.modules.shared.enums.MissionType;
import com.drc.aidbridge.modules.mission.internal.entity.Mission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MissionJpaRepository extends JpaRepository<Mission, UUID> {

        Optional<Mission> findBySosRequestId(UUID sosRequestId);

        Optional<Mission> findByAidRequestId(UUID aidRequestId);

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
         * Tìm tất cả missions đang active (không phải COMPLETED hoặc CANCELLED)
         */
        @Query("SELECT m FROM Mission m WHERE m.status NOT IN (:completed, :cancelled) ORDER BY m.createdAt DESC")
        Page<Mission> findAllActiveWithStatuses(@Param("completed") MissionStatus completed,
                        @Param("cancelled") MissionStatus cancelled, Pageable pageable);

        /**
         * Wrapper method với default params
         */
        default Page<Mission> findAllActive(Pageable pageable) {
                return findAllActiveWithStatuses(MissionStatus.COMPLETED, MissionStatus.CANCELLED, pageable);
        }

        /**
         * Tìm tất cả missions đang active theo hub
         */
        @Query("SELECT m FROM Mission m WHERE m.hubId = :hubId AND m.status NOT IN (:completed, :cancelled) ORDER BY m.createdAt DESC")
        Page<Mission> findActiveByHubIdWithStatuses(@Param("hubId") UUID hubId,
                        @Param("completed") MissionStatus completed, @Param("cancelled") MissionStatus cancelled,
                        Pageable pageable);

        /**
         * Wrapper method với default params
         */
        default Page<Mission> findActiveByHubId(UUID hubId, Pageable pageable) {
                return findActiveByHubIdWithStatuses(hubId, MissionStatus.COMPLETED, MissionStatus.CANCELLED, pageable);
        }

        /**
         * Tìm tất cả missions đang active theo type
         */
        @Query("SELECT m FROM Mission m WHERE m.missionType = :type AND m.status NOT IN (:completed, :cancelled) ORDER BY m.createdAt DESC")
        Page<Mission> findActiveByTypeWithStatuses(@Param("type") MissionType type,
                        @Param("completed") MissionStatus completed, @Param("cancelled") MissionStatus cancelled,
                        Pageable pageable);

        /**
         * Wrapper method với default params
         */
        default Page<Mission> findActiveByType(MissionType type, Pageable pageable) {
                return findActiveByTypeWithStatuses(type, MissionStatus.COMPLETED, MissionStatus.CANCELLED, pageable);
        }

        /**
         * Tìm tất cả missions đang active theo hub và type
         */
        @Query("SELECT m FROM Mission m WHERE m.hubId = :hubId AND m.missionType = :type AND m.status NOT IN (:completed, :cancelled) ORDER BY m.createdAt DESC")
        Page<Mission> findActiveByHubIdAndTypeWithStatuses(@Param("hubId") UUID hubId, @Param("type") MissionType type,
                        @Param("completed") MissionStatus completed, @Param("cancelled") MissionStatus cancelled,
                        Pageable pageable);

        /**
         * Wrapper method với default params
         */
        default Page<Mission> findActiveByHubIdAndType(UUID hubId, MissionType type, Pageable pageable) {
                return findActiveByHubIdAndTypeWithStatuses(hubId, type, MissionStatus.COMPLETED,
                                MissionStatus.CANCELLED, pageable);
        }

        /**
         * Đếm missions theo status (cho stats)
         */
        long countByMissionTypeAndStatus(MissionType missionType, MissionStatus status);

        /**
         * Đếm missions đang active theo type
         */
        @Query("SELECT COUNT(m) FROM Mission m WHERE m.missionType = :type AND m.status NOT IN (:completed, :cancelled)")
        long countActiveByTypeWithStatuses(@Param("type") MissionType type, @Param("completed") MissionStatus completed,
                        @Param("cancelled") MissionStatus cancelled);

        /**
         * Wrapper method với default params
         */
        default long countActiveByType(MissionType type) {
                return countActiveByTypeWithStatuses(type, MissionStatus.COMPLETED, MissionStatus.CANCELLED);
        }

        /**
         * Đếm tổng missions đang active
         */
        @Query("SELECT COUNT(m) FROM Mission m WHERE m.status NOT IN (:completed, :cancelled)")
        long countAllActiveWithStatuses(@Param("completed") MissionStatus completed,
                        @Param("cancelled") MissionStatus cancelled);

        /**
         * Wrapper method với default params
         */
        default long countAllActive() {
                return countAllActiveWithStatuses(MissionStatus.COMPLETED, MissionStatus.CANCELLED);
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
}
