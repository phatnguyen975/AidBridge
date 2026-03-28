package com.drc.aidbridge.repository;

import com.drc.aidbridge.entity.Mission;
import com.drc.aidbridge.entity.enums.MissionStatus;
import com.drc.aidbridge.entity.enums.MissionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MissionRepository extends JpaRepository<Mission, UUID> {

    Optional<Mission> findBySosRequestId(UUID sosRequestId);

    // Filter by single field
    Page<Mission> findByMissionType(MissionType missionType, Pageable pageable);

    Page<Mission> findByStatus(MissionStatus status, Pageable pageable);

    Page<Mission> findByVolunteerId(UUID volunteerId, Pageable pageable);

    // Filter by two fields
    Page<Mission> findByMissionTypeAndStatus(MissionType missionType, MissionStatus status, Pageable pageable);

    Page<Mission> findByMissionTypeAndVolunteerId(MissionType missionType, UUID volunteerId, Pageable pageable);

    Page<Mission> findByStatusAndVolunteerId(MissionStatus status, UUID volunteerId, Pageable pageable);

    // Filter by all three fields
    Page<Mission> findByMissionTypeAndStatusAndVolunteerId(
            MissionType missionType, MissionStatus status, UUID volunteerId, Pageable pageable);

    // Find active missions for a volunteer (for current mission endpoint)
    List<Mission> findByVolunteerIdAndStatusIn(UUID volunteerId, List<MissionStatus> statuses);

    // Count missions by status
    long countByStatus(MissionStatus status);

    // Count missions by volunteer
    long countByVolunteerIdAndStatus(UUID volunteerId, MissionStatus status);
}
