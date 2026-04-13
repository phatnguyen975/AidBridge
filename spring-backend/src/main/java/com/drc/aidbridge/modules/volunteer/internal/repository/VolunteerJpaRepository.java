package com.drc.aidbridge.modules.volunteer.internal.repository;

import com.drc.aidbridge.modules.volunteer.internal.entity.Volunteer;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.drc.aidbridge.modules.volunteer.internal.repository.VolunteerStatisticsProjection;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VolunteerJpaRepository extends JpaRepository<Volunteer, UUID> {
        Optional<Volunteer> findByUserId(UUID userId);

        boolean existsByUserId(UUID userId);

        /**
         * Tìm các volunteers có current_location trong bán kính (meters) từ một vị trí
         * cho trước.
         * Sử dụng PostGIS ST_DWithin để tìm kiếm hiệu quả với geography type.
         *
         * @param location     điểm trung tâm tìm kiếm
         * @param radiusMeters bán kính tìm kiếm (đơn vị: meters)
         * @return danh sách volunteers trong bán kính, sắp xếp theo khoảng cách tăng
         *         dần
         */
        @Query(value = "SELECT v.* FROM volunteer_profiles v " +
                        "WHERE v.current_location IS NOT NULL " +
                        "AND ST_DWithin(v.current_location, CAST(:location AS geography), :radiusMeters) " +
                        "ORDER BY ST_Distance(v.current_location, CAST(:location AS geography))", nativeQuery = true)
        List<Volunteer> findNearbyVolunteers(
                        @Param("location") Point location,
                        @Param("radiusMeters") double radiusMeters);

        /**
         * Tìm tất cả volunteers có current_location, sắp xếp theo khoảng cách từ một vị
         * trí cho trước.
         * Sử dụng PostGIS ST_Distance để tính khoảng cách trên mặt cầu (geography).
         *
         * @param location điểm tham chiếu để tính khoảng cách
         * @return danh sách tất cả volunteers có vị trí, sắp xếp theo khoảng cách tăng
         *         dần
         */
        @Query(value = "SELECT v.* FROM volunteer_profiles v " +
                        "WHERE v.current_location IS NOT NULL " +
                        "ORDER BY ST_Distance(v.current_location, CAST(:location AS geography))", nativeQuery = true)
        List<Volunteer> findVolunteersOrderByDistance(@Param("location") Point location);

        /**
         * Tính khoảng cách (meters) từ volunteer đến một vị trí cho trước.
         * Sử dụng PostGIS ST_Distance với geography type.
         * 
         * @param volunteerId ID của volunteer
         * @param location    vị trí đích
         * @return khoảng cách tính bằng meters, null nếu volunteer không có location
         */
        @Query(value = "SELECT ST_Distance(v.current_location, CAST(:location AS geography)) " +
                        "FROM volunteer_profiles v " +
                        "WHERE v.id = :volunteerId " +
                        "AND v.current_location IS NOT NULL", nativeQuery = true)
        Double calculateDistanceToLocation(
                        @Param("volunteerId") UUID volunteerId,
                        @Param("location") Point location);

    // Batch update: Mark volunteers offline if no heartbeat received within timeout
    @Modifying
    @Transactional
    @Query("UPDATE Volunteer v SET v.isOnline = false WHERE v.isOnline = true AND v.lastActiveAt < :cutoffTime")
    int updateOfflineVolunteers(@Param("cutoffTime") Instant cutoffTime);

    /**
     * Native query returning a projection interface mapped to typed getters.
     */
    @Query(value = "SELECT "
            + "(SELECT COUNT(*) FROM missions WHERE volunteer_id = :volunteerId AND status = 'COMPLETED') AS \"totalTasksCompleted\", "
            + "(SELECT COUNT(*) FROM missions WHERE volunteer_id = :volunteerId AND mission_type = 'RESCUE') AS \"rescueMissions\", "
            + "(SELECT COUNT(*) FROM missions WHERE volunteer_id = :volunteerId AND mission_type = 'DELIVERY') AS \"deliveryMissions\", "
            + "(SELECT AVG(r.score) FROM ratings r WHERE r.ratee_id = (SELECT user_id FROM volunteer_profiles WHERE id = :volunteerId)) AS \"avgRating\", "
            + "(SELECT COUNT(*) FROM ratings r WHERE r.ratee_id = (SELECT user_id FROM volunteer_profiles WHERE id = :volunteerId)) AS \"totalRatings\", "
            + "(SELECT AVG(EXTRACT(EPOCH FROM (da.responded_at - da.created_at))) FROM dispatch_attempts da WHERE da.volunteer_id = :volunteerId AND da.responded_at IS NOT NULL) AS \"avgResponseSeconds\", "
            + "(SELECT COALESCE(SUM(s.people_count),0) FROM missions m JOIN sos_requests s ON m.sos_request_id = s.id WHERE m.volunteer_id = :volunteerId AND m.status = 'COMPLETED') AS \"peopleHelped\"",
            nativeQuery = true)
    VolunteerStatisticsProjection findStatisticsByVolunteerId(@Param("volunteerId") UUID volunteerId);

   
}
