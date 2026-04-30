package com.drc.aidbridge.modules.mission.internal.repository;

import com.drc.aidbridge.modules.mission.internal.entity.DispatchAttempt;
import com.drc.aidbridge.modules.shared.enums.DispatchResponse;
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
public interface DispatchAttemptJpaRepository extends JpaRepository<DispatchAttempt, UUID> {

        /**
         * Tìm tất cả dispatch attempts của một mission (phân trang)
         */
        Page<DispatchAttempt> findByMissionIdOrderByCreatedAtDesc(UUID missionId, Pageable pageable);

        /**
         * Tìm tất cả dispatch attempts của một mission (không phân trang)
         */
        List<DispatchAttempt> findByMissionIdOrderByCreatedAtDesc(UUID missionId);

        /**
         * Tìm dispatch attempt đang pending cho volunteer và mission cụ thể
         */
        Optional<DispatchAttempt> findByMissionIdAndVolunteerIdAndResponse(
                        UUID missionId, UUID volunteerId, DispatchResponse response);

        /**
         * Tìm tất cả dispatch attempts đang pending của một volunteer
         */
        List<DispatchAttempt> findByVolunteerIdAndResponseOrderByCreatedAtDesc(
                        UUID volunteerId, DispatchResponse response);

        Optional<DispatchAttempt> findTopByVolunteerIdAndResponseOrderByCreatedAtDesc(UUID volunteerId, DispatchResponse response);

        /**
         * Tìm dispatch attempt mới nhất đang pending cho mission
         */
        Optional<DispatchAttempt> findTopByMissionIdAndResponseOrderByCreatedAtDesc(
                        UUID missionId, DispatchResponse response);

        /**
         * Đếm số lần dispatch cho một mission
         */
        long countByMissionId(UUID missionId);

        /**
         * Đếm số lần dispatch theo response status cho một mission
         */
        long countByMissionIdAndResponse(UUID missionId, DispatchResponse response);

        /**
         * Kiểm tra xem volunteer đã được dispatch cho mission này chưa
         */
        boolean existsByMissionIdAndVolunteerId(UUID missionId, UUID volunteerId);

        /**
         * Tìm các dispatch attempts hết hạn (PENDING và đã quá thời gian)
         */
        @Query("SELECT da FROM DispatchAttempt da WHERE da.response = :response AND da.createdAt < :expiryTime")
        List<DispatchAttempt> findExpiredAttempts(
                        @Param("response") DispatchResponse response,
                        @Param("expiryTime") Instant expiryTime);

        /**
         * Lấy batch number tiếp theo cho mission
         */
        @Query("SELECT COALESCE(MAX(da.batchNumber), 0) + 1 FROM DispatchAttempt da WHERE da.missionId = :missionId")
        Integer getNextBatchNumber(@Param("missionId") UUID missionId);
}
