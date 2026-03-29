package com.drc.aidbridge.modules.mission.internal.repository;

import com.drc.aidbridge.entity.enums.MissionStatus;
import com.drc.aidbridge.entity.enums.MissionType;
import com.drc.aidbridge.modules.mission.internal.entity.Mission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
