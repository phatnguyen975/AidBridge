package com.drc.aidbridge.modules.volunteer.internal.repository;

import com.drc.aidbridge.modules.volunteer.internal.entity.Volunteer;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VolunteerJpaRepository extends JpaRepository<Volunteer, UUID> {
    Optional<Volunteer> findByUserId(UUID userId);
    boolean existsByUserId(UUID userId);

    /**
     * Tìm các volunteers đang online trong bán kính (meters) từ một vị trí cho trước.
     * Sử dụng PostGIS ST_DWithin để tìm kiếm hiệu quả với geography type.
     * 
     * @param location điểm trung tâm tìm kiếm
     * @param radiusMeters bán kính tìm kiếm (đơn vị: meters)
     * @return danh sách volunteers trong bán kính, sắp xếp theo khoảng cách tăng dần
     */
    @Query(value = "SELECT v.* FROM volunteer_profiles v " +
            "WHERE v.is_online = true " +
            "AND v.current_location IS NOT NULL " +
            "AND ST_DWithin(v.current_location, CAST(:location AS geography), :radiusMeters) " +
            "ORDER BY ST_Distance(v.current_location, CAST(:location AS geography))", 
            nativeQuery = true)
    List<Volunteer> findNearbyOnlineVolunteers(
            @Param("location") Point location, 
            @Param("radiusMeters") double radiusMeters);

    /**
     * Tìm tất cả volunteers đang online, sắp xếp theo khoảng cách từ một vị trí cho trước.
     * Sử dụng PostGIS ST_Distance để tính khoảng cách trên mặt cầu (geography).
     * 
     * @param location điểm tham chiếu để tính khoảng cách
     * @return danh sách tất cả volunteers online, sắp xếp theo khoảng cách tăng dần
     */
    @Query(value = "SELECT v.* FROM volunteer_profiles v " +
            "WHERE v.is_online = true " +
            "AND v.current_location IS NOT NULL " +
            "ORDER BY ST_Distance(v.current_location, CAST(:location AS geography))", 
            nativeQuery = true)
    List<Volunteer> findOnlineVolunteersOrderByDistance(@Param("location") Point location);

    /**
     * Tính khoảng cách (meters) từ volunteer đến một vị trí cho trước.
     * Sử dụng PostGIS ST_Distance với geography type.
     * 
     * @param volunteerId ID của volunteer
     * @param location vị trí đích
     * @return khoảng cách tính bằng meters, null nếu volunteer không có location
     */
    @Query(value = "SELECT ST_Distance(v.current_location, CAST(:location AS geography)) " +
            "FROM volunteer_profiles v " +
            "WHERE v.id = :volunteerId " +
            "AND v.current_location IS NOT NULL", 
            nativeQuery = true)
    Double calculateDistanceToLocation(
            @Param("volunteerId") UUID volunteerId, 
            @Param("location") Point location);
}
